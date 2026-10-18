package fr.gtcolis.gtc;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Locale;
import java.util.Random;

/** Generation d'Ohio City (10 x 7 blocs d'immeubles) dans la dimension gtcolis:ohio_city. */
public final class CityGen {
    public static final int NX = 10, NY = 7, CELL = 36, ROAD = 6, COUNT = NX * NY;
    public static final int CLUB = 22, SHOP = 12, ARM = 8;
    public static final int GROUND = 4;         // dernier bloc de sol (asphalte)
    public static final int FEET = 5;           // hauteur des pieds
    public static final int CEIL = 10;          // plafond du rez-de-chaussee
    public static final int SIDE = 26;          // taille d'un immeuble

    private static final String[][] NM = {
            {"Ohio Mart", "mart"}, {"Sigma Nails", "nails"}, {"Skibidi Kebab", "kebab"}, {"Rizz Pressing", "pressing"},
            {"Aura Farm", "bar"}, {"Mewing Clinic", "clinic"}, {"Colis Corp", "mart"}, {"Fanum Tax", "bar"},
            {"Brainrot SA", "mart"}, {"Gyatt Gym", "gym"}, {"Mogging Bar", "bar"}, {"Crayon x14", "mart"},
            {"Looksmax Barber", "nails"}, {"Ligma Burger", "kebab"}, {"Delulu Cafe", "bar"}, {"Skill Issue Arcade", "gym"},
            {"NPC Boutique", "pressing"}, {"Rizzler Pharma", "clinic"}, {"Gigachad Gym", "gym"}, {"Grimace Shake", "bar"},
            {"Tung Tabac", "mart"}, {"Sus Sushi", "kebab"}, {"Ohio Hotel", "clinic"}, {"Based Barbershop", "nails"}};
    public static final int[][] RLP = {{2, 2}, {5, 1}, {3, 4}, {6, 3}, {1, 3}, {8, 2}, {9, 5}, {7, 6}, {4, 6}};
    private static final String[] COLORS = {"light_purple", "dark_purple", "aqua", "gold", "green"};

    private CityGen() {}

    // ------------------------------------------------------------------ donnees
    public static String name(int i) {
        return switch (i) {
            case CLUB -> "Club Ohio";
            case SHOP -> "Auto Skibidi";
            case ARM -> "Armurerie Sigma";
            case 0 -> "Casino Le Gyatt";
            case 6 -> "Garage Brainrot";
            case 11 -> "Friperie Ohio";
            default -> NM[i % NM.length][0];
        };
    }

    /** Type d'interieur : club, shop, armu, casino, garage, friperie ou celui du jeu. */
    public static String kind(int i) {
        return switch (i) {
            case CLUB -> "club";
            case SHOP -> "shop";
            case ARM -> "armu";
            case 0 -> "casino";
            case 6 -> "garage";
            case 11 -> "friperie";
            default -> NM[i % NM.length][1];
        };
    }

    private static int x0(int i) { return (i / NY) * CELL + ROAD + 2; }
    private static int z0(int i) { return (i % NY) * CELL + ROAD + 2; }

    public static BlockPos doorOutside(int i) { return new BlockPos(x0(i) + 12, FEET, z0(i) + 27); }

    public static BlockPos relayPos(int k) { return new BlockPos(RLP[k][0] * CELL + 3, FEET, RLP[k][1] * CELL + 3); }

    public static BlockPos spawnPoint() { return new BlockPos(1 * CELL + 3, FEET, 1 * CELL + 3); }

    /** Ramene (x,z) sur l'axe d'une route. vertical = route nord-sud. */
    public static int[] snapToRoad(int x, int z, boolean vertical) {
        int maxX = NX * CELL + ROAD - 3, maxZ = NY * CELL + ROAD - 3;
        if (vertical) {
            int r = Math.max(0, Math.min(NX, Math.round((x - 3) / (float) CELL)));
            return new int[]{r * CELL + 3, Math.max(3, Math.min(maxZ, z))};
        }
        int r = Math.max(0, Math.min(NY, Math.round((z - 3) / (float) CELL)));
        return new int[]{Math.max(3, Math.min(maxX, x)), r * CELL + 3};
    }

    public static boolean isBuilt(ServerLevel l) {
        BlockPos r = relayPos(0);
        return l.getBlockState(r).is(ModRegistry.POINT_RELAIS.get());
    }

