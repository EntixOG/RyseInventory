package io.github.rysefoxx.inventory.plugin.pagination;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnegative;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaginationData {

    List<Integer> slots = new ArrayList<>();
    List<Integer> pages = new ArrayList<>();

    public PaginationData(@NotNull PaginationData paginationData) {
        this.slots.addAll(paginationData.slots);
        this.pages.addAll(paginationData.pages);
    }

    public @NotNull PaginationData newInstance() {
        return new PaginationData(this);
    }

    public void add(@Nonnegative int slot, @Nonnegative int page) {
        this.slots.add(slot);
        this.pages.add(page);
    }

    public int getFirstSlot() {
        if (this.slots.isEmpty()) return -1;
        return this.slots.remove(0);
    }

    public int getFirstPage() {
        if (this.pages.isEmpty()) return -1;
        return this.pages.remove(0);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        PaginationData paginationData = (PaginationData) object;
        return Objects.equals(slots, paginationData.slots) && Objects.equals(pages, paginationData.pages);
    }
}
