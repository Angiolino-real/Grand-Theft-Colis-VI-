package fr.gtcolis.gtc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

public final class Util {
    public static final ResourceKey<Level> OHIO = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(GtColis.MODID, "ohio_city"));

    private Util() {}

    public static boolean inCity(Level l) {
        return l.dimension().equals(OHIO);
    }

    public static ServerLevel ohio(MinecraftServer s) {
        return s.getLevel(OHIO);
    }

    /** Position au sol (pieds) ou null si inadaptee. */
    public static BlockPos ground(ServerLevel l, int x, int z) {
        if (!l.hasChunkAt(new BlockPos(x, 0, z))) return null;
        int y = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (inCity(l) && y > 6) return null;
        BlockPos p = new BlockPos(x, y, z);
        BlockState below = l.getBlockState(p.below());
        if (!below.canOcclude() || !l.getFluidState(p).isEmpty()) return null;
        return p;
    }

    public static int carry(Player p) {
        return p.getInventory().clearOrCountMatchingItems(s -> s.is(ModRegistry.COLIS.get()), 0, p.inventoryMenu.getCraftSlots());
    }

    public static void removeColis(Player p, int n) {
        p.getInventory().clearOrCountMatchingItems(s -> s.is(ModRegistry.COLIS.get()), n <= 0 ? -1 : n, p.inventoryMenu.getCraftSlots());
        p.inventoryMenu.broadcastChanges();
    }

    public static int ammo(Player p) {
        return p.getInventory().clearOrCountMatchingItems(s -> s.is(ModRegistry.MUNITIONS.get()), 0, p.inventoryMenu.getCraftSlots());
    }

    public static void give(ServerPlayer p, ItemStack st) {
        if (!p.getInventory().add(st)) p.drop(st, false);
    }

    public static void msg(ServerPlayer p, String s) {
        p.sendSystemMessage(Component.literal(s));
    }

    public static void bar(ServerPlayer p, String s) {
        p.displayClientMessage(Component.literal(s), true);
    }

    public static String stars(int n) {
        return "\u2605".repeat(Math.max(0, n)) + "\u2606".repeat(Math.max(0, 6 - n));
    }
}
