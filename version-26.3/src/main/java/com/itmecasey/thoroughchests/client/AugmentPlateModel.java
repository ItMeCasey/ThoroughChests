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
 * The augment overlay: a flat 4x4 plate one pixel thick on the top of the lid in its right back corner (seen from the front), hinged on
 * the lid so it follows the lid animation. Its top face carries the augment's design: the layer is 64x64 and the texture sheet 192x192,
 * so the 12x12 design is drawn at a third of a chest pixel per design pixel, and the face sits at 12,0 of the sheet.
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
        // Pivot like the chest's lid (y 9, z 1). The box sits on the lid top (world y 14), x 10..14 and z 2..6 (right and back).
        root.addOrReplaceChild("plate", CubeListBuilder.create().texOffs(0, 0).addBox(10.0F, 5.0F, 1.0F, 4.0F, 1.0F, 4.0F), PartPose.offset(0.0F, 9.0F, 1.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(Float open) {
        super.setupAnim(open);
        this.plate.xRot = -(open * (float) (Math.PI / 2));
    }
}
