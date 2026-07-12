package com.nef.notenoughfakepixel.config.features;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Category;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import org.lwjgl.input.Keyboard;

public class Inventory {

    @Expose
    @Category(name = "Item Search", desc = "Item Search settings.")
    public ItemSearch itemSearch = new ItemSearch();

    @Expose
    @ConfigOption(name = "Item Search", desc = "Enable Item Search.")
    public boolean itemSearchEnabled = true;

    public static class ItemSearch {

        @Expose
        @ConfigOption(name = "Open Item Search Key", desc = "Keybind for opening the item search screen.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_O)
        public int openItemSearchKey = Keyboard.KEY_O;

        @Expose
        @ConfigOption(name = "Teleport to Island", desc = "Automatically teleport to your island when locating an item stored in an island chest. If disabled, sends a chat message instead.")
        @ConfigEditorBoolean
        public boolean tpToIsland = true;

        @Expose
        @ConfigOption(name = "Teleport to Museum", desc = "Automatically teleport to the Museum when locating a donated item. If disabled, sends a chat message instead.")
        @ConfigEditorBoolean
        public boolean tpToMuseum = true;
    }
}
