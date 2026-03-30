package com.jeremyseq.inhabitants.events;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.entities.bogre.skill.BogreSkills;
import com.jeremyseq.inhabitants.effects.ModEffects;

import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.damagesource.DamageTypes;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

import java.util.*;

@Mod.EventBusSubscriber(modid = Inhabitants.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEvents {

    private static final Map<UUID, Boolean> SNEAK_STATES = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            // immaterial sneak
            if (event.player.hasEffect(ModEffects.IMMATERIAL.get()) &&
                !event.player.level().isClientSide) {
                HandleImmaterialSneak(event.player);
            }
        }
    }

    private static void HandleImmaterialSneak(Player player) {
        boolean isSneaking = player.isCrouching();
        boolean wasSneaking = SNEAK_STATES.getOrDefault(player.getUUID(), false);
        
        if (isSneaking && !wasSneaking) {
            // Only descend if we are actually phasing inside a solid block
            if (isInsideBlock(player)) {
                player.teleportTo(
                    player.getX(),
                    player.getY() - 1.0,
                    player.getZ()
                );
            }
        }

        SNEAK_STATES.put(player.getUUID(), isSneaking);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        SNEAK_STATES.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        BogreSkills.CARVING.onBlockBroken(event.getPlayer().level(), event.getPos());
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity() != null &&
            event.getEntity().hasEffect(ModEffects.IMMATERIAL.get())) {
            
            if (event.getSource().is(DamageTypes.IN_WALL)) {
                event.setCanceled(true);
            }
        }
    }

    private static boolean isInsideBlock(Player player) {
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        
        for (double dy : new double[]{0.1D, 0.8D, 1.62D}) {
            BlockPos pos = BlockPos.containing(px, py + dy, pz);

            if (!player.level().getBlockState(pos)
            .getCollisionShape(player.level(),
            pos,
            CollisionContext.empty()).isEmpty()) {
                return true;
            }
        }

        return false;
    }
}
