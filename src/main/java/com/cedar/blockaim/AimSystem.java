package com.cedar.blockaim;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import java.util.ArrayList;
import java.util.List;

public class AimSystem implements Runnable {
    private Minecraft mc;
    private static boolean isActive = false; // New variable to track active state
    private static final double MAX_REACH_DISTANCE = 5.0;
    private static final double EASE_OUT_EXPO_EXPONENT = 10.0;
    public static double AIM_SMOOTHNESS = 0.3;

    public AimSystem(int[] blockIDs) {
        this.mc = Minecraft.getMinecraft();
    }

    @Override
    public void run() {
        Vec3 previousDirection = null;
        long startTime = System.currentTimeMillis();

        while (true) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }

            if (mc.thePlayer == null || mc.theWorld == null || !isActive) {
                continue;
            }

            EntityPlayerSP player = mc.thePlayer;
            World world = mc.theWorld;

            BlockPos closestBlockPos = null;
            Vec3 closestExposedFaceCenter = null;
            double closestDistance = Double.MAX_VALUE;

            for (int x = (int) player.posX - 50; x < player.posX + 50; x++) {
                for (int y = (int) player.posY - 50; y < player.posY + 50; y++) {
                    for (int z = (int) player.posZ - 50; z < player.posZ + 50; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        Block block = world.getBlockState(pos).getBlock();
                        int blockID = Block.getIdFromBlock(block);

                        for (int id : ConfigurationFile.BlockIDs) {
                            if (blockID == id && hasFullyExposedFace(world, pos)) {
                                Vec3 playerPos = new Vec3(player.posX, player.posY + player.getEyeHeight(), player.posZ);

                                // 检查方块的每个暴露面
                                for (Vec3 exposedFaceCenter : getExposedFaceCenters(world, pos)) {
                                    if (exposedFaceCenter != null) {
                                        // 用射线检查是否有遮挡
                                        if (!isPathClear(playerPos, exposedFaceCenter, world)) {
                                            continue; // 如果路径被遮挡，跳过该面
                                        }

                                        double distance = player.getDistanceSq(exposedFaceCenter.xCoord, exposedFaceCenter.yCoord, exposedFaceCenter.zCoord);

                                        // 如果这个面是最接近的无阻挡面，记录它
                                        if (distance < MAX_REACH_DISTANCE * MAX_REACH_DISTANCE && distance < closestDistance) {
                                            closestDistance = distance;
                                            closestBlockPos = pos;
                                            closestExposedFaceCenter = exposedFaceCenter;
                                        }
                                    }
                                }
                                break; // 找到匹配的方块后，继续寻找下一个方块
                            }
                        }
                    }
                }
            }

