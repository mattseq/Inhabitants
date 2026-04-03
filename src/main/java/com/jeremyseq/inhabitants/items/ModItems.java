package com.jeremyseq.inhabitants.items;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.audio.ModSoundEvents;
import com.jeremyseq.inhabitants.entities.ModEntities;
import com.jeremyseq.inhabitants.items.food.*;
import com.jeremyseq.inhabitants.items.javelin.JavelinItem;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Inhabitants.MODID);

    public static final RegistryObject<Item> GIANT_BONE = ITEMS.register("giant_bone",
            GiantBoneItem::new);

    public static final RegistryObject<Item> FISH_SNOT_CHOWDER = ITEMS.register("fish_snot_chowder",
            FishSnotChowderItem::new);
    public static final RegistryObject<Item> UNCANNY_POTTAGE = ITEMS.register("uncanny_pottage",
            UncannyPottageItem::new);
    public static final RegistryObject<Item> MARINATED_SPIDER = ITEMS.register("marinated_spider",
            MarinatedSpiderItem::new);
    public static final RegistryObject<Item> BAKED_BRAINS = ITEMS.register("baked_brains",
            BakedBrainsItem::new);
    public static final RegistryObject<Item> DIMENSIONAL_SERVING = ITEMS.register("dimensional_serving",
            DimensionalServingItem::new);

    public static final RegistryObject<Item> CREATIVE_TAB = ITEMS.register("creative_tab",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> BOGRE_SPAWN_EGG = ITEMS.register("bogre_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.BOGRE, 0x36786A, 0xA35242, new Item.Properties()));

    public static final RegistryObject<Item> WARPED_CLAM_ITEM = ITEMS.register("warped_clam",
            () -> new WarpedClamItem(new Item.Properties()));

    public static final RegistryObject<Item> IMPALER_SPAWN_EGG = ITEMS.register("impaler_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.IMPALER, 0x969090, 0x9d9382, new Item.Properties()));

    public static final RegistryObject<Item> IMPALER_SPIKE = ITEMS.register("impaler_spike",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> MUSIC_DISC_BOGRE = ITEMS.register("music_disc_bogre",
            () -> new RecordItem(6, ModSoundEvents.BOGRE_SONG, new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 2100));

    public static final RegistryObject<Item> TOTEM_OF_OFFERING = ITEMS.register("totem_of_offering",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)) {
                @Override
                public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
                    super.appendHoverText(stack, level, tooltip, flag);
                    tooltip.add(Component.translatable("item.inhabitants.totem_of_offering.desc").withStyle(ChatFormatting.GRAY));
                }
            });

    public static final RegistryObject<Item> JAVELIN = ITEMS.register("javelin",
            () -> new JavelinItem(new Item.Properties().stacksTo(16)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
