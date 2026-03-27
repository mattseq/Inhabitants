package com.jeremyseq.inhabitants.entities.bogre.ai;

import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;
import com.jeremyseq.inhabitants.entities.bogre.bogre_cauldron.BogreCauldronEntity;
import com.jeremyseq.inhabitants.entities.bogre.skill.*;
import com.jeremyseq.inhabitants.entities.bogre.utilities.*;
import com.jeremyseq.inhabitants.recipe.BogreRecipeManager;
import com.jeremyseq.inhabitants.recipe.CookingRecipe;
import com.jeremyseq.inhabitants.recipe.CarvingRecipe;
import com.jeremyseq.inhabitants.recipe.TransformationRecipe;
import com.jeremyseq.inhabitants.recipe.IBogreRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.*;

/**
 * Bogre Skilling Goal
 * - Handles the logic for Bogre skills
 */
public final class BogreSkillingGoal {
    public static final float HOSTILE_RANGE = 20.0f;
    public static final float FORGET_RANGE = 40.0f;
    public static final int COOKING_START_OFFSET = 10;

    private BogreSkillingGoal() {}

    public static void aiStep(BogreEntity bogre) {
        if (bogre.getCraftingState() == BogreAi.SkillingState.DELIVERING) {
            BogreDeliveryGoal.handleDelivery(bogre);
            return;
        }

        IBogreRecipe activeRecipe = bogre.getAi().getActiveRecipe();
        if (activeRecipe == null) {
            bogre.setAIState(BogreAi.State.NEUTRAL);
            return;
        }

        if (bogre.getCraftingState() == BogreAi.SkillingState.NONE) {
            bogre.setCraftingState(BogreAi.SkillingState.MOVING_TO_TARGET);
        }

        if (bogre.getCraftingState() == BogreAi.SkillingState.PLACING_ITEM) {
            BogreSkills.forType(activeRecipe.getBogreRecipeType()).handlePlacingItem(bogre);
            return;
        }

        BogreSkills.forType(activeRecipe.getBogreRecipeType()).aiStep(bogre);
    }

    public static void handleSkills(BogreEntity bogre) {
        BogreAi ai = bogre.getAi();

        List<BogreCauldronEntity> cauldrons = bogre.level().getEntitiesOfClass(
                BogreCauldronEntity.class,
                bogre.getBoundingBox().inflate(FORGET_RANGE),
                cauldron -> cauldron.isAlive() && !cauldron.isCooking());

        for (BogreCauldronEntity cauldron : cauldrons) {
            if (cauldron.getItemCount() > 0 && cauldron.hasHeatSource() && 
                cauldron.getItemHandler().getStackInSlot(4).is(Items.BOWL)) {
                
                List<Item> items = new ArrayList<>();
                for (int i = 0; i < 4; i++) {
                    ItemStack stack = cauldron.getItemHandler().getStackInSlot(i);
                    if (!stack.isEmpty())
                        items.add(stack.getItem());
                }

                Optional<CookingRecipe> recipe = BogreRecipeManager.getCookingRecipe(items);

                if (recipe.isPresent()) {
                    if (bogre.cauldronPos == null ||
                            !bogre.isValidCauldron(bogre.cauldronPos)) {
                        bogre.cauldronPos = cauldron.blockPosition();
                    }

                    if (bogre.cauldronPos.equals(cauldron.blockPosition())) {
                        ai.enterSkilling();
                        ai.setActiveRecipe(recipe.get());
                        captureResultOwner(bogre, null); // for now, we will use nearest player
                        bogre.setCraftingState(BogreAi.SkillingState.MOVING_TO_TARGET);
                        bogre.resetCookingTicks();
                        return;
                    }
                }
            }
        }

        // --- Carving ---
        List<BlockPos> carveableBlocks = BogreDetectionHelper.findCarvableBlocks(bogre, (int) BogreAi.ROAR_RANGE);

        if (carveableBlocks != null && !carveableBlocks.isEmpty()) {
            Optional<CarvingRecipe> recipe = BogreRecipeManager.getCarvingRecipe(
                    bogre.level().getBlockState(carveableBlocks.get(0)).getBlock());
            if (recipe.isPresent()) {
                ai.enterSkilling();
                ai.setActiveRecipe(recipe.get());
                captureResultOwner(bogre, null); // for now, we will use nearest player
                bogre.setCraftingState(BogreAi.SkillingState.MOVING_TO_TARGET);
                CarvingSkill.setCarveTicks(bogre, 0);
                return;
            }
        }

        // --- Transformation ---

        ItemEntity transformationItem = BogreDetectionHelper
                .findTransformationItem(bogre, (int) TransformationSkill.detectionRadius);

        if (transformationItem != null) {
            Optional<TransformationRecipe> recipe = BogreRecipeManager
                    .getTransformationRecipe(transformationItem.getItem().getItem());
            if (recipe.isPresent()) {
                ai.enterSkilling();
                ai.setActiveRecipe(recipe.get());
                captureResultOwner(bogre, transformationItem.getOwner());
                bogre.setCraftingState(BogreAi.SkillingState.MOVING_TO_TARGET);
                TransformationSkill.setTransformationTicks(bogre, 0);
                return;
            }
        }
    }

    private static void captureResultOwner(BogreEntity bogre, Entity owner) {
        if (owner == null) {
            owner = bogre.level().getNearestPlayer(bogre, FORGET_RANGE);
        }
        if (owner != null) {
            bogre.getAi().setResultOwnerUUID(owner.getUUID());
        }
    }

    public static void throwHeldItem(BogreEntity bogre) {
        BogreUtil.throwHeldItem(bogre);
    }

    public static BogreCauldronEntity getCauldronEntity(BogreEntity bogre) {
        return bogre.getCauldronEntity();
    }

    public static ItemEntity findTransformationItem(BogreEntity bogre, int range) {
        return BogreDetectionHelper.findTransformationItem(bogre, range);
    }

    public static List<BlockPos> findCarvableBlocks(BogreEntity bogre, int radius) {
        return BogreDetectionHelper.findCarvableBlocks(bogre, radius);
    }

    public static BlockPos getAveragePosition(List<BlockPos> positions) {
        return BogreUtil.getAveragePosition(positions);
    }
}
