package com.jeremyseq.inhabitants.gui.cauldron;

import com.jeremyseq.inhabitants.ModSoundEvents;
import com.jeremyseq.inhabitants.gui.components.GuiLayout;
import com.jeremyseq.inhabitants.gui.components.ModGuiTextures;
import com.jeremyseq.inhabitants.recipe.CookingRecipe;
import com.jeremyseq.inhabitants.gui.components.RenderUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.mojang.blaze3d.systems.RenderSystem;

import org.jetbrains.annotations.NotNull;

import org.lwjgl.glfw.GLFW;

public class CauldronScreen extends AbstractContainerScreen<CauldronMenu> {
    private final CauldronRecipeBook recipeBookComponent = new CauldronRecipeBook();
    private boolean widthTooNarrow;
    private boolean hasPlayedOpenSound = false;
    private ImageButton tipButton;
    private ImageButton recipeButton;
    private GuiLayout bgLayout;
    
    public CauldronScreen(CauldronMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.widthTooNarrow = this.width < 335;

        assert this.minecraft != null;
        this.recipeBookComponent.init(this.width, this.height, this.minecraft, this.widthTooNarrow, this.menu);
        this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;

        playOpenSound();
        createTipButton();
        createRecipeButton();

        this.addRenderableWidget(this.tipButton);
        this.addRenderableWidget(this.recipeButton);
        this.addRenderableWidget(this.recipeBookComponent);
    }

