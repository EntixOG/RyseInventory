package io.github.rysefoxx.inventory.plugin.listener;

import io.github.rysefoxx.inventory.plugin.enums.InventoryOptions;
import io.github.rysefoxx.inventory.plugin.pagination.InventoryManager;
import io.github.rysefoxx.inventory.plugin.pagination.RyseInventory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record BlockBreakListener(InventoryManager manager) implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(@NotNull BlockBreakEvent event) {
        Block block = event.getBlock();
        Location toCheck = block.getLocation().clone().add(0, 1, 0);

        List<Player> onBlock = new ArrayList<>();

        Bukkit.getOnlinePlayers().forEach(onlinePlayer -> {
            if (onlinePlayer.getLocation().getBlockX() == toCheck.getBlockX() &&
                    onlinePlayer.getLocation().getBlockY() == toCheck.getBlockY() &&
                    onlinePlayer.getLocation().getBlockZ() == toCheck.getBlockZ()) {
                onBlock.add(onlinePlayer);
            }
        });

        if (!onBlock.isEmpty()) {
            onBlock.forEach(affectedPlayer -> {
                if (!manager.hasInventory(affectedPlayer.getUniqueId())) return;
                RyseInventory mainInventory = manager.getInventories().get(affectedPlayer.getUniqueId());
                if (!mainInventory.getOptions().contains(InventoryOptions.NO_BLOCK_BREAK)) return;
                event.setCancelled(true);
            });
        }
    }
}
