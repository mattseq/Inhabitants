package com.jeremyseq.inhabitants.compat.jei;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.recipe.CarvingRecipe;
import com.jeremyseq.inhabitants.gui.components.ModGuiTextures;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;

import org.jetbrains.annotations.NotNull;

public class CarvingRecipeCategory implements IRecipeCategory<CarvingRecipe> {
    private final IDrawable background;
    private final IDrawable icon;
    private final Component title;

    public CarvingRecipeCategory(IGuiHelper helper) {
        this.title = Component.translatable("gui.inhabitants.jei.carving");
        this.background = helper.createBlankDrawable(160, 60);

        this.icon = helper.drawableBuilder(
            ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/misc/bogre_face_icon.png"), 
            0, 0, 16, 16)
            .setTextureSize(16, 16)
            .build();
    }

    @Override
    public @NotNull RecipeType<CarvingRecipe> getRecipeType() {
        return InhabitantsJEIPlugin.CARVING;
    }

    @Override
    public @NotNull Component getTitle() {
        return title;
    }

    @Override
    public @NotNull IDrawable getBackground() {
        return background;
    }

    @Override
    public @NotNull IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CarvingRecipe recipe, @NotNull IFocusGroup focuses) {
        // block
        builder.addSlot(RecipeIngredientRole.INPUT, 11, 11)
                .addItemStack(new ItemStack(recipe.triggerBlock().asItem()));

        // result
        builder.addSlot(RecipeIngredientRole.OUTPUT, 131, 11)
                .addItemStack(recipe.result());
    }

    @Override
    public void draw(
        CarvingRecipe recipe, 
        @NotNull IRecipeSlotsView recipeSlotsView,
        @NotNull GuiGraphics guiGraphics,
        double mouseX, 
        double mouseY) {
        
        int slotSize = 18;
        int bgWidth = background.getWidth();
        Font font = Minecraft.getInstance().font;
        
        guiGraphics.blit(
            ModGuiTextures.SLOT, 
            10, 10, 0, 0, 
            slotSize, slotSize, slotSize, slotSize);
        guiGraphics.blit(
            ModGuiTextures.SLOT, 
            130, 10, 0, 0, 
            slotSize, slotSize, slotSize, slotSize);
        
        String arrow = "→";
        guiGraphics.drawString(font,
            Component.literal(arrow).withStyle(ChatFormatting.GRAY), 
            (bgWidth - font.width(arrow)) / 2, 
            15, 
            0x808080, 
            true);

        Component instruction = Component.literal("Place " + recipe.requiredBlocks() + " blocks horizontally")
                .withStyle(ChatFormatting.DARK_GRAY);
        Component instruction2 = Component.literal("place next to bogre")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
        
        guiGraphics.drawString(font, instruction, (bgWidth - font.width(instruction)) / 2, 40, 0x404040, false);
        guiGraphics.drawString(font, instruction2, (bgWidth - font.width(instruction2)) / 2, 50, 0x808080, false);

        Component hitsText = Component.literal("x" + recipe.hammer_hits() + " Hits").withStyle(ChatFormatting.GRAY);
        guiGraphics.drawString(font, hitsText, (bgWidth - font.width(hitsText)) / 2, 25, 0x808080, false);
    }
}
