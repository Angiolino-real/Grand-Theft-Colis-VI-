package fr.gtcolis.gtc;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue PARCELS_EVERYWHERE;
    public static final ForgeConfigSpec.BooleanValue COPS_ENABLED;
    public static final ForgeConfigSpec.BooleanValue PHONE_CALLS;
    public static final ForgeConfigSpec.BooleanValue RANDOM_EVENTS;
    public static final ForgeConfigSpec.IntValue PARCEL_CAP;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        PARCELS_EVERYWHERE = b.comment("Faire apparaitre des colis dans toutes les dimensions (sinon seulement Ohio City)").define("parcelsEverywhere", false);
        COPS_ENABLED = b.comment("La Douane (douaniers) poursuit les joueurs recherches").define("copsEnabled", true);
        PHONE_CALLS = b.comment("Appels entrants sur le telephone").define("phoneCalls", true);
        RANDOM_EVENTS = b.comment("Evenements aleatoires (portefeuille, colis de camion, controle...)").define("randomEvents", true);
        PARCEL_CAP = b.comment("Nombre max de colis au sol autour d'un joueur").defineInRange("parcelCap", 14, 2, 60);
        SPEC = b.build();
    }
}
