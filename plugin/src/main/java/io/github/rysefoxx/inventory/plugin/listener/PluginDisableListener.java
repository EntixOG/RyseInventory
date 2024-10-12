package io.github.rysefoxx.inventory.plugin.listener;

import io.github.rysefoxx.inventory.plugin.pagination.InventoryManager;
import io.github.rysefoxx.inventory.plugin.pagination.RyseInventory;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public record PluginDisableListener(InventoryManager manager, Plugin plugin) implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPluginDisable(@NotNull PluginDisableEvent event) {
        Plugin disabledPlugin = event.getPlugin();
        if (disabledPlugin != plugin) return;
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (!manager.hasInventory(player.getUniqueId())) return;
            RyseInventory inventory = manager.getInventories().get(player.getUniqueId());
            inventory.close(player);
        });

    }
}
