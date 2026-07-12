package com.nef.notenoughfakepixel.features.skyblock.inventory.itemsearch.sources;

import com.nef.notenoughfakepixel.features.skyblock.inventory.itemsearch.sources.ItemContext.TrackedEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Source registry and item de-duplication for the item search screen. */
public enum ItemSources {

    CHEST(new ChestItemSource()),
    STORAGE(new StorageItemSource()),
    MUSEUM(new MenuItemSource(StorageMenu.MUSEUM)),
    WARDROBE(new MenuItemSource(StorageMenu.WARDROBE)),
    PETS(new MenuItemSource(StorageMenu.PETS)),
    ACCESSORY_BAG(new MenuItemSource(StorageMenu.ACCESSORY_BAG)),
    INVENTORY(new InventoryItemSource()),
    BUNDLE(() -> Collections.emptyList());

    private final ItemSource source;

    ItemSources(ItemSource source) {
        this.source = source;
    }

    public static List<TrackedEntry> getAllItems() {
        List<TrackedEntry> merged = new ArrayList<>();
        for (ItemSources source : values()) {
            for (TrackedEntry entry : source.source.getAll()) {
                merge(merged, entry);
            }
        }
        return merged;
    }

    private static void merge(List<TrackedEntry> merged, TrackedEntry incoming) {
        for (int i = 0; i < merged.size(); i++) {
            TrackedEntry existing = merged.get(i);
            if (ItemMatcher.matches(existing.getItemStack(), incoming.getItemStack())) {
                merged.set(i, existing.add(incoming));
                return;
            }
        }
        merged.add(incoming);
    }
}
