package com.tian_nu.AdvancedTurret.client;

import com.tian_nu.AdvancedTurret.TurretMod;
import com.tian_nu.AdvancedTurret.blocks.LaserTurretBlock;
import com.tian_nu.AdvancedTurret.blocks.entitys.LaserTurretBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 激光渲染事件处理器
 *
 * <p>使用 RenderLevelStageEvent 在世界渲染后绘制激光光束</p>
 */
@Mod.EventBusSubscriber(modid = TurretMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class LaserRenderHandler {

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // 在半透明渲染阶段绘制激光
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        // 获取相机位置
        var camera = event.getCamera();
        double camX = camera.getPosition().x;
        double camY = camera.getPosition().y;
        double camZ = camera.getPosition().z;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

        // 遍历所有已加载的激光炮塔
        var player = Minecraft.getInstance().player;
        if (player == null) return;

        int renderDistance = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16;

        // 获取玩家周围的区块
        BlockPos playerPos = player.blockPosition();
        int minX = playerPos.getX() - renderDistance;
        int maxX = playerPos.getX() + renderDistance;
        int minZ = playerPos.getZ() - renderDistance;
        int maxZ = playerPos.getZ() + renderDistance;

        // 遍历区块内的方块实体
        for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
            for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) continue;

                var chunk = level.getChunk(cx, cz);
                for (var beEntry : chunk.getBlockEntities().entrySet()) {
                    BlockEntity be = beEntry.getValue();
                    if (be instanceof LaserTurretBlockEntity laserTurret) {
                        renderLaserForTurret(laserTurret, poseStack, bufferSource, camX, camY, camZ);
                    }
                }
            }
        }
        
        // 确保缓冲区被刷新
        bufferSource.endBatch();
    }

    private static void renderLaserForTurret(LaserTurretBlockEntity turret, PoseStack poseStack, MultiBufferSource bufferSource,
                                              double camX, double camY, double camZ) {
        BlockPos pos = turret.getBlockPos();

        // 检查光束是否激活
        Boolean beamActive = turret.getAnimData(LaserTurretBlockEntity.BEAM_ACTIVE);
        Boolean hasTarget = turret.getAnimData(LaserTurretBlockEntity.HAS_TARGET);
        
        if (!Boolean.TRUE.equals(beamActive) || !Boolean.TRUE.equals(hasTarget)) return;

        // 获取目标位置
        Double targetX = turret.getAnimData(LaserTurretBlockEntity.TARGET_POS_X);
        Double targetY = turret.getAnimData(LaserTurretBlockEntity.TARGET_POS_Y);
        Double targetZ = turret.getAnimData(LaserTurretBlockEntity.TARGET_POS_Z);
        
        if (targetX == null || targetY == null || targetZ == null) return;

        // 相对于相机的位置
        double relX = pos.getX() - camX;
        double relY = pos.getY() - camY;
        double relZ = pos.getZ() - camZ;

        poseStack.pushPose();
        poseStack.translate(relX, relY, relZ);

        // 计算起点（炮塔中心）
        Direction facing = turret.getBlockState().getValue(LaserTurretBlock.FACING);
        Vec3 startWorld = turret.calculateMuzzlePosition(pos, facing);
        Vec3 startLocal = new Vec3(startWorld.x - pos.getX(), startWorld.y - pos.getY(), startWorld.z - pos.getZ());

        // 计算终点（目标位置，转换为局部坐标）
        Vec3 endLocal = new Vec3(targetX - pos.getX(), targetY - pos.getY(), targetZ - pos.getZ());

        // 计算透明度
        float alpha = 0.4F;
        Integer fireRateCount = turret.getAnimData(LaserTurretBlockEntity.FIRE_RATE_COUNT);
        if (fireRateCount != null && fireRateCount > 0) {
            alpha = Math.min(1.0F, alpha + fireRateCount * 0.06F);
        }

        // 绘制激光
        renderLaserBeam(poseStack, bufferSource, startLocal, endLocal, 0.06F, alpha);

        poseStack.popPose();
    }

    /**
     * 渲染激光光束（从起点到终点的实心方柱体）
     */
    private static void renderLaserBeam(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 start, Vec3 end, float radius, float alpha) {
        Vec3 direction = end.subtract(start);
        double length = direction.length();
        if (length < 0.001) return;
        
        direction = direction.normalize();
        float len = (float) length;
        float r = radius / 2.0F; // 半径

        // 计算从Y轴到目标方向的旋转
        poseStack.pushPose();
        poseStack.translate(start.x, start.y, start.z);

        // 旋转使Y轴对齐到激光方向
        if (Math.abs(direction.y) < 0.999) {
            Vec3 axis = new Vec3(0, 1, 0).cross(direction).normalize();
            float angle = (float) Math.acos(direction.y);
            poseStack.mulPose(new org.joml.Quaternionf().fromAxisAngleRad((float) axis.x, (float) axis.y, (float) axis.z, angle));
        } else if (direction.y < 0) {
            poseStack.mulPose(new org.joml.Quaternionf().rotationX((float) Math.PI));
        }

        // 使用调试填充盒子渲染类型（更可靠）
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.debugFilledBox());
        
        org.joml.Matrix4f mat4 = poseStack.last().pose();
        org.joml.Matrix3f mat3 = poseStack.last().normal();

        float red = 1.0F, green = 0.0F, blue = 0.0F;

        // 绘制6个实心面
        // +X 面
        drawFilledQuad(consumer, mat4, mat3, r, 0, -r, r, len, r, red, green, blue, alpha, 1, 0, 0);
        // -X 面
        drawFilledQuad(consumer, mat4, mat3, -r, 0, r, -r, len, -r, red, green, blue, alpha, -1, 0, 0);
        // +Z 面
        drawFilledQuad(consumer, mat4, mat3, -r, 0, r, r, len, r, red, green, blue, alpha, 0, 0, 1);
        // -Z 面
        drawFilledQuad(consumer, mat4, mat3, r, 0, -r, -r, len, r, red, green, blue, alpha, 0, 0, -1);
        // 底面（y=0）
        drawFilledQuad(consumer, mat4, mat3, r, 0, r, -r, 0, -r, red, green, blue, alpha, 0, -1, 0);
        // 顶面（y=len）
        drawFilledQuad(consumer, mat4, mat3, -r, len, -r, r, len, r, red, green, blue, alpha, 0, 1, 0);

        poseStack.popPose();
    }

    /**
     * 绘制填充的四边形（两个三角形）
     * 使用 POSITION_COLOR 格式（适用于 debugFilledBox）
     */
    private static void drawFilledQuad(VertexConsumer consumer, org.joml.Matrix4f mat4, org.joml.Matrix3f mat3,
                                        float x1, float y1, float z1, float x2, float y2, float z2,
                                        float r, float g, float b, float a, float nx, float ny, float nz) {
        // 四个顶点：(x1,y1,z1), (x2,y1,z2), (x2,y2,z2), (x1,y2,z1)
        // 第一个三角形
        consumer.vertex(mat4, x1, y1, z1).color(r, g, b, a).endVertex();
        consumer.vertex(mat4, x2, y1, z2).color(r, g, b, a).endVertex();
        consumer.vertex(mat4, x2, y2, z2).color(r, g, b, a).endVertex();
        // 第二个三角形
        consumer.vertex(mat4, x1, y1, z1).color(r, g, b, a).endVertex();
        consumer.vertex(mat4, x2, y2, z2).color(r, g, b, a).endVertex();
        consumer.vertex(mat4, x1, y2, z1).color(r, g, b, a).endVertex();
    }
}
