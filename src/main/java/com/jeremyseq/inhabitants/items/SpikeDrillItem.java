package com.jeremyseq.inhabitants.items;

import com.jeremyseq.inhabitants.animation.FPVAnimationPlayer;
import com.jeremyseq.inhabitants.audio.*;
import com.jeremyseq.inhabitants.enchantments.ModEnchantments;
import com.jeremyseq.inhabitants.networking.*;
import com.jeremyseq.inhabitants.Inhabitants;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.inventory.ClickAction;

import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.ItemStackedOnOtherEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.mojang.blaze3d.vertex.PoseStack;

import org.jetbrains.annotations.*;

import java.util.*;
import java.util.function.Consumer;

public class SpikeDrillItem extends PickaxeItem {
    public static final int baseTemperatureMax = 120;
    public static final String TAG_TEMPERATURE = "inhabitants:drill_temperature";
    public static final String TAG_LAST_USED = "inhabitants:drill_last_used";
    
    private static final Map<UUID, Long> clientStartTicks = new HashMap<>();
    private static final Map<UUID, Long> clientLastUseTicks = new HashMap<>();

    private static final int ticksPerSecond = 20;
    private static final int missTicks = 5;
    private static final int overheatCooldownTicks = 2 * ticksPerSecond;
    private static final int snowballCooldownTicks = ticksPerSecond;
    private static final int useDurationTicks = 60 * 60 * ticksPerSecond;
    
    private static final int drillPacketInterval = 1;

    public SpikeDrillItem(Properties properties) {
        super(Tiers.IRON, 1, -2.8F, properties);
    }

    @Override
    public boolean isValidRepairItem(
        @NotNull ItemStack toRepair,
        @NotNull ItemStack repair
    ) {
        return repair.is(ModItems.IMPALER_SPIKE.get()) ||
            super.isValidRepairItem(toRepair, repair);
    }

    @Override
    public float getDestroySpeed(
        @NotNull ItemStack pStack,
        @NotNull BlockState pState
    ) {
        return 0.0F;
    }

    @Override
    public boolean canAttackBlock(
        @NotNull BlockState pState,
        @NotNull Level pLevel,
        @NotNull BlockPos pPos,
        @NotNull Player pPlayer
    ) {
        return false;
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) { return UseAnim.CUSTOM; }

    @Override
    public boolean shouldCauseReequipAnimation(
        ItemStack oldStack,
        ItemStack newStack,
        boolean slotChanged
    ) {
        return oldStack.getItem() != newStack.getItem() || slotChanged;
    }

    @Override
    public boolean isCorrectToolForDrops(
        @NotNull ItemStack stack,
        @NotNull BlockState state
    ) {
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) {

            return EnchantmentHelper.getItemEnchantmentLevel(
                ModEnchantments.DIAMOND_TIP.get(), stack) > 0;
        }

