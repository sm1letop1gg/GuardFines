package ru.guard.fines;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class FineMenu implements Listener {
    private final GuardFinesPlugin plugin;

    public FineMenu(GuardFinesPlugin plugin) { this.plugin = plugin; }

    public void open(Player player) {
        boolean admin = player.hasPermission("guardfines.admin");
        List<Fine> fines = plugin.repository().openIssuedBy(player.getUniqueId(), admin);
        int size = Math.max(9, Math.min(54, ((Math.min(fines.size(), 54) + 8) / 9) * 9));
        Component title = plugin.text().component(plugin.getConfig().getString("gui-title", "&4Невыплаченные штрафы"));
        Inventory inventory = Bukkit.createInventory(null, size, title);
        for (int i = 0; i < Math.min(size, fines.size()); i++) inventory.setItem(i, item(fines.get(i)));
        player.openInventory(inventory);
    }

    private ItemStack item(Fine fine) {
        ItemStack stack = new ItemStack(fine.status() == FineStatus.OVERDUE ? Material.RED_DYE : Material.PAPER);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(plugin.text().component("&fШтраф &c#" + fine.id() + " &7— &e" + plugin.text().amount(fine.amount())));
        List<Component> lore = new ArrayList<>();
        lore.add(plugin.text().component("&7Получатель: &f" + fine.targetName()));
        lore.add(plugin.text().component("&7Выдал: &f" + fine.issuerName()));
        lore.add(plugin.text().component("&7Статус: " + (fine.status() == FineStatus.OVERDUE ? "&c" : "&e") + fine.status().displayName()));
        lore.add(plugin.text().component("&7Срок: &f" + plugin.text().date(fine.deadline())));
        lore.add(plugin.text().component("&7Причина: &f" + fine.reason()));
        lore.add(Component.empty());
        lore.add(plugin.text().component("&aЛКМ &7— подтвердить выплату"));
        lore.add(plugin.text().component("&cПКМ &7— отменить штраф"));
        meta.lore(lore);
        meta.setCustomModelData((int) Math.min(Integer.MAX_VALUE, fine.id()));
        stack.setItemMeta(meta);
        return stack;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Component title = plugin.text().component(plugin.getConfig().getString("gui-title", "&4Невыплаченные штрафы"));
        if (!event.getView().title().equals(title)) return;
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || !clicked.getItemMeta().hasCustomModelData()) return;
        long id = clicked.getItemMeta().getCustomModelData();
        if (event.isLeftClick()) plugin.changeStatus(player, id, FineStatus.PAID);
        else if (event.isRightClick()) plugin.changeStatus(player, id, FineStatus.CANCELLED);
        open(player);
    }
}
