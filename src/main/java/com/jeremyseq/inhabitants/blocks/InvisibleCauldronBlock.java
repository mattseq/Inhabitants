package com.jeremyseq.inhabitants.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.Shapes;

import org.jetbrains.annotations.NotNull;

public class InvisibleCauldronBlock extends Block {
    private static final VoxelShape topPart = Block.box(-10, 17, -10, 26, 20, 26);
    private static final VoxelShape bottomPart = Block.box(-8, 0, -8, 24, 17, 24);

    private static final VoxelShape colShape = Shapes.or(topPart, bottomPart);

    public InvisibleCauldronBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state,
    @NotNull BlockGetter world, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return colShape;
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state,
    @NotNull BlockGetter world, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return colShape;
    }
}