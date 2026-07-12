package com.nef.notenoughfakepixel.features.skyblock.inventory.storage;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.utils.ItemUtils;
import com.nef.notenoughfakepixel.utils.StringUtils;
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

@RegisterEvents
public class StorageTracker {
    private static final Pattern BACKPACK = Pattern.compile(".*Backpack (\\d+)/(\\d+)");
    private static final Pattern ENDER_CHEST = Pattern.compile("Ender Chest \\(Page (\\d+)\\)");
    private static final Pattern PAGE = Pattern.compile("Page\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final int ROW_WIDTH = 9;

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

        if (!Config.feature.inventory.itemSearchEnabled) {
            discardActive();
            return;
        }

        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        if (!(screen instanceof GuiChest)) {
            saveActive();
            return;
        }

        ContainerChest container = (ContainerChest) ((GuiChest) screen).inventorySlots;
        IInventory lower = container.getLowerChestInventory();
        if (lower == activeInventory) {
            updateActivePage();
            return;
        }

        String title = lower.getDisplayName().getUnformattedText().trim();
        CaptureTarget target = CaptureTarget.fromTitle(title);

        if (sameSession(target, title)) {
            activeContainer = container;
            activeInventory = lower;
            activeTitle = title;
            updateActivePage();
            return;
        }

        saveActive();
        beginCapture(container, lower);
    }

    private boolean sameSession(CaptureTarget target, String title) {
        if (target == null || activeInventory == null) return false;
        if (target.type != activeType) return false;
        if (target.isPaged()) return true;
        return title.equals(activeTitle);
    }

    private void beginCapture(ContainerChest container, IInventory lower) {
        String title = lower.getDisplayName().getUnformattedText().trim();
        CaptureTarget target = CaptureTarget.fromTitle(title);
        if (target == null) return;

        activeContainer = container;
        activeInventory = lower;
        activeType = target.type;
        activeIndex = target.isPaged() ? computeCurrentPage(container, lower) : target.index;
        activeTitle = title;
        activeItems = getVisibleSlots();
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

    private void discardActive() {
        activeInventory = null;
        activeContainer = null;
        activeItems = Collections.emptyList();
    }

    private void updateActivePage() {
        List<ItemStack> items = getVisibleSlots();

        if (isEffectivelyEmpty(items)) {
            return;
        }

        boolean paged = activeType == Storage.StorageType.PETS || activeType == Storage.StorageType.ACCESSORY_BAG;
        if (paged) {
            int currentPage = computeCurrentPage(activeContainer, activeInventory);
            if (currentPage != activeIndex) {
                switchPage(currentPage, items);
                return;
            }
        }

        activeItems = items;
    }

    private int countNonNull(List<ItemStack> items) {
        int c = 0;
        for (ItemStack s : items) if (s != null) c++;
        return c;
    }

    private boolean isEffectivelyEmpty(List<ItemStack> items) {
        for (ItemStack stack : items) {
            if (stack != null) return false;
        }
        return true;
    }

    private void switchPage(int index, List<ItemStack> items) {
        if (index == activeIndex) return;
        Storage.saveEntry(activeType, activeIndex, activeTitle, activeItems);
        Storage.save();
        activeIndex = index;
        activeItems = items;
    }

    private List<ItemStack> getVisibleSlots() {
        List<ItemStack> items = new ArrayList<>();
        for (Slot slot : activeContainer.inventorySlots) {
            if (slot.inventory == Minecraft.getMinecraft().thePlayer.inventory) continue;
            ItemStack stack = slot.getStack();
            items.add(stack == null ? null : stack.copy());
        }
        return items;
    }

    private int computeCurrentPage(ContainerChest container, IInventory lower) {
        int lastSlot = lower.getSizeInventory() - 1;
        int firstSlotLastRow = lower.getSizeInventory() - ROW_WIDTH;

        ItemStack next = getStackAt(container, lower, lastSlot);
        String nextName = next == null ? "<null>" : StringUtils.stripFormattingFast(next.getDisplayName());
        if (next != null && nextName.toLowerCase().startsWith("next")) {
            Integer target = readPageNumberFromLore(next);
            if (target != null) return target - 1;
        }

        ItemStack previous = getStackAt(container, lower, firstSlotLastRow);
        String prevName = previous == null ? "<null>" : StringUtils.stripFormattingFast(previous.getDisplayName());
        if (previous != null) {
            String name = prevName.toLowerCase();
            if (name.startsWith("previous")) {
                Integer target = readPageNumberFromLore(previous);
                if (target != null) return target + 1;
            }
        }

        return 1;
    }

    private ItemStack getStackAt(ContainerChest container, IInventory lower, int slotIndex) {
        for (Slot slot : container.inventorySlots) {
            if (slot.inventory != lower || slot.getSlotIndex() != slotIndex) continue;
            return slot.getStack();
        }
        return null;
    }

    private Integer readPageNumberFromLore(ItemStack stack) {
        List<String> lore = ItemUtils.getLoreLines(stack);
        for (String line : lore) {
            String stripped = StringUtils.stripFormattingFast(line);
            Matcher m = PAGE.matcher(stripped);
            boolean found = m.find();
            if (found) return Integer.parseInt(m.group(1));
        }
        return null;
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

        boolean isPaged() {
            return type == Storage.StorageType.PETS || type == Storage.StorageType.ACCESSORY_BAG;
        }
    }
}