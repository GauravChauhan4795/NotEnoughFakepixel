package com.nef.notenoughfakepixel.config.gui.commands;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.config.gui.config.ConfigEditor;
import com.nef.notenoughfakepixel.config.gui.core.GuiScreenElementWrapper;
import com.nef.notenoughfakepixel.env.registers.RegisterCommand;
import com.nef.notenoughfakepixel.utils.ListUtils;
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor;
import io.github.notenoughupdates.moulconfig.processor.ConfigProcessorDriver;
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@RegisterCommand
public class NefCommand extends SimpleCommand {

    @Override
    public String getName() {
        return "nef";
    }

    @Override
    public String getUsage() {
        return "/nef <category?>";
    }

    @Override
    public List<String> getAliases() {
        return ListUtils.of("notenoughfakepixel");
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            Config.screenToOpen = new GuiScreenElementWrapper(new ConfigEditor(Config.feature));
            return;
        }

        String category = StringUtils.join(args, " ");
        if ("fishing".equalsIgnoreCase(category)) {
            MoulConfigProcessor<com.nef.notenoughfakepixel.Configuration> processor = MoulConfigProcessor.withDefaults(Config.feature);
            new ConfigProcessorDriver(processor).processConfig(Config.feature);
            Config.screenToOpen = new io.github.notenoughupdates.moulconfig.gui.GuiScreenElementWrapper(
                new MoulConfigEditor<>(processor)
            );
            return;
        }

        Config.screenToOpen = new GuiScreenElementWrapper(new ConfigEditor(Config.feature, category));
    }
}
