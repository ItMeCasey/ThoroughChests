package com.itmecasey.thoroughchests.client;

import com.itmecasey.thoroughchests.ChestLevel;
import com.itmecasey.thoroughchests.ModContent;
import com.itmecasey.thoroughchests.block.TcChestBlock;
import com.itmecasey.thoroughchests.block.TcChestBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Draws a chest with the vanilla chest model and lid animation, using the texture of its level. */
public class TcChestRenderer implements BlockEntityRenderer<TcChestBlockEntity, TcChestRenderer.State> {
    /** What the renderer needs from the block entity. */
    public static class State extends BlockEntityRenderState {
        public Direction facing = Direction.SOUTH;
        public float open;
        public ChestLevel level = ChestLevel.COPPER;
    }

    private final SpriteGetter sprites;
    private final ChestModel model;

    public TcChestRenderer(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
        this.model = new ChestModel(context.bakeLayer(ModelLayers.CHEST));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TcChestBlockEntity chest, State state, float partialTicks, Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(chest, state, partialTicks, cameraPosition, breakProgress);
        state.facing = chest.getBlockState().getValue(TcChestBlock.FACING);
        state.open = chest.getOpenNess(partialTicks);
        state.level = chest.level();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(ChestRenderer.modelTransformation(state.facing));
        float open = 1.0F - state.open;
        open = 1.0F - open * open * open;
        SpriteId sprite = Sheets.CHEST_MAPPER.apply(ModContent.id(state.level.id()));
        collector.submitModel(model, open, poseStack, state.lightCoords, OverlayTexture.NO_OVERLAY, -1, sprite, sprites, 0, state.breakProgress);
        poseStack.popPose();
    }
}
