/*
 * MIT License
 *
 * Copyright (c) 2022. Rysefoxx
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package io.github.rysefoxx.inventory.plugin.pagination;

import io.github.rysefoxx.inventory.plugin.animator.SlideAnimation;
import io.github.rysefoxx.inventory.plugin.content.IntelligentItem;
import io.github.rysefoxx.inventory.plugin.content.InventoryContents;
import io.github.rysefoxx.inventory.plugin.enums.*;
import io.github.rysefoxx.inventory.plugin.events.*;
import io.github.rysefoxx.inventory.plugin.listener.*;
import io.github.rysefoxx.inventory.plugin.listener.own.*;
import io.github.rysefoxx.inventory.plugin.other.EventCreator;
import io.github.rysefoxx.inventory.plugin.util.InventoryUtil;
import lombok.AccessLevel;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import space.arim.morepaperlib.MorePaperLib;
import space.arim.morepaperlib.scheduling.ScheduledTask;

import java.util.*;
import java.util.function.Consumer;

/**
 * @author Rysefoxx | Rysefoxx#6772
 * @since 2/17/2022
 */
public class InventoryManager {

    private final Plugin plugin;
    @Getter(AccessLevel.PUBLIC)
    private final MorePaperLib morePaperLib;
    @Getter(AccessLevel.PROTECTED)
    private boolean invoked = false;

    @Getter
    private final HashMap<UUID, RyseInventory> inventories = new HashMap<>();

    @Getter
    private final HashMap<UUID, InventoryContents> content = new HashMap<>();


    private final Set<IntelligentItem> items = new HashSet<>();
    private final List<RyseInventory> cachedInventories = new ArrayList<>();
    private final HashMap<UUID, ScheduledTask> updaterTask = new HashMap<>();
    private final HashMap<UUID, List<RyseInventory>> lastInventories = new HashMap<>();
    private final HashMap<UUID, Long> lastOpen = new HashMap<>();

    public InventoryManager(final Plugin plugin) {
        this.plugin = plugin;
        this.morePaperLib = new MorePaperLib(plugin);
    }


    /*
     * Used to prevent multi open on menus, this is a 100ms delay between opening menus.
     * @param uuid The uuid of the player.
     * @return true if the player can open the menu, false if not.
     * */
    public boolean canOpen(UUID uuid) {
        return !lastOpen.containsKey(uuid) || System.currentTimeMillis() - lastOpen.get(uuid) > 500;
    }

    /*
     * Used to set the last open time of the player.
     * @param uuid The uuid of the player.
     * */
    public void setLastOpen(UUID uuid) {
        lastOpen.put(uuid, System.currentTimeMillis());
    }

    /**
     * Adds the IntelligentItem to the list if this item has an ID.
     *
     * @param item The item to add.
     * @throws NullPointerException If the item ID is null.
     */
    public void register(@NotNull final IntelligentItem item) throws NullPointerException {
        if (item.getId() == null) throw new NullPointerException("The item has no ID!");
        this.items.add(item);
    }

    /**
     * @param id The id of the item
     * @return Returns the first IntelligentItem that matches the ID. If no item is found, null is returned.
     */
    public @Nullable IntelligentItem getItemById(@NotNull Object id) {
        return this.items.stream().filter(item -> item.getId() == id).findFirst().orElse(null);
    }

    /**
     * @param id The id of the item
     * @return Returns all IntelligentItems that match the ID. If no item is found, an empty list is returned.
     */
    public @NotNull List<IntelligentItem> getAllItemsById(@NotNull Object id) {
        List<IntelligentItem> result = new ArrayList<>();
        for (IntelligentItem item : this.items) {
            if (item.getId() != id) continue;

            result.add(item);
        }
        return result;
    }

    /**
     * With this method you can get the inventory from the player.
     *
     * @param uuid The UUID of the player.
     * @return if the player has no inventory open.
     */
    public @NotNull Optional<RyseInventory> getInventory(@NotNull UUID uuid) {
        if (!hasInventory(uuid)) return Optional.empty();
        return Optional.ofNullable(this.inventories.get(uuid));
    }

