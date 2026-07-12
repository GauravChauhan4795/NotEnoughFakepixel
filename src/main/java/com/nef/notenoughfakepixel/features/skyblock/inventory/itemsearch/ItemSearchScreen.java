package com.nef.notenoughfakepixel.features.skyblock.inventory.itemsearch;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.features.skyblock.inventory.itemsearch.sources.ItemContext.TrackedEntry;
import com.nef.notenoughfakepixel.features.skyblock.inventory.itemsearch.sources.ItemSources;
import com.nef.notenoughfakepixel.utils.KeybindHelper;
import com.nef.notenoughfakepixel.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ItemSearchScreen extends GuiScreen {

    private static final ResourceLocation BACKGROUND_TEXTURE =
            new ResourceLocation("textures/gui/container/generic_54.png");

    private static final int SLOT_UV_X = 7;
    private static final int SLOT_UV_Y = 17;

    private static final int SLOT_SIZE = 18;
    private static final int COLUMNS = 15;
    private static final int ROWS = 6;
    private static final int BORDER = 7;
    private static final int PANEL_COLOR = 0xFFC6C6C6;
    private static final int SEARCH_BOX_WIDTH = 120;
    private static final int SEARCH_BOX_HEIGHT = 16;
    private static final int SCROLLBAR_WIDTH = 4;

    private GuiTextField searchBox;
    private final List<TrackedEntry> allItems = new ArrayList<>();
    private List<TrackedEntry> filtered = new ArrayList<>();
    private int hoveredIndex = -1;
    private int scrollOffsetRows = 0;
    private boolean draggingScrollbar = false;

    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private int gridX;
    private int gridY;
    private int scrollbarX;

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        allItems.clear();
        allItems.addAll(ItemSources.getAllItems());

        panelWidth = BORDER * 2 + COLUMNS * SLOT_SIZE;
        panelHeight = BORDER * 2 + ROWS * SLOT_SIZE;

        panelLeft = (width - panelWidth) / 2;
        panelTop = (height - panelHeight) / 2;

        gridX = panelLeft + BORDER;
        gridY = panelTop + BORDER;
        scrollbarX = panelLeft + panelWidth - BORDER + 1;

        searchBox = new GuiTextField(0, fontRendererObj,
                panelLeft + panelWidth - SEARCH_BOX_WIDTH, panelTop - SEARCH_BOX_HEIGHT - 4,
                SEARCH_BOX_WIDTH, SEARCH_BOX_HEIGHT);
        searchBox.setFocused(true);
        searchBox.setMaxStringLength(64);

        refilter();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    private void refilter() {
        ItemFilter textFilter = new ItemFilter.Text(searchBox.getText());
        List<TrackedEntry> results = new ArrayList<>();
        for (TrackedEntry entry : allItems) {
            if (textFilter.test(entry)) results.add(entry);
        }
        filtered = results;
        scrollOffsetRows = 0;
    }

    private int totalRows() {
        return (filtered.size() + COLUMNS - 1) / COLUMNS;
    }

    private int maxScrollOffsetRows() {
        return Math.max(0, totalRows() - ROWS);
    }

    private void scrollBy(int rows) {
        scrollOffsetRows = Math.max(0, Math.min(maxScrollOffsetRows(), scrollOffsetRows + rows));
    }

    private int scrollbarTrackHeight() {
        return ROWS * SLOT_SIZE;
    }

    private int scrollbarThumbHeight() {
        float visibleRatio = Math.min(1.0F, ROWS / (float) totalRows());
        return Math.max(8, Math.round(scrollbarTrackHeight() * visibleRatio));
    }

    private void updateScrollFromMouseY(int mouseY) {
        int maxOffset = maxScrollOffsetRows();
        if (maxOffset <= 0) return;

        int thumbHeight = scrollbarThumbHeight();
        int thumbTravel = scrollbarTrackHeight() - thumbHeight;
        if (thumbTravel <= 0) return;

        float proportion = (mouseY - gridY - thumbHeight / 2F) / thumbTravel;
        proportion = Math.max(0F, Math.min(1F, proportion));

        scrollOffsetRows = Math.round(proportion * maxOffset);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            scrollBy(wheel < 0 ? 1 : -1);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        Gui.drawRect(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, PANEL_COLOR);

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(BACKGROUND_TEXTURE);
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int x = gridX + col * SLOT_SIZE;
                int y = gridY + row * SLOT_SIZE;
                this.drawTexturedModalRect(x, y, SLOT_UV_X, SLOT_UV_Y, SLOT_SIZE, SLOT_SIZE);
            }
        }

        searchBox.drawTextBox();
        drawScrollbar(mouseX, mouseY);

        hoveredIndex = -1;
        int index = scrollOffsetRows * COLUMNS;
        for (int row = 0; row < ROWS && index < filtered.size(); row++) {
            int itemsInRow = Math.min(COLUMNS, filtered.size() - index);
            int startCol = (COLUMNS - itemsInRow) / 2;

            for (int col = 0; col < itemsInRow; col++) {
                int i = index + col;
                int x = gridX + (startCol + col) * SLOT_SIZE + 1;
                int y = gridY + row * SLOT_SIZE + 1;

                boolean hovered = mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
                if (hovered) {
                    hoveredIndex = i;
                    Gui.drawRect(x - 1, y - 1, x + 17, y + 17, 0x80FFFFFF);
                }
                RenderUtils.drawItemStack(filtered.get(i).getItemStack(), x, y);
            }

            index += itemsInRow;
        }

        if (hoveredIndex != -1) {
            drawItemTooltip(filtered.get(hoveredIndex), mouseX, mouseY);
        }
    }

    private void drawItemTooltip(TrackedEntry entry, int mouseX, int mouseY) {
        ItemStack stack = entry.getItemStack();
        List<String> tooltip = new ArrayList<>(stack.getTooltip(mc.thePlayer, false));

        tooltip.add("");
        tooltip.add(EnumChatFormatting.GRAY + "Amount: " + EnumChatFormatting.WHITE + entry.getCount());
        for (String line : entry.getContext().getLocationLines()) {
            tooltip.add(EnumChatFormatting.GRAY + line);
        }

        this.drawHoveringText(tooltip, mouseX, mouseY);
    }

    private void drawScrollbar(int mouseX, int mouseY) {
        int maxOffset = maxScrollOffsetRows();
        if (maxOffset <= 0) return;

        int trackTop = gridY;
        int trackHeight = scrollbarTrackHeight();
        Gui.drawRect(scrollbarX, trackTop, scrollbarX + SCROLLBAR_WIDTH, trackTop + trackHeight, 0xFF8B8B8B);

        int thumbHeight = scrollbarThumbHeight();
        int thumbTravel = trackHeight - thumbHeight;
        int thumbY = trackTop + Math.round(thumbTravel * (scrollOffsetRows / (float) maxOffset));

        boolean hovered = mouseX >= scrollbarX && mouseX < scrollbarX + SCROLLBAR_WIDTH
                && mouseY >= trackTop && mouseY < trackTop + trackHeight;

        if (draggingScrollbar || hovered) {
            int glow = 2;
            Gui.drawRect(scrollbarX - glow, thumbY - glow,
                    scrollbarX + SCROLLBAR_WIDTH + glow, thumbY + thumbHeight + glow, 0x50FFFFFF);
        }

        Gui.drawRect(scrollbarX, thumbY, scrollbarX + SCROLLBAR_WIDTH, thumbY + thumbHeight, 0xFFFFFFFF);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
            return;
        }
        searchBox.textboxKeyTyped(typedChar, keyCode);
        refilter();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton == 0 && maxScrollOffsetRows() > 0
                && mouseX >= scrollbarX && mouseX < scrollbarX + SCROLLBAR_WIDTH
                && mouseY >= gridY && mouseY < gridY + scrollbarTrackHeight()) {
            draggingScrollbar = true;
            updateScrollFromMouseY(mouseY);
            return;
        }

        searchBox.mouseClicked(mouseX, mouseY, mouseButton);

        if (hoveredIndex != -1 && mouseButton == 0) {
            TrackedEntry entry = filtered.get(hoveredIndex);
            ItemHighlighter.setFilter(new ItemFilter.Reference(entry.getItemStack()));
            entry.getContext().open();
            if (mc.currentScreen == this) {
                mc.displayGuiScreen(null);
            }
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (draggingScrollbar && clickedMouseButton == 0) {
            updateScrollFromMouseY(mouseY);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        draggingScrollbar = false;
    }
}

@RegisterEvents
class ItemSearchKeybind {

    private boolean keyHeld;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (!Config.feature.inventory.itemSearchEnabled) {
            keyHeld = false;
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            keyHeld = false;
            return;
        }

        boolean keyDown = KeybindHelper.isKeyDown(Config.feature.inventory.itemSearch.openItemSearchKey);
        if (keyDown && !keyHeld && mc.currentScreen == null) {
            mc.displayGuiScreen(new ItemSearchScreen());
        }
        keyHeld = keyDown;
    }
}