package com.nef.notenoughfakepixel.features.skyblock.inventory.itemsearch.sources;

import net.minecraft.item.ItemStack;

import java.util.List;

public interface ItemContext {

    ItemSources getSource();

    List<String> getLocationLines();

    void open();

    interface TrackedEntry {

        ItemStack getItemStack();

        ItemContext getContext();

        int getCount();

        TrackedEntry add(TrackedEntry other);
    }
}