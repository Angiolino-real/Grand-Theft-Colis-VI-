package fr.gtcolis.gtc;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Catalogue des commerces (partage client/serveur) + execution des achats cote serveur. */
public final class Shops {
    public record Offer(String label, int price) {}
    public record Shop(String title, List<Offer> offers) {}

    public static final String[] COLORS = {"Rouge", "Bleu", "Jaune", "Vert", "Blanc", "Noir"};
    public static final int[] RGB = {0xC82828, 0x2850C8, 0xEBBE28, 0x32AA5A, 0xEBEBEB, 0x3C3C42};
    public static final String[] BODIES = {"Coupe", "Berline", "Fourgon"};
    public static final String[] TOPS = {"Rouge", "Bleu", "Vert", "Noir", "Blanc", "Violet"};
    public static final int[] TOP_RGB = {0xC82828, 0x2850C8, 0x32AA5A, 0x202024, 0xF0F0F0, 0x7B2CFF};

    public static final Map<String, Shop> ALL = new LinkedHashMap<>();
    private static final Random R = new Random();

    static {
        ALL.put("armurier", new Shop("Armurerie Sigma", List.of(
                new Offer("Batte", 300), new Offer("Pistolet (+36 munitions)", 600), new Offer("Fusil a pompe (+16)", 1200),
                new Offer("Mitraillette (+120)", 2000), new Offer("Munitions x32", 150))));
        ALL.put("concessionnaire", new Shop("Auto Skibidi", List.of(
                new Offer("Cle de voiture", 1200), new Offer("Reparer la voiture proche", 150))));
        java.util.ArrayList<Offer> g = new java.util.ArrayList<>();
        for (String c : COLORS) g.add(new Offer("Peinture " + c, 100));
        for (String b : BODIES) g.add(new Offer("Carrosserie " + b, 300));
        ALL.put("garage", new Shop("Garage Brainrot", g));
        java.util.ArrayList<Offer> f = new java.util.ArrayList<>();
        for (String c : TOPS) f.add(new Offer("Haut " + c, 60));
        ALL.put("friperie", new Shop("Friperie Ohio", f));
        ALL.put("casino", new Shop("Casino Le Gyatt", List.of(
                new Offer("Machine a sous", 50), new Offer("Des : Haut (8+)", 50), new Offer("Des : Bas (6-)", 50))));
        ALL.put("indic", new Shop("Indic", List.of(new Offer("Du boulot", 0), new Offer("Un tuyau", 50))));
        ALL.put("flic", new Shop("Flic corrompu", List.of(new Offer("Payer : dossier efface", 200), new Offer("Le menacer", 0))));
    }

    private Shops() {}

    private static boolean has(ServerPlayer p, Item it) {
        return p.getInventory().clearOrCountMatchingItems(s -> s.is(it), 0, p.inventoryMenu.getCraftSlots()) > 0;
    }

    private static void ammo(ServerPlayer p, int n) {
        while (n > 0) {
            int k = Math.min(64, n);
            Util.give(p, new ItemStack(ModRegistry.MUNITIONS.get(), k));
            n -= k;
        }
    }

    private static VoitureEntity car(ServerPlayer p) {
        VoitureEntity best = null;
        double bd = 1e9;
        for (VoitureEntity c : p.level().getEntitiesOfClass(VoitureEntity.class, p.getBoundingBox().inflate(16))) {
            double d = c.distanceToSqr(p);
            if (d < bd) { bd = d; best = c; }
        }
        return best;
    }

    private static void weapon(ServerPlayer p, Item gun, int mag, String name) {
        if (has(p, gun)) {
            ammo(p, mag);
            Util.bar(p, "\u00a7eMUNITIONS +" + mag);
        } else {
            Util.give(p, new ItemStack(gun));
            ammo(p, mag);
            Util.bar(p, "\u00a7aACHETE : " + name);
        }
    }

