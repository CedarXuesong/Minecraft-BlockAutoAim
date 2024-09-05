package com.cedar.blockaim;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

import java.io.*;

import static com.cedar.blockaim.Main.toggleAimKey;

public class ConfigurationFile {
    public static JsonObject cedarConfig = new JsonObject();//配置文件
    public static int[] BlockIDs;
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
            e.printStackTrace();
        }
        return content.toString().trim();
    }

    // Reload configuration and parse JSON
    public static void reload() {
        String jsonContent = readConfig();
        if (jsonContent.equals("")) {
            BlockIDs = new int[]{0};
            AimSystem.AIM_SMOOTHNESS = 0.3; // 默认值
            toggleAimKey.setKeyCode(0);
            return;
        }

        try {
            JsonObject jsonObject = gson.fromJson(jsonContent, JsonObject.class);
            cedarConfig = jsonObject;
            // 加载 BlockIDs
            if (jsonObject.has("BlockIDs")) {
                String blockIDsString = jsonObject.get("BlockIDs").getAsString();
                String[] stringArray = blockIDsString.split(":");
                int[] intArray = new int[stringArray.length];

                for (int i = 0; i < stringArray.length; i++) {
                    try {
                        intArray[i] = Integer.parseInt(stringArray[i]);
                    } catch (NumberFormatException e) {
                        if (Minecraft.getMinecraft().thePlayer != null) {
                            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§c无效数值！"));
                        }
                        System.out.println("读取的方块id为空！可能有非法字符！");
                        BlockIDs = new int[]{0};
                        return;
                    }
                }
                BlockIDs = intArray;
            } else {
                BlockIDs = new int[]{0};
                if (Minecraft.getMinecraft().thePlayer != null) {
                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§c配置文件中缺少 BlockIDs 键！"));
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
            if (Minecraft.getMinecraft().thePlayer != null) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§b配置文件加载成功！"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            BlockIDs = new int[]{0};
            AimSystem.AIM_SMOOTHNESS = 0.3; // 默认值
            toggleAimKey.setKeyCode(0);
            if (Minecraft.getMinecraft().thePlayer != null) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§c配置文件解析错误！"));
            }
            System.err.println("配置文件解析错误！");
        }
    }
}
