package com.nef.notenoughfakepixel.features.skyblock.inventory.itemsearch.sources;

import com.nef.notenoughfakepixel.features.skyblock.inventory.itemsearch.ItemHighlighter;
import com.nef.notenoughfakepixel.features.skyblock.inventory.itemsearch.sources.ItemContext.TrackedEntry;
import com.nef.notenoughfakepixel.features.skyblock.inventory.storage.ChestStorage;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class ChestItemSource implements ItemSource {
    @Override
    public List<TrackedEntry> getAll() {
        List<TrackedEntry> items = new ArrayList<>();
        for (ChestStorage.ChestEntry entry : ChestStorage.getAllChests()) {
            ItemContext context = new ChestItemContext(new BlockPos(entry.x, entry.y, entry.z));
            for (ChestStorage.SavedItem saved : entry.items) {
                ItemStack stack = ChestStorage.deserialize(saved.nbt);
                if (stack != null) items.add(new SingleTrackedEntry(stack, context));
            }
        }
        return items;
    }

    private static class ChestItemContext implements ChestPosContext {
        private final BlockPos pos;

        private ChestItemContext(BlockPos pos) {
            this.pos = pos;
        }

        @Override public BlockPos getPos() { return pos; }
        @Override public ItemSources getSource() { return ItemSources.CHEST; }
        @Override public List<String> getLocationLines() { return Collections.singletonList("Island Chest"); }
        @Override public void open() { ItemHighlighter.requestChestHighlight(Collections.singletonList(pos)); }
    }
}
