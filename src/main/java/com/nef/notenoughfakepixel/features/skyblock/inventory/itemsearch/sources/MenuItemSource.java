package com.nef.notenoughfakepixel.features.skyblock.inventory.itemsearch.sources;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.features.skyblock.inventory.itemsearch.sources.ItemContext.TrackedEntry;
import com.nef.notenoughfakepixel.features.skyblock.inventory.storage.ChestStorage;
import com.nef.notenoughfakepixel.features.skyblock.inventory.storage.Storage;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.ChatUtils;
import com.nef.notenoughfakepixel.variables.Area;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

enum StorageMenu {
    MUSEUM(Storage.StorageType.MUSEUM, "Museum", "/warp museum"),
    WARDROBE(Storage.StorageType.WARDROBE, "Wardrobe", "/wardrobe"),
    PETS(Storage.StorageType.PETS, "Pets", "/pets"),
    ACCESSORY_BAG(Storage.StorageType.ACCESSORY_BAG, "Accessory Bag", "/accessory");

    final Storage.StorageType storageType;
    final String name;
    final String command;

    StorageMenu(Storage.StorageType storageType, String name, String command) {
        this.storageType = storageType;
        this.name = name;
        this.command = command;
    }
}

final class MenuItemSource implements ItemSource {
    private final StorageMenu menu;

    MenuItemSource(StorageMenu menu) {
        this.menu = menu;
    }

    @Override
    public List<TrackedEntry> getAll() {
        List<TrackedEntry> items = new ArrayList<>();
        for (Storage.VirtualEntry entry : Storage.getAll()) {
            if (entry.type != menu.storageType) continue;
            ItemContext context = new MenuItemContext(menu, entry.displayName);
            for (ChestStorage.SavedItem saved : entry.items) {
                ItemStack stack = ChestStorage.deserialize(saved.nbt);
                if (stack != null) items.add(new SingleTrackedEntry(stack, context));
            }
        }
        return items;
    }

    private static class MenuItemContext implements ItemContext {
        private final StorageMenu menu;
        private final String title;

        private MenuItemContext(StorageMenu menu, String title) {
            this.menu = menu;
            this.title = title;
        }

        @Override public ItemSources getSource() { return ItemSources.valueOf(menu.name()); }
        @Override public List<String> getLocationLines() {
            return Collections.singletonList(title == null || title.equals(menu.name) ? menu.name : title);
        }

        @Override
        public void open() {
            if (menu == StorageMenu.MUSEUM) {
                if (SkyblockData.getCurrentArea() == Area.MUSEUM) return;
                if (!Config.feature.inventory.itemSearch.tpToMuseum) {
                    ChatUtils.notifyChat("§eItem is in your Museum.");
                    return;
                }
            }
            Minecraft.getMinecraft().thePlayer.sendChatMessage(menu.command);
        }
    }
}
