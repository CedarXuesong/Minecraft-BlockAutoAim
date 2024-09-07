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

public class AimSystem {
    private Minecraft mc;
    private static boolean isActive = false;
    private static final double MAX_REACH_DISTANCE = 5.0;
    private static final double EASE_OUT_EXPO_EXPONENT = 10.0;
    public static double AIM_SMOOTHNESS = 0.3;

    private volatile BlockPos closestBlockPos = null;
    private volatile Vec3 closestExposedFaceCenter = null; // Closest exposed face center
    private Thread blockFinderThread;
    private Thread aimingThread;

    public AimSystem() {
        this.mc = Minecraft.getMinecraft();
    }

    // Start the threads
    public void start() {
        blockFinderThread = new Thread(new BlockFinderThread());
        aimingThread = new Thread(new AimingThread());
        blockFinderThread.start();
        aimingThread.start();
    }

    // Stop the threads
    public void stop() {
        if (blockFinderThread != null) {
            blockFinderThread.interrupt();
        }
        if (aimingThread != null) {
            aimingThread.interrupt();
        }
    }

    // Thread for finding blocks
    private class BlockFinderThread implements Runnable {
        @Override
        public void run() {
            while (true) {
                if (Thread.interrupted()) return;
                try {
                    Thread.sleep(50);  // 每 50 毫秒执行一次查找
                } catch (InterruptedException e) {
                    return;
                }

                if (mc.thePlayer == null || mc.theWorld == null || !isActive) {
                    continue;
                }

                EntityPlayerSP player = mc.thePlayer;
                World world = mc.theWorld;

                BlockPos newClosestBlockPos = null;
                Vec3 newClosestExposedFaceCenter = null;
                double closestDistance = Double.MAX_VALUE;

                // 优先查找正在被破坏的方块
                BlockPos breakingBlockPos = mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                        ? mc.objectMouseOver.getBlockPos() : null;

                if (breakingBlockPos != null) {
                    Block block = world.getBlockState(breakingBlockPos).getBlock();
                    int blockID = Block.getIdFromBlock(block);

                    for (int id : ConfigurationFile.BlockIDs) {
                        if (blockID == id && hasFullyExposedFace(world, breakingBlockPos)) {
                            // 获取这个方块的最接近的暴露面
                            Vec3 closestFace = getClosestExposedFaceCenter(world, breakingBlockPos, player);
                            if (closestFace != null) {
                                closestBlockPos = breakingBlockPos;
                                closestExposedFaceCenter = closestFace;
                                break;  // 优先级高，立即返回该方块
                            }
                        }
                    }
                }

                // 如果没有找到正在被破坏的方块，继续遍历其他方块
                if (closestBlockPos == null) {
                    for (int x = (int) player.posX - 50; x < player.posX + 50; x++) {
                        for (int y = (int) player.posY - 50; y < player.posY + 50; y++) {
                            for (int z = (int) player.posZ - 50; z < player.posZ + 50; z++) {
                                BlockPos pos = new BlockPos(x, y, z);
                                Block block = world.getBlockState(pos).getBlock();
                                int blockID = Block.getIdFromBlock(block);

                                for (int id : ConfigurationFile.BlockIDs) {
                                    if (blockID == id && hasFullyExposedFace(world, pos)) {
                                        Vec3 playerPos = new Vec3(player.posX, player.posY + player.getEyeHeight(), player.posZ);

                                        // 获取最接近的暴露面
                                        for (Vec3 exposedFaceCenter : getExposedFaceCenters(world, pos)) {
                                            if (exposedFaceCenter != null && isPathClear(playerPos, exposedFaceCenter, world)) {
                                                double distance = player.getDistanceSq(exposedFaceCenter.xCoord, exposedFaceCenter.yCoord, exposedFaceCenter.zCoord);
                                                if (distance < MAX_REACH_DISTANCE * MAX_REACH_DISTANCE && distance < closestDistance) {
                                                    closestDistance = distance;
                                                    newClosestBlockPos = pos;
                                                    newClosestExposedFaceCenter = exposedFaceCenter;
                                                }
                                            }
                                        }
                                        break;  // 找到匹配的方块后，跳过该方块的进一步检查
                                    }
                                }
                            }
                        }
                    }

                    // 更新找到的最接近的方块
                    closestBlockPos = newClosestBlockPos;
                    closestExposedFaceCenter = newClosestExposedFaceCenter;
                }

                // 检查当前瞄准的方块是否超出 MAX_REACH_DISTANCE 或已经破坏
                if (closestBlockPos != null) {
                    Block block = world.getBlockState(closestBlockPos).getBlock();
                    Vec3 playerPos = new Vec3(player.posX, player.posY + player.getEyeHeight(), player.posZ);
                    Vec3 targetPos = new Vec3(closestBlockPos.getX() + 0.5, closestBlockPos.getY() + 0.5, closestBlockPos.getZ() + 0.5);
                    double distance = playerPos.distanceTo(targetPos);

                    if (block == Blocks.air || distance > MAX_REACH_DISTANCE) {
                        closestBlockPos = null;
                        closestExposedFaceCenter = null;
                    }
                }
            }
        }
    }


