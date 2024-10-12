package io.github.rysefoxx.inventory.plugin.listener;

import io.github.rysefoxx.inventory.plugin.other.EventCreator;
import io.github.rysefoxx.inventory.plugin.pagination.InventoryManager;
import io.github.rysefoxx.inventory.plugin.pagination.RyseInventory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public record PlayerQuitListener(InventoryManager manager) implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    @SuppressWarnings("unchecked")
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!manager.hasInventory(player.getUniqueId())) return;
        RyseInventory mainInventory = manager.getInventories().get(player.getUniqueId());
        EventCreator<PlayerQuitEvent> customEvent = (EventCreator<PlayerQuitEvent>) mainInventory.getEvent(PlayerQuitEvent.class);
        if (customEvent == null) return;
        customEvent.accept(event);
    }

}
