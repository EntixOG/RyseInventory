package io.github.rysefoxx.inventory.plugin.listener.own;

import io.github.rysefoxx.inventory.plugin.events.RyseInventoryCloseEvent;
import io.github.rysefoxx.inventory.plugin.other.EventCreator;
import io.github.rysefoxx.inventory.plugin.pagination.InventoryManager;
import io.github.rysefoxx.inventory.plugin.pagination.RyseInventory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public record RyseInventoryCloseListener(InventoryManager manager) implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    @SuppressWarnings("unchecked")
    public void onRyseInventoryClose(@NotNull RyseInventoryCloseEvent event) {
        Player player = event.getPlayer();
        if (!manager.hasInventory(player.getUniqueId())) return;
        RyseInventory mainInventory = manager.getInventories().get(player.getUniqueId());
        EventCreator<RyseInventoryCloseEvent> customEvent = (EventCreator<RyseInventoryCloseEvent>) mainInventory.getEvent(RyseInventoryCloseEvent.class);
        if (customEvent == null) return;
        customEvent.accept(event);
    }
}
