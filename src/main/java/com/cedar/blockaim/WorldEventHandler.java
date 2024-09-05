package com.cedar.blockaim;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

import static com.cedar.blockaim.ConfigurationFile.cedarConfig;
import static com.cedar.blockaim.Main.VERSION;
import static com.cedar.blockaim.Main.getToggleAimKey;

public class WorldEventHandler {
    @SubscribeEvent
    public void onPlayerJoinWorld(PlayerEvent.PlayerLoggedInEvent event) {
        EntityPlayer player = event.player;
        player.addChatMessage(new ChatComponentText("§eCedar雪松 §r§eBlockAim "+VERSION));
    }

    @SubscribeEvent
    public  void onPlayerLeaveWorld(PlayerEvent.PlayerLoggedOutEvent event){
        cedarConfig.addProperty("AimKey",getToggleAimKey().getKeyCode());
        ConfigurationFile.setConfig(cedarConfig);
    }
}
