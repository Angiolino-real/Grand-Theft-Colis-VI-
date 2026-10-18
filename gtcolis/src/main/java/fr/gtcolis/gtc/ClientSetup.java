package fr.gtcolis.gtc;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GtColis.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    public static final ModelLayerLocation CAR_LAYER = new ModelLayerLocation(new ResourceLocation(GtColis.MODID, "voiture"), "main");
    public static final KeyMapping KEY_PHONE = new KeyMapping("key.gtcolis.phone", InputConstants.KEY_UP, "key.categories.gtcolis");

    private ClientSetup() {}

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(ModRegistry.DOUANIER.get(), c -> new HumanoidX<DouanierEntity>(c, "douanier"));
        e.registerEntityRenderer(ModRegistry.CONTACT.get(), c -> new HumanoidX<ContactEntity>(c, "contact"));
        e.registerEntityRenderer(ModRegistry.MARCHAND.get(), c -> new HumanoidX<MerchantEntity>(c, "marchand"));
        e.registerEntityRenderer(ModRegistry.VOITURE.get(), VoitureRenderer::new);
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(CAR_LAYER, VoitureModel::createLayer);
    }

    @SubscribeEvent
    public static void overlays(RegisterGuiOverlaysEvent e) {
        e.registerAboveAll("gtc_hud", Hud::render);
    }

    @SubscribeEvent
    public static void keys(RegisterKeyMappingsEvent e) {
        e.register(KEY_PHONE);
    }
}
