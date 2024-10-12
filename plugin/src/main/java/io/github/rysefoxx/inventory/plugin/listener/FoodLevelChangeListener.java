package io.github.rysefoxx.inventory.plugin.listener;

import io.github.rysefoxx.inventory.plugin.enums.InventoryOptions;
import io.github.rysefoxx.inventory.plugin.pagination.InventoryManager;
import io.github.rysefoxx.inventory.plugin.pagination.RyseInventory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.jetbrains.annotations.NotNull;

public record FoodLevelChangeListener(InventoryManager manager) implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onFoodLevelChange(@NotNull FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!manager.hasInventory(player.getUniqueId())) return;
        RyseInventory mainInventory = manager.getInventories().get(player.getUniqueId());
        if (!mainInventory.getOptions().contains(InventoryOptions.NO_HUNGER)) return;
        event.setCancelled(true);
    }
}
