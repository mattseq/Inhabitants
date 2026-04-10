package com.jeremyseq.inhabitants.events;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.audio.InhabitantsAudio;
import com.jeremyseq.inhabitants.audio.ModSoundEvents;
import com.jeremyseq.inhabitants.effects.ModEffects;
import com.jeremyseq.inhabitants.networking.AscendPacketC2S;
import com.jeremyseq.inhabitants.networking.ModNetworking;
import com.jeremyseq.inhabitants.audio.ModTickableSounds;
import com.jeremyseq.inhabitants.util.GhostTracker;
import com.jeremyseq.inhabitants.items.SpikeDrillItem;

import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.RenderBlockScreenEffectEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.RenderHandEvent;

@Mod.EventBusSubscriber(modid = Inhabitants.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ModClientEvents {

    public static boolean isMuffled = false;
    private static boolean wasMuffled = false;
    private static boolean lastJumpState = false;
    private static boolean lastSneakState = false;
    private static BlockPos lastPhasingPos = null;
    public static float muffleLerp = 0.0F;
    private static ModTickableSounds.ConcussionBuzz concussionBuzz = null;
    private static float lastCameraYaw = 0;
    private static float lastCameraPitch = 0;
    private static boolean wasConcussed = false;

    @SubscribeEvent
    public static void onRenderBlockOverlay(RenderBlockScreenEffectEvent event) {
        if (event.getOverlayType() == RenderBlockScreenEffectEvent.OverlayType.BLOCK) {
            if (event.getPlayer() != null &&
                event.getPlayer().hasEffect(ModEffects.IMMATERIAL.get())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Minecraft mc = Minecraft.getInstance();
            HandleImmaterialEffect(mc);
            HandleConcussionEffect(mc);
            
            // track nearby entities for echo trail effect
            if (mc.level != null && mc.player != null &&
                mc.player.hasEffect(ModEffects.CONCUSSION.get())) {
                
                float curYaw = mc.player.getYRot();
                float curPitch = mc.player.getXRot();
                boolean isCameraMoving = Math.abs(curYaw - mc.player.yRotO) > 0.1F || 
                                         Math.abs(curPitch - mc.player.xRotO) > 0.1F;

                for (Entity entity : mc.level.entitiesForRendering()) {
                    if (entity != mc.player && entity.distanceToSqr(mc.player) < 1024) {
                        GhostTracker.record(entity, isCameraMoving, curYaw, curPitch);
                    }
                }
                GhostTracker.cleanup(mc.level.getGameTime());
            }
        }
    }

    // handle immaterial effect
    private static void HandleImmaterialEffect(Minecraft mc) {
        if (mc.player != null && mc.player.hasEffect(ModEffects.IMMATERIAL.get())) {
            BlockPos eyePos = BlockPos.containing(mc.player.getEyePosition());

            if (mc.player.level() != null &&
            !mc.player.level()
            .getBlockState(eyePos)
            .getCollisionShape(
            mc.player.level(),
            eyePos,
            CollisionContext.empty()).isEmpty()) {
                isMuffled = true;
            } else {
                isMuffled = false;
            }
        } else {
            isMuffled = false;
        }

        if (isMuffled && !wasMuffled) {
            mc.player.playSound(ModSoundEvents.IMMATERIAL_ENTER_WALL.get(), 1.0F, 1.0F);
        } else if (!isMuffled && wasMuffled) {
            mc.player.playSound(ModSoundEvents.IMMATERIAL_EXIT_WALL.get(), 1.0F, 1.0F);
        }

        wasMuffled = isMuffled;

        boolean isConcussed = mc.player != null && mc.player.hasEffect(ModEffects.CONCUSSION.get());
        
        if (isMuffled || isConcussed) {
            muffleLerp = Math.min(1.0F, muffleLerp + 0.1F);
        } else {
            muffleLerp = Math.max(0.0F, muffleLerp - 0.1F);
        }

        if (muffleLerp < 0.05F) muffleLerp = 0.0F;
            
        if (mc.player != null && mc.player.hasEffect(ModEffects.IMMATERIAL.get())) {
            BlockPos currentPos = mc.player.blockPosition();
            
            if (!currentPos.equals(lastPhasingPos)) {
                    
                int cx = currentPos.getX() >> 4;
                int cy = currentPos.getY() >> 4;
                int cz = currentPos.getZ() >> 4;

                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            mc.levelRenderer.setSectionDirty(cx + x, cy + y, cz + z);
                        }
                    }
                }
                lastPhasingPos = currentPos;
            }
        } else {
            lastPhasingPos = null;
        }
            
        if (mc.player != null &&
            mc.player.hasEffect(ModEffects.IMMATERIAL.get())) {
            boolean currentJumpState = mc.options.keyJump.isDown();

            if (currentJumpState && !lastJumpState) {
                ModNetworking.sendToServer(new AscendPacketC2S());
            }

            lastJumpState = currentJumpState;
            
            boolean currentSneakState = mc.player.isCrouching();
            if (currentSneakState && !lastSneakState) {
                if (isLocalPlayerInsideBlock(mc.player)) {
                    double nudgeX = mc.player.getX();
                    double nudgeY = mc.player.getY() - 0.4;
                    double nudgeZ = mc.player.getZ();
                    
                    mc.player.setPos(
                        nudgeX, 
                        nudgeY, 
                        nudgeZ
                    );
                    mc.player.setDeltaMovement(
                        mc.player.getDeltaMovement().x, 
                        -1.0, 
                        mc.player.getDeltaMovement().z
                    );
                }
            }
            lastSneakState = currentSneakState;

        } else {
            lastJumpState = false;
            lastSneakState = false;
        }
            
        // apply lowpass filter
        InhabitantsAudio.updateFilter(muffleLerp);
    }

    // handle concussion effect
    private static void HandleConcussionEffect(Minecraft mc) {
        boolean isConcussed = mc.player != null && mc.player.hasEffect(ModEffects.CONCUSSION.get());

        if (isConcussed) {
            if (concussionBuzz == null) {
                concussionBuzz = new ModTickableSounds.ConcussionBuzz(mc.player);
                mc.getSoundManager().play(concussionBuzz);
            }
            if (!wasConcussed) {

                lastCameraYaw = mc.player.getYRot();
                lastCameraPitch = mc.player.getXRot();

                mc.gameRenderer.loadEffect(
                    ResourceLocation.fromNamespaceAndPath(
                    "minecraft", 
                    "shaders/post/phosphor.json"));
            }
        } else {
            if (concussionBuzz != null) {
                if (concussionBuzz.isStopped()) {
                    concussionBuzz = null;
                }
            }
            if (wasConcussed) {
                mc.gameRenderer.shutdownEffect();
            }
        }
        wasConcussed = isConcussed;
    }

    // fade fog to black
    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (event.getCamera().getEntity() instanceof Player player) {
            if (player.hasEffect(ModEffects.IMMATERIAL.get()) && muffleLerp > 0.0F) {
                float targetFar = 20.0F;
                float finalFar = Mth.lerp(muffleLerp, event.getFarPlaneDistance(), targetFar);
                
                event.setNearPlaneDistance(0.0F);
                event.setFarPlaneDistance(finalFar);
                event.setCanceled(true);
            }
        }
    }

    // fade sky color to black
    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (event.getCamera().getEntity() instanceof Player player) {
            if (player.hasEffect(ModEffects.IMMATERIAL.get()) && muffleLerp > 0.0F) {
                event.setRed(Mth.lerp(muffleLerp, event.getRed(), 0.0F));
                event.setGreen(Mth.lerp(muffleLerp, event.getGreen(), 0.0F));
                event.setBlue(Mth.lerp(muffleLerp, event.getBlue(), 0.0F));
            }
        }
    }

    // swaying/hallucination effect
    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (event.getCamera().getEntity() instanceof Player player) {
            float targetYaw = event.getYaw();
            float targetPitch = event.getPitch();

            if (player.hasEffect(ModEffects.CONCUSSION.get()) && muffleLerp > 0.1F) {
                float ticks = 
                    (float)((double)Minecraft.getInstance().level.getGameTime() +
                    (double)event.getPartialTick());

                //camera lag "smoothing"
                float lagFactor = Mth.lerp(muffleLerp, 1.0F, 0.1F);
                lastCameraYaw = Mth.rotLerp(lagFactor, lastCameraYaw, targetYaw);
                lastCameraPitch = Mth.lerp(lagFactor, lastCameraPitch, targetPitch);
                
                event.setYaw(lastCameraYaw);
                event.setPitch(lastCameraPitch);

                // hallucination
                float roll = Mth.sin(ticks * 0.05F) * 5.0F * muffleLerp;
                event.setRoll(roll);

                float yawSway = Mth.cos(ticks * 0.03F) * 2.0F * muffleLerp;
                float pitchSway = Mth.sin(ticks * 0.04F) * 2.0F * muffleLerp;
                event.setYaw(event.getYaw() + yawSway);
                event.setPitch(event.getPitch() + pitchSway);
            } else {
                // keep smooth sync when effect is off
                lastCameraYaw = targetYaw;
                lastCameraPitch = targetPitch;
            }
        }
    }

    //hallucinatory depth
    @SubscribeEvent
    public static void onComputeFieldOfView(ViewportEvent.ComputeFov event) {
        if (event.getCamera().getEntity() instanceof Player player) {
            if (player.hasEffect(ModEffects.CONCUSSION.get()) && muffleLerp > 0.1F) {
                float ticks =
                    (float)((double)Minecraft.getInstance().level.getGameTime() +
                    (double)event.getPartialTick());

                float fov = Mth.sin(ticks * 0.08F) * 3.0F * muffleLerp;
                event.setFOV(event.getFOV() + fov);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.isUsingItem()) {

            if (event.getHand() == InteractionHand.OFF_HAND) {
                ItemStack mainHandItem = mc.player.getMainHandItem();

                if (mainHandItem.getItem() instanceof SpikeDrillItem &&
                        mc.player.getUsedItemHand() == InteractionHand.MAIN_HAND) {
                    event.setCanceled(true);
                }
            } else if (event.getHand() == InteractionHand.MAIN_HAND) {
                ItemStack offHandItem = mc.player.getOffhandItem();
                ItemStack mainHandItem = mc.player.getMainHandItem();
                
                if (offHandItem.getItem() instanceof SpikeDrillItem &&
                        mainHandItem.isEmpty() &&
                        mc.player.getUsedItemHand() == InteractionHand.OFF_HAND) {
                    event.setCanceled(true);
                }
            }
        }
    }

    private static boolean isLocalPlayerInsideBlock(Player player) {
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        
        for (double dy : new double[]{0.1D, 0.8D, 1.62D}) {
            BlockPos pos = BlockPos.containing(px, py + dy, pz);
            if (!player.level().getBlockState(pos)
                .getCollisionShape(player.level(), pos, CollisionContext.empty())
                .isEmpty()) {
                return true;
            }
        }
        return false;
    }

}
