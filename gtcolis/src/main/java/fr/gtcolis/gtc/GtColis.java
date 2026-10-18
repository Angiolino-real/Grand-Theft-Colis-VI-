package fr.gtcolis.gtc;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(GtColis.MODID)
public class GtColis {
    public static final String MODID = "gtcolis";

    public GtColis() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModRegistry.register(bus);
        bus.addListener(this::common);
        bus.addListener(this::attributes);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        MinecraftForge.EVENT_BUS.register(new ServerEvents());
    }

    private void common(FMLCommonSetupEvent e) {
        e.enqueueWork(Net::init);
    }

    private void attributes(EntityAttributeCreationEvent e) {
        e.put(ModRegistry.DOUANIER.get(), DouanierEntity.attrs().build());
        e.put(ModRegistry.CONTACT.get(), ContactEntity.attrs().build());
        e.put(ModRegistry.MARCHAND.get(), MerchantEntity.attrs().build());
        e.put(ModRegistry.VOITURE.get(), VoitureEntity.attrs().build());
    }
}
