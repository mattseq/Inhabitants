package com.jeremyseq.inhabitants.entities.javelin;


import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;

import org.jetbrains.annotations.NotNull;

import software.bernie.geckolib.renderer.GeoEntityRenderer;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;

public class JavelinRenderer extends GeoEntityRenderer<JavelinEntity> {
    public JavelinRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new JavelinModel());
    }

    @Override
    public void render(JavelinEntity animatable,
        float entityYaw,
        float partialTick,
        PoseStack poseStack,
        @NotNull MultiBufferSource bufferSource,
        int packedLight) {
        
        poseStack.pushPose();
        
        float yRot = Mth.lerp(partialTick, animatable.yRotO, animatable.getYRot());
        float xRot = Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot());

        poseStack.mulPose(Axis.YP.rotationDegrees(yRot - 90.0f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(xRot - 90.0f));
        poseStack.translate(0, -1.25f, 0);

        super.render(animatable, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();

        if (Minecraft.getInstance().hitResult instanceof EntityHitResult ehr &&
            ehr.getEntity() == animatable) {
            
            VertexConsumer lines = bufferSource.getBuffer(RenderType.lines());
            
            AABB aabb = animatable.getBoundingBox().move(
                -animatable.getX(), 
                -animatable.getY(), 
                -animatable.getZ()
            );

            LevelRenderer.renderLineBox(poseStack, lines, aabb, 0.0f, 0.0f, 0.0f, 0.4f);
        }
    }
}