    /**
     * Get all players who have a certain inventory open
     *
     * @param inventory The inventory that is filtered by.
     * @return The list with all found players.
     */
    public @NotNull List<UUID> getOpenedPlayers(@NotNull RyseInventory inventory) {
        List<UUID> players = new ArrayList<>();
        Bukkit.getOnlinePlayers().forEach(player -> {
            Optional<RyseInventory> optional = getInventory(player.getUniqueId());

            optional.ifPresent(savedInventory -> {
                if (!inventory.equals(savedInventory)) return;
                players.add(player.getUniqueId());
            });
        });
        return players;
    }

    /**
     * Get the last inventory that the player had open.
     *
     * @param uuid Player UUID
     * @return if there is no final inventory.
     */
    public @NotNull Optional<RyseInventory> getLastInventory(@NotNull UUID uuid) {
        if (!this.lastInventories.containsKey(uuid)) return Optional.empty();
        if (this.lastInventories.get(uuid).isEmpty()) return Optional.empty();
        RyseInventory inventory = this.lastInventories.get(uuid).remove(this.lastInventories.get(uuid).size() - 1);
        inventory.setBackward();

        return Optional.of(inventory);
    }

    /**
     * With this method you can get the inventory from the inventory identifier.
     *
     * @param identifier The ID to identify
     * @return if no inventory with the ID could be found.
     * <p>
     * Only works if the inventory has also been assigned an identifier.
     */
    public @NotNull Optional<RyseInventory> getInventory(@NotNull Object identifier) {
        Optional<RyseInventory> optional = this.inventories.values()
                .stream()
                .filter(inventory -> Objects.equals(inventory.getIdentifier(), identifier))
                .findFirst();

        if (optional.isPresent())
            return optional;

        return this.cachedInventories
                .stream()
                .filter(inventory -> Objects.equals(inventory.getIdentifier(), identifier))
                .findFirst();
    }

    /**
     * With this method you can get the inventory content from the player.
     *
     * @param uuid The UUID of the player.
     * @return the player inventory content.
     */
    public @NotNull Optional<InventoryContents> getContents(@NotNull UUID uuid) {
        if (!this.content.containsKey(uuid)) return Optional.empty();
        return Optional.ofNullable(this.content.get(uuid));
    }

    /**
     * Registers the standard events
     */
    public void invoke() {
        Bukkit.getPluginManager().registerEvents(new BlockBreakListener(this), this.plugin);
        Bukkit.getPluginManager().registerEvents(new EntityDamageListener(this), this.plugin);
        Bukkit.getPluginManager().registerEvents(new FoodLevelChangeListener(this), this.plugin);
        Bukkit.getPluginManager().registerEvents(new InventoryClickListener(this), this.plugin);
        Bukkit.getPluginManager().registerEvents(new InventoryCloseListener(this), this.plugin);
        Bukkit.getPluginManager().registerEvents(new InventoryDragListener(this), this.plugin);
        Bukkit.getPluginManager().registerEvents(new PlayerPickupItemListener(this), this.plugin);
        Bukkit.getPluginManager().registerEvents(new PlayerQuitListener(this), this.plugin);
        Bukkit.getPluginManager().registerEvents(new PluginDisableListener(this, this.plugin), this.plugin);
        Bukkit.getPluginManager().registerEvents(new PotionSplashListener(this), this.plugin);

        Bukkit.getPluginManager().registerEvents(new RyseInventoryCloseListener(this), this.plugin);
        Bukkit.getPluginManager().registerEvents(new RyseInventoryOpenListener(this), this.plugin);
        Bukkit.getPluginManager().registerEvents(new RyseInventoryPreCloseListener(this), this.plugin);
        Bukkit.getPluginManager().registerEvents(new RyseInventoryPreOpenListener(this), this.plugin);
        Bukkit.getPluginManager().registerEvents(new RyseInventoryTitleChangeListener(this), this.plugin);
        invoked = true;
    }

