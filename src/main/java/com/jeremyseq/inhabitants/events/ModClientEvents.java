package com.jeremyseq.inhabitants.events;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.InhabitantsAudio;
import com.jeremyseq.inhabitants.ModSoundEvents;
import com.jeremyseq.inhabitants.effects.ModEffects;
import com.jeremyseq.inhabitants.networking.AscendPacketC2S;
import com.jeremyseq.inhabitants.networking.ModNetworking;

import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.RenderBlockScreenEffectEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent;

@Mod.EventBusSubscriber(modid = Inhabitants.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ModClientEvents {

    public static boolean isMuffled = false;
    private static boolean wasMuffled = false;
    private static boolean lastJumpState = false;
    private static BlockPos lastPhasingPos = null;
    public static float muffleLerp = 0.0F;

    @SubscribeEvent
    public static void onRenderBlockOverlay(RenderBlockScreenEffectEvent event) {
        if (event.getOverlayType() == RenderBlockScreenEffectEvent.OverlayType.BLOCK) {
            if (event.getPlayer() != null &&
                event.getPlayer().hasEffect(ModEffects.IMMATERIAL.get())) {
                event.setCanceled(true);
            }
        }
    }

    // handle immaterial effect
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Minecraft mc = Minecraft.getInstance();
            HandleImmaterialEffect(mc);
            
        }
    }

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
            
        if (isMuffled) {
            muffleLerp = Math.min(1.0F, muffleLerp + 0.1F);
        } else {
            muffleLerp = Math.max(0.0F, muffleLerp - 0.1F);
        }
            
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
        } else {
            lastJumpState = false;
        }
            
        // apply lowpass filter
        InhabitantsAudio.updateFilter(muffleLerp);
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
}
