package ru.guard.fines;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.*;

public final class FineCommand implements CommandExecutor, TabCompleter {
    private final GuardFinesPlugin plugin;

    public FineCommand(GuardFinesPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("guardfines.use")) return send(sender, "no-permission");
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) { help(sender, label); return true; }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "give", "выдать" -> give(sender, args);
            case "list", "список" -> list(sender);
            case "paid", "оплачен" -> status(sender, args, FineStatus.PAID);
            case "cancel", "отменить" -> status(sender, args, FineStatus.CANCELLED);
            case "reload" -> reload(sender);
            default -> { help(sender, label); yield true; }
        };
    }

    private boolean give(CommandSender sender, String[] args) {
        if (!(sender instanceof Player issuer)) return send(sender, "players-only");
        if (args.length < 4) { help(sender, "fine"); return true; }
        OfflinePlayer target = findPlayer(args[1]);
        if (target == null || target.getName() == null) return send(sender, "player-not-found");
        double amount;
        try { amount = Double.parseDouble(args[2].replace(',', '.')); }
        catch (NumberFormatException ex) { return send(sender, "invalid-number"); }
        if (!Double.isFinite(amount) || amount <= 0) return send(sender, "invalid-number");
        Duration duration = parseDuration(args[3]);
        if (duration == null) return send(sender, "invalid-duration");
        String reason = args.length > 4 ? String.join(" ", Arrays.copyOfRange(args, 4, args.length)) : "Не указана";
        Fine fine = plugin.repository().create(issuer.getUniqueId(), issuer.getName(), target.getUniqueId(), target.getName(),
                amount, reason, System.currentTimeMillis() + duration.toMillis());
        sender.sendMessage(plugin.text().message("fine-created", plugin.values(fine)));
        Player online = target.getPlayer();
        if (online != null) online.sendMessage(plugin.text().message("fine-received", plugin.values(fine)));
        return true;
    }

    private boolean list(CommandSender sender) {
        if (!(sender instanceof Player player)) return send(sender, "players-only");
        plugin.menu().open(player);
        return true;
    }

    private boolean status(CommandSender sender, String[] args, FineStatus status) {
        if (!(sender instanceof Player player)) return send(sender, "players-only");
        if (args.length < 2) { help(sender, "fine"); return true; }
        try { plugin.changeStatus(player, Long.parseLong(args[1]), status); }
        catch (NumberFormatException ex) { send(sender, "fine-not-found"); }
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission("guardfines.admin")) return send(sender, "no-permission");
        plugin.reloadConfig();
        return send(sender, "reload");
    }

    private OfflinePlayer findPlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        for (OfflinePlayer player : Bukkit.getOfflinePlayers())
            if (player.getName() != null && player.getName().equalsIgnoreCase(name)) return player;
        return null;
    }

    private Duration parseDuration(String input) {
        if (!input.matches("(?i)^[1-9]\\d*[mhdw]$")) return null;
        long value;
        try { value = Long.parseLong(input.substring(0, input.length() - 1)); }
        catch (NumberFormatException ex) { return null; }
        try {
            return switch (Character.toLowerCase(input.charAt(input.length() - 1))) {
                case 'm' -> Duration.ofMinutes(value); case 'h' -> Duration.ofHours(value);
                case 'd' -> Duration.ofDays(value); case 'w' -> Duration.ofDays(Math.multiplyExact(value, 7));
                default -> null;
            };
        } catch (ArithmeticException ex) { return null; }
    }

    private boolean send(CommandSender sender, String key) { sender.sendMessage(plugin.text().message(key, Map.of())); return true; }
    private void help(CommandSender sender, String label) {
        sender.sendMessage(plugin.text().component("&8&m---------------- &cGuardFines &8&m----------------"));
        sender.sendMessage(plugin.text().component("&f/" + label + " give <игрок> <сумма> <срок> [причина]"));
        sender.sendMessage(plugin.text().component("&7Срок: 30m, 12h, 7d или 2w"));
        sender.sendMessage(plugin.text().component("&f/" + label + " list &7— открыть таблицу"));
        sender.sendMessage(plugin.text().component("&f/" + label + " paid <id> &7— подтвердить оплату"));
        sender.sendMessage(plugin.text().component("&f/" + label + " cancel <id> &7— отменить штраф"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return filter(List.of("give", "list", "paid", "cancel", "help"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        if (args.length == 2 && (args[0].equalsIgnoreCase("paid") || args[0].equalsIgnoreCase("cancel")) && sender instanceof Player p)
            return filter(plugin.repository().openIssuedBy(p.getUniqueId(), p.hasPermission("guardfines.admin")).stream().map(f -> Long.toString(f.id())).toList(), args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) return List.of("100", "500", "1000");
        if (args.length == 4 && args[0].equalsIgnoreCase("give")) return List.of("30m", "12h", "1d", "7d");
        return List.of();
    }

    private List<String> filter(List<String> values, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        return values.stream().filter(v -> v.toLowerCase(Locale.ROOT).startsWith(p)).toList();
    }
}
