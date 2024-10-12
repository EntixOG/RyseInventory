package io.github.rysefoxx.inventory.plugin.listener;

import io.github.rysefoxx.inventory.plugin.enums.InventoryOptions;
import io.github.rysefoxx.inventory.plugin.pagination.InventoryManager;
import io.github.rysefoxx.inventory.plugin.pagination.RyseInventory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PotionSplashEvent;
import org.jetbrains.annotations.NotNull;

public record PotionSplashListener(InventoryManager manager) implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onPotionSplash(@NotNull PotionSplashEvent event) {
        for (LivingEntity entity : event.getAffectedEntities()) {
            if (!(entity instanceof Player player)) continue;
            if (!manager.hasInventory(player.getUniqueId())) continue;
            RyseInventory mainInventory = manager.getInventories().get(player.getUniqueId());
            if (!mainInventory.getOptions().contains(InventoryOptions.NO_POTION_EFFECT)) continue;
            event.setCancelled(true);
        }
    }
}
