package com.itmecasey.thoroughchests.block;

import com.itmecasey.thoroughchests.ChestLevel;
import com.itmecasey.thoroughchests.ModContent;
import com.itmecasey.thoroughchests.ScriptEvents;
import com.itmecasey.thoroughchests.menu.TcChestMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** A Thorough Chest of one {@link ChestLevel}: a single block with a big inventory, vanilla lid animation and sounds. */
public class TcChestBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 14.0);

    private final ChestLevel level;

    public TcChestBlock(ChestLevel level, Properties properties) {
        super(properties);
        this.level = level;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false));
    }

    public ChestLevel level() {
        return level;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(WATERLOGGED, fluid.is(Fluids.WATER));
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader reader, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
            BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(reader));
        }
        return super.updateShape(state, reader, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TcChestBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        if (world.isClientSide()) {
            return createTickerHelper(type, ModContent.CHEST, TcChestBlockEntity::clientTick);
        }
        return createTickerHelper(type, ModContent.CHEST, TcChestBlockEntity::serverTick);
    }

    @Override
    protected void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (world.getBlockEntity(pos) instanceof TcChestBlockEntity chest) {
            chest.recheckOpen();
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        return world.getBlockEntity(pos) instanceof TcChestBlockEntity chest ? chest.comparatorSignal() : 0;
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter getter, BlockPos pos) {
        if (getter.getBlockEntity(pos) instanceof TcChestBlockEntity chest && !chest.mayAccess(player)) {
            return 0.0F;
        }
        return super.getDestroyProgress(state, player, getter, pos);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state) {
        return state.getFluidState().isEmpty();
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (stack.getItem() instanceof com.itmecasey.thoroughchests.item.UpgradeCardItem card && card.target().upgradesFrom(level)) {
            if (world.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (world.getBlockEntity(pos) instanceof TcChestBlockEntity chest && !chest.mayAccess(player)) {
                com.itmecasey.thoroughchests.item.AugmentItem.say(player, Component.translatable("message.thoroughchests.locked", chest.ownerName()));
                return InteractionResult.FAIL;
            }
            if (upgrade(world, pos, card.target())) {
                if (!player.hasInfiniteMaterials()) {
                    stack.shrink(1);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;
        }
        return super.useItemOn(stack, state, world, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(world instanceof ServerLevel) || !(world.getBlockEntity(pos) instanceof TcChestBlockEntity chest)) {
            return InteractionResult.SUCCESS;
        }
        if (!chest.mayAccess(player)) {
            com.itmecasey.thoroughchests.item.AugmentItem.say(player, Component.translatable("message.thoroughchests.locked", chest.ownerName()));
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer) {
            int rows = TcChestMenu.rowsFor(player, chest);
            player.openMenu(new SimpleMenuProvider((id, inv, p) -> new TcChestMenu(ModContent.menu(rows), rows, id, inv, chest), chest.displayName()));
        }
        return InteractionResult.SUCCESS;
    }

    /** Replaces the chest at pos by the chest of the target level and moves everything across. Returns false if the block is no chest. */
    public static boolean upgrade(Level world, BlockPos pos, ChestLevel target) {
        BlockState old = world.getBlockState(pos);
        if (!(old.getBlock() instanceof TcChestBlock block) || !(world.getBlockEntity(pos) instanceof TcChestBlockEntity oldEntity)) {
            return false;
        }
        ChestLevel from = block.level();
        BlockState state = ModContent.chest(target).defaultBlockState().setValue(FACING, old.getValue(FACING)).setValue(WATERLOGGED, old.getValue(WATERLOGGED));
        oldEntity.keepOnRemove();
        world.setBlock(pos, state, 3);
        if (world.getBlockEntity(pos) instanceof TcChestBlockEntity fresh) {
            fresh.adopt(oldEntity);
            fresh.setChanged();
        }
        world.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5F, 1.4F);
        ScriptEvents.fire("chest_upgraded", "dimension", String.valueOf(world.dimension().identifier()), "x", pos.getX(), "y", pos.getY(),
                "z", pos.getZ(), "from", from.id(), "to", target.id());
        return true;
    }

    /** Used by tests and the menu: a chest's menu container check. */
    public static boolean isOpenBy(Player player, TcChestBlockEntity chest) {
        AbstractContainerMenu menu = player.containerMenu;
        return menu instanceof TcChestMenu m && m.chest() == chest;
    }
}
