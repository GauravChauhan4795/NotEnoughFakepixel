package com.nef.notenoughfakepixel.config.features;

import com.google.gson.annotations.Expose;
import com.nef.notenoughfakepixel.config.gui.core.config.annotations.ConfigAccordionId;
import com.nef.notenoughfakepixel.config.gui.core.config.annotations.ConfigEditorAccordion;
import com.nef.notenoughfakepixel.config.gui.core.config.annotations.ConfigEditorBoolean;
import com.nef.notenoughfakepixel.config.gui.core.config.annotations.ConfigOption;

public class Fishing {

    @Expose
    @ConfigOption(name = "Fishing Alert", desc = "Settings for the fishing bite alert.")
    @ConfigEditorAccordion(id = 0)
    public boolean fishingAlertAccordion = false;

    @Expose
    @ConfigOption(name = "Enable Alert", desc = "Activate the fishing alert feature.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean fishingCountdown = true;

    @Expose
    @ConfigOption(name = "Enable Incoming Message", desc = "Show INCOMING when a fish is approaching the bobber.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean fishingIncomingMessage = true;

    @Expose
    @ConfigOption(name = "Enable Bite ETA", desc = "Show a countdown until the fish bites.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean fishingBiteEta = true;

    @Expose
    @ConfigOption(name = "Enable Slug Mode", desc = "Suppresses all alerts for 20s after the bobber enters lava, matching the Slug fish catch requirement.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean fishingSlugMode = false;

    @Expose
    @ConfigOption(name = "Notify Legendary Creatures", desc = "Notify when a legendary creature is caught.")
    @ConfigEditorBoolean
    public boolean fishingLegendaryCreatures = true;

    @Expose
    @ConfigOption(name = "Notify on Trophy Fish", desc = "Notify when a trophy fish is caught.")
    @ConfigEditorBoolean
    public boolean fishingTrophyFish = true;
}
