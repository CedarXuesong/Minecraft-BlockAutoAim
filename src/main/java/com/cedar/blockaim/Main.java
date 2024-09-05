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

import static com.cedar.blockaim.ConfigurationFile.cedarConfig;

@Mod(modid = Main.MODID, version = Main.VERSION)
public class Main
{
    public static KeyBinding toggleAimKey;
    private ConfigurationFile configurationFile;
    public static final String MODID = "BlockAim";
    public static final String VERSION = "1.1";
    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        ConfigurationFile.init("config/cedar/config.cfg");
        toggleAimKey = new KeyBinding("§eAimBlock", 0, "§l§a雪松BlockAim");
        MinecraftForge.EVENT_BUS.register(new CommandCedarHelper());
        MinecraftForge.EVENT_BUS.register(new KeyInputHandler());
        MinecraftForge.EVENT_BUS.register(new WorldEventHandler());
        ClientRegistry.registerKeyBinding(toggleAimKey);
        ConfigurationFile.reload();//重载配置
        ClientCommandHandler.instance.registerCommand(new CommandCedarHelper());
        AimSystem aimSystem = new AimSystem(ConfigurationFile.BlockIDs);
        Thread aimSystemThread = new Thread(aimSystem);
        aimSystemThread.start();
    }
    public static KeyBinding getToggleAimKey() {
        return toggleAimKey;
    }

    public static class KeyInputHandler {
        @SubscribeEvent
        public void onKeyInput(InputEvent.KeyInputEvent event) {
            if (toggleAimKey.isPressed()) {
                AimSystem.toggleActive(); // Call method to toggle AimSystem
            }
        }
    }

    public ConfigurationFile getConfigurationFile() {
        return configurationFile;
    }
}
