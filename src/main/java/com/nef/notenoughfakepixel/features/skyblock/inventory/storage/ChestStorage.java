package com.nef.notenoughfakepixel.features.skyblock.inventory.storage;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.ConfigHandler;
import com.nef.notenoughfakepixel.variables.Location;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class ChestStorage {

    public static class SavedItem {
        public int slot;
        public String nbt;
    }

    public static class ChestEntry {
        public int x, y, z;
        public boolean hasSecond;
        public int x2, y2, z2;
        public List<SavedItem> items = new ArrayList<>();
        public long lastUpdated;
    }

    public static class ChestStorageFile {
        public Map<String, Map<String, ChestEntry>> profiles = new HashMap<>();
    }

    private static ChestStorageFile data = new ChestStorageFile();
    private static final File FILE = new File(Config.configDirectory, "itemsearch_chests.json");

    public static void load() {
        ChestStorageFile loaded = ConfigHandler.loadConfig(ChestStorageFile.class, FILE, ConfigHandler.GSON);
        data = (loaded != null) ? loaded : new ChestStorageFile();
    }

    public static void save() {
        ConfigHandler.saveConfig(data, FILE, ConfigHandler.GSON);
    }

    private static Map<String, ChestEntry> currentProfileMap() {
        return data.profiles.computeIfAbsent(profileKey(), k -> new HashMap<>());
    }

    private static String profileKey() {
        String p = SkyblockData.getCurrentProfile();
        return (p == null) ? "generic" : p;
    }

    private static String posKey(int x, int y, int z) {
        return x + "," + y + "," + z;
    }

    public static void saveChest(int x, int y, int z, Integer x2, Integer y2, Integer z2, List<ItemStack> items) {
        ChestEntry entry = new ChestEntry();
        entry.x = x; entry.y = y; entry.z = z;
        if (x2 != null) {
            entry.hasSecond = true;
            entry.x2 = x2; entry.y2 = y2; entry.z2 = z2;
        }
        entry.lastUpdated = System.currentTimeMillis();

        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack == null) continue;
            SavedItem saved = new SavedItem();
            saved.slot = i;
            saved.nbt = serialize(stack);
            if (saved.nbt != null) entry.items.add(saved);
        }

        currentProfileMap().put(posKey(x, y, z), entry);
    }

    public static Collection<ChestEntry> getAllChests() {
        return currentProfileMap().values();
    }

    public static String serialize(ItemStack stack) {
        try {
            NBTTagCompound tag = new NBTTagCompound();
            stack.writeToNBT(tag);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            CompressedStreamTools.writeCompressed(tag, out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            return null;
        }
    }

    public static ItemStack deserialize(String base64) {
        if (base64 == null) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            NBTTagCompound tag = CompressedStreamTools.readCompressed(new ByteArrayInputStream(bytes));
            return ItemStack.loadItemStackFromNBT(tag);
        } catch (IOException e) {
            return null;
        }
    }
}

@RegisterEvents
class ChestTracker {

    static { ChestStorage.load(); }

    private static BlockPos pendingPos;
    private static BlockPos pendingSecondPos;
    private static long pendingTime;

    private static IInventory activeInventory;
    private static BlockPos activePos;
    private static BlockPos activeSecondPos;

    private GuiScreen lastScreen;

    private static boolean isChestBlock(Block block) {
        return block == Blocks.chest || block == Blocks.trapped_chest;
    }

    @SubscribeEvent
    public void onInteract(PlayerInteractEvent event) {
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return;
        if (SkyblockData.getCurrentLocation() != Location.PRIVATE_ISLAND) return;

        World world = event.world;
        BlockPos pos = event.pos;
        Block block = world.getBlockState(pos).getBlock();
        if (!isChestBlock(block)) return;

        pendingPos = pos;
        pendingSecondPos = findNeighbourChest(world, pos, block);
        pendingTime = System.currentTimeMillis();
    }

    private BlockPos findNeighbourChest(World world, BlockPos pos, Block block) {
        BlockPos[] neighbors = { pos.north(), pos.south(), pos.east(), pos.west() };
        for (BlockPos n : neighbors) {
            if (world.getBlockState(n).getBlock() == block) return n;
        }
        return null;
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (pendingPos == null) return;
        if (System.currentTimeMillis() - pendingTime > 1000) {
            pendingPos = null;
            pendingSecondPos = null;
            return;
        }
        if (!(event.gui instanceof GuiChest)) return;

        ContainerChest container = (ContainerChest) ((GuiChest) event.gui).inventorySlots;
        activeInventory = container.getLowerChestInventory();
        activePos = pendingPos;
        activeSecondPos = pendingSecondPos;

        pendingPos = null;
        pendingSecondPos = null;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        GuiScreen current = Minecraft.getMinecraft().currentScreen;
        if (lastScreen instanceof GuiChest && !(current instanceof GuiChest)) {
            saveActiveChest();
        }
        lastScreen = current;
    }

    private void saveActiveChest() {
        if (activeInventory == null || activePos == null) return;

        int size = activeInventory.getSizeInventory();
        List<ItemStack> firstHalf = new ArrayList<>();
        List<ItemStack> secondHalf = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            ItemStack stack = activeInventory.getStackInSlot(i);
            if (i < 27) firstHalf.add(stack);
            else secondHalf.add(stack);
        }

        ChestStorage.saveChest(
                activePos.getX(), activePos.getY(), activePos.getZ(),
                activeSecondPos != null ? activeSecondPos.getX() : null,
                activeSecondPos != null ? activeSecondPos.getY() : null,
                activeSecondPos != null ? activeSecondPos.getZ() : null,
                firstHalf
        );

        if (activeSecondPos != null && !secondHalf.isEmpty()) {
            ChestStorage.saveChest(
                    activeSecondPos.getX(), activeSecondPos.getY(), activeSecondPos.getZ(),
                    activePos.getX(), activePos.getY(), activePos.getZ(),
                    secondHalf
            );
        }

        ChestStorage.save();

        activeInventory = null;
        activePos = null;
        activeSecondPos = null;
    }
}