    private void playOpenSound() {
        if (!this.hasPlayedOpenSound) {
            Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(ModSoundEvents.CAULDRON_GUI_OPEN.get(), 1.0F, 1.0F));
            this.hasPlayedOpenSound = true;
        }
    }

    private void createTipButton() {
        GuiLayout btn = new GuiLayout(this.leftPos, this.topPos,
            this.imageWidth, this.imageHeight)
            .width("14").height("14")
            .margin(65, 9, 0, 0).align("right", "top");

        this.tipButton = new ImageButton(
                btn.x, btn.y, btn.width, btn.height, 0, 0, 0,
                ModGuiTextures.TIP_BUTTON, 14, 14, (button) -> {
                    // tips ? pfft..
                    // kinda feel lazy doing it rn
                    // TODO REMINDER: tip gui
                }
        ) {
            @Override
            public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
                ResourceLocation texture = ModGuiTextures.TIP_BUTTON;
                if (this.isHoveredOrFocused()) {
                    boolean clicking = GLFW.glfwGetMouseButton(
                        Minecraft.getInstance().getWindow().getWindow(),
                        GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS;
                    texture = clicking ? ModGuiTextures.TIP_BUTTON_PRESSED : ModGuiTextures.TIP_BUTTON_HIGHLIGHT;
                }

                RenderSystem.enableBlend();
                guiGraphics.blit(texture, this.getX(), this.getY(), 0, 0,
                    this.width, this.height, this.width, this.height);
                
                RenderSystem.disableBlend();
            }
        };
    }

    private void createRecipeButton() {
        int btnY = this.height / 2 - 49;
        this.recipeButton = new ImageButton(
            this.leftPos + 5, btnY, 20, 18, 0, 0, 19,
            ModGuiTextures.RECIPE_BUTTON, (button) -> {

            this.recipeBookComponent.toggleVisibility();
            this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
            this.recipeBookComponent.updatePosition(this.widthTooNarrow, this.width, this.imageWidth);
            this.repositionElements();

        });
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // who needs a default label when you're my label?
        // however... you can call me Bader ;)
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        renderHeatSourceTooltip(guiGraphics, mouseX, mouseY);
        renderGhostItems(guiGraphics);
        renderGhostItemTooltips(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        this.bgLayout = new GuiLayout(this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
        RenderUtil.blit(guiGraphics, ModGuiTextures.CAULDRON_GUI, bgLayout);
        
        GuiLayout fireBounds = bgLayout.createChild()
            .width("16").height("16")
            .margin(27, 0, 0, 115)
            .align("left", "top");
            
        ResourceLocation fireTex = menu.hasHeatSource() ?
            ModGuiTextures.FIRE_SOURCE_ON : ModGuiTextures.FIRE_SOURCE_OFF;
        
        RenderUtil.blit(guiGraphics, fireTex, fireBounds);


        renderSlots(guiGraphics);
        renderCookingProgress(guiGraphics);
    }

    private void renderSlots(GuiGraphics guiGraphics) {
        CookingRecipe ghostRecipe = CauldronRecipeBook.activeGhostRecipe;

        // ingredient slots 0-3
        for (int i = 0; i < 4; i++) {
            Slot slot = menu.getSlot(i);
            boolean isGhost = !slot.hasItem() && ghostRecipe != null && i < ghostRecipe.ingredients().size();
            GuiLayout slotLayout = bgLayout.createChild().width(18).height(18).offset(slot.x - 1, slot.y - 1);
            RenderUtil.blit(guiGraphics,
                isGhost ? ModGuiTextures.GHOST_SLOT : ModGuiTextures.SLOT, slotLayout);
        }

        // bowl slot 4
        Slot bowlSlot = menu.getSlot(4);
        boolean bowlIsGhost = !bowlSlot.hasItem() && ghostRecipe != null;
        GuiLayout bowlLayout = bgLayout.createChild().width(18).height(18).offset(bowlSlot.x - 1, bowlSlot.y - 1);
        RenderUtil.blit(guiGraphics, bowlIsGhost ?
            ModGuiTextures.GHOST_SLOT : ModGuiTextures.BOWL_SLOT, bowlLayout);

    }

    private void renderGhostItems(GuiGraphics guiGraphics) {
        CookingRecipe ghostRecipe = CauldronRecipeBook.activeGhostRecipe;
        if (ghostRecipe == null) return;

        long time = System.currentTimeMillis() / 50;

        // ghost ingredients
        for (int i = 0; i < ghostRecipe.ingredients().size(); i++) {
            Slot slot = menu.getSlot(i);
            if (!slot.hasItem()) {
                Item displayItem = ghostRecipe.getDisplayItem(i, time);
                if (displayItem != Items.AIR) {
                    RenderUtil.renderGhostItem(guiGraphics, new ItemStack(displayItem),
                        this.leftPos + slot.x, this.topPos + slot.y);
                }
            }
        }

        // ghost bowl
        Slot bowlSlot = menu.getSlot(4);
        if (!bowlSlot.hasItem()) {
            RenderUtil.renderGhostItem(guiGraphics, new ItemStack(Items.BOWL),
                this.leftPos + bowlSlot.x, this.topPos + bowlSlot.y);
        }
    }

    private void renderGhostItemTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        CookingRecipe ghostRecipe = CauldronRecipeBook.activeGhostRecipe;
        if (ghostRecipe == null) return;

        long time = System.currentTimeMillis() / 50;

        // ghost ingredients
        for (int i = 0; i < ghostRecipe.ingredients().size(); i++) {
            Slot slot = menu.getSlot(i);
            if (!slot.hasItem() && this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                Item displayItem = ghostRecipe.getDisplayItem(i, time);
                guiGraphics.renderTooltip(this.font, new ItemStack(displayItem), mouseX, mouseY);
                return;
            }
        }

        // ghost bowl
        Slot bowlSlot = menu.getSlot(4);
        if (!bowlSlot.hasItem() && this.isHovering(bowlSlot.x, bowlSlot.y, 16, 16, mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, new ItemStack(Items.BOWL), mouseX, mouseY);
        }
    }

    private void renderCookingProgress(GuiGraphics guiGraphics) {
        int progress = menu.getCookingProgress();
        if (progress > 0) {
            int stage = (progress - 1) / 20;
            if (stage >= 0 && stage < 8) {
                GuiLayout bubbles = bgLayout.createChild()
                    .width("16").height("32").margin(20, 0, 0, 80).align("left", "top");

                RenderUtil.blit(guiGraphics, ModGuiTextures.BUBBLES[stage], bubbles);
            }
        }
        RenderSystem.disableBlend();
    }

    private void renderHeatSourceTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        GuiLayout fireBounds = bgLayout.createChild()
        .width("16").height("16").margin(27, 0, 0, 115).align("left", "top");
        
        if (!menu.hasHeatSource() && fireBounds.isHovered(mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font,
                RenderUtil.getLabel("gui.inhabitants.missing_heat_source"),
                mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // clicking a ghost slot clears the ghost recipe
        if (CauldronRecipeBook.activeGhostRecipe != null) {
            for (int i = 0; i < 5; i++) { // 0-4 (ingredients + bowl)
                Slot slot = menu.getSlot(i);
                if (!slot.hasItem() && isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                    CauldronRecipeBook.activeGhostRecipe = null;
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
        // if there's something strange, in your neighborhood,
        // who you gonna call?
        // GHOSTBUSTERS!
    }
}