package com.jeremyseq.inhabitants.blocks.impaler_head;

import com.jeremyseq.inhabitants.blocks.entity.ModBlockEntities;
import com.jeremyseq.inhabitants.particles.*;
import com.jeremyseq.inhabitants.audio.ModSoundEvents;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.sounds.SoundSource;

import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.keyframe.event.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ImpalerHeadBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int attackTimer = -1;
    private boolean wasPowered = false;
    private int particleSequenceCount = 0;
    private boolean isMuted = true;

    public ImpalerHeadBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.IMPALER_HEAD_BLOCK_ENTITY.get(), pPos, pBlockState);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
            .triggerableAnim("attack", RawAnimation.begin().then("attack", Animation.LoopType.PLAY_ONCE))
            .setCustomInstructionKeyframeHandler(this::instructionKeyframeHandler)
            .setSoundKeyframeHandler(this::soundKeyframeHandler));
    }

    private void soundKeyframeHandler(SoundKeyframeEvent<ImpalerHeadBlockEntity> event) {
        if (event.getKeyframeData()
            .getSound().equals("attack_sound"))
        {
            if (this.level != null && this.level.isClientSide && !this.isMuted)
            {
                this.level.playLocalSound(
                    this.worldPosition.getX() + 0.5,
                    this.worldPosition.getY() + 0.5,
                    this.worldPosition.getZ() + 0.5,
                    ModSoundEvents.IMPALER_HEAD_ATTACK.get(), SoundSource.BLOCKS,
                    0.4F, 1.0F, false);
            }
        }
    }

    private void instructionKeyframeHandler(CustomInstructionKeyframeEvent<ImpalerHeadBlockEntity> event) {
        String instructions = event.getKeyframeData().getInstructions();
        if (instructions.contains("trigger_attack")) {
            if (this.level != null && this.level.isClientSide) {
                this.particleSequenceCount = 3;
            }
        }
    }

    private void spawnSingleAttackParticle() {
        if (!(this.level instanceof ClientLevel clientLevel)) return;

        Direction facing = this.getBlockState().getValue(ImpalerHeadBlock.FACING);
        double centerX = this.worldPosition.getX() + 0.5;
        double centerY = this.worldPosition.getY() + 0.15;
        double centerZ = this.worldPosition.getZ() + 0.5;
        
        double startX = centerX + facing.getStepX() * 0.4;
        double startY = centerY + facing.getStepY() * 0.15;
        double startZ = centerZ + facing.getStepZ() * 0.4;
        
        double baseSpeed = 0.2;
        double vx = facing.getStepX() * baseSpeed;
        double vy = facing.getStepY() * baseSpeed;
        double vz = facing.getStepZ() * baseSpeed;

        clientLevel.addParticle(ModParticles.IMPALER_HEAD_ATTACK.get(),
            startX, startY, startZ,
            vx, vy, vz);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag pTag) {
        super.saveAdditional(pTag);
        pTag.putBoolean("isMuted", this.isMuted);
    }

    @Override
    public void load(@NotNull CompoundTag pTag) {
        super.load(pTag);
        this.isMuted = pTag.getBoolean("isMuted");
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putBoolean("isMuted", this.isMuted);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public boolean isMuted() {
        return isMuted;
    }

    public void setMuted(boolean muted) {
        isMuted = muted;
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(
                this.worldPosition,
                this.getBlockState(),
                this.getBlockState(),
                3);
        }
    }

    public static void tick(
        Level level,
        BlockPos pos,
        BlockState state,
        ImpalerHeadBlockEntity blockEntity
    ) {
        if (level.isClientSide) {
            boolean isPowered = state.getValue(ImpalerHeadBlock.POWERED);
            if (isPowered && !blockEntity.wasPowered) {
                ImpalerSpikeRaiseParticle.spawnSpikeBurst((ClientLevel) level,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    0.6, 15);
            }

            blockEntity.wasPowered = isPowered;
            if (blockEntity.particleSequenceCount > 0) {
                blockEntity.spawnSingleAttackParticle();
                blockEntity.particleSequenceCount--;
            }
        }

        if (blockEntity.attackTimer > 0) {
            blockEntity.attackTimer--;
            
            if (blockEntity.attackTimer == 18) {
                blockEntity.performAttack();
            }
            
            if (blockEntity.attackTimer == 0) {
                blockEntity.attackTimer = -1;
            }
        }
        
        if (blockEntity.attackTimer == -1 &&
            state.getValue(ImpalerHeadBlock.POWERED)) {
            blockEntity.triggerAttack();
        }
    }

    public void triggerAttack() {
        if (this.level == null ||
            this.level.isClientSide ||
            this.attackTimer != -1) return;

        this.triggerAnim("attack_controller", "attack");

        this.attackTimer = 23;
    }

    private void performAttack() {
        if (this.level == null || this.level.isClientSide) return;
        
        BlockState state = this.getBlockState();
        if (state.getBlock() instanceof ImpalerHeadBlock) {
            Direction facing = state.getValue(ImpalerHeadBlock.FACING);
            BlockPos targetPos = this.worldPosition.relative(facing);

            AABB area = new AABB(targetPos);
            List<LivingEntity> entities = this.level.getEntitiesOfClass(LivingEntity.class, area);

            for (LivingEntity entity : entities) {
                entity.hurt(this.level.damageSources().magic(), 1.0F);
            }
        }
    }
}
