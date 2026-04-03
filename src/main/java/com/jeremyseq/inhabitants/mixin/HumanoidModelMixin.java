package com.jeremyseq.inhabitants.mixin;

import com.jeremyseq.inhabitants.items.ModItems;
import com.jeremyseq.inhabitants.items.javelin.JavelinItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin<T extends LivingEntity> {

    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;

    @Inject(
        method = "setupAnim",
        at = @At("TAIL")
    )
    private void inhabitants$animateArm(
        T pEntity, 
        float pLimbSwing, 
        float pLimbSwingAmount, 
        float pAgeInTicks, 
        float pNetHeadYaw, 
        float pHeadPitch, 
        CallbackInfo ci) {
        
        if (pEntity.isUsingItem() && pEntity.getUseItem().getItem() instanceof JavelinItem) {

            float useDuration = (float)pEntity.getUseItem().getUseDuration();
            float partialTicks = Minecraft.getInstance().getFrameTime();
            float remainingTicks = (float)pEntity.getUseItemRemainingTicks() - partialTicks + 1.0f;
            float chargeProgress = Mth.clamp((useDuration - remainingTicks) / 60.0f, 0.0f, 1.0f);
            
            ModelPart arm = pEntity.getMainArm() == HumanoidArm.RIGHT ? rightArm : leftArm;

            if (pEntity.getUsedItemHand() == net.minecraft.world.InteractionHand.OFF_HAND) {
                arm = pEntity.getMainArm() == HumanoidArm.RIGHT ? leftArm : rightArm;
            }
            
            applyArmRaise(arm, chargeProgress);
            applyArmShake(arm, pAgeInTicks, chargeProgress);
        }
        
        if (pEntity instanceof Player player &&
            pEntity.attackAnim > 0 &&
            player.getCooldowns().isOnCooldown(ModItems.JAVELIN.get())) {
            
            applyArmThrow(pEntity);
        }
    }

    private void applyArmRaise(ModelPart arm, float chargeProgress) {
        float raiseLerp = Mth.clamp(chargeProgress * 5.0f, 0.0f, 1.0f);
        arm.xRot = Mth.lerp(raiseLerp, 0.0f, arm.xRot);
        arm.zRot += (1.0f - raiseLerp) * -0.2f;
    }

    private void applyArmThrow(T entity) {
        float swingProgress = entity.getAttackAnim(Minecraft.getInstance().getFrameTime());
        ModelPart arm = entity.getMainArm() == HumanoidArm.RIGHT ? rightArm : leftArm;

        if (entity.swingingArm == net.minecraft.world.InteractionHand.OFF_HAND) {
            arm = entity.getMainArm() == HumanoidArm.RIGHT ? leftArm : rightArm;
        }

        float followThroughX = -2.0f + (swingProgress * 2.8f);
        arm.xRot = followThroughX;
        arm.zRot += Mth.sin(swingProgress * (float)Math.PI) * 0.2f;
    }


    private void applyArmShake(ModelPart arm, float ageInTicks, float chargeProgress) {
        if (chargeProgress > 0.2f) {
            float intensity = (chargeProgress - 0.2f) * 0.015f;
            float shakeX = Mth.sin(ageInTicks * 3.5f) * intensity;
            float shakeY = Mth.cos(ageInTicks * 4.0f) * intensity;

            arm.xRot += shakeX;
            arm.zRot += shakeY;
        }
    }
}
