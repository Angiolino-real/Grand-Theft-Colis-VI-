package fr.gtcolis.gtc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/** Toutes les missions (PNJ + telephone). Etat uniquement en memoire serveur. */
public final class MissionManager {
    public static final String[] PHONE_KINDS = {"chase", "tail", "heist", "smuggle", "race", "escort"};
    public static final String[][] PHONE_INFO = {
            {"chase", "Course-poursuite", "Seme la Douane et rejoins une planque.", "700"},
            {"tail", "Filature", "Suis une cible sans te faire reperer.", "600"},
            {"heist", "Braquage", "Menace un caissier, arme, puis fuis.", "1000"},
            {"smuggle", "Contrebande", "Charge, passe les barrages, livre.", "900"},
            {"race", "Course de rue", "6 checkpoints, chrono, en voiture.", "800"},
            {"escort", "Escorte", "Protege un VIP jusqu'au bout.", "750"}};
    private static final Random R = new Random();

    public static class Waypoint {
        public BlockPos pos;
        public String name;
        public Waypoint(BlockPos p, String n) { pos = p; name = n; }
    }

    static class Mission {
        String kind, desc;
        int reward, phase, t, counter, idx, need;
        BlockPos target;
        String targetName = "";
        List<BlockPos> cps = new ArrayList<>();
        UUID npc, contact;
        int lost, lost2;
        long deadline;
        ServerBossEvent bar;
    }

    private static final Map<UUID, Mission> MIS = new HashMap<>();
    public static final Map<UUID, Waypoint> WAY = new HashMap<>();

    private MissionManager() {}

    // ------------------------------------------------------------ infos pour le HUD
    public static String descOf(ServerPlayer p) {
        Mission m = MIS.get(p.getUUID());
        return m == null ? "" : m.desc;
    }

    public static BlockPos targetOf(ServerPlayer p) {
        Mission m = MIS.get(p.getUUID());
        if (m != null) return m.target;
        Waypoint w = WAY.get(p.getUUID());
        return w == null ? null : w.pos;
    }

    public static String targetName(ServerPlayer p) {
        Mission m = MIS.get(p.getUUID());
        if (m != null) return m.targetName;
        Waypoint w = WAY.get(p.getUUID());
        return w == null ? "" : w.name;
    }

    public static boolean has(ServerPlayer p) { return MIS.containsKey(p.getUUID()); }

    public static void forget(ServerPlayer p) {
        Mission m = MIS.remove(p.getUUID());
        if (m != null) cleanup(p, m);
    }

    // ------------------------------------------------------------ outils
    private static void cleanup(ServerPlayer p, Mission m) {
        if (m.bar != null) m.bar.removeAllPlayers();
        if (m.npc != null && p.level() instanceof ServerLevel sl) {
            Entity e = sl.getEntity(m.npc);
            if (e != null) e.discard();
        }
    }

    private static Mission create(ServerPlayer p, String kind, String desc, int reward) {
        abandon(p, false);
        Mission m = new Mission();
        m.kind = kind; m.desc = desc; m.reward = reward;
        m.bar = new ServerBossEvent(Component.literal(desc), BossEvent.BossBarColor.PINK, BossEvent.BossBarOverlay.PROGRESS);
        m.bar.setProgress(0F);
        m.bar.addPlayer(p);
        MIS.put(p.getUUID(), m);
        return m;
    }

    private static void setBar(Mission m, String title, float prog) {
        m.bar.setName(Component.literal(title));
        m.bar.setProgress(Math.max(0F, Math.min(1F, prog)));
    }

    public static void abandon(ServerPlayer p, boolean say) {
        Mission m = MIS.remove(p.getUUID());
        if (m == null) return;
        cleanup(p, m);
        if (say) Util.bar(p, "\u00a7cMission abandonnee");
        GtcData.log(p, "Mission abandonnee : " + m.desc);
        Net.sync(p);
    }

