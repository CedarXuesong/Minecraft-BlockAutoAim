package com.cedar.blockaim;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MovingObjectPosition;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.cedar.blockaim.ConfigurationFile.IgnoreFeetBlocks;
import static com.cedar.blockaim.ConfigurationFile.cedarConfig;
import static com.cedar.blockaim.Main.VERSION;
import static com.cedar.blockaim.Main.VERSIONCODE;

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
                            cedarConfig.addProperty("BlockNames", args[2]);
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
                        case"ignorefeet":
                            cedarConfig.addProperty("IgnoreFeet",args[2]);
                            ConfigurationFile.setConfig(cedarConfig);
                            ConfigurationFile.reload();
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
                sender.addChatMessage(new ChatComponentText("§e这方块是 "+ getAimBlockName()+" !"));
                break;
            case "reload":
                ConfigurationFile.reload();
                break;
            case "?":
            case "help":
            default:
                commandHelper(sender);
                break;
        }
    }

    // 实现命令补全
    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        // 检查是否正在补全第一个参数
        switch (args.length){
            case 1:
                return getListOfStringsMatchingLastWord(args, Arrays.asList("set", "whatblock","wb","reload", "help"));
            case 2:
                if (Objects.equals(args[0], "set")){
                    return getListOfStringsMatchingLastWord(args, Arrays.asList("aimblocks", "aimspeed", "ignorefeet","help"));
                }
                return null;
            case 3:
                switch (args[1]){
                    case "aimblocks":
                        return getListOfStringsMatchingLastWord(args, Collections.singletonList(getAimBlockName()));
                    case "aimspeed":
                        return getListOfStringsMatchingLastWord(args, Collections.singletonList(AimSystem.AIM_SMOOTHNESS));
                    case "ignorefeet":
                        return getListOfStringsMatchingLastWord(args, Arrays.asList("True","False"));
                }
        }
        return null;
    }

    private void commandHelper(ICommandSender sender){
        sender.addChatMessage(new ChatComponentText("§l§b[CedarBlockAim] §r§a帮助页面 §r§l§b[1]"));
        sender.addChatMessage(new ChatComponentText(""));
        sender.addChatMessage(new ChatComponentText("§e/cedar help(?) §b查看帮助页面"));
        sender.addChatMessage(new ChatComponentText("§e/cedar set [settings/help] §b设置项"));
        sender.addChatMessage(new ChatComponentText("§e/cedar wb(whatblcok) §b查看当前瞄准的方块的命名空间"));
        sender.addChatMessage(new ChatComponentText("§e/cedar relaod §b重载"));
        sender.addChatMessage(new ChatComponentText(""));
        sender.addChatMessage(new ChatComponentText("§b§lBy Cedar "+ VERSION +" "+VERSIONCODE));
    }

    private void SettingCommanderHelper(ICommandSender sender){
        sender.addChatMessage(new ChatComponentText("§eCedarBlockAim SET 帮助页面"));
        sender.addChatMessage(new ChatComponentText(""));
        sender.addChatMessage(new ChatComponentText("§e/cedar set aimblocks [AimBLockID] 设置需要瞄准的方块id(当前瞄准的方块是 "+getAimBlockName()+"，可用英文冒号分隔多个方块)"));
        sender.addChatMessage(new ChatComponentText("§e/cedar set aimspeed [Num] 设置瞄准速度(当前"+AimSystem.AIM_SMOOTHNESS+")"));
        sender.addChatMessage(new ChatComponentText("§e/cedar set ignorefeet [Boolean] 设置是否忽略脚下方块(当前"+IgnoreFeetBlocks.toString()+")"));
        sender.addChatMessage(new ChatComponentText(""));
        sender.addChatMessage(new ChatComponentText("§e§lBy Cedar "+ VERSION +" "+VERSIONCODE));
    }

    public String getAimBlockName(){
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            BlockPos blockPos = mc.objectMouseOver.getBlockPos();
            Block block = mc.theWorld.getBlockState(blockPos).getBlock();
            int metadata = block.getMetaFromState(mc.theWorld.getBlockState(blockPos));
            return Block.getIdFromBlock(block)+"."+metadata;
        } else {
            return "0.0";
        }
    }
    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }
}
