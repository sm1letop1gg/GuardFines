package ru.guard.fines;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public final class Text {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private final GuardFinesPlugin plugin;

    public Text(GuardFinesPlugin plugin) { this.plugin = plugin; }

    public Component message(String key, Map<String, String> values) {
        String raw = plugin.getConfig().getString("prefix", "") +
                plugin.getConfig().getString("messages." + key, "&cСообщение не настроено: " + key);
        return component(replace(raw, values));
    }

    public Component component(String raw) { return LEGACY.deserialize(raw); }
    public String date(long millis) {
        String pattern = plugin.getConfig().getString("date-format", "dd.MM.yyyy HH:mm");
        return DateTimeFormatter.ofPattern(pattern).withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(millis));
    }
    public String amount(double value) { return new DecimalFormat("0.##").format(value); }
    public static String replace(String input, Map<String, String> values) {
        String result = input;
        for (Map.Entry<String, String> e : values.entrySet()) result = result.replace("{" + e.getKey() + "}", e.getValue());
        return result;
    }
}
