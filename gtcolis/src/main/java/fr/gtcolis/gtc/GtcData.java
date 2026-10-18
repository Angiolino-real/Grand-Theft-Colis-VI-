package fr.gtcolis.gtc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Donnees du joueur (stockees dans ses donnees persistantes, copiees a la mort). */
public final class GtcData {
    public static final float[] TH = {1, 3, 5, 8, 12, 17};
    public static final int[] COPS_FOR = {0, 1, 2, 3, 4, 6, 8};
    public static final Set<UUID> DIRTY = new HashSet<>();
    private static final Map<UUID, Deque<String>> JOURNAL = new HashMap<>();

    private GtcData() {}

    public static CompoundTag raw(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains("PlayerPersisted")) root.put("PlayerPersisted", new CompoundTag());
        CompoundTag pp = root.getCompound("PlayerPersisted");
        if (!pp.contains("gtc")) {
            CompoundTag t = new CompoundTag();
            t.putInt("lives", 3);
            pp.put("gtc", t);
        }
        return pp.getCompound("gtc");
    }

    public static void setRaw(Player p, CompoundTag t) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains("PlayerPersisted")) root.put("PlayerPersisted", new CompoundTag());
        root.getCompound("PlayerPersisted").put("gtc", t);
    }

    private static void dirty(Player p) {
        DIRTY.add(p.getUUID());
    }

    public static int cash(Player p) { return raw(p).getInt("cash"); }
    public static void setCash(Player p, int v) { raw(p).putInt("cash", Math.max(0, v)); dirty(p); }
    public static void addCash(Player p, int v) { setCash(p, cash(p) + v); }
    public static boolean pay(Player p, int v) {
        if (cash(p) < v) return false;
        addCash(p, -v);
        return true;
    }

    public static int stars(Player p) { return raw(p).getInt("stars"); }
    public static void setStars(Player p, int v) { raw(p).putInt("stars", Math.max(0, Math.min(6, v))); dirty(p); }
    public static float heat(Player p) { return raw(p).getFloat("heat"); }
    public static void setHeat(Player p, float v) { raw(p).putFloat("heat", Math.max(0, v)); }
    public static int lives(Player p) { return raw(p).getInt("lives"); }
    public static void setLives(Player p, int v) { raw(p).putInt("lives", v); dirty(p); }
    public static int msDone(Player p) { return raw(p).getInt("ms"); }
    public static void addMsDone(Player p) { raw(p).putInt("ms", msDone(p) + 1); dirty(p); }
    public static boolean started(Player p) { return raw(p).getBoolean("started"); }
    public static void setStarted(Player p) { raw(p).putBoolean("started", true); }

    public static void addHeat(ServerPlayer p, float v) {
        setHeat(p, heat(p) + v);
        raise(p);
    }

    /** Recalcule les etoiles a partir de la chaleur. */
    public static void raise(ServerPlayer p) {
        int n = 0;
        for (float t : TH) if (heat(p) >= t) n++;
        if (n > stars(p)) {
            setStars(p, n);
            Util.bar(p, "\u00a7cDOUANE " + "\u2605".repeat(n));
            log(p, "Douane : " + n + " etoile(s).");
        }
    }

    /** Montee d'etoiles due aux colis portes. */
    public static void parcelStars(ServerPlayer p) {
        int s = Math.min(6, Util.carry(p) >> 1);
        if (s > stars(p)) {
            setStars(p, s);
            Util.bar(p, "\u00a7cDOUANE " + "\u2605".repeat(s));
        }
    }

    public static void dropStar(ServerPlayer p) {
        int s = Math.max(0, stars(p) - 1);
        setStars(p, s);
        setHeat(p, s > 0 ? Math.min(heat(p), TH[s] - 0.5F) : 0);
        Util.bar(p, "\u00a7aDOUANE -1 \u2605");
        // retire les douaniers en trop
        var list = p.level().getEntitiesOfClass(DouanierEntity.class, p.getBoundingBox().inflate(128));
        list.sort((a, b) -> Double.compare(b.distanceToSqr(p), a.distanceToSqr(p)));
        int keep = COPS_FOR[s];
        for (int i = 0; i < list.size() - keep; i++) list.get(i).discard();
    }

    public static void killCops(ServerPlayer p) {
        p.level().getEntitiesOfClass(DouanierEntity.class, p.getBoundingBox().inflate(160)).forEach(e -> e.discard());
    }

    public static void clearWanted(ServerPlayer p) {
        setStars(p, 0);
        setHeat(p, 0);
        killCops(p);
    }

    public static void arrest(ServerPlayer p) {
        long now = p.level().getGameTime();
        if (now < raw(p).getLong("inv")) return;
        int l = lives(p) - 1;
        setLives(p, l);
        Util.removeColis(p, -1);
        clearWanted(p);
        raw(p).putLong("inv", now + 120);
        Util.bar(p, "\u00a7cARRETE MDR \u00a77- tu perds tes colis");
        log(p, "Arrete par la Douane. Vies : " + Math.max(l, 0));
        if (l <= 0) {
            Util.msg(p, "\u00a74\u00a7lT'ES CUIT FREROT \u00a7r\u00a77Aura finale : " + cash(p) + ". "
                    + (cash(p) > 3000 ? "Tu es un vrai sigma, la Douane pleure." : cash(p) > 800 ? "Pas mal. Ton colis arrive dans 3 a 6 semaines." : "Meme un crayon x14 a plus de rizz que toi."));
            MissionManager.abandon(p, false);
            setCash(p, 0);
            setLives(p, 3);
        }
    }

    /** Livraison au point relais. */
    public static boolean deliver(ServerPlayer p) {
        int n = Util.carry(p);
        if (n <= 0) return false;
        Util.removeColis(p, -1);
        int gain = n * n * 100;
        addCash(p, gain);
        clearWanted(p);
        Util.msg(p, "\u00a7a+" + gain + " CASH \u00a77(" + n + " colis livres)");
        log(p, "Livraison de " + n + " colis : +" + gain);
        p.level().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.4F);
        return true;
    }

    // ---- Journal
    public static void log(Player p, String s) {
        Deque<String> d = JOURNAL.computeIfAbsent(p.getUUID(), k -> new ArrayDeque<>());
        d.addFirst(s);
        while (d.size() > 8) d.removeLast();
        dirty(p);
    }

    public static String journal(Player p) {
        Deque<String> d = JOURNAL.get(p.getUUID());
        return d == null ? "" : String.join("\n", d);
    }
}
