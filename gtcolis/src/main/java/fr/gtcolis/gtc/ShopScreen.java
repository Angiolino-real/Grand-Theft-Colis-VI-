package fr.gtcolis.gtc;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ShopScreen extends Screen {
    private final String id;
    private final Shops.Shop shop;

    public ShopScreen(String id) {
        super(Component.literal(Shops.ALL.containsKey(id) ? Shops.ALL.get(id).title() : "Commerce"));
        this.id = id;
        this.shop = Shops.ALL.get(id);
    }

    @Override
    protected void init() {
        if (shop == null) return;
        int w = 220, x = (width - w) / 2, y = height / 2 - (shop.offers().size() * 22) / 2 + 6;
        for (int i = 0; i < shop.offers().size(); i++) {
            Shops.Offer o = shop.offers().get(i);
            int idx = i;
            String label = o.label() + (o.price() > 0 ? "  -  " + o.price() + " CASH" : "  -  gratuit");
            addRenderableWidget(Button.builder(Component.literal(label), b -> Net.CH.sendToServer(new Net.Action("buy", id, idx)))
                    .bounds(x, y + i * 22, w, 20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Partir"), b -> onClose())
                .bounds(x, y + shop.offers().size() * 22 + 6, w, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.drawCenteredString(font, title, width / 2, height / 2 - (shop == null ? 0 : shop.offers().size() * 11) - 22, 0xFFC6FF00);
        g.drawCenteredString(font, "CASH : " + ClientState.cash, width / 2, height / 2 - (shop == null ? 0 : shop.offers().size() * 11) - 10, 0xFFD8D4C8);
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
