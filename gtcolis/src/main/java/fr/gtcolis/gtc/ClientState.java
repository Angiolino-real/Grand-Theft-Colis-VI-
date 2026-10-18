package fr.gtcolis.gtc;

import net.minecraft.core.BlockPos;

/** Etat du joueur cote client (alimente par Net.Sync). */
public final class ClientState {
    public static int cash, stars, lives = 3, ms;
    public static String mission = "", targetName = "", journal = "";
    public static BlockPos target;

    private ClientState() {}

    public static void apply(Net.Sync s) {
        cash = s.cash; stars = s.stars; lives = s.lives; ms = s.ms;
        mission = s.mission; targetName = s.tname; journal = s.journal;
        target = s.hasT ? BlockPos.of(s.target) : null;
    }
}
