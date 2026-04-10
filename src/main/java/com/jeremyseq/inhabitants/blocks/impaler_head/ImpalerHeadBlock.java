package com.jeremyseq.inhabitants.blocks.impaler_head;

import com.jeremyseq.inhabitants.blocks.entity.ModBlockEntities;

import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ImpalerHeadBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    protected static final VoxelShape SHAPE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 8.0D, 12.0D);

    public ImpalerHeadBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(POWERED, false));
    }

    @Override
    public @NotNull VoxelShape getShape(
            @NotNull BlockState pState,
            @NotNull BlockGetter pLevel,
            @NotNull BlockPos pPos,
            @NotNull CollisionContext pContext) {
        return SHAPE;
    }

    @Override
    public @NotNull InteractionResult use(
            @NotNull BlockState pState,
            @NotNull Level pLevel,
            @NotNull BlockPos pPos,
            @NotNull Player pPlayer,
            @NotNull InteractionHand pHand,
            @NotNull BlockHitResult pHit
    ) {
        return InteractionResult.PASS;
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState pState) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pPos, @NotNull BlockState pState) {
        return new ImpalerHeadBlockEntity(pPos, pState);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState()
            .setValue(FACING, pContext.getHorizontalDirection().getOpposite())
            .setValue(POWERED, pContext.getLevel().hasNeighborSignal(pContext.getClickedPos()));
    }

    @Override
    public void neighborChanged(
            @NotNull BlockState pState,
            Level pLevel,
            @NotNull BlockPos pPos,
            @NotNull Block pBlock,
            @NotNull BlockPos pFromPos,
            boolean pIsMoving
    ) {
        if (!pLevel.isClientSide) {
            boolean isPowered = pLevel.hasNeighborSignal(pPos);
            if (isPowered != pState.getValue(POWERED)) {
                pLevel.setBlock(pPos, pState.setValue(POWERED, isPowered), 3);
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING, POWERED);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            @NotNull Level pLevel,
            @NotNull BlockState pState,
            @NotNull BlockEntityType<T> pBlockEntityType
    ) {
        return createTickerHelper(pBlockEntityType, ModBlockEntities.IMPALER_HEAD_BLOCK_ENTITY.get(),
                ImpalerHeadBlockEntity::tick);
    }
}
