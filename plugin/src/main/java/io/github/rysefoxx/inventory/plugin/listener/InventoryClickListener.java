package io.github.rysefoxx.inventory.plugin.listener;

import io.github.rysefoxx.inventory.plugin.animator.SlideAnimation;
import io.github.rysefoxx.inventory.plugin.content.IntelligentItem;
import io.github.rysefoxx.inventory.plugin.content.InventoryContents;
import io.github.rysefoxx.inventory.plugin.enums.Action;
import io.github.rysefoxx.inventory.plugin.enums.CloseReason;
import io.github.rysefoxx.inventory.plugin.enums.DisabledInventoryClick;
import io.github.rysefoxx.inventory.plugin.enums.InventoryOpenerType;
import io.github.rysefoxx.inventory.plugin.other.EventCreator;
import io.github.rysefoxx.inventory.plugin.pagination.InventoryManager;
import io.github.rysefoxx.inventory.plugin.pagination.RyseInventory;
import io.github.rysefoxx.inventory.plugin.util.InventoryUtil;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import space.arim.morepaperlib.scheduling.ScheduledTask;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public record InventoryClickListener(InventoryManager manager) implements Listener {


    @EventHandler(priority = EventPriority.LOWEST)
    @SuppressWarnings("unchecked")
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        HumanEntity whoClicked = InventoryUtil.getPlayer(event);
        if (!(whoClicked instanceof Player player)) return;
        if (!manager.hasInventory(player.getUniqueId())) return;
        RyseInventory mainInventory = manager.getInventories().get(player.getUniqueId());

        if (event.getClickedInventory() == null) {
            if (mainInventory.getCloseReasons().contains(CloseReason.CLICK_OUTSIDE))
                player.closeInventory();
            return;
        }

        EventCreator<InventoryClickEvent> customEvent = (EventCreator<InventoryClickEvent>) mainInventory.getEvent(InventoryClickEvent.class);
        if (customEvent != null) {
            manager.getMorePaperLib().scheduling().globalRegionalScheduler().runDelayed(() -> customEvent.accept(event), 2L);
        }

        List<DisabledInventoryClick> list = mainInventory.getIgnoreClickEvent();

        InventoryAction action = event.getAction();
        Inventory clickedInventory = event.getClickedInventory();
        Inventory bottomInventory = InventoryUtil.getPlayerBottomInventory(player);
        Inventory topInventory = InventoryUtil.getPlayerTopInventory(player);
        int slot = event.getSlot();
        ClickType clickType = event.getClick();
        InventoryContents contents = manager.getContent().get(player.getUniqueId());

        ItemStack itemStack = event.getCurrentItem();
        if (itemStack == null) return;
        if (clickedInventory == bottomInventory) {
            if (!list.contains(DisabledInventoryClick.BOTTOM) && !list.contains(DisabledInventoryClick.BOTH)) {
                event.setCancelled(true);
                return;
            }

            if (mainInventory.getCloseReasons().contains(CloseReason.CLICK_BOTTOM_INVENTORY)) {
                mainInventory.close(player);
                return;
            }

            if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                if (!mainInventory.getEnabledActions().contains(Action.MOVE_TO_OTHER_INVENTORY)) {
                    event.setCancelled(true);
                    return;
                }

                int[] data = checkForExistingItem(topInventory, itemStack, mainInventory);
                int targetSlot = data[0];
                int targetAmount = data[1];

                Optional<IntelligentItem> itemOptional = contents.get(targetSlot);

                if (cancelEventIfItemHasConsumer(event, mainInventory, targetSlot, itemOptional)) return;

                if (adjustItemStackAmount(topInventory, event, mainInventory, contents, targetSlot, targetAmount))
                    return;

                event.setCancelled(true);
                adjustItemStackAmountToMaxStackSize(itemStack, mainInventory, topInventory, contents, targetSlot, targetAmount);
                return;
            }

            if (action == InventoryAction.NOTHING)
                event.setCancelled(true);

            return;
        }

        if (clickedInventory == topInventory) {
            if (!hasContents(player.getUniqueId()))
                return;
            if (slot < 0 || (mainInventory.getInventoryOpenerType() == InventoryOpenerType.CHEST && slot > mainInventory.size(contents))) {
                return;
            }

            SlideAnimation animation = mainInventory.getSlideAnimator();

            if (animation != null && mainInventory.activeSlideAnimatorTasks() > 0 && animation.isBlockClickEvent()) {
                event.setCancelled(true);
                return;
            }

            if (!list.contains(DisabledInventoryClick.TOP) && !list.contains(DisabledInventoryClick.BOTH)) {
                if (event.getClick() == ClickType.DOUBLE_CLICK
                        && !mainInventory.getEnabledActions().contains(Action.DOUBLE_CLICK)) {
                    event.setCancelled(true);
                    return;
                }
                if (!mainInventory.getIgnoredSlots().containsKey(slot))
                    event.setCancelled(true);
            }

            if (mainInventory.getIgnoredSlots().containsKey(slot)) {
                Consumer<InventoryClickEvent> consumer = mainInventory.getIgnoredSlots().get(slot);

                if (consumer != null) {
                    consumer.accept(event);

                    if (event.isCancelled())
                        return;
                }

                modifyItemStackAmountViaCursor(event, itemStack, mainInventory, slot, clickType, contents);
                subtractItemStackAmountWhenRightClick(event, itemStack, mainInventory, slot, clickType, contents);
            }

            Optional<IntelligentItem> optional = contents.get(slot);

            if (optional.isEmpty() && mainInventory.getCloseReasons().contains(CloseReason.CLICK_EMPTY_SLOT)) {
                event.setCancelled(true);
                mainInventory.close(player);
                return;
            }

            optional.ifPresent(item -> {
                if (item.getDefaultConsumer() == null) {
                    event.setCancelled(false);
                    return;
                }

                if (list.contains(DisabledInventoryClick.TOP) || list.contains(DisabledInventoryClick.BOTH)) {
                    event.setCancelled(false);
                    return;
                }

                if (item.getDelayTask() != null && item.getDelayTask().getExecutionState() == ScheduledTask.ExecutionState.RUNNING)
                    return;

                int delay = item.getDelay();
                if (manager.getMorePaperLib().scheduling().isUsingFolia() && delay <= 0) {
                    delay = 1;
                }
                item.setDelayTask(manager.getMorePaperLib().scheduling().globalRegionalScheduler().runDelayed(() -> {
                    if (!item.isCanClick()) {
                        item.getError().cantClick(player, item);
                        return;
                    }
                    item.setDelayTask(null);
                    item.getDefaultConsumer().accept(event);
                    player.updateInventory();
                }, delay));
            });
        }
    }

    @Contract(pure = true)
    private boolean hasContents(@NotNull UUID uuid) {
        return manager.getContent().containsKey(uuid);
    }


    /**
     * This function checks if an item exists in an inventory, and if it does, it returns the slot number and the
     * amount of the item in that slot.
     *
     * @param topInventory The inventory to check for the item in.
     * @param itemStack    The item you want to check for.
     * @return An array of integers.
     */
    private int @NotNull [] checkForExistingItem(@NotNull Inventory topInventory,
                                                 @Nullable ItemStack itemStack,
                                                 @NotNull RyseInventory mainInventory) {
        int[] data = new int[2];
        data[0] = -1;
        for (int i = 0; i < topInventory.getSize(); i++) {
            ItemStack inventoryItem = topInventory.getItem(i);
            if (!mainInventory.getIgnoredSlots().containsKey(i)) continue;

            if (inventoryItem == null || inventoryItem.getType() == Material.AIR) {
                data[0] = i;
                break;
            }

            if (inventoryItem.isSimilar(itemStack) && inventoryItem.getAmount() < inventoryItem.getMaxStackSize()) {
                data[0] = i;
                data[1] = inventoryItem.getAmount();
                break;
            }
        }
        return data;
    }

    /**
     * If the item in the target slot has a consumer, cancel the event
     *
     * @param event         The InventoryClickEvent that was called.
     * @param mainInventory The inventory that the player is currently viewing.
     * @param targetSlot    The slot that the player is trying to click on.
     * @param itemOptional  The item that is being clicked on.
     * @return A boolean value.
     */
    private boolean cancelEventIfItemHasConsumer(@NotNull InventoryClickEvent event,
                                                 @NotNull RyseInventory mainInventory,
                                                 int targetSlot,
                                                 Optional<IntelligentItem> itemOptional) {
        if (!mainInventory.getIgnoredSlots().containsKey(targetSlot)) {
            if (itemOptional.isPresent() && itemOptional.get().getDefaultConsumer() != null) return true;

            event.setCancelled(true);
            return true;
        }
        return false;
    }


    /**
     * If the amount of the item in the target slot plus the amount of the item in the cursor slot is less than or
     * equal to the max stack size of the item, then set the amount of the item in the target slot to the amount of the
     * item in the target slot plus the amount of the item in the cursor slot
     *
     * @param mainInventory The RyseInventory instance
     * @param contents      The InventoryContents object that contains all the information about the inventory.
     * @param targetSlot    The slot in the inventory that the item is being moved to.
     * @param targetAmount  The amount of the item that you want to add to the target slot.
     * @return A boolean value.
     */
    public boolean adjustItemStackAmount(@NotNull Inventory topInventory,
                                         InventoryClickEvent event,
                                         RyseInventory mainInventory,
                                         InventoryContents contents,
                                         int targetSlot, int targetAmount) {
        ItemStack itemStack = event.getCurrentItem();
        if (itemStack.getAmount() + targetAmount <= itemStack.getMaxStackSize()) {
            event.setCancelled(true);

            ItemStack topItem = topInventory.getItem(targetSlot);

            if (topItem != null && topItem.getType() != Material.AIR)
                if (!itemStack.isSimilar(topInventory.getItem(targetSlot)))
                    return true;


            event.setCurrentItem(null);

            ItemStack finalItemStack = itemStack.clone();
            finalItemStack.setAmount(itemStack.getAmount() + targetAmount);

            topInventory.setItem(targetSlot, finalItemStack);

            if (!mainInventory.isIgnoreManualItems()) {
                contents.pagination().setItem(
                        targetSlot,
                        contents.pagination().page() - 1,
                        IntelligentItem.ignored(finalItemStack),
                        true);
            }
            return true;
        }
        return false;
    }


    /**
     * If the item stack is greater than the max stack size, set the item stack to the max stack size and subtract the
     * max stack size from the original item stack
     *
     * @param itemStack     The itemstack that is being moved
     * @param mainInventory The RyseInventory instance
     * @param topInventory  The inventory that the player is currently viewing.
     * @param contents      The InventoryContents object that is passed to the InventoryListener.
     * @param targetSlot    The slot in the top inventory that the item is being moved to.
     * @param targetAmount  The amount of items to be moved to the target slot.
     */
    public void adjustItemStackAmountToMaxStackSize(@NotNull ItemStack itemStack,
                                                    @NotNull RyseInventory mainInventory,
                                                    @NotNull Inventory topInventory,
                                                    InventoryContents contents,
                                                    int targetSlot, int targetAmount) {
        ItemStack toSet = new ItemStack(itemStack.getType(), itemStack.getMaxStackSize());
        topInventory.setItem(targetSlot, toSet);

        if (!mainInventory.isIgnoreManualItems()) {
            contents.pagination().setItem(
                    targetSlot,
                    contents.pagination().page() - 1,
                    IntelligentItem.ignored(toSet),
                    true);
        }

        itemStack.setAmount(itemStack.getAmount() - targetAmount);
    }


    /**
     * If the cursor is not empty, and the item in the slot is similar to the cursor, then set the amount of the item
     * in the slot to the amount of the item in the slot plus the amount of the cursor
     *
     * @param event         The InventoryClickEvent that was fired.
     * @param itemStack     The itemstack in the slot that was clicked.
     * @param mainInventory The inventory that the player is currently viewing.
     * @param slot          The slot that was clicked
     * @param clickType     The type of click that was performed.
     * @param contents      The InventoryContents object that contains all the information about the inventory.
     */
    private void modifyItemStackAmountViaCursor(@NotNull InventoryClickEvent event,
                                                ItemStack itemStack,
                                                RyseInventory mainInventory,
                                                int slot,
                                                ClickType clickType,
                                                InventoryContents contents) {
        if (event.getCursor() == null || event.getCursor().getType() == Material.AIR) return;

        ItemStack cursor = event.getCursor().clone();
        if (clickType == ClickType.RIGHT) {
            cursor.setAmount(itemStack != null && itemStack.isSimilar(cursor)
                    ? itemStack.getAmount() + 1
                    : 1);
        } else {
            cursor.setAmount(itemStack != null && itemStack.isSimilar(cursor)
                    ? itemStack.getAmount() + cursor.getAmount()
                    : cursor.getAmount());
        }

        if (mainInventory.isIgnoreManualItems())
            return;

        contents.pagination().setItem(
                slot,
                contents.pagination().page() - 1,
                IntelligentItem.ignored(cursor),
                true);
    }

    /**
     * When the player right clicks on an item, the item's amount is halved. Otherwise the item is removed.
     *
     * @param event         The InventoryClickEvent that was called.
     * @param itemStack     The itemstack that was clicked
     * @param mainInventory The inventory that is being opened.
     * @param slot          The slot that was clicked
     * @param clickType     The type of click that was performed.
     * @param contents      The InventoryContents object that contains all the information about the inventory.
     */
    private void subtractItemStackAmountWhenRightClick(@NotNull InventoryClickEvent event,
                                                       ItemStack itemStack,
                                                       RyseInventory mainInventory,
                                                       int slot,
                                                       ClickType clickType,
                                                       InventoryContents contents) {
        if (event.getCursor() != null && event.getCursor().getType() != Material.AIR)
            return;

        if (clickType == ClickType.RIGHT) {
            if (mainInventory.isIgnoreManualItems()) return;
            if (itemStack == null) return;

            ItemStack finalItemStack = itemStack.clone();
            finalItemStack.setAmount(itemStack.getAmount() / 2);
            contents.pagination().setItem(
                    slot,
                    contents.pagination().page() - 1,
                    IntelligentItem.ignored(finalItemStack),
                    true);
            return;
        }

        if (itemStack != null && itemStack.getType() != Material.AIR)
            contents.pagination().remove(slot);
    }
}
