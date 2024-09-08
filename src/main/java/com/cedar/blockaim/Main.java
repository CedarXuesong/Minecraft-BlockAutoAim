package com.cedar.blockaim;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;

@Mod(modid = Main.MODID, version = Main.VERSION)
public class Main
{
    public static KeyBinding toggleAimKey;
    private ConfigurationFile configurationFile;
    public static final String MODID = "BlockAim";
    public static final String VERSION = "1.1.6";
    public static final String VERSIONCODE = "20240908.2025";
    public static final String CFG_FILE_PATH = "config/cedar/config.cfg";
    @EventHandler
    public void load(FMLInitializationEvent event)
    {
        ConfigurationFile.init(CFG_FILE_PATH);
        toggleAimKey = new KeyBinding("§eAimBlock", 0, "Cedar雪松的模组");
        MinecraftForge.EVENT_BUS.register(new CommandCedarHelper());
        MinecraftForge.EVENT_BUS.register(new KeyInputHandler());
        MinecraftForge.EVENT_BUS.register(new WorldEventHandler());
        ClientRegistry.registerKeyBinding(toggleAimKey);
        ConfigurationFile.reload();
        ClientCommandHandler.instance.registerCommand(new CommandCedarHelper());
        AimSystem aimSystem = new AimSystem();
        aimSystem.start();
    }
    public static KeyBinding getToggleAimKey() {
        return toggleAimKey;
    }

    public static class KeyInputHandler {
        @SubscribeEvent
        public void onKeyInput(InputEvent.KeyInputEvent event) {
            if (toggleAimKey.isPressed()) {
                AimSystem.toggleActive();
            }
        }
    }

    public ConfigurationFile getConfigurationFile() {
        return configurationFile;
    }
}
