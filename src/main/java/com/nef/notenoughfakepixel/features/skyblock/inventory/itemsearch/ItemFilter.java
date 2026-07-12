package com.nef.notenoughfakepixel.features.skyblock.inventory.itemsearch;

import com.nef.notenoughfakepixel.features.skyblock.inventory.itemsearch.sources.ItemMatcher;
import com.nef.notenoughfakepixel.features.skyblock.inventory.itemsearch.sources.ItemContext.TrackedEntry;
import com.nef.notenoughfakepixel.utils.ItemUtils;
import com.nef.notenoughfakepixel.utils.StringUtils;
import net.minecraft.item.ItemStack;

import java.util.Locale;

public interface ItemFilter {

    boolean test(ItemStack stack);

    default boolean test(TrackedEntry entry) {
        return test(entry.getItemStack());
    }

    // Match items against item/enchants/name/etc
    class Reference implements ItemFilter {
        private final ItemStack reference;

        public Reference(ItemStack reference) {
            this.reference = reference;
        }

        public ItemStack getReference() {
            return reference;
        }

        @Override
        public boolean test(ItemStack stack) {
            return ItemMatcher.matches(reference, stack);
        }
    }

    // Match items display name or lore with search string
    class Text implements ItemFilter {
        private final String query;

        public Text(String query) {
            this.query = query.trim().toLowerCase(Locale.ROOT);
        }

        @Override
        public boolean test(ItemStack stack) {
            if (query.isEmpty()) return true;
            if (StringUtils.stripFormattingFast(stack.getDisplayName()).toLowerCase(Locale.ROOT).contains(query)) return true;
            for (String line : ItemUtils.getLoreLines(stack)) {
                if (StringUtils.stripFormattingFast(line).toLowerCase(Locale.ROOT).contains(query)) return true;
            }
            return false;
        }
    }
}