    // ------------------------------------------------------------------ taxi & carte
    public static void taxi(ServerPlayer p, int idx) {
        ServerLevel ohio = Util.ohio(p.server);
        if (ohio == null || idx < 0 || idx >= COUNT) return;
        if (!isBuilt(ohio)) { Util.bar(p, "\u00a7cOhio City n'existe pas encore (un OP doit faire /gtc ville)"); return; }
        if (!GtcData.pay(p, 50)) { Util.bar(p, "\u00a7cTaxi : 50 CASH requis"); return; }
        BlockPos d = doorOutside(idx);
        p.teleportTo(ohio, d.getX() + 0.5, FEET, d.getZ() + 1.0, 0F, 0F);
        Util.bar(p, "\u00a7eTaxi : " + name(idx) + " (-50 CASH)");
        GtcData.log(p, "Taxi vers " + name(idx));
    }

    public static void waypoint(ServerPlayer p, String key, int idx) {
        if (key.equals("clear")) { MissionManager.WAY.remove(p.getUUID()); Net.sync(p); return; }
        if (!Util.inCity(p.level())) { Util.bar(p, "\u00a7cLa carte ne marche qu'a Ohio City (/gtc ohio)"); return; }
        if (key.equals("relay")) {
            BlockPos best = null;
            double bd = 1e18;
            for (int k = 0; k < RLP.length; k++) {
                BlockPos r = relayPos(k);
                double d = r.distSqr(p.blockPosition());
                if (d < bd) { bd = d; best = r; }
            }
            MissionManager.WAY.put(p.getUUID(), new MissionManager.Waypoint(best, "Point Relais"));
        } else if (idx >= 0 && idx < COUNT) {
            MissionManager.WAY.put(p.getUUID(), new MissionManager.Waypoint(doorOutside(idx), name(idx)));
        }
        Util.bar(p, "\u00a7aDestination definie");
        Net.sync(p);
    }

    // ------------------------------------------------------------------ job de generation
    private static class Job {
        ServerLevel level;
        int next;
        int total = COUNT + RLP.length;
        java.util.UUID starter;
    }

    private static Job job;

    public static boolean busy() { return job != null; }

    public static void start(ServerPlayer p) {
        if (job != null) { Util.msg(p, "\u00a7eLa generation est deja en cours."); return; }
        MinecraftServer s = p.server;
        ServerLevel l = Util.ohio(s);
        if (l == null) {
            Util.msg(p, "\u00a7cDimension gtcolis:ohio_city introuvable (le mod est-il bien charge ?).");
            return;
        }
        run(s, "execute in gtcolis:ohio_city run kill @e[type=!minecraft:player]");
        job = new Job();
        job.level = l;
        job.starter = p.getUUID();
        Util.msg(p, "\u00a7eGeneration d'Ohio City lancee (" + job.total + " etapes). Ca peut prendre une a deux minutes, ne t'eloigne pas trop...");
    }

    public static void tick(MinecraftServer server) {
        if (job == null) return;
        long end = System.nanoTime() + 28_000_000L;
        do {
            runTask(job.next++);
        } while (job.next < job.total && System.nanoTime() < end);
        ServerPlayer sp = server.getPlayerList().getPlayer(job.starter);
        if (sp != null && job.next % 7 == 0) Util.bar(sp, "\u00a7eOhio City : " + (job.next * 100 / job.total) + " %");
        if (job.next >= job.total) {
            server.getPlayerList().broadcastSystemMessage(Component.literal("\u00a7a\u2714 Ohio City est prete ! Utilise /gtc ohio pour y aller."), false);
            job = null;
        }
    }

