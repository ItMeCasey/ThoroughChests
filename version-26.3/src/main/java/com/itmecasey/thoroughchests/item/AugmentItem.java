package com.itmecasey.thoroughchests.item;

import com.itmecasey.thoroughchests.Augment;
import com.itmecasey.thoroughchests.AugmentRules;
import com.itmecasey.thoroughchests.block.TcChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

/**
 * An augment of one kind and level. Sneak-use on a chest installs it (or upgrades the installed lower level). The Void augment also
 * stores a link: sneak-use on any block with a vanilla-style inventory.
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
            if (!chest.mayAccess(player)) {
                say(player, Component.translatable("message.thoroughchests.locked", chest.ownerName()));
                return InteractionResult.FAIL;
            }
            ItemStack stack = context.getItemInHand();
            if (chest.installFrom(stack, player)) {
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;
        }
        if (augment == Augment.VOID && player.isSecondaryUseActive()) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (HopperBlockEntity.getContainerAt(level, pos) != null) {
                CompoundTag tag = new CompoundTag();
                CustomData existing = context.getItemInHand().get(DataComponents.CUSTOM_DATA);
                if (existing != null) {
                    tag = existing.copyTag();
                }
                tag.putString("link_dim", String.valueOf(level.dimension().identifier()));
                tag.putInt("link_x", pos.getX());
                tag.putInt("link_y", pos.getY());
                tag.putInt("link_z", pos.getZ());
                context.getItemInHand().set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                say(player, Component.translatable("message.thoroughchests.linked", pos.getX(), pos.getY(), pos.getZ()));
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }
}
