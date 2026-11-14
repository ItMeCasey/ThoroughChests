package com.itmecasey.thoroughchests.client;

import com.itmecasey.thoroughchests.ModContent;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * The augment overlay: a flat 12x12 plate one pixel thick, centered on the chest's front, hinged on the lid so it follows the lid
 * animation. Its south face carries the augment's design (the texture sheet is 32x32 and the face sits at 14,1).
 */
public class AugmentPlateModel extends Model<Float> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ModContent.id("augment_plate"), "main");
    private final ModelPart plate;

    public AugmentPlateModel(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
        this.plate = root.getChild("plate");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // Pivot like the chest's lid (y 9, z 1). The box spans x 2..14 and, in the world, y 1..13: the middle of the 14 pixel high front.
        root.addOrReplaceChild("plate", CubeListBuilder.create().texOffs(0, 0).addBox(2.0F, -8.0F, 14.0F, 12.0F, 12.0F, 1.0F), PartPose.offset(0.0F, 9.0F, 1.0F));
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(Float open) {
        super.setupAnim(open);
        this.plate.xRot = -(open * (float) (Math.PI / 2));
    }
}
