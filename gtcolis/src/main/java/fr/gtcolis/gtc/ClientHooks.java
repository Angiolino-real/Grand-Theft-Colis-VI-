package fr.gtcolis.gtc;

import net.minecraft.client.Minecraft;

/** Points d'entree client (appeles via DistExecutor). */
public final class ClientHooks {
    private ClientHooks() {}

    public static void sync(Net.Sync s) { ClientState.apply(s); }

    public static void openPhone() { Minecraft.getInstance().setScreen(new PhoneScreen()); }

    public static void openShop(String id) { Minecraft.getInstance().setScreen(new ShopScreen(id)); }

    public static void openCall(int idx) { Minecraft.getInstance().setScreen(new CallScreen(idx)); }
}
