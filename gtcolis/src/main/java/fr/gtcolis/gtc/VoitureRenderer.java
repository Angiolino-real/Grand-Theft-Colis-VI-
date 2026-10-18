package fr.gtcolis.gtc;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class VoitureRenderer extends MobRenderer<VoitureEntity, VoitureModel> {
    private static final ResourceLocation[] TEX = new ResourceLocation[6];

    static {
        for (int i = 0; i < 6; i++) TEX[i] = new ResourceLocation(GtColis.MODID, "textures/entity/voiture_" + i + ".png");
    }

    public VoitureRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new VoitureModel(ctx.bakeLayer(ClientSetup.CAR_LAYER)), 0.9F);
    }

    @Override
    public ResourceLocation getTextureLocation(VoitureEntity e) {
        return TEX[Math.floorMod(e.getColor(), 6)];
    }

    @Override
    protected void scale(VoitureEntity e, PoseStack ps, float partial) {
        switch (e.getBody()) {
            case 0 -> ps.scale(1.0F, 0.85F, 1.05F);
            case 2 -> ps.scale(1.15F, 1.45F, 1.3F);
            default -> { }
        }
    }
}
