package com.itmecasey.thoroughchests.jade;

import com.itmecasey.thoroughchests.block.TcChestBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * The Thorough Chests lines in Jade: level and slots used, the installed augment and the owner of a locked chest. The server works the
 * numbers out and sends them in the block's data. Jade finds this class by its annotation, so it is never loaded unless Jade is installed.
 */
@WailaPlugin
public class ThoroughChestsJadePlugin implements IWailaPlugin {
    private static final Identifier UID = Identifier.fromNamespaceAndPath("thoroughchests", "chest");

    @Override
    public void register(IWailaCommonRegistration registration) {
        IServerDataProvider<BlockAccessor> provider = new IServerDataProvider<>() {
            @Override
            public void appendServerData(CompoundTag data, BlockAccessor accessor) {
                if (accessor.getBlockEntity() instanceof TcChestBlockEntity chest) {
                    data.putInt("used", chest.usedSlots());
                    data.putInt("slots", chest.getContainerSize());
                    data.putString("augment", chest.augment() == null ? "" : chest.augmentStack().getHoverName().getString());
                    data.putString("owner", chest.hasLock() ? chest.ownerName() : "");
                }
            }

            @Override
            public Identifier getUid() {
                return UID;
            }
        };
        registration.registerBlockDataProvider(provider, TcChestBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        IBlockComponentProvider provider = new IBlockComponentProvider() {
            @Override
            public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
                CompoundTag data = accessor.getServerData();
                if (!data.contains("slots")) {
                    return;
                }
                tooltip.add(Component.translatable("smwti.thoroughchests.level", levelName(accessor), data.getIntOr("used", 0), data.getIntOr("slots", 0)));
                String augment = data.getStringOr("augment", "");
                if (!augment.isEmpty()) {
                    tooltip.add(Component.literal(augment));
                }
                String owner = data.getStringOr("owner", "");
                if (!owner.isEmpty()) {
                    tooltip.add(Component.translatable("smwti.thoroughchests.owner", owner));
                }
            }

            @Override
            public Identifier getUid() {
                return UID;
            }
        };
        registration.registerBlockComponent(provider, Block.class);
    }

    private static String levelName(BlockAccessor accessor) {
        return accessor.getBlock() instanceof com.itmecasey.thoroughchests.block.TcChestBlock block ? block.level().title() : "";
    }
}
