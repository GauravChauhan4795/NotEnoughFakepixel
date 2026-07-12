package com.nef.notenoughfakepixel.features.skyblock.inventory.storage;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.ConfigHandler;
import net.minecraft.item.ItemStack;

import java.io.File;
import java.util.*;

public class Storage {

    public enum StorageType { ENDER_CHEST_PAGE, BACKPACK, MUSEUM, WARDROBE, PETS, ACCESSORY_BAG }

    public static class VirtualEntry {
        public StorageType type;
        public int index;
        public String displayName;
        public List<ChestStorage.SavedItem> items = new ArrayList<>();
        public long lastUpdated;
    }

    public static class StorageFile {
        public Map<String, Map<String, VirtualEntry>> profiles = new HashMap<>();
    }

    private static StorageFile data = new StorageFile();
    private static final File FILE = new File(Config.configDirectory, "itemsearch_storage.json");

    public static void load() {
        StorageFile loaded = ConfigHandler.loadConfig(StorageFile.class, FILE, ConfigHandler.GSON);
        data = (loaded != null) ? loaded : new StorageFile();
    }

    public static void save() {
        ConfigHandler.saveConfig(data, FILE, ConfigHandler.GSON);
    }

    private static Map<String, VirtualEntry> currentProfileMap() {
        return data.profiles.computeIfAbsent(profileKey(), k -> new HashMap<>());
    }

    private static String profileKey() {
        String p = SkyblockData.getCurrentProfile();
        return (p == null) ? "generic" : p;
    }

    public static void saveEntry(StorageType type, int index, String displayName, List<ItemStack> items) {
        VirtualEntry entry = new VirtualEntry();
        entry.type = type;
        entry.index = index;
        entry.displayName = displayName;
        entry.lastUpdated = System.currentTimeMillis();

        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack == null) continue;
            ChestStorage.SavedItem saved = new ChestStorage.SavedItem();
            saved.slot = i;
            saved.nbt = ChestStorage.serialize(stack);
            if (saved.nbt != null) entry.items.add(saved);
        }

        currentProfileMap().put(key(type, index), entry);
    }

    private static String key(StorageType type, int index) {
        return type.name() + "_" + index;
    }

    public static Collection<VirtualEntry> getAll() {
        return currentProfileMap().values();
    }

    public static int nextIndex(StorageType type) {
        int highest = 0;
        for (VirtualEntry entry : currentProfileMap().values()) {
            if (entry.type == type) highest = Math.max(highest, entry.index);
        }
        return highest + 1;
    }
}