    private static void end(ServerPlayer p, boolean ok, String text) {
        Mission m = MIS.remove(p.getUUID());
        if (m == null) return;
        cleanup(p, m);
        if (ok) {
            GtcData.addCash(p, m.reward);
            GtcData.addMsDone(p);
            Util.msg(p, "\u00a7a\u2714 " + (text == null ? "Mission accomplie" : text) + " \u00a77: +" + m.reward + " CASH. SKIBIDI.");
            GtcData.log(p, "Mission reussie : " + m.desc + " (+" + m.reward + ")");
            p.level().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 1F, 1F);
        } else {
            Util.msg(p, "\u00a7c\u2718 " + (text == null ? "Mission echouee." : text));
            GtcData.log(p, "Mission echouee : " + m.desc);
        }
        Net.sync(p);
    }

    private static double dist2d(ServerPlayer p, BlockPos t) {
        double dx = p.getX() - (t.getX() + 0.5), dz = p.getZ() - (t.getZ() + 0.5);
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Point sur une route (ville) ou au sol (autres dimensions), a distance min..max du joueur. */
    public static BlockPos pointAround(ServerPlayer p, int min, int max) {
        ServerLevel l = p.serverLevel();
        for (int i = 0; i < 40; i++) {
            double a = R.nextDouble() * Math.PI * 2, d = min + R.nextDouble() * (max - min);
            int x = (int) (p.getX() + Math.cos(a) * d), z = (int) (p.getZ() + Math.sin(a) * d);
            if (Util.inCity(l)) {
                int[] rp = CityGen.snapToRoad(x, z, R.nextBoolean());
                x = rp[0]; z = rp[1];
            }
            BlockPos g = Util.ground(l, x, z);
            if (g != null) return g;
        }
        return p.blockPosition();
    }

    private static boolean copsNear(ServerPlayer p, double r) {
        return !p.level().getEntitiesOfClass(DouanierEntity.class, p.getBoundingBox().inflate(r)).isEmpty();
    }

    private static ContactEntity spawnNpc(ServerPlayer p, BlockPos pos, int role, String name) {
        ContactEntity c = ModRegistry.CONTACT.get().create(p.level());
        if (c == null) return null;
        c.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, R.nextFloat() * 360F, 0F);
        c.setRole(role);
        c.setCustomName(Component.literal(name));
        c.setCustomNameVisible(true);
        c.owner = p.getUUID();
        p.level().addFreshEntity(c);
        return c;
    }

    // ------------------------------------------------------------ demarrage
    public static void start(ServerPlayer p, String kind) {
        boolean city = Util.inCity(p.level());
        switch (kind) {
            case "chase" -> {
                Mission m = create(p, kind, "Course-poursuite : seme la Douane et rejoins la planque", 700);
                m.target = pointAround(p, 90, 140);
                m.targetName = "Planque";
                GtcData.setHeat(p, Math.max(GtcData.heat(p), GtcData.TH[2]));
                GtcData.raise(p);
            }
            case "tail" -> {
                Mission m = create(p, kind, "Filature : suis la cible sans te faire reperer", 600);
                ContactEntity c = spawnNpc(p, pointAround(p, 18, 26), 2, "\u00a77Cible");
                if (c != null) m.npc = c.getUUID();
            }
            case "heist" -> {
                Mission m = create(p, kind, "Braquage : va au magasin, arme a feu en main", 1000);
                m.target = city ? CityGen.doorOutside(R.nextInt(CityGen.COUNT)) : pointAround(p, 80, 130);
                m.targetName = city ? "Commerce a braquer" : "Cible";
            }
            case "smuggle" -> {
                Mission m = create(p, kind, "Contrebande : recupere la marchandise", 900);
                m.target = pointAround(p, 40, 80);
                m.targetName = "Marchandise";
            }
            case "race" -> {
                Mission m = create(p, kind, "Course de rue : 6 checkpoints, en voiture", 800);
                BlockPos last = p.blockPosition();
                for (int i = 0; i < 6; i++) {
                    ServerLevel l = p.serverLevel();
                    BlockPos c = null;
                    for (int k = 0; k < 40 && c == null; k++) {
                        double a = R.nextDouble() * Math.PI * 2, d = 25 + R.nextDouble() * 20;
                        int x = (int) (last.getX() + Math.cos(a) * d), z = (int) (last.getZ() + Math.sin(a) * d);
                        if (city) { int[] rp = CityGen.snapToRoad(x, z, R.nextBoolean()); x = rp[0]; z = rp[1]; }
                        c = Util.ground(l, x, z);
                    }
                    if (c == null) c = last.offset(R.nextInt(40) - 20, 0, R.nextInt(40) - 20);
                    m.cps.add(c);
                    last = c;
                }
                m.target = m.cps.get(0);
                m.targetName = "Checkpoint 1/6";
                m.deadline = p.level().getGameTime() + 20 * 170;
            }
            case "escort" -> {
                Mission m = create(p, kind, "Escorte : protege le VIP jusqu'a destination", 750);
                ContactEntity c = spawnNpc(p, pointAround(p, 3, 6), 1, "\u00a7cVIP");
                if (c != null) { m.npc = c.getUUID(); c.addTag("gtc_vip"); }
                m.target = pointAround(p, 100, 140);
                m.targetName = "Destination du VIP";
            }
            default -> { Util.bar(p, "\u00a7cMission inconnue"); return; }
        }
        Util.msg(p, "\u00a7e\u2605 Mission lancee : \u00a7f" + descOf(p));
        GtcData.log(p, "Mission lancee : " + descOf(p));
        Net.sync(p);
    }

    // ------------------------------------------------------------ PNJ du jeu : bring / goto / dance
    public static void talk(ServerPlayer p, ContactEntity c) {
        Mission m = MIS.get(p.getUUID());
        if (m != null && m.kind.equals("bring") && c.getUUID().equals(m.contact)) {
            int carry = Util.carry(p);
            if (carry >= m.need) {
                Util.removeColis(p, m.need);
                GtcData.setStars(p, Math.min(GtcData.stars(p), Math.min(6, Util.carry(p) >> 1)));
                c.cd = 300;
                c.setMark(false);
                end(p, true, "Livraison au contact");
            } else {
                Util.bar(p, "\u00a7eIl m'en faut " + m.need + ", t'en as " + carry);
            }
            return;
        }
        if (m == null && c.cd <= 0) {
            giveMission(p, c);
            return;
        }
        Util.bar(p, "\u00a77Reviens plus tard, frerot.");
    }

    private static void giveMission(ServerPlayer p, ContactEntity c) {
        double r = R.nextDouble();
        boolean city = Util.inCity(p.level());
        if (r < 0.4) {
            int k = 2 + R.nextInt(3);
            Mission m = create(p, "bring", "Ramene " + k + " colis au contact \u2605", k * 150);
            m.need = k; m.contact = c.getUUID();
            m.target = c.blockPosition(); m.targetName = "Contact";
            c.setMark(true);
            Util.msg(p, "\u00a7d\u00ab Freret, j'ai besoin de " + k + " colis pour mon aura. Reviens me voir. \u00bb");
        } else if (r < 0.75 || !city) {
            String nm;
            Mission m = create(p, "goto", "", 300);
            if (city) {
                int b = R.nextInt(CityGen.COUNT);
                m.target = CityGen.doorOutside(b);
                nm = CityGen.name(b);
            } else {
                m.target = pointAround(p, 70, 120);
                nm = "la planque";
            }
            m.targetName = nm;
            m.desc = "Va chez " + nm;
            Util.msg(p, "\u00a7d\u00ab Va chez " + nm + " dire que j'arrive, sigma. \u00bb");
        } else {
            Mission m = create(p, "dance", "Danse 6 s sur la piste du Club Ohio", 500);
            m.target = CityGen.doorOutside(CityGen.CLUB);
            m.targetName = "Club Ohio";
            Util.msg(p, "\u00a7d\u00ab Va danser au Club Ohio, ca farme du rizz. \u00bb");
        }
        Net.sync(p);
    }

    /** Appele par le tick quand le joueur danse sur une piste. */
    public static void danceTick(ServerPlayer p) {
        Mission m = MIS.get(p.getUUID());
        if (m == null || !m.kind.equals("dance")) return;
        m.counter++;
        setBar(m, m.desc, m.counter / 120F);
        if (m.counter >= 120) end(p, true, "Soiree reussie");
    }

    // ------------------------------------------------------------ tick (chaque tick, cote serveur)
    public static void tick(ServerPlayer p) {
        Mission m = MIS.get(p.getUUID());
        if (m == null) return;
        m.t++;
        ServerLevel l = p.serverLevel();
        switch (m.kind) {
            case "goto" -> {
                if (m.target != null && dist2d(p, m.target) < 6) end(p, true, "Message transmis");
            }
            case "bring" -> {
                Entity e = m.contact == null ? null : l.getEntity(m.contact);
                if (e == null || !e.isAlive()) end(p, false, "Ton contact a disparu... mission annulee.");
                else m.target = e.blockPosition();
            }
            case "chase" -> {
                if (m.target != null) {
                    double d = dist2d(p, m.target);
                    setBar(m, m.desc, 1F - (float) Math.min(1, d / 140.0));
                    if (d < 6) {
                        if (GtcData.stars(p) == 0 || !copsNear(p, 32)) end(p, true, "Douane semee");
                        else if (m.t % 40 == 0) Util.bar(p, "\u00a7eSeme d'abord les douaniers !");
                    }
                }
            }
            case "tail" -> tickTail(p, m, l);
            case "heist" -> tickHeist(p, m);
            case "smuggle" -> tickSmuggle(p, m);
            case "race" -> tickRace(p, m, l);
            case "escort" -> tickEscort(p, m, l);
            default -> {}
        }
        if (m.target != null && m.t % 10 == 0) Net.sync(p);
    }

    private static void tickTail(ServerPlayer p, Mission m, ServerLevel l) {
        Entity e = m.npc == null ? null : l.getEntity(m.npc);
        if (e == null || !e.isAlive()) { end(p, false, "La cible a disparu."); return; }
        double d = p.distanceTo(e);
        int total = 20 * 90;
        if (d > 50) { if (++m.lost > 100) { end(p, false, "Tu as perdu la cible."); return; } } else m.lost = 0;
        if (d < 4.5) { if (++m.lost2 > 60) { end(p, false, "La cible t'a repere !"); return; } } else m.lost2 = 0;
        setBar(m, "Filature : reste a distance (" + (int) d + " m)", m.t / (float) total);
        if (m.t >= total) end(p, true, "Cible suivie");
    }

    private static boolean holdingGun(ServerPlayer p) {
        return p.getMainHandItem().getItem() instanceof GunItem;
    }

    private static void tickHeist(ServerPlayer p, Mission m) {
        if (m.phase == 0) {
            double d = dist2d(p, m.target);
            if (d < 12 && holdingGun(p)) {
                m.counter++;
                setBar(m, "Braquage : menace le caissier...", m.counter / 150F);
                if (m.counter > 150) {
                    m.phase = 1;
                    m.target = null;
                    m.desc = "Braquage : fuis avec la caisse !";
                    GtcData.setHeat(p, Math.max(GtcData.heat(p), GtcData.TH[2]));
                    GtcData.raise(p);
                    Util.msg(p, "\u00a7cAlarme ! Fuis avec la caisse (fais tomber ta cote de recherche a 0).");
                    Net.sync(p);
                }
            } else {
                m.counter = Math.max(0, m.counter - 2);
                if (d < 12 && m.t % 60 == 0) Util.bar(p, "\u00a7eSors une arme a feu !");
                setBar(m, m.desc, 0F);
            }
        } else {
            setBar(m, "Braquage : fuis ! (" + Util.stars(GtcData.stars(p)) + ")", 0.5F);
            if (GtcData.stars(p) == 0) end(p, true, "Braquage reussi");
        }
    }

    private static void tickSmuggle(ServerPlayer p, Mission m) {
        if (m.phase == 0) {
            if (dist2d(p, m.target) < 5) {
                for (int i = 0; i < 3; i++) Util.give(p, new ItemStack(ModRegistry.COLIS.get()));
                m.phase = 1;
                m.target = pointAround(p, 70, 110);
                m.targetName = "Point de livraison";
                m.desc = "Contrebande : livre la marchandise (3 colis)";
                GtcData.setHeat(p, Math.max(GtcData.heat(p), GtcData.TH[0]));
                GtcData.raise(p);
                GtcData.parcelStars(p);
                Util.msg(p, "\u00a7eMarchandise chargee ! Passe les barrages et livre.");
                Net.sync(p);
            }
        } else {
            double d = dist2d(p, m.target);
            setBar(m, m.desc, 1F - (float) Math.min(1, d / 110.0));
            if (d < 5) {
                if (Util.carry(p) >= 3) {
                    Util.removeColis(p, 3);
                    end(p, true, "Marchandise livree");
                } else if (m.t % 40 == 0) Util.bar(p, "\u00a7cIl te faut 3 colis !");
            }
        }
    }

    private static void tickRace(ServerPlayer p, Mission m, ServerLevel l) {
        long left = m.deadline - l.getGameTime();
        if (left <= 0) { end(p, false, "Temps ecoule !"); return; }
        BlockPos cp = m.cps.get(m.idx);
        m.target = cp;
        m.targetName = "Checkpoint " + (m.idx + 1) + "/6";
        boolean inCar = p.getVehicle() instanceof VoitureEntity;
        setBar(m, "Course : checkpoint " + (m.idx + 1) + "/6 - " + (left / 20) + " s" + (inCar ? "" : " (monte dans une voiture !)"), m.idx / 6F);
        if (m.t % 3 == 0)
            for (int i = 0; i < 8; i++)
                l.sendParticles(ParticleTypes.END_ROD, cp.getX() + 0.5, cp.getY() + i * 0.7, cp.getZ() + 0.5, 1, 0.15, 0, 0.15, 0);
        if (inCar && dist2d(p, cp) < 5) {
            m.idx++;
            l.playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, net.minecraft.sounds.SoundSource.PLAYERS, 1F, 1.5F);
            if (m.idx >= 6) end(p, true, "Course terminee");
            else Net.sync(p);
        }
    }

    private static void tickEscort(ServerPlayer p, Mission m, ServerLevel l) {
        Entity e = m.npc == null ? null : l.getEntity(m.npc);
        if (e == null || !e.isAlive()) { end(p, false, "Le VIP est mort."); return; }
        double dp = p.distanceTo(e);
        if (dp > 40) { if (++m.lost > 240) { end(p, false, "Tu as abandonne le VIP."); return; } } else m.lost = 0;
        double dd = Math.sqrt(Math.pow(e.getX() - m.target.getX(), 2) + Math.pow(e.getZ() - m.target.getZ(), 2));
        float hp = e instanceof net.minecraft.world.entity.LivingEntity le ? le.getHealth() / le.getMaxHealth() : 1F;
        setBar(m, "Escorte : VIP " + (int) (hp * 100) + " %  - " + (int) dd + " m", 1F - (float) Math.min(1, dd / 140.0));
        if (m.t % 900 == 0) {
            GtcData.setHeat(p, Math.max(GtcData.heat(p), GtcData.TH[1]));
            GtcData.raise(p);
            for (int i = 0; i < 2; i++) {
                BlockPos g = Util.ground(l, (int) e.getX() + R.nextInt(30) - 15, (int) e.getZ() + R.nextInt(30) - 15);
                if (g != null) ModRegistry.DOUANIER.get().spawn(l, g, MobSpawnType.EVENT);
            }
            Util.msg(p, "\u00a7cEmbuscade ! Protege le VIP.");
        }
        if (dd < 7 && dp < 25) end(p, true, "VIP en securite");
    }
}