    public static void buy(ServerPlayer p, String id, int idx) {
        Shop sh = ALL.get(id);
        if (sh == null || idx < 0 || idx >= sh.offers().size()) return;
        Offer o = sh.offers().get(idx);
        if (o.price() > 0 && GtcData.cash(p) < o.price()) {
            Util.bar(p, "\u00a7cPas assez de CASH (" + o.price() + " requis, tu as " + GtcData.cash(p) + ")");
            return;
        }
        switch (id) {
            case "armurier" -> {
                GtcData.addCash(p, -o.price());
                switch (idx) {
                    case 0 -> {
                        if (has(p, ModRegistry.BATTE.get())) { GtcData.addCash(p, o.price()); Util.bar(p, "\u00a7eTu as deja la batte"); }
                        else { Util.give(p, new ItemStack(ModRegistry.BATTE.get())); Util.bar(p, "\u00a7aACHETE : Batte"); }
                    }
                    case 1 -> weapon(p, ModRegistry.PISTOLET.get(), 36, "Pistolet");
                    case 2 -> weapon(p, ModRegistry.FUSIL.get(), 16, "Fusil a pompe");
                    case 3 -> weapon(p, ModRegistry.MITRAILLETTE.get(), 120, "Mitraillette");
                    default -> { ammo(p, 32); Util.bar(p, "\u00a7eMUNITIONS +32"); }
                }
            }
            case "concessionnaire" -> {
                if (idx == 0) {
                    if (has(p, ModRegistry.CLE_VOITURE.get())) { Util.bar(p, "\u00a7eTu as deja ta caisse"); return; }
                    GtcData.addCash(p, -o.price());
                    Util.give(p, new ItemStack(ModRegistry.CLE_VOITURE.get()));
                    Util.msg(p, "\u00a7aVoiture achetee ! Utilise la cle sur le sol pour la faire apparaitre, sigma.");
                    GtcData.log(p, "Achat d'une voiture.");
                } else {
                    VoitureEntity c = car(p);
                    if (c == null) { Util.bar(p, "\u00a7cAucune voiture a proximite"); return; }
                    GtcData.addCash(p, -o.price());
                    c.setHealth(c.getMaxHealth());
                    Util.bar(p, "\u00a7aREPAREE");
                }
            }
            case "garage" -> {
                VoitureEntity c = car(p);
                if (c == null) { Util.bar(p, "\u00a7cAucune voiture a proximite (gare-la pres du garage)"); return; }
                GtcData.addCash(p, -o.price());
                if (idx < 6) c.setColor(idx); else c.setBody(idx - 6);
                Util.bar(p, "\u00a7aFAIT !");
            }
            case "friperie" -> {
                GtcData.addCash(p, -o.price());
                ItemStack st = new ItemStack(Items.LEATHER_CHESTPLATE);
                ((DyeableLeatherItem) st.getItem()).setColor(st, TOP_RGB[idx]);
                Util.give(p, st);
                Util.bar(p, "\u00a7aNOUVEAU HAUT");
            }
            case "casino" -> {
                GtcData.addCash(p, -o.price());
                if (idx == 0) {
                    String[] S = {"7", "\u25C6", "\u25CF", "\u25B2"};
                    String a = S[R.nextInt(4)], b = S[R.nextInt(4)], c = S[R.nextInt(4)];
                    int w = 0;
                    if (a.equals(b) && b.equals(c)) w = a.equals("7") ? 1000 : 300;
                    else if (a.equals(b) || b.equals(c) || a.equals(c)) w = 80;
                    GtcData.addCash(p, w);
                    Util.msg(p, "\u00a76[Casino] \u00a7f" + a + " " + b + " " + c + (w > 0 ? " \u00a7a: +" + w + " CASH" : " \u00a7c: perdu"));
                } else {
                    int d1 = 1 + R.nextInt(6), d2 = 1 + R.nextInt(6), t = d1 + d2;
                    boolean win = idx == 1 ? t >= 8 : t <= 6;
                    if (win) GtcData.addCash(p, 100);
                    Util.msg(p, "\u00a76[Des] \u00a7f" + d1 + " + " + d2 + " = " + t + (win ? " \u00a7a: gagne, +100 CASH" : " \u00a7c: perdu"));
                }
            }
            case "indic" -> {
                if (idx == 0) {
                    MissionManager.start(p, MissionManager.PHONE_KINDS[R.nextInt(MissionManager.PHONE_KINDS.length)]);
                } else {
                    GtcData.addCash(p, -o.price());
                    int b = R.nextInt(CityGen.COUNT);
                    Util.msg(p, "\u00a77L'indic : \u00ab Du mouvement du cote de \u00a7e" + CityGen.name(b) + "\u00a77. \u00bb");
                }
            }
            case "flic" -> {
                if (idx == 0) {
                    GtcData.addCash(p, -o.price());
                    GtcData.clearWanted(p);
                    Util.bar(p, "\u00a7aDossier efface");
                } else {
                    GtcData.setHeat(p, Math.max(GtcData.heat(p), GtcData.TH[1]));
                    GtcData.raise(p);
                }
            }
            default -> {}
        }
    }
}
