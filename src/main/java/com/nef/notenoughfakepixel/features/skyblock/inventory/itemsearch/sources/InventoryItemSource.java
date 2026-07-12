package com.nef.notenoughfakepixel.features.skyblock.inventory.itemsearch.sources;

import com.nef.notenoughfakepixel.features.skyblock.inventory.itemsearch.sources.ItemContext.TrackedEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class InventoryItemSource implements ItemSource {
    private static final ItemContext CONTEXT = new InventoryItemContext();

    @Override
    public List<TrackedEntry> getAll() {
        List<TrackedEntry> items = new ArrayList<>();
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return items;

        InventoryPlayer inventory = mc.thePlayer.inventory;
        addAll(items, inventory.mainInventory);
        addAll(items, inventory.armorInventory);
        return items;
    }

    private void addAll(List<TrackedEntry> items, ItemStack[] stacks) {
        for (ItemStack stack : stacks) {
            if (stack != null) items.add(new SingleTrackedEntry(stack, CONTEXT));
        }
    }

    private static class InventoryItemContext implements ItemContext {
        @Override public ItemSources getSource() { return ItemSources.INVENTORY; }
        @Override public List<String> getLocationLines() { return Collections.singletonList("Your inventory"); }
        @Override public void open() {
            Minecraft mc = Minecraft.getMinecraft();
            mc.displayGuiScreen(new GuiInventory(mc.thePlayer));
        }
    }
}
