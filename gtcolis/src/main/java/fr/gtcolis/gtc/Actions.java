package fr.gtcolis.gtc;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.Random;

/** Traitement cote serveur des actions envoyees par le client. */
public final class Actions {
    private static final Random R = new Random();

    private Actions() {}

    public static void handle(ServerPlayer p, Net.Action a) {
        switch (a.type) {
            case "buy" -> Shops.buy(p, a.a, a.b);
            case "mission" -> MissionManager.start(p, a.a);
            case "abandon" -> MissionManager.abandon(p, true);
            case "contact" -> {
                if (a.b == 0) {
                    MissionManager.start(p, MissionManager.PHONE_KINDS[R.nextInt(MissionManager.PHONE_KINDS.length)]);
                } else if (a.b == 1) {
                    if (GtcData.pay(p, 300)) {
                        GtcData.clearWanted(p);
                        Util.msg(p, "\u00a77Avocat : \u00ab Ton dossier a disparu (300 CASH). \u00bb");
                    } else Util.bar(p, "\u00a7cPas assez de CASH (300)");
                }
            }
            case "taxi" -> CityGen.taxi(p, a.b);
            case "waypoint" -> CityGen.waypoint(p, a.a, a.b);
            case "callans" -> ServerEvents.answerCall(p, a.b, a.b >= 100);
            default -> {}
        }
    }
}
