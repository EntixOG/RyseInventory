package io.github.rysefoxx.inventory.plugin.listener;

import io.github.rysefoxx.inventory.plugin.enums.DisabledEvents;
import io.github.rysefoxx.inventory.plugin.other.EventCreator;
import io.github.rysefoxx.inventory.plugin.pagination.InventoryManager;
import io.github.rysefoxx.inventory.plugin.pagination.RyseInventory;
import io.github.rysefoxx.inventory.plugin.util.InventoryUtil;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

public record InventoryDragListener(InventoryManager manager) implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    @SuppressWarnings("unchecked")
    public void onInventoryDrag(@NotNull InventoryDragEvent event) {
        HumanEntity whoClicked = InventoryUtil.getPlayer(event);
        if (!(whoClicked instanceof Player player)) return;
        if (!manager.hasInventory(player.getUniqueId())) return;

        Inventory topInventory = player.getOpenInventory().getTopInventory();
        RyseInventory mainInventory = manager.getInventories().get(player.getUniqueId());

        EventCreator<InventoryDragEvent> customEvent = (EventCreator<InventoryDragEvent>) mainInventory.getEvent(InventoryDragEvent.class);
        if (customEvent != null) {
            customEvent.accept(event);
            return;
        }

        if (mainInventory.getDisabledEvents().contains(DisabledEvents.INVENTORY_DRAG)) return;

        event.getRawSlots().forEach(integer -> {
            if (integer >= topInventory.getSize()) return;
            event.setCancelled(true);
        });
    }
}
