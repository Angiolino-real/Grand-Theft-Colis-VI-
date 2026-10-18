package fr.gtcolis.gtc;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public final class Net {
    private static final String V = "1";
    public static final SimpleChannel CH = NetworkRegistry.newSimpleChannel(new ResourceLocation(GtColis.MODID, "main"), () -> V, V::equals, V::equals);

    private Net() {}

    public static void init() {
        int i = 0;
        CH.registerMessage(i++, Sync.class, Sync::enc, Sync::new, Sync::handle);
        CH.registerMessage(i++, OpenShop.class, OpenShop::enc, OpenShop::new, OpenShop::handle);
        CH.registerMessage(i++, Call.class, Call::enc, Call::new, Call::handle);
        CH.registerMessage(i++, Action.class, Action::enc, Action::new, Action::handle);
    }

    public static void to(ServerPlayer sp, Object msg) {
        CH.send(PacketDistributor.PLAYER.with(() -> sp), msg);
    }

    public static void sync(ServerPlayer sp) {
        BlockPos t = MissionManager.targetOf(sp);
        to(sp, new Sync(GtcData.cash(sp), GtcData.stars(sp), GtcData.lives(sp), GtcData.msDone(sp),
                MissionManager.descOf(sp), t != null, t == null ? 0L : t.asLong(), MissionManager.targetName(sp), GtcData.journal(sp)));
    }

    // ---------- Serveur -> client : etat du joueur
    public static class Sync {
        public final int cash, stars, lives, ms;
        public final String mission, tname, journal;
        public final boolean hasT;
        public final long target;

        public Sync(int cash, int stars, int lives, int ms, String mission, boolean hasT, long target, String tname, String journal) {
            this.cash = cash; this.stars = stars; this.lives = lives; this.ms = ms; this.mission = mission;
            this.hasT = hasT; this.target = target; this.tname = tname; this.journal = journal;
        }

        public Sync(FriendlyByteBuf b) {
            cash = b.readInt(); stars = b.readInt(); lives = b.readInt(); ms = b.readInt();
            mission = b.readUtf(); hasT = b.readBoolean(); target = b.readLong(); tname = b.readUtf(); journal = b.readUtf(8192);
        }

        public void enc(FriendlyByteBuf b) {
            b.writeInt(cash); b.writeInt(stars); b.writeInt(lives); b.writeInt(ms);
            b.writeUtf(mission); b.writeBoolean(hasT); b.writeLong(target); b.writeUtf(tname); b.writeUtf(journal, 8192);
        }

        public void handle(Supplier<NetworkEvent.Context> c) {
            c.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.sync(this)));
            c.get().setPacketHandled(true);
        }
    }

    // ---------- Serveur -> client : ouvre un commerce
    public static class OpenShop {
        public final String id;

        public OpenShop(String id) { this.id = id; }
        public OpenShop(FriendlyByteBuf b) { id = b.readUtf(); }
        public void enc(FriendlyByteBuf b) { b.writeUtf(id); }

        public void handle(Supplier<NetworkEvent.Context> c) {
            c.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openShop(id)));
            c.get().setPacketHandled(true);
        }
    }

    // ---------- Serveur -> client : appel entrant
    public static class Call {
        public final int idx;

        public Call(int idx) { this.idx = idx; }
        public Call(FriendlyByteBuf b) { idx = b.readInt(); }
        public void enc(FriendlyByteBuf b) { b.writeInt(idx); }

        public void handle(Supplier<NetworkEvent.Context> c) {
            c.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openCall(idx)));
            c.get().setPacketHandled(true);
        }
    }

    // ---------- Client -> serveur : actions (achat, telephone...)
    public static class Action {
        public final String type, a;
        public final int b;

        public Action(String type, String a, int b) { this.type = type; this.a = a; this.b = b; }
        public Action(FriendlyByteBuf buf) { type = buf.readUtf(64); a = buf.readUtf(64); b = buf.readInt(); }
        public void enc(FriendlyByteBuf buf) { buf.writeUtf(type, 64); buf.writeUtf(a, 64); buf.writeInt(b); }

        public void handle(Supplier<NetworkEvent.Context> c) {
            ServerPlayer sp = c.get().getSender();
            c.get().enqueueWork(() -> {
                if (sp != null) Actions.handle(sp, this);
            });
            c.get().setPacketHandled(true);
        }
    }
}
