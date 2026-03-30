package com.jeremyseq.inhabitants.mixin;

import com.jeremyseq.inhabitants.effects.ModEffects;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class ImmaterialMixin {

    // handles horizontal phasing through blocks
    @Mixin(BlockBehaviour.class)
    public static class Physics {
        @Inject(
            method = "getCollisionShape",
            at = @At("RETURN"),
            cancellable = true
        )
        private void inhabitants$immaterialCollision(BlockState pState,
            BlockGetter pLevel,
            BlockPos pPos,
            CollisionContext pContext,
            CallbackInfoReturnable<VoxelShape> cir) {

            if (pContext instanceof EntityCollisionContext ctx &&
                    ctx.getEntity() instanceof Player player) {
                if (player.hasEffect(ModEffects.IMMATERIAL.get())) {
                    VoxelShape shape = cir.getReturnValue();
                    if (shape != null && !shape.isEmpty()) {
                        double shapeMinY = shape.min(Direction.Axis.Y) + pPos.getY();
                        double shapeMaxY = shape.max(Direction.Axis.Y) + pPos.getY();

                        // if Player is strictly above the block, preserve floor collision
                        if (player.getY() >= shapeMaxY - 0.001)
                            return;
                        // if Player is strictly below the block, preserve ceiling collision
                        if (player.getBoundingBox().maxY <= shapeMinY + 0.001)
                            return;

                        cir.setReturnValue(Shapes.empty());
                    }
                }
            }
        }
    }

    // blocking sky visibility when immaterial
    @Mixin(LevelRenderer.class)
    public static class Visuals {
        @Inject(
            method = "doesMobEffectBlockSky",
            at = @At("RETURN"),
            cancellable = true
        )
        private void inhabitants$blockSkyWhenImmaterial(Camera pCamera,
            CallbackInfoReturnable<Boolean> cir) {

            if (!cir.getReturnValue()) {
                if (pCamera.getEntity() instanceof LivingEntity living &&
                        living.hasEffect(ModEffects.IMMATERIAL.get())) {
                    BlockPos eyePos = BlockPos.containing(pCamera.getPosition());

                    if (!living.level().getBlockState(eyePos).getCollisionShape(living.level(),
                            eyePos, CollisionContext.empty()).isEmpty()) {

                        cir.setReturnValue(true);
                    }
                }
            }
        }
    }

    // force blocks below player to render when immaterial
    @Mixin(Block.class)
    public static class BlockRendering {
        @Inject(
            method = "shouldRenderFace",
            at = @At("HEAD"),
            cancellable = true
        )
        private static void inhabitants$forceRenderNearPhasingPlayer(BlockState pState,
            BlockGetter pLevel,
            BlockPos pPos,
            Direction pFace,
            BlockPos pNeighborPos,
            CallbackInfoReturnable<Boolean> cir) {

            Minecraft mc = Minecraft.getInstance();

            if (mc.player != null && mc.player.hasEffect(ModEffects.IMMATERIAL.get())) {
                if (pPos.getY() < mc.player.getY()) {
                    double distSq = mc.player.distanceToSqr(
                        pPos.getX() + 0.5,
                        mc.player.getY(),
                        pPos.getZ() + 0.5);
                    
                    if (distSq < 9.0) {
                        cir.setReturnValue(true);
                    }
                }
            }
        }
    }
}
