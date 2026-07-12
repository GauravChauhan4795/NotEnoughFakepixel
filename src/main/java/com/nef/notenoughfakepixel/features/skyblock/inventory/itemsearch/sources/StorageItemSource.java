package com.nef.notenoughfakepixel.features.skyblock.inventory.itemsearch.sources;

import com.nef.notenoughfakepixel.features.skyblock.inventory.itemsearch.sources.ItemContext.TrackedEntry;
import com.nef.notenoughfakepixel.features.skyblock.inventory.storage.ChestStorage;
import com.nef.notenoughfakepixel.features.skyblock.inventory.storage.Storage;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class StorageItemSource implements ItemSource {
    @Override
    public List<TrackedEntry> getAll() {
        List<TrackedEntry> items = new ArrayList<>();
        for (Storage.VirtualEntry entry : Storage.getAll()) {
            if (entry.type != Storage.StorageType.ENDER_CHEST_PAGE && entry.type != Storage.StorageType.BACKPACK) continue;
            ItemContext context = entry.type == Storage.StorageType.ENDER_CHEST_PAGE
                    ? new EnderChestItemContext(entry.index) : new BackpackItemContext(entry.index);
            for (ChestStorage.SavedItem saved : entry.items) {
                ItemStack stack = ChestStorage.deserialize(saved.nbt);
                if (stack != null) items.add(new SingleTrackedEntry(stack, context));
            }
        }
        return items;
    }

    private abstract static class StorageItemContext implements AbstractStorageItemContext {
        final int index;

        StorageItemContext(int index) { this.index = index; }
        @Override public int getIndex() { return index; }
        @Override public ItemSources getSource() { return ItemSources.STORAGE; }
    }

    private static class BackpackItemContext extends StorageItemContext {
        BackpackItemContext(int index) { super(index); }
        @Override public List<String> getLocationLines() { return Collections.singletonList("Backpack " + index); }
        @Override public void open() { Minecraft.getMinecraft().thePlayer.sendChatMessage("/storage " + index); }
    }

    private static class EnderChestItemContext extends StorageItemContext {
        EnderChestItemContext(int index) { super(index); }
        @Override public List<String> getLocationLines() { return Collections.singletonList("Ender Chest Page " + index); }
        @Override public void open() { Minecraft.getMinecraft().thePlayer.sendChatMessage("/echest " + index); }
    }
}
