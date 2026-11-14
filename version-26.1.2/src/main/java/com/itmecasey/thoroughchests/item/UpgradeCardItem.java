package com.itmecasey.thoroughchests.item;

import com.itmecasey.thoroughchests.ChestLevel;
import com.itmecasey.thoroughchests.block.TcChestBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** A card that moves a chest exactly one level up, to the level named by the card. The contents stay. */
public class UpgradeCardItem extends Item {
    private final ChestLevel target;

    public UpgradeCardItem(ChestLevel target, Properties properties) {
        super(properties.stacksTo(16));
        this.target = target;
    }

    public ChestLevel target() {
        return target;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        if (!(state.getBlock() instanceof TcChestBlock chest)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!target.upgradesFrom(chest.level())) {
            if (player != null) {
                AugmentItem.say(player, Component.translatable("message.thoroughchests.wrong_card", target.title()));
            }
            return InteractionResult.FAIL;
        }
        if (player != null && level.getBlockEntity(pos) instanceof com.itmecasey.thoroughchests.block.TcChestBlockEntity be && !be.mayAccess(player)) {
            AugmentItem.say(player, Component.translatable("message.thoroughchests.locked", be.ownerName()));
            return InteractionResult.FAIL;
        }
        if (TcChestBlock.upgrade(level, pos, target)) {
            if (player == null || !player.hasInfiniteMaterials()) {
                context.getItemInHand().shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }
}