        return super.isCorrectToolForDrops(stack, state);
    }

    public static int getTemperatureMax(ItemStack stack) {
        int level = EnchantmentHelper.getItemEnchantmentLevel(
            ModEnchantments.THERMAL_CAPACITY.get(), stack);

        return baseTemperatureMax * (1 + level);
    }

    public static int getTemperature(ItemStack stack) {
        if (stack.hasTag()) {
            assert stack.getTag() != null;
            return stack.getTag().getInt(TAG_TEMPERATURE);
        } else {
            return 0;
        }
    }

    public static void setTemperature(ItemStack stack, int temperature) {
        stack.getOrCreateTag().putInt(
            TAG_TEMPERATURE,
            Math.max(0, Math.min(getTemperatureMax(stack), temperature)));
    }

    public static void addTemperature(
        ItemStack stack,
        int amount,
        long gameTime
    ) {
        int current = getTemperature(stack);
        setTemperature(stack, current + amount);
        stack.getOrCreateTag().putLong(TAG_LAST_USED, gameTime);
    }

    @Override
    public void onUseTick(
        @NotNull Level level,
        @NotNull LivingEntity entity,
        @NotNull ItemStack stack,
        int count
    ) {
        if (entity instanceof Player player) {
            BlockHitResult hit = getPlayerPOVHitResult(
                level,
                player,
                ClipContext.Fluid.NONE
            );

            boolean overBlock = hit.getType() == HitResult.Type.BLOCK;
            double reach = player.getAttributeValue(ForgeMod.BLOCK_REACH.get());
            boolean inReach = hit.getLocation()
                .distanceTo(player.getEyePosition()) <= reach;

            if (!overBlock || !inReach) {
                int missTicks = stack.getOrCreateTag().getInt("drill_missTicks") + 1;

                if (missTicks > missTicks) {
                    player.releaseUsingItem();
                    stack.getOrCreateTag().putInt("drill_missTicks", 0);
                    return;
                } else {
                    stack.getOrCreateTag().putInt("drill_missTicks", missTicks);
                }
            } else {
                stack.getOrCreateTag().putInt("drill_missTicks", 0);
            }

            if (level.isClientSide) {
                clientStartTicks.putIfAbsent(player.getUUID(), level.getGameTime());
                clientLastUseTicks.put(player.getUUID(), level.getGameTime());

                int elapsed = useDurationTicks - count;
                if (elapsed % drillPacketInterval == 0) {
                    ModNetworking.sendToServer(new DrillDamagePacketC2S());
                }
            }
        }
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(
        @NotNull Level level,
        @NotNull Player player,
        @NotNull InteractionHand hand
    ) {
        ItemStack drill = player.getItemInHand(hand);
        ItemStack otherHand = player.getItemInHand(hand == InteractionHand.MAIN_HAND ?
            InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        
        if (otherHand.getItem() == Items.SNOWBALL) {
            return InteractionResultHolder.fail(drill);
        }

        // overheat
        if (getTemperature(drill) >= getTemperatureMax(drill)) {
            player.getCooldowns().addCooldown(this, overheatCooldownTicks);
            if (!level.isClientSide) {
                player.hurt(level.damageSources().onFire(), 2.0f);

                level.playSound(null,
                    player.blockPosition(),
                    SoundEvents.LAVA_EXTINGUISH,
                    SoundSource.PLAYERS,
                    1.0f,
                    1.0f
                );
            }

            player.displayClientMessage(
                Component.translatable("tooltip.inhabitants.spike_drill.overheated")
                .withStyle(ChatFormatting.RED), true);
            
            return InteractionResultHolder.fail(drill);
        }
        
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);

        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.fail(drill);
        }
        
        double reach = player.getAttributeValue(ForgeMod.BLOCK_REACH.get());
        if (hit.getLocation().distanceTo(player.getEyePosition()) > reach) {
            return InteractionResultHolder.fail(drill);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(drill);
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack) {
        return useDurationTicks;
    }

    @Override
    public void releaseUsing(
        @NotNull ItemStack stack,
        @NotNull Level level,
        @NotNull LivingEntity entityLiving,
        int timeLeft
    ) {
        super.releaseUsing(stack, level, entityLiving, timeLeft);
        
        if (!level.isClientSide) {
            DrillDamagePacketC2S.clearMomentum(entityLiving);
        }
    }

    public static float calculatingDrillSpeed(Player player, ItemStack stack, float partialTick) {
        if (player == null) return 0f;
        
        Long start = clientStartTicks.get(player.getUUID());
        Long last  = clientLastUseTicks.get(player.getUUID());
        
        if (start == null || last == null) return 0f;
        
        if (player.level().getGameTime() - last > 40) {
            clientStartTicks.remove(player.getUUID());
            return 0f;
        }

        float duration = (player.level().getGameTime() - start) + partialTick;
        return Math.min(1.0f, duration / (float) DrillDamagePacketC2S.RAMP_UP_TICKS);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private boolean wasOverheated = false;
            private boolean initialized = false;
            private long miningStartTick = -1;

            private void init(Player player) {
                if (initialized) return;
                initialized = true;

                FPVAnimationPlayer.INSTANCE.setTriggerCallback((id, p, arm, stack) -> {
                    if ("inhabitants:drill_dig".equals(id)) {
                        HitResult hit = Minecraft.getInstance().hitResult;

                        if (hit instanceof BlockHitResult bhr) {
                            BlockPos pos = bhr.getBlockPos();
                            BlockState state = p.level().getBlockState(pos);
                            SoundType soundType = state.getSoundType();
                            
                            float ratio = calculatingDrillSpeed(p, stack, 0f);
                            float pitch = (soundType.getPitch() * 1.2F) + (ratio * 0.8F);
                            float volume = soundType.getVolume() - (0.3f * p.getRandom().nextFloat());

                            p.level().playLocalSound(
                                pos.getX(), pos.getY(), pos.getZ(),
                                soundType.getHitSound(),
                                SoundSource.BLOCKS,
                                volume,
                                pitch,
                                false
                            );
                            
                            for (int i = 0; i < 5; i++) {
                                Minecraft.getInstance().particleEngine
                                    .addBlockHitEffects(pos, bhr);
                            }
                        }
                    } else if ("inhabitants:drill_start_sound".equals(id)) {

                        p.level().playLocalSound(
                            p.getX(), p.getY(), p.getZ(),
                            ModSoundEvents.DRILL_START.get(), SoundSource.PLAYERS,
                            0.25f, 1.0f, false);
                        
                    } else if ("inhabitants:drill_loop_sound".equals(id)) {
                        if (ModTickableSounds.DrillLoop.currentSound == null ||
                            ModTickableSounds.DrillLoop.currentSound.isStopped()) {
                            
                            ModTickableSounds.DrillLoop.currentSound = new ModTickableSounds.DrillLoop(p);
                            Minecraft.getInstance().getSoundManager()
                                .play(ModTickableSounds.DrillLoop.currentSound);
                        }
                    } else if ("inhabitants:drill_stop_sound".equals(id)) {
                        if (ModTickableSounds.DrillLoop.currentSound != null) {

                            ModTickableSounds.DrillLoop.currentSound.stopLoop();
                            ModTickableSounds.DrillLoop.currentSound = null;
                        }

                        p.level().playLocalSound(
                            p.getX(), p.getY(), p.getZ(),
                            ModSoundEvents.DRILL_STOPPED.get(), SoundSource.PLAYERS,
                            0.25f, 1.0f, false);
                    }
                });
            }

            @Override
            public boolean applyForgeHandTransform(
                PoseStack poseStack,
                LocalPlayer player,
                HumanoidArm arm,
                ItemStack itemInHand,
                float partialTick,
                float equipProcess,
                float swingProcess
            ) {
                init(player);

                boolean isOverheated = SpikeDrillItem.getTemperature(itemInHand) >=
                    SpikeDrillItem.getTemperatureMax(itemInHand);
                
                if (isOverheated && !wasOverheated) {
                    FPVAnimationPlayer.INSTANCE.playOverridePhase(arm, "spike_drill_overheat");
                }

                wasOverheated = isOverheated;

                float ratio = calculatingDrillSpeed(player, itemInHand, partialTick);
                float speedMultiplier = 0.9f;
                
                if (ratio >= 0.1f) {
                    float alpha = (ratio - 0.1f) / 0.9f;
                    speedMultiplier = Mth.lerp(alpha, 0.9f, 1.5f);
                }

                FPVAnimationPlayer.INSTANCE.setSpeed(arm, speedMultiplier);

                return FPVAnimationPlayer.INSTANCE.apply("spike_drill_start",
                    poseStack,
                    player,
                    arm,
                    itemInHand,
                    partialTick,
                    equipProcess,
                    true
                );
            }
        });
    }
    
    @Override
    public void appendHoverText(
        @NotNull ItemStack stack,
        @Nullable Level level,
        @NotNull List<Component> tooltipComponents,
        @NotNull TooltipFlag isAdvanced
    ) {
        tooltipComponents.add(
            Component.translatable("tooltip.inhabitants.spike_drill.temperature",
            getTemperature(stack), getTemperatureMax(stack))
            .withStyle(getTemperature(stack) >= getTemperatureMax(stack) ?
            ChatFormatting.RED : ChatFormatting.GOLD)
        );
        tooltipComponents.add(
            Component.translatable("tooltip.inhabitants.spike_drill")
            .withStyle(ChatFormatting.GRAY)
        );
        tooltipComponents.add(Component.literal(" "));
    }

    @Mod.EventBusSubscriber(modid = Inhabitants.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class InventoryEvents {
        @SubscribeEvent
        public static void onItemStackedOnOther(ItemStackedOnOtherEvent event) {
            Player player = event.getPlayer();
            ItemStack carried = event.getCarriedItem();
            ItemStack stackedOn = event.getStackedOnItem();

            ItemStack drill = ItemStack.EMPTY;
            ItemStack snowball = ItemStack.EMPTY;

            if (carried.is(Items.SNOWBALL) &&
                stackedOn.getItem() instanceof SpikeDrillItem) {
                snowball = carried;
                drill = stackedOn;
            } else if (carried.getItem() instanceof SpikeDrillItem &&
                stackedOn.is(Items.SNOWBALL)) {
                drill = carried;
                snowball = stackedOn;
            }

            if (!drill.isEmpty() && !snowball.isEmpty() &&
                event.getClickAction() == ClickAction.PRIMARY) {
                if (getTemperature(drill) > 0) {
                    addTemperature(drill, -20, player.level().getGameTime());
                    
                    if (!player.isCreative()) {
                        snowball.shrink(1);
                    }

                    player.level().playSound(null, player.blockPosition(), 
                        SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 1.0f);
                    
                    player.getCooldowns().addCooldown(ModItems.SPIKE_DRILL.get(), 20);
                    
                    event.setCanceled(true);
                }
            }
        }
    }
}
