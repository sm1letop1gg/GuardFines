package ru.guard.fines;

import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GuardFinesPlugin extends JavaPlugin implements Listener {
    private FineRepository repository;
    private Text text;
    private FineMenu menu;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        text = new Text(this);
        repository = new FineRepository(this);
        repository.load();
        menu = new FineMenu(this);
        FineCommand handler = new FineCommand(this);
        PluginCommand command = getCommand("fine");
        if (command == null) throw new IllegalStateException("Команда fine отсутствует в plugin.yml");
        command.setExecutor(handler);
        command.setTabCompleter(handler);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(menu, this);
        getServer().getScheduler().runTaskTimer(this, repository::all, 20L * 60, 20L * 60);
        getLogger().info("GuardFines включён. Загружено штрафов: " + repository.all().size());
    }

    @Override
    public void onDisable() { if (repository != null) repository.save(); }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        long delay = Math.max(0, getConfig().getLong("notify-delay-ticks", 30));
        getServer().getScheduler().runTaskLater(this, () -> notifyPlayer(event.getPlayer()), delay);
    }

    private void notifyPlayer(Player player) {
        List<Fine> fines = repository.openForTarget(player.getUniqueId());
        if (fines.isEmpty()) return;
        long overdue = fines.stream().filter(f -> f.status() == FineStatus.OVERDUE).count();
        player.sendMessage(text.message("login-header", Map.of("unpaid", Long.toString(fines.size() - overdue), "overdue", Long.toString(overdue))));
        for (Fine fine : fines.stream().limit(8).toList())
            player.sendMessage(text.message("login-line", values(fine)));
    }

    public void changeStatus(Player actor, long id, FineStatus newStatus) {
        Fine fine = repository.find(id).orElse(null);
        if (fine == null) { actor.sendMessage(text.message("fine-not-found", Map.of())); return; }
        if (!fine.issuerId().equals(actor.getUniqueId()) && !actor.hasPermission("guardfines.admin")) {
            actor.sendMessage(text.message("not-owner", Map.of())); return;
        }
        fine.updateOverdue(System.currentTimeMillis());
        if (!fine.status().isOpen()) { actor.sendMessage(text.message("already-closed", Map.of())); return; }
        fine.close(newStatus, System.currentTimeMillis());
        repository.save();
        actor.sendMessage(text.message(newStatus == FineStatus.PAID ? "fine-paid" : "fine-cancelled", values(fine)));
        Player target = getServer().getPlayer(fine.targetId());
        if (target != null) target.sendMessage(text.message(newStatus == FineStatus.PAID ? "fine-paid" : "fine-cancelled", values(fine)));
    }

    public Map<String, String> values(Fine fine) {
        Map<String, String> v = new HashMap<>();
        v.put("id", Long.toString(fine.id())); v.put("issuer", fine.issuerName()); v.put("player", fine.targetName());
        v.put("amount", text.amount(fine.amount())); v.put("reason", fine.reason()); v.put("deadline", text.date(fine.deadline()));
        v.put("status", fine.status().displayName());
        return v;
    }

    public FineRepository repository() { return repository; }
    public Text text() { return text; }
    public FineMenu menu() { return menu; }
}