            // 如果找到最接近的暴露面，就瞄准它
            if (closestExposedFaceCenter != null) {
                Vec3 playerPos = new Vec3(player.posX, player.posY + player.getEyeHeight(), player.posZ);
                Vec3 direction = closestExposedFaceCenter.subtract(playerPos).normalize();

                double elapsedTime = (System.currentTimeMillis() - startTime) / 1000.0;
                double t = Math.min(elapsedTime, 1.0);
                double easeFactor = easeOutExpo(t);

                double pitch = Math.toDegrees(Math.asin(-direction.yCoord));
                double yaw = Math.toDegrees(Math.atan2(direction.zCoord, direction.xCoord)) - 90.0;

                if (previousDirection != null) {
                    double currentPitch = mc.thePlayer.rotationPitch;
                    double currentYaw = mc.thePlayer.rotationYaw;

                    // 平滑的 Pitch 和 Yaw
                    double smoothedPitch = currentPitch + (pitch - currentPitch) * easeFactor * AIM_SMOOTHNESS;
                    double smoothedYaw = smoothYawTransition(currentYaw, yaw, easeFactor * AIM_SMOOTHNESS);

                    mc.thePlayer.rotationPitch = (float) smoothedPitch;
                    mc.thePlayer.rotationYaw = (float) smoothedYaw;
                } else {
                    mc.thePlayer.rotationPitch = (float) pitch;
                    mc.thePlayer.rotationYaw = (float) yaw;
                }

                previousDirection = direction;
            }
        }
    }
    private boolean hasFullyExposedFace(World world, BlockPos pos) {
        // Check each face direction to ensure it is exposed (air block)
        return isFaceExposed(world, pos, -1, 0, 0) || // Left
                isFaceExposed(world, pos, 1, 0, 0) || // Right
                isFaceExposed(world, pos, 0, 1, 0) || // Top
                isFaceExposed(world, pos, 0, -1, 0) || // Bottom
                isFaceExposed(world, pos, 0, 0, 1) || // Front
                isFaceExposed(world, pos, 0, 0, -1);  // Back
    }

    private boolean isFaceExposed(World world, BlockPos pos, int offsetX, int offsetY, int offsetZ) {
        BlockPos adjacentPos = pos.add(offsetX, offsetY, offsetZ);
        return world.getBlockState(adjacentPos).getBlock() == Blocks.air;
    }

    private double easeOutExpo(double t) {
        return 1 - Math.pow(2, -EASE_OUT_EXPO_EXPONENT * t);
    }

    public static void toggleActive() {
        isActive = !isActive;
        if (isActive){
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§e[Cedar BlockAim] BlockAim §l§aON"));
        }else {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§e[Cedar BlockAim] BlockAim §l§cOFF"));
        }
        System.out.println("AimSystem is now " + (isActive ? "active" : "inactive"));
    }

    // 确保 Yaw 通过最短路径变化
    private double smoothYawTransition(double currentYaw, double targetYaw, double smoothFactor) {
        double yawDifference = calculateShortestYawDifference(currentYaw, targetYaw);
        return currentYaw + yawDifference * smoothFactor;
    }

    // 计算当前 Yaw 和目标 Yaw 之间的最短差异
    private double calculateShortestYawDifference(double currentYaw, double targetYaw) {
        double yawDifference = targetYaw - currentYaw;

        // 将差异标准化到 [-180, 180] 之间
        while (yawDifference < -180) yawDifference += 360;
        while (yawDifference > 180) yawDifference -= 360;

        return yawDifference;
    }

    // 获取方块的所有暴露面的中心位置
    private List<Vec3> getExposedFaceCenters(World world, BlockPos pos) {
        List<Vec3> exposedFaces = new ArrayList<>();

        if (isFaceExposed(world, pos, -1, 0, 0)) { // 左侧面
            exposedFaces.add(new Vec3(pos.getX(), pos.getY() + 0.5, pos.getZ() + 0.5));
        }
        if (isFaceExposed(world, pos, 1, 0, 0)) { // 右侧面
            exposedFaces.add(new Vec3(pos.getX() + 1, pos.getY() + 0.5, pos.getZ() + 0.5));
        }
        if (isFaceExposed(world, pos, 0, 1, 0)) { // 顶面
            exposedFaces.add(new Vec3(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5));
        }
        if (isFaceExposed(world, pos, 0, -1, 0)) { // 底面
            exposedFaces.add(new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
        }
        if (isFaceExposed(world, pos, 0, 0, 1)) { // 前面
            exposedFaces.add(new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 1));
        }
        if (isFaceExposed(world, pos, 0, 0, -1)) { // 后面
            exposedFaces.add(new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ()));
        }

        return exposedFaces;
    }


    // 检查射线路径是否有遮挡
    private boolean isPathClear(Vec3 playerPos, Vec3 targetPos, World world) {
        MovingObjectPosition rayTraceResult = world.rayTraceBlocks(playerPos, targetPos);
        return rayTraceResult == null || rayTraceResult.typeOfHit == MovingObjectPosition.MovingObjectType.MISS; // 如果路径没有遮挡，返回true
    }
}
