package fr.gtcolis.gtc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class VoitureModel extends EntityModel<VoitureEntity> {
    private final ModelPart root;

    public VoitureModel(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-7F, -10F, -16F, 14F, 6F, 32F), PartPose.offset(0F, 24F, 0F));
        r.addOrReplaceChild("cabin", CubeListBuilder.create().texOffs(0, 40).addBox(-6F, -16F, -6F, 12F, 6F, 16F), PartPose.offset(0F, 24F, 0F));
        CubeListBuilder wheel = CubeListBuilder.create().texOffs(60, 40).addBox(-1.5F, -3F, -3F, 3F, 6F, 6F);
        r.addOrReplaceChild("w1", wheel, PartPose.offset(8.5F, 21F, 10F));
        r.addOrReplaceChild("w2", wheel, PartPose.offset(-8.5F, 21F, 10F));
        r.addOrReplaceChild("w3", wheel, PartPose.offset(8.5F, 21F, -10F));
        r.addOrReplaceChild("w4", wheel, PartPose.offset(-8.5F, 21F, -10F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(VoitureEntity e, float limbSwing, float limbSwingAmount, float age, float yaw, float pitch) { }

    @Override
    public void renderToBuffer(PoseStack ps, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
        root.render(ps, vc, light, overlay, r, g, b, a);
    }
}
