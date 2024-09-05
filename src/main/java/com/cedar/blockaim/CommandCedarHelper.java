package com.cedar.blockaim;

import com.google.gson.JsonObject;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MovingObjectPosition;

import static com.cedar.blockaim.ConfigurationFile.cedarConfig;

public class CommandCedarHelper extends CommandBase {

    public String getCommandName(){
        return "cedar";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/cedar help";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            commandHelper(sender);
            return;
        }
        String subCommand = args[0];
        switch (subCommand) {
            case "set":
                if (args.length > 2){
                    switch (args[1]){
                        case "aimblocks":
                            cedarConfig.addProperty("BlockIDs", args[2]);
                            ConfigurationFile.setConfig(cedarConfig);
                            ConfigurationFile.reload();
                            break;
                        case "aimspeed":
                            try {
                                AimSystem.AIM_SMOOTHNESS=Double.parseDouble(args[2]);
                                cedarConfig.addProperty("AimSpeed",AimSystem.AIM_SMOOTHNESS);
                                ConfigurationFile.setConfig(cedarConfig);
                                sender.addChatMessage(new ChatComponentText("§e速度已实时生效 "+AimSystem.AIM_SMOOTHNESS));
                            }catch (NumberFormatException e){
                                sender.addChatMessage(new ChatComponentText("§c数值无效!"));
                                return;
                            }
                            break;
                        default:
                            SettingCommanderHelper(sender);
                            break;
                    }
                }else {
                    SettingCommanderHelper(sender);
                }
                break;
            case "wb":
            case "whatblock":
                sender.addChatMessage(new ChatComponentText("§e这方块的ID是 "+getAimBlockId()+" !"));
                break;
            case "relaod":
                ConfigurationFile.reload();
                break;
            case "?":
            case "help":
            default:
                commandHelper(sender);
                break;
        }
    }

    private void commandHelper(ICommandSender sender){
        sender.addChatMessage(new ChatComponentText("§l§b[CedarBlockAim] §r§a帮助页面 §r§l§b[1]"));
        sender.addChatMessage(new ChatComponentText(""));
        sender.addChatMessage(new ChatComponentText("§e/cedar help(?) §b查看帮助页面"));
        sender.addChatMessage(new ChatComponentText("§e/cedar set [settings/help] §b设置项"));
        sender.addChatMessage(new ChatComponentText("§e/cedar wb(whatblcok) §b查看当前瞄准的方块的方块id"));
        sender.addChatMessage(new ChatComponentText("§e/cedar relaod §b重载"));
        sender.addChatMessage(new ChatComponentText(""));
        sender.addChatMessage(new ChatComponentText("§b§lBy Cedar"));
    }

    private void SettingCommanderHelper(ICommandSender sender){
        sender.addChatMessage(new ChatComponentText("§eCedarBlockAim SET 帮助页面"));
        sender.addChatMessage(new ChatComponentText(""));
        sender.addChatMessage(new ChatComponentText("§e/cedar set aimblocks [AimBLockID] 设置需要瞄准的方块ID(默认为0，可用英文冒号分隔)"));
        sender.addChatMessage(new ChatComponentText("§e/cedar set aimspeed [num] 设置瞄准速度(当前"+AimSystem.AIM_SMOOTHNESS+")"));
        sender.addChatMessage(new ChatComponentText(""));
        sender.addChatMessage(new ChatComponentText("§eBy Cedar"));
    }

    public int getAimBlockId(){
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            BlockPos blockPos = mc.objectMouseOver.getBlockPos();
            Block block = mc.theWorld.getBlockState(blockPos).getBlock();
            return Block.getIdFromBlock(block);
        } else {
            return 0;
        }
    }
    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }
}
