package com.itmecasey.thoroughchests.item;

import com.itmecasey.thoroughchests.block.TcChestBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** The key part of a chest lock. Put in a chest's lock slot, or sneak-use on a chest, it makes the placing player the owner. */
public class LockItem extends Item {
    public LockItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof TcChestBlockEntity chest)) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!chest.hasLock() && chest.installLock(context.getItemInHand(), player)) {
            return InteractionResult.SUCCESS;
        }
        AugmentItem.say(player, Component.translatable("message.thoroughchests.locked", chest.ownerName()));
        return InteractionResult.FAIL;
    }
}
