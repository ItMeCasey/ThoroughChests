package com.itmecasey.thoroughchests.item;

import com.itmecasey.thoroughchests.Augment;
import com.itmecasey.thoroughchests.AugmentRules;
import com.itmecasey.thoroughchests.block.TcChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * An augment of one kind and level. Sneak-use on a chest installs it (or upgrades the installed lower level).
 */
public class AugmentItem extends Item {
    private final Augment augment;
    private final int level;

    public AugmentItem(Augment augment, int level, Properties properties) {
        super(properties.stacksTo(16));
        this.augment = augment;
        this.level = level;
    }

    public Augment augment() {
        return augment;
    }

    public int level() {
        return level;
    }

    public AugmentRules.Installed installed() {
        return new AugmentRules.Installed(augment, level);
    }

    public static void say(Player player, Component message) {
        if (player instanceof ServerPlayer sp && sp.connection != null) {
            sp.sendSystemMessage(message, true);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (level.getBlockEntity(pos) instanceof TcChestBlockEntity chest) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            ItemStack stack = context.getItemInHand();
            if (chest.installFrom(stack, player)) {
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }
}
