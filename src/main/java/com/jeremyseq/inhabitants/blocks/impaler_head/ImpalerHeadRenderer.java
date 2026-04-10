package com.jeremyseq.inhabitants.blocks.impaler_head;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.blocks.ModBlocks;

import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class ImpalerHeadRenderer extends GeoBlockRenderer<ImpalerHeadBlockEntity> {
    private static final ResourceLocation NORMAL_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID,
            "textures/entity/impaler_head/impaler_head_texture.png");
    private static final ResourceLocation DRIPSTONE_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID,
            "textures/entity/impaler_head/dripstone_impaler_head_texture.png");

    public ImpalerHeadRenderer() {
        super(new ImpalerHeadModel());
    }

    @Override
    public ResourceLocation getTextureLocation(ImpalerHeadBlockEntity animatable) {
        if (animatable.getBlockState().is(ModBlocks.DRIPSTONE_IMPALER_HEAD.get())) {
            return DRIPSTONE_TEXTURE;
        }

        return NORMAL_TEXTURE;
    }
}
