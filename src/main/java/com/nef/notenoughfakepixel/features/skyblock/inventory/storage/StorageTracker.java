package com.nef.notenoughfakepixel.features.skyblock.inventory.storage;

import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Captures the searchable containers currently open on the client. */
@RegisterEvents
public class StorageTracker {
    private static final Pattern BACKPACK = Pattern.compile(".*Backpack (\\d+)/(\\d+)");
    private static final Pattern ENDER_CHEST = Pattern.compile("Ender Chest \\(Page (\\d+)\\)");

    static { Storage.load(); }

    private IInventory activeInventory;
    private ContainerChest activeContainer;
    private Storage.StorageType activeType;
    private int activeIndex;
    private String activeTitle;
    private List<ItemStack> activeItems = Collections.emptyList();

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        if (!(screen instanceof GuiChest)) {
            saveActive();
            return;
        }

        ContainerChest container = (ContainerChest) ((GuiChest) screen).inventorySlots;
        IInventory lower = container.getLowerChestInventory();
        if (lower == activeInventory) {
            captureVisibleSlots();
            return;
        }

        saveActive();
        beginCapture(container, lower);
    }

    private void beginCapture(ContainerChest container, IInventory lower) {
        String title = lower.getDisplayName().getUnformattedText().trim();
        CaptureTarget target = CaptureTarget.fromTitle(title);
        if (target == null) return;

        activeContainer = container;
        activeInventory = lower;
        activeType = target.type;
        activeIndex = target.index;
        activeTitle = title;
        captureVisibleSlots();
    }

    private void saveActive() {
        if (activeInventory == null) return;

        List<ItemStack> items = new ArrayList<>(activeItems);
        if (activeType == Storage.StorageType.MUSEUM) {
            items.removeIf(this::isMuseumPlaceholderDye);
        }
        Storage.saveEntry(activeType, activeIndex, activeTitle, items);
        Storage.save();

        activeInventory = null;
        activeContainer = null;
        activeItems = Collections.emptyList();
    }

    private void captureVisibleSlots() {
        List<ItemStack> items = new ArrayList<>();
        for (Slot slot : activeContainer.inventorySlots) {
            if (slot.inventory == Minecraft.getMinecraft().thePlayer.inventory) continue;
            ItemStack stack = slot.getStack();
            items.add(stack == null ? null : stack.copy());
        }
        activeItems = items;
    }

    private boolean isMuseumPlaceholderDye(ItemStack stack) {
        return stack != null && stack.getItem() == Items.dye
                && (stack.getItemDamage() == 8 || stack.getItemDamage() == 10);
    }

    private static class CaptureTarget {
        final Storage.StorageType type;
        final int index;

        CaptureTarget(Storage.StorageType type, int index) {
            this.type = type;
            this.index = index;
        }

        static CaptureTarget fromTitle(String title) {
            Matcher enderChest = ENDER_CHEST.matcher(title);
            if (enderChest.find()) return new CaptureTarget(Storage.StorageType.ENDER_CHEST_PAGE, Integer.parseInt(enderChest.group(1)));

            Matcher backpack = BACKPACK.matcher(title);
            if (backpack.find()) return new CaptureTarget(Storage.StorageType.BACKPACK, Integer.parseInt(backpack.group(1)));

            if (title.startsWith("Museum ") && (title.endsWith("Armor") || title.endsWith("Weapons")
                    || title.endsWith("Rarities") || title.endsWith("Special Items"))) {
                return new CaptureTarget(Storage.StorageType.MUSEUM, title.hashCode());
            }
            if (title.startsWith("Wardrobe")) return new CaptureTarget(Storage.StorageType.WARDROBE, 0);
            if (title.startsWith("Pets")) return new CaptureTarget(Storage.StorageType.PETS, 0);
            if (title.startsWith("Accessory Bag")) return new CaptureTarget(Storage.StorageType.ACCESSORY_BAG, 0);
            return null;
        }
    }
}
