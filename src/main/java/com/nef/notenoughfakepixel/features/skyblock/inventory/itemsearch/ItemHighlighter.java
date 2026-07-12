package com.nef.notenoughfakepixel.features.skyblock.inventory.itemsearch;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.mixin.accesors.AccessorGuiContainer;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.ChatUtils;
import com.nef.notenoughfakepixel.utils.RenderUtils;
import com.nef.notenoughfakepixel.variables.Area;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

// Item slot highlight + Chest highlights in island
@RegisterEvents
public class ItemHighlighter {

    private static final long CHEST_HIGHLIGHT_MS = 15_000;
    private static final long TIMEOUT_MS = 10_000;
    private static final int CD_TICKS = 20;

    private static final Color CHEST_COLOR = new Color(255, 215, 0, 200);
    private static final int SLOT_TINT_COLOR = 0xB0400000;

    private static final Map<BlockPos, Long> chestExpiries = new LinkedHashMap<>();

    private static final Map<BlockPos, Long> pendingChests = new LinkedHashMap<>();
    private static int arrivalGraceTicksLeft = -1;

    private static ItemFilter activeFilter;

    private ItemHighlighter() {
    }

    public static void requestChestHighlight(Collection<BlockPos> positions) {
        if (positions.isEmpty()) return;

        boolean onIsland = SkyblockData.getCurrentArea() == Area.ISLAND;
        String plural = positions.size() == 1 ? "chest" : "chests";

        if (onIsland) {
            addChests(positions);
            ChatUtils.notifyChat(EnumChatFormatting.GREEN + "Highlighting " + positions.size() + " " + plural + ".");
            return;
        }

        if (Config.feature.inventory.itemSearch.tpToIsland) {
            Minecraft.getMinecraft().thePlayer.sendChatMessage("/is");
            addChestsPending(positions);
            ChatUtils.notifyChat(EnumChatFormatting.GREEN + "Teleporting to your island and highlighting the " + plural + ".");
        } else {
            ChatUtils.notifyChat(EnumChatFormatting.YELLOW + "Item is in "
                    + (positions.size() == 1 ? "an island chest" : positions.size() + " island chests") + ".");
        }
    }

    public static void addChest(BlockPos pos) {
        chestExpiries.put(pos, System.currentTimeMillis() + CHEST_HIGHLIGHT_MS);
    }

    public static void addChests(Collection<BlockPos> positions) {
        for (BlockPos pos : positions) addChest(pos);
    }

    private static void addChestsPending(Collection<BlockPos> positions) {
        long expiresAt = System.currentTimeMillis() + TIMEOUT_MS;
        for (BlockPos pos : positions) pendingChests.put(pos, expiresAt);
        arrivalGraceTicksLeft = -1;
    }

    public static void clearChests() {
        chestExpiries.clear();
    }

    public static void setFilter(ItemFilter filter) {
        activeFilter = filter;
    }

    public static void clearFilter() {
        activeFilter = null;
    }

    public static ItemFilter getFilter() {
        return activeFilter;
    }

    public static int getSlotTintColor() {
        return SLOT_TINT_COLOR;
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (chestExpiries.isEmpty()) return;

        long now = System.currentTimeMillis();
        Iterator<Map.Entry<BlockPos, Long>> it = chestExpiries.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Long> entry = it.next();
            if (now > entry.getValue()) {
                it.remove();
                continue;
            }

            BlockPos pos = entry.getKey();
            RenderUtils.renderBoxAtCoords(
                    pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1,
                    event.partialTicks,
                    CHEST_COLOR,
                    true
            );
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (pendingChests.isEmpty()) return;

        long now = System.currentTimeMillis();
        pendingChests.entrySet().removeIf(entry -> now > entry.getValue());
        if (pendingChests.isEmpty()) {
            arrivalGraceTicksLeft = -1;
            return;
        }

        if (SkyblockData.getCurrentArea() != Area.ISLAND) {
            arrivalGraceTicksLeft = -1;
            return;
        }

        if (arrivalGraceTicksLeft < 0) {
            arrivalGraceTicksLeft = CD_TICKS;
            return;
        }

        if (arrivalGraceTicksLeft > 0) {
            arrivalGraceTicksLeft--;
            return;
        }

        addChests(pendingChests.keySet());
        pendingChests.clear();
        arrivalGraceTicksLeft = -1;
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        clearChests();
        clearFilter();
    }
}

@RegisterEvents
class GuiSlotHighlighter {

    @SubscribeEvent
    public void onDrawScreenPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.gui instanceof GuiContainer)) return;

        ItemFilter filter = ItemHighlighter.getFilter();
        if (filter == null) return;

        GuiContainer gui = (GuiContainer) event.gui;
        AccessorGuiContainer accessor = (AccessorGuiContainer) gui;

        int guiLeft = accessor.getGuiLeft();
        int guiTop = accessor.getGuiTop();

        for (Slot slot : gui.inventorySlots.inventorySlots) {
            if (!slot.getHasStack()) continue;
            if (!filter.test(slot.getStack())) continue;

            int x = guiLeft + slot.xDisplayPosition;
            int y = guiTop + slot.yDisplayPosition;
            Gui.drawRect(x, y, x + 16, y + 16, ItemHighlighter.getSlotTintColor());
        }
    }
}