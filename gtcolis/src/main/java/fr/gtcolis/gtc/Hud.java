package fr.gtcolis.gtc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;

public final class Hud {
    private Hud() {}

    private static int chip(GuiGraphics g, Font f, int x, int y, String text, int bg, int fg) {
        int w = f.width(text) + 8;
        g.fill(x, y, x + w, y + 12, bg);
        g.renderOutline(x, y, w, 12, 0x33FFFFFF);
        g.drawString(f, text, x + 4, y + 2, fg, false);
        return w + 4;
    }

    public static void render(ForgeGui gui, GuiGraphics g, float pt, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        Font f = mc.font;
        int x = 6, y = 6;
        int carry = mc.player.getInventory().clearOrCountMatchingItems(s -> s.is(ModRegistry.COLIS.get()), 0, mc.player.inventoryMenu.getCraftSlots());
        x += chip(g, f, x, y, "CASH " + ClientState.cash, 0xCC121315, 0xFFD8D4C8);
        x += chip(g, f, x, y, "DOUANE " + Util.stars(ClientState.stars), 0xCC461616, ClientState.stars > 0 ? 0xFFFF6A6A : 0xFFD8D4C8);
        x += chip(g, f, x, y, "COLIS " + carry, 0xCC121315, 0xFFD8D4C8);
        chip(g, f, x, y, "VIES " + "\u2764".repeat(Math.max(0, ClientState.lives)), 0xCC121315, 0xFFFF4D6D);
        y += 16;
        if (!ClientState.mission.isEmpty()) {
            chip(g, f, 6, y, "MISSION : " + ClientState.mission, 0xCC2A1A3A, 0xFFE8D8FF);
            y += 16;
        }
        if (ClientState.target != null) {
            double dx = ClientState.target.getX() + 0.5 - mc.player.getX(), dz = ClientState.target.getZ() + 0.5 - mc.player.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            double ang = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;           // yaw monde de la cible
            double rel = ((ang - mc.player.getYRot()) % 360 + 540) % 360 - 180; // relatif a la vue (-180..180)
            String[] arr = {"\u2191", "\u2197", "\u2192", "\u2198", "\u2193", "\u2199", "\u2190", "\u2196"};
            int k = (int) Math.floorMod(Math.round(rel / 45.0), 8);
            String name = ClientState.targetName.isEmpty() ? "Cible" : ClientState.targetName;
            chip(g, f, 6, y, arr[k] + " " + name + " - " + (int) dist + " m", 0xCC10301A, 0xFFB8FF70);
        }
    }
}
