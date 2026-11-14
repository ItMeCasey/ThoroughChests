package com.itmecasey.thoroughchests.compat;

import com.itmecasey.smwti.api.CommonRegistration;
import com.itmecasey.smwti.api.ISmwtiPlugin;
import com.itmecasey.smwti.api.SmwtiPlugin;
import com.itmecasey.thoroughchests.block.TcChestBlockEntity;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The Thorough Chests lines in Show Me What This Is: level and slots used, the installed augment and the most common items.
 * The server works the lines out. SMWTI finds this class by its annotation; nothing calls into it, and it is never loaded unless SMWTI
 * is installed.
 */
@SmwtiPlugin
public class ThoroughChestsSmwtiPlugin implements ISmwtiPlugin {
    @Override
    public void register(CommonRegistration registration) {
        registration.serverBlock((tooltip, level, pos, entity, detailed) -> {
            if (!(entity instanceof TcChestBlockEntity chest)) {
                return;
            }
            tooltip.add(Component.translatable("smwti.thoroughchests.level", chest.level().title(), chest.usedSlots(), chest.getContainerSize()));
            if (chest.augment() != null) {
                tooltip.add(Component.translatable("smwti.thoroughchests.augment", chest.augmentStack().getHoverName()));
            }
            Map<String, Integer> counts = new LinkedHashMap<>();
            Map<String, ItemStack> samples = new LinkedHashMap<>();
            for (ItemStack s : chest.rawItems()) {
                if (!s.isEmpty()) {
                    String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
                    counts.merge(id, s.getCount(), Integer::sum);
                    samples.putIfAbsent(id, s);
                }
            }
            List<Map.Entry<String, Integer>> top = new ArrayList<>(counts.entrySet());
            top.sort((a, b) -> b.getValue() - a.getValue());
            for (int i = 0; i < Math.min(3, top.size()); i++) {
                tooltip.add(Component.translatable("smwti.thoroughchests.contents", top.get(i).getValue(), samples.get(top.get(i).getKey()).getHoverName()));
            }
        });
    }
}
