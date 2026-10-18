package fr.gtcolis.gtc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Telephone : Missions, Carte, Taxi, Armes, Journal, Stats, Contacts, Reglages. */
public class PhoneScreen extends Screen {
    private static final int W = 200, H = 236;
    private String page = "home";
    private int pg = 0;
    private int px, py;

    public PhoneScreen() {
        super(Component.literal("Telephone"));
    }

    private void act(String type, String a, int b) {
        Net.CH.sendToServer(new Net.Action(type, a, b));
    }

    private Button btn(int x, int y, int w, int h, String label, Runnable r) {
        return addRenderableWidget(Button.builder(Component.literal(label), b -> r.run()).bounds(x, y, w, h).build());
    }

    private void go(String p) {
        page = p;
        pg = 0;
        rebuildWidgets();
    }

    @Override
    protected void init() {
        px = (width - W) / 2;
        py = (height - H) / 2;
        int x = px + 10, w = W - 20, y = py + 34;
        switch (page) {
            case "home" -> {
                String[][] apps = {{"Missions", "m"}, {"Carte", "c"}, {"Taxi", "t"}, {"Armes", "a"}, {"Journal", "j"}, {"Stats", "s"}, {"Contacts", "k"}, {"Reglages", "r"}};
                for (int i = 0; i < apps.length; i++) {
                    String key = apps[i][1];
                    btn(x + (i % 2) * (w / 2 + 2), y + (i / 2) * 40, w / 2 - 2, 34, apps[i][0], () -> go(key));
                }
            }
            case "m" -> {
                for (int i = 0; i < MissionManager.PHONE_INFO.length; i++) {
                    String[] mi = MissionManager.PHONE_INFO[i];
                    btn(x, y + i * 24, w, 20, mi[1] + "  (" + mi[3] + ")", () -> { act("mission", mi[0], 0); onClose(); });
                }
                btn(x, y + 6 * 24 + 4, w, 20, "Retour", () -> go("home"));
            }
            case "c" -> {
                String[][] pl = {{"relay", "Point Relais le plus proche", "0"}, {"b", "Club Ohio", String.valueOf(CityGen.CLUB)}, {"b", "Auto Skibidi", String.valueOf(CityGen.SHOP)},
                        {"b", "Armurerie Sigma", String.valueOf(CityGen.ARM)}, {"b", "Casino Le Gyatt", "0"}, {"b", "Garage Brainrot", "6"}, {"b", "Friperie Ohio", "11"}};
                for (int i = 0; i < pl.length; i++) {
                    String[] q = pl[i];
                    btn(x, y + i * 22, w, 20, q[1], () -> { act("waypoint", q[0], Integer.parseInt(q[2])); onClose(); });
                }
                btn(x, y + 7 * 22, w / 2 - 2, 20, "Effacer", () -> { act("waypoint", "clear", 0); onClose(); });
                btn(x + w / 2 + 2, y + 7 * 22, w / 2 - 2, 20, "Retour", () -> go("home"));
            }
            case "t" -> pagedBuildings(x, y, w);
            case "a" -> {
                Item[] gun = {ModRegistry.BATTE.get(), ModRegistry.PISTOLET.get(), ModRegistry.FUSIL.get(), ModRegistry.MITRAILLETTE.get()};
                int n = 0;
                for (Item it : gun) {
                    if (find(it) < 0) continue;
                    btn(x, y + n * 24, w, 20, new ItemStack(it).getHoverName().getString(), () -> { equip(it); onClose(); });
                    n++;
                }
                if (n == 0) { /* texte dessine dans render */ }
                btn(x, y + 6 * 24, w, 20, "Retour", () -> go("home"));
            }
            case "k" -> {
                btn(x, y, w, 24, "Indic : un boulot, gratuit", () -> { act("contact", "", 0); onClose(); });
                btn(x, y + 28, w, 24, "Avocat : efface ton dossier (300)", () -> { act("contact", "", 1); onClose(); });
                btn(x, y + 56, w, 24, "Taxi : voyage vers un commerce (50)", () -> go("t"));
                btn(x, y + 6 * 24, w, 20, "Retour", () -> go("home"));
            }
            case "r" -> {
                btn(x, y, w, 24, "Abandonner la mission", () -> { act("abandon", "", 0); onClose(); });
                btn(x, y + 28, w, 24, "Effacer la destination", () -> { act("waypoint", "clear", 0); onClose(); });
                btn(x, y + 6 * 24, w, 20, "Retour", () -> go("home"));
            }
            default -> btn(x, y + 6 * 24 + 8, w, 20, "Retour", () -> go("home"));
        }
    }

    private void pagedBuildings(int x, int y, int w) {
        int per = 7, pages = (CityGen.COUNT + per - 1) / per;
        for (int i = 0; i < per; i++) {
            int idx = pg * per + i;
            if (idx >= CityGen.COUNT) break;
            btn(x, y + i * 22, w, 20, CityGen.name(idx), () -> { act("taxi", "", idx); onClose(); });
        }
        int by = y + per * 22 + 2;
        btn(x, by, 40, 20, "<", () -> { pg = (pg + pages - 1) % pages; rebuildWidgets(); });
        btn(x + 44, by, w - 88, 20, "Retour (" + (pg + 1) + "/" + pages + ")", () -> go("home"));
        btn(x + w - 40, by, 40, 20, ">", () -> { pg = (pg + 1) % pages; rebuildWidgets(); });
    }

    private int find(Item it) {
        var inv = Minecraft.getInstance().player.getInventory();
        for (int i = 0; i < 36; i++) if (inv.getItem(i).is(it)) return i;
        return -1;
    }

    private void equip(Item it) {
        Minecraft mc = Minecraft.getInstance();
        int i = find(it);
        if (i < 0 || mc.player == null) return;
        if (i < 9) {
            mc.player.getInventory().selected = i;
        } else {
            mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, i, mc.player.getInventory().selected, ClickType.SWAP, mc.player);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.fill(px - 5, py - 5, px + W + 5, py + H + 5, 0xFF2A2C31);
        g.fill(px, py, px + W, py + H, 0xFF0C0D0F);
        g.drawString(font, java.time.LocalTime.now().toString().substring(0, 5), px + 10, py + 8, 0xFF9A968A, false);
        String cash = "CASH " + ClientState.cash;
        g.drawString(font, cash, px + W - 10 - font.width(cash), py + 8, 0xFFD8D4C8, false);
        int x = px + 10, y = py + 34;
        List<String> lines = new ArrayList<>();
        switch (page) {
            case "s" -> {
                lines.add("CASH : " + ClientState.cash);
                lines.add("Douane : " + ClientState.stars + "/6");
                lines.add("Vies : " + ClientState.lives);
                lines.add("Missions reussies : " + ClientState.ms);
                lines.add("Mission : " + (ClientState.mission.isEmpty() ? "aucune" : ClientState.mission));
            }
            case "j" -> {
                if (ClientState.journal.isEmpty()) lines.add("Rien pour l'instant.");
                else for (String s : ClientState.journal.split("\n")) lines.add(s);
            }
            case "a" -> {
                if (find(ModRegistry.BATTE.get()) < 0 && find(ModRegistry.PISTOLET.get()) < 0 && find(ModRegistry.FUSIL.get()) < 0 && find(ModRegistry.MITRAILLETTE.get()) < 0)
                    lines.add("Aucune arme. Va a l'armurerie.");
            }
            default -> { }
        }
        for (String l : lines)
            for (var seq : font.split(Component.literal(l), W - 20)) {
                g.drawString(font, seq, x, y, 0xFFD8D4C8, false);
                y += 11;
            }
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
