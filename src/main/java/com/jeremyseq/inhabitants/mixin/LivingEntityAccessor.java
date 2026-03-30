package com.jeremyseq.inhabitants.mixin;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("activeEffects")
    Map<MobEffect, MobEffectInstance> inhabitants$getActiveEffects();

    @Accessor("jumping")
    boolean inhabitants$isJumping();
}
