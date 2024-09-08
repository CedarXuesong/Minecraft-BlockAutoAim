package com.cedar.blockaim;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.beans.binding.BooleanExpression;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

import java.io.*;

import static com.cedar.blockaim.Main.CFG_FILE_PATH;
import static com.cedar.blockaim.Main.toggleAimKey;

public class ConfigurationFile {
    public static JsonObject cedarConfig = new JsonObject();//配置文件
    public static IBlockState[] BlockNames;
    public static Boolean IgnoreFeetBlocks;
    private static File configFile;
    private static ConfigurationFile configFile2;
    private static Gson gson = new Gson();

    public ConfigurationFile(String filePath) {
        this.configFile = new File(filePath);
        File parentDir = configFile.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
        try {
            if (!configFile.exists()) {
                configFile.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void init(String filePath) {
        if (configFile2 == null) {
            configFile2 = new ConfigurationFile(filePath);
        }
    }

    // Set configuration in JSON format
    public static void setConfig(JsonObject config) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFile))) {
            writer.write(gson.toJson(config));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Read configuration from JSON file
    public static String readConfig() {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            ConfigurationFile.init(CFG_FILE_PATH);
            if (Minecraft.getMinecraft().thePlayer!=null){
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§e请不要在游戏运行时删除配置文件"));
            }
            e.printStackTrace();
        }
        return content.toString().trim();
    }

    // Reload configuration and parse JSON
    public static void reload() {
        String jsonContent = readConfig();
        if (jsonContent.equals("")) {
            BlockNames = new IBlockState[]{getBlockStateFromIDAndMeta(1, 0)};
            AimSystem.AIM_SMOOTHNESS = 0.3; // 默认值
            toggleAimKey.setKeyCode(0);
            IgnoreFeetBlocks = false;
            return;
        }

        try {
            JsonObject jsonObject = gson.fromJson(jsonContent, JsonObject.class);
            cedarConfig = jsonObject;
            // 加载 BlockIDs
            if (jsonObject.has("BlockNames")) {
                String blockNamesString = jsonObject.get("BlockNames").getAsString();
                String[] stringArray = blockNamesString.split(":");
                IBlockState[] blockArray = new IBlockState[stringArray.length];

                for (int i = 0; i < stringArray.length; i++) {
                    String[] blockdata = stringArray[i].split("\\.");
                    if (blockdata.length>2){
                        if (Minecraft.getMinecraft().thePlayer != null) {
                            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(stringArray[i]+" §c无效数值！可能有非法字符"));
                        }
                    } else if (blockdata.length == 1) {
                        try {
                            blockArray[i] = getBlockStateFromIDAndMeta(Integer.valueOf(blockdata[0]),0);
                        } catch (NumberFormatException e) {
                            if (Minecraft.getMinecraft().thePlayer != null) {
                                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(stringArray[i]+ " §c无效数值！可能有非法字符"));
                            }
                            blockArray[i] = getBlockStateFromIDAndMeta(0,0);
                            return;
                        }
                    }
                    try {
                        blockArray[i] = getBlockStateFromIDAndMeta(Integer.valueOf(blockdata[0]),Integer.valueOf(blockdata[1]));
                    } catch (NumberFormatException e) {
                        if (Minecraft.getMinecraft().thePlayer != null) {
                            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(stringArray[i] + " §c无效数值！可能有非法字符"));
                        }
                        BlockNames = new IBlockState[]{getBlockStateFromIDAndMeta(1, 0)};
                        return;
                    }
                }
                BlockNames = blockArray;
            } else {
                BlockNames = new IBlockState[]{getBlockStateFromIDAndMeta(1, 0)};
                if (Minecraft.getMinecraft().thePlayer != null) {
                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§c配置文件中缺少 BlockNames 键！"));
                }
            }

            // 加载 AimSpeed
            if (jsonObject.has("AimSpeed")) {
                try {
                    AimSystem.AIM_SMOOTHNESS = jsonObject.get("AimSpeed").getAsDouble();
                } catch (NumberFormatException e) {
                    AimSystem.AIM_SMOOTHNESS = 0.3; // 默认值
                    System.err.println("AimSpeed 非法，使用默认值。");
                }
            } else {
                AimSystem.AIM_SMOOTHNESS = 0.3; // 默认值
            }

            // 加载 AimKey
            if (jsonObject.has("AimKey")) {
                try {
                    toggleAimKey.setKeyCode(jsonObject.get("AimKey").getAsInt());
                } catch (NumberFormatException e) {
                    toggleAimKey.setKeyCode(0);
                    System.err.println("AimKey 非法，使用默认值。");
                }
            } else {
                toggleAimKey.setKeyCode(0);
            }

            // 加载IgnoreFeetBlcoks
            if (jsonObject.has("IgnoreFeet")) {
                IgnoreFeetBlocks = jsonObject.get("IgnoreFeet").getAsBoolean();
            } else {
                IgnoreFeetBlocks = false;
            }


            if (Minecraft.getMinecraft().thePlayer != null) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§b配置文件加载成功！"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            BlockNames = new IBlockState[]{getBlockStateFromIDAndMeta(1, 0)};
            AimSystem.AIM_SMOOTHNESS = 0.3; // 默认值
            toggleAimKey.setKeyCode(0);
            if (Minecraft.getMinecraft().thePlayer != null) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§c配置文件解析错误！"));
            }
            System.err.println("配置文件解析错误！");
        }
    }


    public static IBlockState getBlockStateFromIDAndMeta(int blockID, int metadata) {
        // 通过ID获取Block实例
        Block block = Block.getBlockById(blockID);
        if (block == null) {
            return null;  // 如果ID无效，返回null
        }
        // 通过Block实例和metadata获取对应的BlockState
        IBlockState blockState = block.getStateFromMeta(metadata);
        return blockState;
    }
}
