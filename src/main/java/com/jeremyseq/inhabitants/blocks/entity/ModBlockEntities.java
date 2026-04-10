package com.jeremyseq.inhabitants.blocks.entity;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.blocks.ModBlocks;
import com.jeremyseq.inhabitants.blocks.impaler_head.ImpalerHeadRenderer;
import com.jeremyseq.inhabitants.blocks.impaler_head.ImpalerHeadBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Inhabitants.MODID);

    public static final RegistryObject<BlockEntityType<ImpalerHeadBlockEntity>> IMPALER_HEAD_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("impaler_head_block_entity",
                () -> BlockEntityType.Builder.of(ImpalerHeadBlockEntity::new,
                ModBlocks.IMPALER_HEAD.get(), ModBlocks.DRIPSTONE_IMPALER_HEAD.get()).build(null));


    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        BlockEntityRenderers.register(IMPALER_HEAD_BLOCK_ENTITY.get(),
                context -> new ImpalerHeadRenderer());
    }
}
