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

import io.github.rysefoxx.inventory.plugin.pattern.SlotIteratorPattern;
import io.github.rysefoxx.inventory.plugin.util.SlotUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.Nonnegative;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SlotIterator {

    int slot = -1;
    int endPosition = -1;
    SlotIteratorType type = SlotIteratorType.HORIZONTAL;
    boolean override;
    List<Integer> blackList = new ArrayList<>();
    SlotIteratorPattern pattern;

    @Contract(" -> new")
    public static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * @return the start column
     */
    public @Nonnegative int getColumn() {
        return SlotUtils.toRowAndColumn(this.slot).getRight();
    }

    /**
     * @return the start row
     */
    public @Nonnegative int getRow() {
        return SlotUtils.toRowAndColumn(this.slot).getLeft();
    }

    /**
     * @return all slots where no items should be placed.
     * @throws UnsupportedOperationException When modifying the returned list.
     */
    @Unmodifiable
    public @NotNull List<Integer> getBlackList() throws UnsupportedOperationException {
        return Collections.unmodifiableList(this.blackList);
    }

    protected @NotNull List<Integer> getBlackListInternal() {
        return this.blackList;
    }

    /**
     * An enum that is used to tell the SlotIterator how to place the items.
     */
    public enum SlotIteratorType {
        HORIZONTAL,
        VERTICAL
    }

    public static class Builder {
        private SlotIterator slotIterator = new SlotIterator();

        /**
         * Copies the values of the given {@link SlotIterator} into this builder.
         *
         * @param iterator the {@link SlotIterator} to copy the values from
         * @return The builder object itself.
         */
        public @NotNull Builder copy(@NotNull SlotIterator iterator) {
            this.slotIterator = iterator;
            return this;
        }

        /**
         * Adds a slot to the blacklist.
         *
         * @param slot The slot to add to the blacklist.
         * @return The builder object itself.
         */
        public @NotNull Builder blackList(@Nonnegative int slot) {
            this.slotIterator.blackList.add(slot);
            return this;
        }

        /**
         * This can be used to block multiple slots.
         *
         * @param slots The slots to be used for the inventory.
         * @return The builder object itself.
         */
        public @NotNull Builder blackList(@NotNull List<Integer> slots) {
            this.slotIterator.blackList = new ArrayList<>(slots);
            return this;
        }

        /**
         * This can be used to block multiple slots.
         *
         * @param slots The slots to be used for the inventory.
         * @return The builder object itself.
         */
        public @NotNull Builder blackList(int @NotNull ... slots) {
            for (int blackListSlot : slots)
                blackList(blackListSlot);

            return this;
        }

        /**
         * This tells us where the item should stop.
         *
         * @param slot The slot to stop at.
         * @return The Builder object itself.
         * <p>
         * If this method is used, {@link Pagination#setItemsPerPage(int)} is ignored.
         */
        public @NotNull Builder endPosition(@Nonnegative int slot) {
            this.slotIterator.endPosition = slot;
            return this;
        }

        /**
         * This tells us where the item should stop.
         *
         * @param row    The row to stop at.
         * @param column The column to stop at.
         * @return The Builder object itself.
         * <p>
         * If this method is used, {@link Pagination#setItemsPerPage(int)} is ignored.
         */
        public @NotNull Builder endPosition(@Nonnegative int row, @Nonnegative int column) {
            return endPosition(SlotUtils.toSlot(row, column));
        }

        /**
         * Sets the slot to start at.
         *
         * @param startSlot The slot to start at.
         * @return The Builder object itself.
         */
        public @NotNull Builder startPosition(@Nonnegative int startSlot) {
            this.slotIterator.slot = startSlot;
            return this;
        }

        /**
         * Sets the slot to start at.
         *
         * @param row    The row to start at.
         * @param column The column to start at.
         * @return The Builder object itself.
         */
        public @NotNull Builder startPosition(@Nonnegative int row, @Nonnegative int column) {
            return startPosition(SlotUtils.toSlot(row, column));
        }

        /**
         * Using this method, you can decide for yourself how the items should be placed in the inventory.
         * <p>
         * This method has higher priority than #startPosition and #type. If both are used, this method is preferred.
         *
         * @param builder The {@link SlotIteratorPattern} to use.
         * @return The Builder object itself.
         */
        public @NotNull Builder withPattern(@NotNull SlotIteratorPattern builder) {
            this.slotIterator.pattern = builder;
            return this;
        }

        /**
         * This tells us whether the items should be placed vertically or horizontally.
         *
         * @param type The type of the iterator.
         * @return The builder object itself.
         */
        public @NotNull Builder type(@NotNull SlotIteratorType type) {
            this.slotIterator.type = type;
            return this;
        }

        /**
         * This is used to overwrite items.
         *
         * @return The Builder object itself.
         */
        public @NotNull Builder override() {
            this.slotIterator.override = true;
            return this;
        }

        public SlotIterator build() {
            if ((this.slotIterator.slot >= this.slotIterator.endPosition) && this.slotIterator.endPosition != -1)
                throw new IllegalArgumentException("The start slot must be smaller than the end slot");

            if (this.slotIterator.slot == -1 && this.slotIterator.pattern == null)
                throw new IllegalArgumentException("The start slot must be set");

            return this.slotIterator;
        }
    }
}