    // AimingThread负责平滑瞄准
    private class AimingThread implements Runnable {
        @Override
        public void run() {
            Vec3 previousDirection = null;
            long lastUpdateTime = System.currentTimeMillis(); // 记录上一次更新的时间

            while (true) {
                if (Thread.interrupted()) return;
                try {
                    Thread.sleep(1);  // 瞄准线程频繁更新
                } catch (InterruptedException e) {
                    return;
                }

                if (mc.thePlayer == null || mc.theWorld == null || !isActive || closestExposedFaceCenter == null) {
                    continue;
                }

                EntityPlayerSP player = mc.thePlayer;
                Vec3 playerPos = new Vec3(player.posX, player.posY + player.getEyeHeight(), player.posZ);
                if (closestExposedFaceCenter != null) {
                    Vec3 direction = closestExposedFaceCenter.subtract(playerPos).normalize();

                    long currentTime = System.currentTimeMillis();
                    double deltaTime = (currentTime - lastUpdateTime) / 1000.0; // 计算时间差，单位为秒
                    lastUpdateTime = currentTime;

                    // 缓动系数（使瞄准更平滑）
                    double easeFactor = Math.min(deltaTime * AIM_SMOOTHNESS, 1.0); // 确保easeFactor在[0,1]之间

                    double pitch = Math.toDegrees(Math.asin(-direction.yCoord));
                    double yaw = Math.toDegrees(Math.atan2(direction.zCoord, direction.xCoord)) - 90.0;

                    if (previousDirection != null) {
                        double currentPitch = mc.thePlayer.rotationPitch;
                        double currentYaw = mc.thePlayer.rotationYaw;

                        // 平滑的 Pitch 和 Yaw
                        double smoothedPitch = currentPitch + (pitch - currentPitch) * easeFactor;
                        double smoothedYaw = smoothYawTransition(currentYaw, yaw, easeFactor);

                        mc.thePlayer.rotationPitch = (float) smoothedPitch;
                        mc.thePlayer.rotationYaw = (float) smoothedYaw;
                    } else {
                        // 如果是第一次瞄准，直接设置瞄准角度
                        mc.thePlayer.rotationPitch = (float) pitch;
                        mc.thePlayer.rotationYaw = (float) yaw;
                    }

                    previousDirection = direction;
                }
                // 如果距离超出 MAX_REACH_DISTANCE，则停止瞄准
                if (closestBlockPos != null) {
                    Vec3 targetPos = new Vec3(closestBlockPos.getX() + 0.5, closestBlockPos.getY() + 0.5, closestBlockPos.getZ() + 0.5);
                    double distance = playerPos.distanceTo(targetPos);

                    // 如果距离超出 MAX_REACH_DISTANCE，停止瞄准
                    if (distance > MAX_REACH_DISTANCE) {
                        closestBlockPos = null;
                        closestExposedFaceCenter = null;
                    }
                }
            }
        }
    }

    private boolean isFaceExposed(World world, BlockPos pos, int offsetX, int offsetY, int offsetZ) {
        BlockPos adjacentPos = pos.add(offsetX, offsetY, offsetZ);
        return world.getBlockState(adjacentPos).getBlock() == Blocks.air;
    }

    private boolean hasFullyExposedFace(World world, BlockPos pos) {
        return isFaceExposed(world, pos, -1, 0, 0) || isFaceExposed(world, pos, 1, 0, 0) ||
                isFaceExposed(world, pos, 0, -1, 0) || isFaceExposed(world, pos, 0, 1, 0) ||
                isFaceExposed(world, pos, 0, 0, -1) || isFaceExposed(world, pos, 0, 0, 1);
    }

    private List<Vec3> getExposedFaceCenters(World world, BlockPos pos) {
        List<Vec3> exposedFaces = new ArrayList<>();
        if (isFaceExposed(world, pos, -1, 0, 0)) exposedFaces.add(new Vec3(pos.getX(), pos.getY() + 0.5, pos.getZ() + 0.5));
        if (isFaceExposed(world, pos, 1, 0, 0)) exposedFaces.add(new Vec3(pos.getX() + 1, pos.getY() + 0.5, pos.getZ() + 0.5));
        if (isFaceExposed(world, pos, 0, -1, 0)) exposedFaces.add(new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
        if (isFaceExposed(world, pos, 0, 1, 0)) exposedFaces.add(new Vec3(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5));
        if (isFaceExposed(world, pos, 0, 0, -1)) exposedFaces.add(new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ()));
        if (isFaceExposed(world, pos, 0, 0, 1)) exposedFaces.add(new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 1));
        return exposedFaces;
    }

    private Vec3 getClosestExposedFaceCenter(World world, BlockPos pos, EntityPlayerSP player) {
        Vec3 playerPos = new Vec3(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        List<Vec3> exposedFaceCenters = getExposedFaceCenters(world, pos);

        Vec3 closestFace = null;
        double closestDistance = Double.MAX_VALUE;
        for (Vec3 faceCenter : exposedFaceCenters) {
            double distance = playerPos.distanceTo(faceCenter);
            if (distance < closestDistance) {
                closestDistance = distance;
                closestFace = faceCenter;
            }
        }
        return closestFace;
    }

    private double smoothYawTransition(double currentYaw, double targetYaw, double easeFactor) {
        double deltaYaw = targetYaw - currentYaw;

        // 确保角度转换是最短路径
        while (deltaYaw < -180.0) deltaYaw += 360.0;
        while (deltaYaw > 180.0) deltaYaw -= 360.0;

        return currentYaw + deltaYaw * easeFactor;
    }

    private boolean isPathClear(Vec3 start, Vec3 end, World world) {
        MovingObjectPosition result = world.rayTraceBlocks(start, end, false, true, false);
        return result == null || result.typeOfHit == MovingObjectPosition.MovingObjectType.MISS;
    }
    // Toggle active state
    public static void toggleActive() {
        isActive = !isActive;
        if (isActive) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§e[Cedar BlockAim] BlockAim §l§aON"));
        } else {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§e[Cedar BlockAim] BlockAim §l§cOFF"));
        }
    }
}
