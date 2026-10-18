package fr.gtcolis.gtc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/** Appel entrant. */
public class CallScreen extends Screen {
    private final int idx;
    private int t;
    private boolean done;

    public CallScreen(int idx) {
        super(Component.literal("Appel entrant"));
        this.idx = Math.floorMod(idx, Calls.NAMES.length);
    }

    @Override
    protected void init() {
        int cx = width / 2, y = height / 2 + 26;
        addRenderableWidget(Button.builder(Component.literal("\u00a7cRefuser"), b -> answer(false)).bounds(cx - 84, y, 80, 22).build());
        addRenderableWidget(Button.builder(Component.literal("\u00a7aRepondre"), b -> answer(true)).bounds(cx + 4, y, 80, 22).build());
    }

    private void answer(boolean yes) {
        if (done) return;
        done = true;
        Net.CH.sendToServer(new Net.Action("callans", "", idx + (yes ? 100 : 0)));
        onClose();
    }

    @Override
    public void tick() {
        t++;
        if (t % 14 == 1) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL.get(), 1.6F, 0.6F));
        if (t > 340) answer(false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        int cx = width / 2, cy = height / 2;
        g.fill(cx - 100, cy - 60, cx + 100, cy + 60, 0xFF0C0D0F);
        g.renderOutline(cx - 100, cy - 60, 200, 120, (t / 7) % 2 == 0 ? 0xFF6B8F6B : 0xFF2A2C31);
        g.drawCenteredString(font, "Appel entrant", cx, cy - 46, 0xFF9A968A);
        g.drawCenteredString(font, Calls.NAMES[idx], cx, cy - 26, 0xFFD8D4C8);
        g.drawCenteredString(font, Calls.SUBS[idx], cx, cy - 10, 0xFF9A968A);
        super.render(g, mx, my, pt);
    }

    @Override
    public void onClose() {
        if (!done) { done = true; Net.CH.sendToServer(new Net.Action("callans", "", idx)); }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
