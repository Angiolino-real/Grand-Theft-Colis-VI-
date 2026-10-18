package fr.gtcolis.gtc;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Random;

/** Appels entrants. */
public final class Calls {
    public static final String[] NAMES = {"Indic", "Avocat", "Maman", "Numero masque", "Livreur", "Cousin"};
    public static final String[] SUBS = {"Un boulot, gratuit.", "A propos de ton dossier.", "Elle s'inquiete.", "...", "Votre colis...", "Il a un plan."};
    private static final Random R = new Random();

    private Calls() {}

    public static int pick(ServerPlayer p) {
        int i;
        do { i = R.nextInt(NAMES.length); } while (i == 1 && GtcData.stars(p) <= 0);
        return i;
    }

    public static void effect(ServerPlayer p, int idx) {
        switch (idx) {
            case 0 -> {
                Util.msg(p, "\u00a77Indic : \u00ab Ecoute, j'ai du boulot pour toi. \u00bb");
                MissionManager.start(p, MissionManager.PHONE_KINDS[R.nextInt(MissionManager.PHONE_KINDS.length)]);
            }
            case 1 -> {
                if (GtcData.pay(p, 300)) {
                    GtcData.clearWanted(p);
                    Util.msg(p, "\u00a77Avocat : \u00ab C'est reglé, ton dossier est efface (300 CASH). \u00bb");
                } else Util.msg(p, "\u00a77Avocat : \u00ab Il me faut 300 CASH pour m'en occuper. \u00bb");
            }
            case 2 -> {
                GtcData.addCash(p, 20);
                Util.msg(p, "\u00a77Maman : \u00ab T'as mange ? Tiens, un peu d'argent. \u00bb \u00a7a+20 CASH");
            }
            case 3 -> {
                Util.msg(p, "\u00a77Voix inconnue : \u00ab On te surveille. Rejoins une planque, maintenant. \u00bb");
                GtcData.setHeat(p, Math.max(GtcData.heat(p), GtcData.TH[0]));
                GtcData.raise(p);
            }
            case 4 -> Util.msg(p, "\u00a77Livreur : \u00ab Votre colis arrive dans 3 a 6 semaines. Bonne journee ! \u00bb");
            default -> {
                Util.give(p, new ItemStack(ModRegistry.COLIS.get()));
                Util.msg(p, "\u00a77Cousin : \u00ab Y a un colis qui traine, je te l'envoie. \u00bb \u00a7a+1 colis");
                GtcData.parcelStars(p);
            }
        }
    }
}
