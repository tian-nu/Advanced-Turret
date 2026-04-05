package com.tian_nu.AdvancedTurret.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tian_nu.AdvancedTurret.TurretMod;
import com.tian_nu.AdvancedTurret.network.RemoteTerminalBaseInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 远程终端基座高亮渲染：支持全部高亮 + 单基座高亮。
 */
@Mod.EventBusSubscriber(modid = TurretMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class RemoteTerminalHighlightRenderer {

    private static final double HIGHLIGHT_DISTANCE_SQR = 64.0D * 64.0D;

    private RemoteTerminalHighlightRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        boolean highlightAll = RemoteTerminalClientHooks.isHighlightEnabled();
        String highlightOne = RemoteTerminalClientHooks.getHighlightedBaseKey();
        if (!highlightAll && (highlightOne == null || highlightOne.isBlank())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || player.level() == null) {
            return;
        }

        String currentDimension = player.level().dimension().location().toString();
        Vec3 cameraPos = event.getCamera().getPosition();

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        for (RemoteTerminalBaseInfo entry : RemoteTerminalClientHooks.getLatestEntries()) {
            if (!entry.loaded()) {
                continue;
            }
            if (!currentDimension.equals(entry.dimensionId())) {
                continue;
            }
            if (entry.pos().distSqr(player.blockPosition()) > HIGHLIGHT_DISTANCE_SQR) {
                continue;
            }

            String baseKey = buildBaseKey(entry);
            boolean isSingleTarget = baseKey.equals(highlightOne);
            if (!highlightAll && !isSingleTarget) {
                continue;
            }

            AABB box = new AABB(entry.pos()).inflate(0.02D);
            if (isSingleTarget) {
                LevelRenderer.renderLineBox(poseStack, vertexConsumer, box, 1.0F, 0.72F, 0.10F, 1.0F);
            } else {
                LevelRenderer.renderLineBox(poseStack, vertexConsumer, box, 0.05F, 0.95F, 1.0F, 1.0F);
            }
        }

        poseStack.popPose();
        bufferSource.endBatch(RenderType.lines());
    }

    private static String buildBaseKey(RemoteTerminalBaseInfo entry) {
        return entry.dimensionId() + "|" + entry.pos().asLong();
    }
}
