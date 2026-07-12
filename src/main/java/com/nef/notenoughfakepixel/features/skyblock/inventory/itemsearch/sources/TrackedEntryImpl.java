package com.nef.notenoughfakepixel.features.skyblock.inventory.itemsearch.sources;

import com.nef.notenoughfakepixel.features.skyblock.inventory.itemsearch.ItemHighlighter;
import com.nef.notenoughfakepixel.features.skyblock.inventory.itemsearch.sources.ItemContext.TrackedEntry;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

class SingleTrackedEntry implements TrackedEntry {

    private final ItemStack itemStack;
    private final ItemContext context;

    SingleTrackedEntry(ItemStack itemStack, ItemContext context) {
        this.itemStack = itemStack;
        this.context = context;
    }

    @Override
    public ItemStack getItemStack() {
        return itemStack;
    }

    @Override
    public ItemContext getContext() {
        return context;
    }

    @Override
    public int getCount() {
        return itemStack.stackSize;
    }

    @Override
    public TrackedEntry add(TrackedEntry other) {
        return new TrackedEntryBundle(this, other);
    }
}

class TrackedEntryBundle implements TrackedEntry {

    private final List<TrackedEntry> items = new ArrayList<>();
    private final ItemStack displayStack;
    private final BundleItemContext context = new BundleItemContext();

    TrackedEntryBundle(TrackedEntry first, TrackedEntry second) {
        this.displayStack = first.getItemStack();
        addInternal(first);
        addInternal(second);
    }

    private void addInternal(TrackedEntry entry) {
        items.add(entry);
        context.add(entry);
    }

    @Override
    public ItemStack getItemStack() {
        ItemStack copy = displayStack.copy();
        copy.stackSize = getCount();
        return copy;
    }

    @Override
    public ItemContext getContext() {
        return context;
    }

    @Override
    public int getCount() {
        int total = 0;
        for (TrackedEntry trackedEntry : this.items) total += trackedEntry.getCount();
        return total;
    }

    @Override
    public TrackedEntry add(TrackedEntry other) {
        addInternal(other);
        return this;
    }

    public List<TrackedEntry> getItems() {
        return items;
    }

    private static class BundleItemContext implements ItemContext {

        private final Map<ItemSources, Integer> sourceCounts = new LinkedHashMap<>();
        private final Map<BlockPos, ChestPosContext> chests = new LinkedHashMap<>();
        private AbstractStorageItemContext storageContext;
        private final Map<ItemSources, ItemContext> fallbackContexts = new LinkedHashMap<>();

        void add(TrackedEntry entry) {
            ItemContext context = entry.getContext();
            sourceCounts.merge(context.getSource(), entry.getCount(), Integer::sum);

            if (context instanceof ChestPosContext) {
                ChestPosContext chestContext = (ChestPosContext) context;
                chests.putIfAbsent(chestContext.getPos(), chestContext);
                return;
            }

            if (context instanceof AbstractStorageItemContext) {
                mergeStorageContext((AbstractStorageItemContext) context);
                return;
            }

            fallbackContexts.putIfAbsent(context.getSource(), context);
        }

        private void mergeStorageContext(AbstractStorageItemContext incoming) {
            if (storageContext == null || incoming.getIndex() < storageContext.getIndex()) {
                storageContext = incoming;
            }
        }

        List<BlockPos> getChestPositions() {
            return Collections.unmodifiableList(new ArrayList<>(chests.keySet()));
        }

        private ItemSources dominantSource() {
            ItemSources best = null;
            int bestPriority = Integer.MAX_VALUE;
            for (Map.Entry<ItemSources, Integer> entry : sourceCounts.entrySet()) {
                int priority = sourcePriority(entry.getKey());
                if (priority < bestPriority) {
                    bestPriority = priority;
                    best = entry.getKey();
                }
            }
            return best != null ? best : ItemSources.BUNDLE;
        }

        private int sourcePriority(ItemSources source) {
            switch (source) {
                case STORAGE: return 0;
                case CHEST: return 1;
                case MUSEUM: return 2;
                case WARDROBE: return 3;
                case PETS: return 4;
                case ACCESSORY_BAG: return 5;
                case INVENTORY: return 6;
                default: return Integer.MAX_VALUE;
            }
        }

        @Override
        public ItemSources getSource() {
            return dominantSource();
        }

        @Override
        public List<String> getLocationLines() {
            List<Map.Entry<ItemSources, Integer>> sorted = new ArrayList<>(sourceCounts.entrySet());
            sorted.sort((a, b) -> Integer.compare(sourcePriority(a.getKey()), sourcePriority(b.getKey())));

            List<String> lines = new ArrayList<>();
            for (Map.Entry<ItemSources, Integer> entry : sorted) {
                lines.add(prettyName(entry.getKey()) + ": " + entry.getValue());
            }
            if (chests.size() > 1) {
                lines.add(chests.size() + " chests tracked");
            }
            return lines;
        }

        private String prettyName(ItemSources source) {
            String name = source.name().toLowerCase(Locale.ROOT).replace('_', ' ');
            return Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }

        @Override
        public void open() {
            ItemSources dominant = dominantSource();

            if (dominant == ItemSources.CHEST && !chests.isEmpty()) {
                ItemHighlighter.requestChestHighlight(chests.keySet());
                return;
            }

            if (dominant == ItemSources.STORAGE && storageContext != null) {
                storageContext.open();
                return;
            }

            ItemContext fallback = fallbackContexts.get(dominant);
            if (fallback != null) fallback.open();
        }
    }
}
