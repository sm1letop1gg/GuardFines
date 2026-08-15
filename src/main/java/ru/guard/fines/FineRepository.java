package ru.guard.fines;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class FineRepository {
    private final JavaPlugin plugin;
    private final File file;
    private final Map<Long, Fine> fines = new LinkedHashMap<>();
    private long nextId = 1;

    public FineRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "fines.yml");
    }

    public void load() {
        fines.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        nextId = Math.max(1, yaml.getLong("next-id", 1));
        ConfigurationSection root = yaml.getConfigurationSection("fines");
        if (root == null) return;
        for (String key : root.getKeys(false)) {
            try {
                ConfigurationSection s = root.getConfigurationSection(key);
                if (s == null) continue;
                long id = Long.parseLong(key);
                Fine fine = new Fine(id,
                        UUID.fromString(Objects.requireNonNull(s.getString("issuer.uuid"))), s.getString("issuer.name", "Unknown"),
                        UUID.fromString(Objects.requireNonNull(s.getString("target.uuid"))), s.getString("target.name", "Unknown"),
                        s.getDouble("amount"), s.getString("reason", "Не указана"), s.getLong("created-at"),
                        s.getLong("deadline"), FineStatus.valueOf(s.getString("status", "UNPAID")), s.getLong("closed-at"));
                fine.updateOverdue(System.currentTimeMillis());
                fines.put(id, fine);
                nextId = Math.max(nextId, id + 1);
            } catch (RuntimeException ex) {
                plugin.getLogger().warning("Не удалось прочитать штраф " + key + ": " + ex.getMessage());
            }
        }
        save();
    }

    public Fine create(UUID issuerId, String issuerName, UUID targetId, String targetName,
                       double amount, String reason, long deadline) {
        long now = System.currentTimeMillis();
        Fine fine = new Fine(nextId++, issuerId, issuerName, targetId, targetName, amount,
                reason, now, deadline, FineStatus.UNPAID, 0);
        fines.put(fine.id(), fine);
        save();
        return fine;
    }

    public Optional<Fine> find(long id) { return Optional.ofNullable(fines.get(id)); }

    public List<Fine> all() {
        updateStatuses();
        return List.copyOf(fines.values());
    }

    public List<Fine> openForTarget(UUID target) {
        return all().stream().filter(f -> f.targetId().equals(target) && f.status().isOpen()).toList();
    }

    public List<Fine> openIssuedBy(UUID issuer, boolean admin) {
        return all().stream().filter(f -> f.status().isOpen() && (admin || f.issuerId().equals(issuer))).toList();
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("next-id", nextId);
        for (Fine f : fines.values()) {
            String p = "fines." + f.id() + ".";
            yaml.set(p + "issuer.uuid", f.issuerId().toString());
            yaml.set(p + "issuer.name", f.issuerName());
            yaml.set(p + "target.uuid", f.targetId().toString());
            yaml.set(p + "target.name", f.targetName());
            yaml.set(p + "amount", f.amount());
            yaml.set(p + "reason", f.reason());
            yaml.set(p + "created-at", f.createdAt());
            yaml.set(p + "deadline", f.deadline());
            yaml.set(p + "status", f.status().name());
            yaml.set(p + "status-name", f.status().displayName());
            yaml.set(p + "closed-at", f.closedAt());
        }
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                throw new IOException("Не удалось создать папку плагина");
            }
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Не удалось сохранить fines.yml: " + ex.getMessage());
        }
    }

    private void updateStatuses() {
        boolean changed = false;
        long now = System.currentTimeMillis();
        for (Fine fine : fines.values()) {
            FineStatus before = fine.status();
            fine.updateOverdue(now);
            changed |= before != fine.status();
        }
        if (changed) save();
    }
}