    /**
     * Returns true if the given UUID has an inventory.
     *
     * @param uuid The UUID of the player to check for.
     * @return true when the player has an inventory.
     */
    @Contract(pure = true)
    public boolean hasInventory(@NotNull UUID uuid) {
        return this.inventories.containsKey(uuid);
    }

    /**
     * Removes the inventory from the player
     *
     * @param uuid The UUID of the player to remove the inventory from.
     */
    protected void removeInventoryFromPlayer(@NotNull UUID uuid) {
        this.inventories.remove(uuid);
        this.content.remove(uuid);
        this.lastInventories.clear();
        this.lastOpen.clear();
        ScheduledTask task = this.updaterTask.remove(uuid);

        if (task != null)
            task.cancel();
    }

    /**
     * It removes the inventory of the player with the given UUID from the HashMap
     *
     * @param uuid The UUID of the player to remove the inventory of.
     */
    protected void removeInventory(@NotNull UUID uuid) {
        this.inventories.remove(uuid);
    }

    /**
     * It puts the contents of the inventory into a HashMap
     *
     * @param uuid     The UUID of the player who's inventory you want to set.
     * @param contents The InventoryContents object that you want to set.
     */
    @ApiStatus.Internal
    public void setContents(@NotNull UUID uuid, @NotNull InventoryContents contents) {
        this.content.put(uuid, contents);
    }

    /**
     * This function sets the inventory of a player.
     *
     * @param uuid      The UUID of the player
     * @param inventory The inventory to set.
     */
    protected void setInventory(@NotNull UUID uuid,
                                @NotNull RyseInventory inventory) {
        this.inventories.put(uuid, inventory);
    }

    /**
     * It adds the player's current inventory to a list of inventories
     *
     * @param uuid         The UUID of the player
     * @param inventory    The inventory that the player is currently in.
     * @param newInventory The new inventory that the player is switching to.
     */
    protected void setLastInventory(@NotNull UUID uuid,
                                    @NotNull RyseInventory inventory,
                                    @NotNull RyseInventory newInventory) {
        List<RyseInventory> inventoryList = this.lastInventories.getOrDefault(uuid, new ArrayList<>());

        if (inventory.equals(newInventory)) return;

        inventoryList.add(inventory);

        this.lastInventories.put(uuid, inventoryList);
    }

    /**
     * It stops the update task for the specified player
     *
     * @param uuid The UUID of the player to stop updating.
     */
    protected void stopUpdate(@NotNull UUID uuid) {
        if (!this.updaterTask.containsKey(uuid)) return;

        ScheduledTask task = this.updaterTask.remove(uuid);
        task.cancel();
    }

    /**
     * If the player has an inventory, and the inventory is the same as the one passed in, then update the inventory
     *
     * @param player    The player who's inventory is being updated.
     * @param inventory The inventory that will be updated.
     */
    protected void invokeScheduler(@NotNull Player player,
                                   @NotNull RyseInventory inventory) {
        if (this.updaterTask.containsKey(player.getUniqueId())) return;
        if (!inventory.isUpdateTask()) return;

        int delay = inventory.getDelay();
        if (morePaperLib.scheduling().isUsingFolia() && delay == 0) {
            delay = 1;
        }
        morePaperLib.scheduling().globalRegionalScheduler().runAtFixedRate(scheduledTask -> {
            if (!hasInventory(player.getUniqueId())) {
                scheduledTask.cancel();
                return;
            }
            RyseInventory savedInventory = inventories.get(player.getUniqueId());
            if (savedInventory != inventory) {
                scheduledTask.cancel();
                return;
            }
            savedInventory.getProvider().update(player, content.get(player.getUniqueId()));
            this.updaterTask.put(player.getUniqueId(), scheduledTask);
        }, delay, inventory.getPeriod());
    }

    /**
     * Saves the inventory to the cache.
     *
     * @param ryseInventory The inventory to save.
     */
    protected void addToCache(RyseInventory ryseInventory) {
        this.cachedInventories.add(ryseInventory);
    }
}
