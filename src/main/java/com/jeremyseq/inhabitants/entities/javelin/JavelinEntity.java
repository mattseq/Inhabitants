package com.jeremyseq.inhabitants.entities.javelin;

import com.jeremyseq.inhabitants.audio.ModSoundEvents;
import com.jeremyseq.inhabitants.entities.ModEntities;
import com.jeremyseq.inhabitants.items.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class JavelinEntity extends AbstractArrow implements GeoEntity {
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    
    private static final EntityDataAccessor<Integer> bounceIndex =
        SynchedEntityData.defineId(JavelinEntity.class, EntityDataSerializers.INT);
    
    private int bounceCooldown = 0;
    private int lastBounceIndex = 0;
    private BlockPos stuckBlockPos = null;

    private float charge = 0.0f;
    private float baseDamage = 3.0f;
    private int baseKnockback = 2;

    public JavelinEntity(EntityType<? extends JavelinEntity> type, Level level) {
        super(type, level);
        this.setBaseDamage(baseDamage);
        this.setKnockback(baseKnockback);
        this.pickup = Pickup.DISALLOWED;
    }

    public JavelinEntity(Level level, LivingEntity shooter, ItemStack stack) {
        super(ModEntities.JAVELIN.get(), shooter, level);
        this.setBaseDamage(3.0d);
        this.setKnockback(2);
        this.pickup = Pickup.DISALLOWED;
    }

    @Override
    public void playerTouch(@NotNull Player pPlayer) {
        if (!this.level().isClientSide &&
            (this.inGround || this.isNoGravity()) && this.shakeTime <= 0) {
            if (pPlayer.isCrouching()) {
                if (pPlayer.getInventory().add(this.getPickupItem())) {
                    pPlayer.take(this, 1);
                    this.discard();
                }
            }
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(bounceIndex, 0);
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide &&
            this.inGround) {
            if (stuckBlockPos == null) {
                BlockPos currentPos = this.blockPosition();
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            BlockPos checkPos = currentPos.offset(x, y, z);
                            if (!this.level().getBlockState(checkPos).isAir()) {
                                this.stuckBlockPos = checkPos;
                                break;
                            }
                        }
                    }
                }
            }

            if (stuckBlockPos != null &&
                this.level().getBlockState(stuckBlockPos).isAir()) {
                this.spawnAtLocation(this.getPickupItem());
                this.discard();
                return;
            }
        }

        super.tick();

        if (!this.level().isClientSide) {
            if (this.inGround) {
                if (bounceCooldown > 0) {
                    bounceCooldown--;
                } else {
                    checkBounce();
                }
            }
        }
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        super.remove(reason);
    }

    private void checkBounce() {
        AABB box = this.getBoundingBox().inflate(0.2, 0.2, 0.2).move(0, 0.2, 0);
        List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class, box);
        
        for (LivingEntity entity : entities) {
            if (entity.getDeltaMovement().y < 0) {
                launch(entity);
                break;
            }
        }
    }

    private void launch(LivingEntity entity) {
        entity.setDeltaMovement(entity.getDeltaMovement().x,
        0.85,
        entity.getDeltaMovement().z);

        entity.hurtMarked = true;
        entity.resetFallDistance();
        
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
            ModSoundEvents.JAVELIN_BOUNCE.get(), SoundSource.NEUTRAL, 1.0f, 1.0f);
        
        this.bounceCooldown = 10;
        
        if (!this.level().isClientSide) {
            this.entityData.set(bounceIndex, this.entityData.get(bounceIndex) + 1);
        }
    }


    @Override
    protected void onHitBlock(@NotNull BlockHitResult pResult) {
        super.onHitBlock(pResult);
        if (!this.level().isClientSide) {
            this.stuckBlockPos = pResult.getBlockPos();

            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSoundEvents.JAVELIN_ON_BLOCK_HIT.get(), SoundSource.NEUTRAL, 1.0f, 1.0f);
            
            this.entityData.set(bounceIndex, this.entityData.get(bounceIndex) + 1);
        }
    }

    @Override
    protected boolean canHitEntity(@NotNull Entity pEntity) {
        return super.canHitEntity(pEntity);
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult pResult) {
        Entity target = pResult.getEntity();
        if (target instanceof LivingEntity living) {
            if (!this.level().isClientSide) {
                float velocity = (float)this.getDeltaMovement().length();
                int damage = Mth.ceil(baseDamage + this.getCharge());
                
                DamageSource damageSource = this.damageSources()
                    .arrow(this, this.getOwner() == null ? this : this.getOwner());
                
                if (this.getOwner() instanceof LivingEntity owner) {
                    owner.setLastHurtMob(living);
                }
                
                int knockback = baseKnockback + Mth.floor(this.getCharge() * 2.0f);
                this.setKnockback(knockback);

                if (living.hurt(damageSource, (float)damage)) {
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            ModSoundEvents.JAVELIN_ON_ENTITY_HIT.get(), SoundSource.NEUTRAL, 1.0f, 1.0f);
                    
                    this.discard();
                }
            }
        }
    }

    @Override
    protected @NotNull SoundEvent getDefaultHitGroundSoundEvent() {
        return ModSoundEvents.JAVELIN_ON_BLOCK_HIT.get();
    }

    @Override
    protected @NotNull ItemStack getPickupItem() {
        return new ItemStack(ModItems.JAVELIN.get());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main_controller", 0, state -> {
            boolean inAir = !this.inGround;
            
            if (inAir) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("in_air"));
            }
            return PlayState.STOP;
        }));

        controllers.add(new AnimationController<>(this, "bounce_controller", 0, state -> {
            int currentBounce = this.entityData.get(bounceIndex);

            if (currentBounce != lastBounceIndex) {
                lastBounceIndex = currentBounce;
                state.getController().forceAnimationReset();
                return state.setAndContinue(RawAnimation.begin().thenPlay("bounce"));
            }
            
            if (state.getController().getAnimationState() != AnimationController.State.STOPPED) {
                return PlayState.CONTINUE;
            }
            
            return PlayState.STOP;
        }));
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        if (this.stuckBlockPos != null) {
            pCompound.putInt("StuckX", this.stuckBlockPos.getX());
            pCompound.putInt("StuckY", this.stuckBlockPos.getY());
            pCompound.putInt("StuckZ", this.stuckBlockPos.getZ());
        }
        pCompound.putFloat("XRotStored", this.getXRot());
        pCompound.putFloat("YRotStored", this.getYRot());
        pCompound.putFloat("Charge", this.getCharge());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("StuckX")) {
            this.stuckBlockPos = new BlockPos(pCompound.getInt("StuckX"),
                pCompound.getInt("StuckY"),
                pCompound.getInt("StuckZ"));
        }
        if (pCompound.contains("XRotStored")) {
            this.setXRot(pCompound.getFloat("XRotStored"));
            this.setYRot(pCompound.getFloat("YRotStored"));
        }
        if (pCompound.contains("Charge")) {
            this.setCharge(pCompound.getFloat("Charge"));
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public void setCharge(float charge) {
        this.charge = Mth.clamp(charge, 0.0f, 1.0f);
    }

    public float getCharge() {
        return this.charge;
    }
}
