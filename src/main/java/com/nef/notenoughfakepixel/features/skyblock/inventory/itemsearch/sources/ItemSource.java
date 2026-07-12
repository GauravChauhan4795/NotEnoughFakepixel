package com.nef.notenoughfakepixel.features.skyblock.inventory.itemsearch.sources;

import com.nef.notenoughfakepixel.features.skyblock.inventory.itemsearch.sources.ItemContext.TrackedEntry;
import net.minecraft.util.BlockPos;

import java.util.List;

interface ItemSource {
    List<TrackedEntry> getAll();
}

interface ChestPosContext extends ItemContext {
    BlockPos getPos();
}

interface AbstractStorageItemContext extends ItemContext {
    int getIndex();
}
