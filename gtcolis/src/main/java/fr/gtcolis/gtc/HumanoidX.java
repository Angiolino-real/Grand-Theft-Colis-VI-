package fr.gtcolis.gtc;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

public class HumanoidX<T extends Mob> extends MobRenderer<T, HumanoidModel<T>> {
    private final ResourceLocation tex;

    public HumanoidX(EntityRendererProvider.Context ctx, String texture) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        this.tex = new ResourceLocation(GtColis.MODID, "textures/entity/" + texture + ".png");
        this.addLayer(new ItemInHandLayer<>(this, ctx.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return tex;
    }
}