    private static void run(MinecraftServer s, String cmd) {
        try {
            s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), cmd);
        } catch (Exception ignored) { }
    }

    private static void runTask(int n) {
        if (n < COUNT) building(job.level, n);
        else relay(job.level, n - COUNT);
    }

    // ------------------------------------------------------------------ outils de blocs
    private static final int FLAGS = 18; // 2 (client) | 16 (pas de mise a jour des voisins)

    private static void set(ServerLevel l, int x, int y, int z, BlockState s) {
        l.setBlock(new BlockPos(x, y, z), s, FLAGS);
    }

    private static void set(ServerLevel l, int x, int y, int z, Block b) {
        set(l, x, y, z, b.defaultBlockState());
    }

    private static void fill(ServerLevel l, int x1, int y1, int z1, int x2, int y2, int z2, Block b) {
        BlockState s = b.defaultBlockState();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = x1; x <= x2; x++)
            for (int y = y1; y <= y2; y++)
                for (int z = z1; z <= z2; z++)
                    l.setBlock(m.set(x, y, z), s, FLAGS);
    }

    private static Block floorFor(String t, int x, int z) {
        boolean chk = ((x >> 1) + (z >> 1)) % 2 == 0;
        return switch (t) {
            case "club" -> Blocks.BLACK_CONCRETE;
            case "shop" -> Blocks.WHITE_CONCRETE;
            case "armu" -> Blocks.STONE_BRICKS;
            case "casino" -> chk ? Blocks.RED_CONCRETE : Blocks.BLACK_CONCRETE;
            case "garage" -> Blocks.GRAY_CONCRETE;
            case "friperie" -> chk ? Blocks.PURPLE_CONCRETE : Blocks.MAGENTA_CONCRETE;
            case "kebab" -> chk ? Blocks.RED_TERRACOTTA : Blocks.WHITE_TERRACOTTA;
            case "clinic" -> Blocks.WHITE_CONCRETE;
            case "pressing" -> Blocks.LIGHT_BLUE_TERRACOTTA;
            case "gym" -> chk ? Blocks.GRAY_CONCRETE : Blocks.BLACK_CONCRETE;
            case "nails" -> Blocks.PINK_CONCRETE;
            case "bar" -> Blocks.DARK_OAK_PLANKS;
            default -> chk ? Blocks.SMOOTH_QUARTZ : Blocks.QUARTZ_BLOCK; // mart
        };
    }

    private static void merchant(ServerLevel l, int x, int z, String shop) {
        MerchantEntity m = ModRegistry.MARCHAND.get().create(l);
        if (m == null) return;
        m.moveTo(x + 0.5, FEET, z + 0.5, 0F, 0F);
        m.setShop(shop);
        l.addFreshEntity(m);
    }

    private static void npc(ServerLevel l, int x, int z, int role, String name) {
        ContactEntity c = ModRegistry.CONTACT.get().create(l);
        if (c == null) return;
        c.moveTo(x + 0.5, FEET, z + 0.5, 0F, 0F);
        c.setRole(role);
        if (name != null) { c.setCustomName(Component.literal(name)); c.setCustomNameVisible(true); }
        l.addFreshEntity(c);
    }

    private static void sign(ServerLevel l, String text, String color, double x, double y, double z, float scale) {
        String cmd = String.format(Locale.ROOT,
                "execute in gtcolis:ohio_city run summon minecraft:text_display %.1f %.1f %.1f {Tags:[\"gtc_sign\"],text:'{\"text\":\"%s\",\"color\":\"%s\",\"bold\":true}',billboard:\"center\",background:1073741824,transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[%.1ff,%.1ff,%.1ff]}}",
                x, y, z, text, color, scale, scale, scale);
        run(l.getServer(), cmd);
    }

    // ------------------------------------------------------------------ un immeuble
    private static void building(ServerLevel l, int i) {
        int col = i / NY, row = i % NY;
        int lx = col * CELL + ROAD, lz = row * CELL + ROAD;
        int x0 = lx + 2, z0 = lz + 2, x1 = x0 + SIDE - 1, z1 = z0 + SIDE - 1;
        String t = kind(i);
        Block face = ModRegistry.facades()[(col * 3 + row * 2) % 5];
        int top = CEIL + 3 + ((col * 7 + row * 13) % 10);
        Random rnd = new Random(i * 7919L + 13);

        // trottoir de la parcelle
        fill(l, lx, GROUND, lz, lx + 29, GROUND, lz + 29, ModRegistry.TROTTOIR.get());
        // sol interieur
        for (int x = x0 + 1; x < x1; x++)
            for (int z = z0 + 1; z < z1; z++)
                set(l, x, GROUND, z, floorFor(t, x, z));
        // murs
        fill(l, x0, FEET, z0, x1, top, z0, face);
        fill(l, x0, FEET, z1, x1, top, z1, face);
        fill(l, x0, FEET, z0, x0, top, z1, face);
        fill(l, x1, FEET, z0, x1, top, z1, face);
        // plafond + toit
        fill(l, x0 + 1, CEIL, z0 + 1, x1 - 1, CEIL, z1 - 1, Blocks.SMOOTH_STONE);
        fill(l, x0, top, z0, x1, top, z1, Blocks.SMOOTH_STONE);
        // vitrines (rez-de-chaussee)
        for (int k = 2; k < SIDE - 2; k++) {
            if ((k % 6) == 2 || (k % 6) == 3) {
                fill(l, x0 + k, 6, z0, x0 + k, 7, z0, Blocks.GLASS);
                fill(l, x0, 6, z0 + k, x0, 7, z0 + k, Blocks.GLASS);
                fill(l, x1, 6, z0 + k, x1, 7, z0 + k, Blocks.GLASS);
                if (k < 10 || k > 15) fill(l, x0 + k, 6, z1, x0 + k, 7, z1, Blocks.GLASS);
            }
        }
        // porte (4 de large, 4 de haut) + encadrement
        fill(l, x0 + 11, FEET, z1, x0 + 14, FEET + 3, z1, Blocks.AIR);
        fill(l, x0 + 10, FEET, z1 + 1, x0 + 15, FEET, z1 + 1, Blocks.LIGHT_GRAY_CONCRETE);
        // lumieres du plafond
        for (int a = 0; a < 4; a++)
            for (int b = 0; b < 4; b++)
                set(l, x0 + 4 + a * 6, CEIL - 1, z0 + 4 + b * 6, Blocks.SEA_LANTERN);
        set(l, x0 + 10, FEET + 3, z1 + 1, Blocks.SEA_LANTERN);
        set(l, x0 + 15, FEET + 3, z1 + 1, Blocks.SEA_LANTERN);
        // lampadaires
        int[][] lamps = {{0, 0}, {14, 0}, {29, 0}, {0, 14}, {29, 14}, {0, 29}, {20, 29}, {29, 29}};
        for (int[] o : lamps) {
            int px = lx + o[0], pz = lz + o[1];
            fill(l, px, FEET, pz, px, FEET + 3, pz, Blocks.POLISHED_BLACKSTONE_WALL);
            set(l, px, FEET + 4, pz, Blocks.SEA_LANTERN);
        }
        interior(l, i, t, x0, z0, rnd);
        // enseigne
        sign(l, name(i), COLORS[(col * 3 + row * 2) % 5], x0 + 12.5, FEET + 5.2, z1 + 1.6, 4F);
    }

    private static void interior(ServerLevel l, int i, String t, int x0, int z0, Random rnd) {
        boolean club = t.equals("club"), gym = t.equals("gym");
        // comptoir
        if (!club && !gym) {
            for (int x = x0 + 6; x <= x0 + 19; x++) {
                set(l, x, FEET, z0 + 6, Blocks.SMOOTH_STONE);
                set(l, x, FEET + 1, z0 + 6, Blocks.SMOOTH_STONE_SLAB);
            }
            Block shelf = t.equals("bar") || t.equals("casino") ? Blocks.BARREL : Blocks.BOOKSHELF;
            if (!t.equals("shop") && !t.equals("garage")) {
                for (int z = z0 + 9; z <= z0 + 20; z++) {
                    fill(l, x0 + 1, FEET, z, x0 + 1, FEET + 1, z, shelf);
                    fill(l, x0 + 24, FEET, z, x0 + 24, FEET + 1, z, shelf);
                }
            }
        }
        // decors par type
        switch (t) {
            case "club" -> {
                fill(l, x0 + 5, GROUND, z0 + 6, x0 + 20, GROUND, z0 + 15, ModRegistry.PISTE_DANSE.get());
                fill(l, x0 + 8, FEET, z0 + 2, x0 + 17, FEET, z0 + 3, Blocks.POLISHED_BLACKSTONE);
                fill(l, x0 + 2, FEET, z0 + 2, x0 + 3, FEET + 2, z0 + 3, Blocks.BLACKSTONE);
                fill(l, x0 + 22, FEET, z0 + 2, x0 + 23, FEET + 2, z0 + 3, Blocks.BLACKSTONE);
                fill(l, x0 + 5, CEIL - 1, z0 + 6, x0 + 20, CEIL - 1, z0 + 6, Blocks.MAGENTA_STAINED_GLASS);
                fill(l, x0 + 5, CEIL - 1, z0 + 15, x0 + 20, CEIL - 1, z0 + 15, Blocks.CYAN_STAINED_GLASS);
                npc(l, x0 + 12, z0 + 2, 3, "\u00a7dDJ");
                for (int k = 0; k < 6; k++) npc(l, x0 + 7 + rnd.nextInt(12), z0 + 7 + rnd.nextInt(8), 3, k % 2 == 0 ? "\u00a7dDanseuse" : "\u00a7bDanseur");
                for (int k = 0; k < 2; k++) npc(l, x0 + 3 + rnd.nextInt(18), z0 + 18 + rnd.nextInt(5), 0, null);
                return;
            }
            case "shop" -> {
                for (int k = 0; k < 3; k++) fill(l, x0 + 4 + k * 8, FEET, z0 + 10, x0 + 8 + k * 8, FEET, z0 + 14, Blocks.QUARTZ_BLOCK);
                for (int k = 0; k < 3; k++) fill(l, x0 + 5 + k * 8, FEET + 1, z0 + 11, x0 + 7 + k * 8, FEET + 1, z0 + 13, k == 0 ? Blocks.LIME_CONCRETE : k == 1 ? Blocks.RED_CONCRETE : Blocks.BLUE_CONCRETE);
                merchant(l, x0 + 12, z0 + 4, "concessionnaire");
            }
            case "armu" -> {
                for (int x = x0 + 3; x <= x0 + 22; x += 2) fill(l, x, FEET, z0 + 1, x, FEET + 2, z0 + 1, Blocks.IRON_BARS);
                merchant(l, x0 + 12, z0 + 4, "armurier");
            }
            case "casino" -> {
                for (int k = 0; k < 5; k++) {
                    fill(l, x0 + 3 + k * 4, FEET, z0 + 12, x0 + 4 + k * 4, FEET + 1, z0 + 12, Blocks.GOLD_BLOCK);
                    set(l, x0 + 3 + k * 4, FEET + 2, z0 + 12, Blocks.REDSTONE_LAMP);
                }
                merchant(l, x0 + 12, z0 + 4, "casino");
            }
            case "garage" -> {
                fill(l, x0 + 4, FEET, z0 + 12, x0 + 9, FEET, z0 + 18, Blocks.YELLOW_CONCRETE);
                fill(l, x0 + 16, FEET, z0 + 12, x0 + 21, FEET, z0 + 18, Blocks.YELLOW_CONCRETE);
                merchant(l, x0 + 12, z0 + 4, "garage");
                if (i == 6) merchant(l, x0 + 3, z0 + 22, "indic");
            }
            case "friperie" -> {
                for (int k = 0; k < 4; k++) fill(l, x0 + 5 + k * 5, FEET, z0 + 14, x0 + 5 + k * 5, FEET + 1, z0 + 14, Blocks.OAK_FENCE);
                merchant(l, x0 + 12, z0 + 4, "friperie");
            }
            case "gym" -> {
                for (int k = 0; k < 5; k++) fill(l, x0 + 3 + k * 4, FEET, z0 + 18, x0 + 4 + k * 4, FEET, z0 + 18, Blocks.IRON_BLOCK);
                for (int k = 0; k < 4; k++) fill(l, x0 + 4 + k * 5, FEET, z0 + 6, x0 + 4 + k * 5, FEET + 1, z0 + 7, Blocks.ANVIL);
            }
            default -> { }
        }
        if (i == 17) merchant(l, x0 + 22, z0 + 22, "flic");
        int n = 2 + (i % 3);
        for (int k = 0; k < n; k++) npc(l, x0 + 4 + rnd.nextInt(18), z0 + 9 + rnd.nextInt(14), 0, null);
    }

    // ------------------------------------------------------------------ points relais
    private static void relay(ServerLevel l, int k) {
        BlockPos r = relayPos(k);
        int cx = r.getX(), cz = r.getZ();
        for (int dx = -3; dx <= 2; dx++)
            for (int dz = -3; dz <= 2; dz++)
                if ((dx + 0.5) * (dx + 0.5) + (dz + 0.5) * (dz + 0.5) <= 8.0)
                    set(l, cx + dx, GROUND, cz + dz, Blocks.LIME_CONCRETE);
        set(l, cx, FEET, cz, ModRegistry.POINT_RELAIS.get());
        fill(l, cx, FEET + 1, cz, cx, FEET + 5, cz, Blocks.LIME_STAINED_GLASS);
        sign(l, "POINT RELAIS", "green", cx + 0.5, FEET + 6.6, cz + 0.5, 3F);
    }
}
