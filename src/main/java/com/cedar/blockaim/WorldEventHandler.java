package com.cedar.blockaim;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

import static com.cedar.blockaim.ConfigurationFile.cedarConfig;
import static com.cedar.blockaim.Main.*;

public class WorldEventHandler {
    @SubscribeEvent
    public void onPlayerJoinWorld(PlayerEvent.PlayerLoggedInEvent event) {
        event.player.addChatMessage(new ChatComponentText("§eCedar雪松 §r§eBlockAim " + VERSION + " §n" + VERSIONCODE));
    }

    @SubscribeEvent
    public  void onPlayerLeaveWorld(PlayerEvent.PlayerLoggedOutEvent event){
        cedarConfig.addProperty("AimKey",getToggleAimKey().getKeyCode());
        ConfigurationFile.setConfig(cedarConfig);
    }
}
