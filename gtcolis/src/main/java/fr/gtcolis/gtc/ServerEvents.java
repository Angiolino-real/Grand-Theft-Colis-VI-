package fr.gtcolis.gtc;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class ServerEvents {
    private static final Random R = new Random();

    static class PS {
        int ev, hide, dance, copCd, callT = 500 + R.nextInt(700), pending = -1, pendT, evT = 800 + R.nextInt(1000);
        boolean near;
    }

    private static final Map<UUID, PS> STATE = new HashMap<>();

    private static PS ps(ServerPlayer p) {
        return STATE.computeIfAbsent(p.getUUID(), k -> new PS());
    }

    // ------------------------------------------------------------------ connexion
    @SubscribeEvent
    public void login(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            if (!GtcData.started(sp)) {
                GtcData.setStarted(sp);
                Util.give(sp, new ItemStack(ModRegistry.TELEPHONE.get()));
                Util.msg(sp, "\u00a7d\u00a7lGRAND THEFT COLIS VI \u00a7r\u00a77- Ramasse les colis (surbrillance), livre-les aux Points Relais, evite la Douane.");
                Util.msg(sp, "\u00a77Telephone : touche \u00a7eFleche haut\u00a77 ou clic droit. \u00a7e/gtc ville\u00a77 (une fois) puis \u00a7e/gtc ohio\u00a77 pour aller a Ohio City.");
            }
            Net.sync(sp);
        }
    }

    @SubscribeEvent
    public void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            MissionManager.forget(sp);
            STATE.remove(sp.getUUID());
        }
    }

    @SubscribeEvent
    public void clone(PlayerEvent.Clone e) {
        GtcData.setRaw(e.getEntity(), GtcData.raw(e.getOriginal()).copy());
    }

    @SubscribeEvent
    public void respawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            MissionManager.forget(sp);
            Net.sync(sp);
        }
    }

    @SubscribeEvent
    public void pickup(PlayerEvent.ItemPickupEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp && e.getStack().is(ModRegistry.COLIS.get())) {
            int n = e.getStack().getCount();
            GtcData.addCash(sp, 50 * n);
            Util.bar(sp, "\u00a7a+" + (50 * n) + " CASH \u00a77" + Util.carry(sp) + " colis");
            GtcData.parcelStars(sp);
        }
    }

    @SubscribeEvent
    public void hurt(LivingHurtEvent e) {
        if (e.getSource().getEntity() instanceof ServerPlayer sp && e.getEntity() instanceof ContactEntity c && c.role == 0) {
            GtcData.addHeat(sp, 0.5F);
        }
    }

    @SubscribeEvent
    public void death(LivingDeathEvent e) {
        if (e.getSource().getEntity() instanceof ServerPlayer sp && e.getEntity() instanceof ContactEntity c && c.role == 0) {
            GtcData.addHeat(sp, 1F);
        }
    }

    // ------------------------------------------------------------------ ticks serveur
    @SubscribeEvent
    public void serverTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        CityGen.tick(e.getServer());
        if (!GtcData.DIRTY.isEmpty()) {
            for (UUID id : List.copyOf(GtcData.DIRTY)) {
                ServerPlayer p = e.getServer().getPlayerList().getPlayer(id);
                if (p != null) Net.sync(p);
            }
            GtcData.DIRTY.clear();
        }
    }

    @SubscribeEvent
    public void playerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.side != LogicalSide.SERVER || !(e.player instanceof ServerPlayer sp)) return;
        if (!GtcData.started(sp)) return;
        PS s = ps(sp);
        ServerLevel lvl = sp.serverLevel();
        int t = sp.tickCount;
        boolean city = Util.inCity(lvl);

        MissionManager.tick(sp);

        // piste de danse
        if (lvl.getBlockState(sp.blockPosition().below()).is(ModRegistry.PISTE_DANSE.get())) {
            s.dance++;
            if (s.dance % 60 == 0) {
                GtcData.addCash(sp, 20);
                Util.bar(sp, "\u00a7dRIZZ +20");
            }
            MissionManager.danceTick(sp);
        }

        if (GtcData.heat(sp) > 0) GtcData.setHeat(sp, GtcData.heat(sp) - 0.0045F);

        int stars = GtcData.stars(sp);
        if (stars > 0 && !sp.isCreative() && !sp.isSpectator()) {
            // evasion
            boolean inside = city && !lvl.canSeeSky(sp.blockPosition());
            if (inside) {
                if (++s.hide > 120) { s.hide = 0; GtcData.dropStar(sp); }
            } else {
                s.hide = 0;
                if (s.near) s.ev = 0;
                else if (++s.ev > 140) { s.ev = 0; GtcData.dropStar(sp); }
            }
            if (t % 20 == 0) {
                var cops = lvl.getEntitiesOfClass(DouanierEntity.class, sp.getBoundingBox().inflate(64));
                s.near = cops.stream().anyMatch(c -> c.distanceToSqr(sp) < 900);
                if (s.copCd > 0) s.copCd--;
                int want = GtcData.COPS_FOR[Math.min(6, stars)];
                if (Config.COPS_ENABLED.get() && cops.size() < want && s.copCd <= 0) spawnCop(lvl, sp, stars);
            }
        } else {
            s.ev = 0;
            s.hide = 0;
        }

        if (t % 20 == 0) {
            int carry = Util.carry(sp);
            if (carry >= 4) sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, Math.min(3, (carry - 4) / 4), false, false, true));
        }

        if (t % 10 == 0 && Util.carry(sp) > 0) {
            BlockPos b = sp.blockPosition();
            for (BlockPos q : BlockPos.betweenClosed(b.offset(-2, -1, -2), b.offset(2, 2, 2))) {
                if (lvl.getBlockState(q).is(ModRegistry.POINT_RELAIS.get())) {
                    GtcData.deliver(sp);
                    break;
                }
            }
        }

        if (t % 5 == 0) {
            for (ItemEntity ie : lvl.getEntitiesOfClass(ItemEntity.class, sp.getBoundingBox().inflate(1.5), i -> i.getItem().is(ModRegistry.PORTEFEUILLE.get()))) {
                ie.discard();
                GtcData.addCash(sp, 150);
                Util.msg(sp, "\u00a7aPortefeuille retrouve : +150 CASH.");
            }
        }

        if (t % 40 == 0 && (city || Config.PARCELS_EVERYWHERE.get()) && !sp.isSpectator()) spawnParcel(lvl, sp);

        // appels entrants
        if (Config.PHONE_CALLS.get()) {
            if (s.pending >= 0) {
                if (--s.pendT <= 0) {
                    s.pending = -1;
                    Util.bar(sp, "\u00a77Appel manque : " + "(personne)");
                }
            } else if (--s.callT <= 0) {
                s.callT = 1200 + R.nextInt(1200);
                s.pending = Calls.pick(sp);
                s.pendT = 360;
                Net.to(sp, new Net.Call(s.pending));
            }
        }

        // evenements aleatoires
        if (Config.RANDOM_EVENTS.get() && --s.evT <= 0) {
            s.evT = 800 + R.nextInt(1000);
            if (GtcData.stars(sp) == 0) randomEvent(lvl, sp);
        }
    }

    public static void answerCall(ServerPlayer p, int code, boolean accept) {
        PS s = ps(p);
        int idx = code % 100;
        if (s.pending != idx) return;
        s.pending = -1;
        if (accept) Calls.effect(p, idx);
        else Util.bar(p, "\u00a77Appel manque : " + Calls.NAMES[idx]);
    }

    private void spawnCop(ServerLevel l, ServerPlayer p, int stars) {
        for (int i = 0; i < 14; i++) {
            double a = R.nextDouble() * Math.PI * 2, d = 24 + R.nextDouble() * 10;
            BlockPos g = Util.ground(l, (int) (p.getX() + Math.cos(a) * d), (int) (p.getZ() + Math.sin(a) * d));
            if (g == null) continue;
            DouanierEntity c = ModRegistry.DOUANIER.get().spawn(l, g, MobSpawnType.EVENT);
            if (c != null) {
                c.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.27 + 0.02 * stars);
                c.setTarget(p);
            }
            return;
        }
    }

    private static BlockPos nearGround(ServerLevel l, ServerPlayer p, int min, int max) {
        for (int i = 0; i < 12; i++) {
            double a = R.nextDouble() * Math.PI * 2, d = min + R.nextDouble() * (max - min);
            BlockPos g = Util.ground(l, (int) (p.getX() + Math.cos(a) * d), (int) (p.getZ() + Math.sin(a) * d));
            if (g != null) return g;
        }
        return null;
    }

    private void dropItem(ServerLevel l, BlockPos g, ItemStack st, boolean glow) {
        ItemEntity ie = new ItemEntity(l, g.getX() + 0.5, g.getY() + 0.3, g.getZ() + 0.5, st);
        ie.setDeltaMovement(0, 0, 0);
        ie.setGlowingTag(glow);
        l.addFreshEntity(ie);
    }

    private void spawnParcel(ServerLevel l, ServerPlayer p) {
        int cnt = l.getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(50), i -> i.getItem().is(ModRegistry.COLIS.get())).size();
        if (cnt >= Config.PARCEL_CAP.get()) return;
        BlockPos g = nearGround(l, p, 8, 45);
        if (g != null) dropItem(l, g, new ItemStack(ModRegistry.COLIS.get()), true);
    }

    private void randomEvent(ServerLevel l, ServerPlayer p) {
        double r = R.nextDouble();
        if (r < 0.3) {
            BlockPos g = nearGround(l, p, 8, 20);
            if (g != null) {
                ItemEntity ie = new ItemEntity(l, g.getX() + 0.5, g.getY() + 0.3, g.getZ() + 0.5, new ItemStack(ModRegistry.PORTEFEUILLE.get()));
                ie.setNeverPickUp();
                ie.setGlowingTag(true);
                l.addFreshEntity(ie);
                Util.msg(p, "\u00a76Un portefeuille a ete perdu non loin d'ici (objet qui brille).");
            }
        } else if (r < 0.55) {
            BlockPos g = nearGround(l, p, 6, 16);
            if (g != null) {
                dropItem(l, g, new ItemStack(ModRegistry.COLIS.get(), 1), true);
                Util.msg(p, "\u00a7bUn colis est tombe d'un camion non loin d'ici.");
            }
        } else if (r < 0.8) {
            MutableComponent c = Component.literal("\u00a79[Douane] \u00a7fControle de routine. Vos papiers ? ");
            c.append(Component.literal("[Payer 100 CASH]").withStyle(st -> st.withColor(ChatFormatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/gtc payer"))));
            c.append(Component.literal(" "));
            c.append(Component.literal("[Fuir]").withStyle(st -> st.withColor(ChatFormatting.RED).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/gtc fuir"))));
            p.sendSystemMessage(c);
        } else if (Util.inCity(l)) {
            Util.msg(p, "\u00a77SMS Indic : du mouvement du cote de \u00a7e" + CityGen.name(R.nextInt(CityGen.COUNT)) + "\u00a77.");
        }
    }

    // ------------------------------------------------------------------ commandes
    @SubscribeEvent
    public void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("gtc")
                .then(Commands.literal("ville").requires(ServerEvents::admin)
                        .executes(c -> { CityGen.start(c.getSource().getPlayerOrException()); return 1; }))
                .then(Commands.literal("ohio").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    ServerLevel o = Util.ohio(p.server);
                    if (o == null) { Util.msg(p, "\u00a7cDimension introuvable."); return 0; }
                    if (!CityGen.isBuilt(o)) { Util.msg(p, "\u00a7eOhio City n'est pas encore construite : fais /gtc ville."); return 0; }
                    BlockPos s = CityGen.spawnPoint();
                    p.teleportTo(o, s.getX() + 0.5, s.getY(), s.getZ() + 0.5, 0F, 0F);
                    return 1;
                }))
                .then(Commands.literal("start").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Util.give(p, new ItemStack(ModRegistry.TELEPHONE.get()));
                    return 1;
                }))
                .then(Commands.literal("payer").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    if (GtcData.pay(p, 100)) Util.msg(p, "\u00a7aDouane : \u00ab Circulez. \u00bb (-100 CASH)");
                    else Util.msg(p, "\u00a7cPas assez de CASH.");
                    return 1;
                }))
                .then(Commands.literal("fuir").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    GtcData.setHeat(p, Math.max(GtcData.heat(p), GtcData.TH[1]));
                    GtcData.raise(p);
                    return 1;
                }))
                .then(Commands.literal("reset").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    MissionManager.forget(p);
                    GtcData.clearWanted(p);
                    GtcData.setCash(p, 0);
                    GtcData.setLives(p, 3);
                    Util.msg(p, "\u00a7eStats reinitialisees.");
                    return 1;
                }))
                .then(Commands.literal("cash").requires(ServerEvents::admin)
                        .then(Commands.argument("n", IntegerArgumentType.integer(-1000000, 1000000)).executes(c -> {
                            GtcData.addCash(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "n"));
                            return 1;
                        })))
                .then(Commands.literal("stars").requires(ServerEvents::admin)
                        .then(Commands.argument("n", IntegerArgumentType.integer(0, 6)).executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            int n = IntegerArgumentType.getInteger(c, "n");
                            GtcData.setStars(p, n);
                            GtcData.setHeat(p, n == 0 ? 0 : GtcData.TH[n - 1]);
                            return 1;
                        })))
                .then(Commands.literal("mission").requires(ServerEvents::admin)
                        .then(Commands.argument("kind", StringArgumentType.word()).executes(c -> {
                            MissionManager.start(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "kind"));
                            return 1;
                        }))));
    }

    private static boolean admin(CommandSourceStack s) {
        return s.hasPermission(2) || !s.getServer().isDedicatedServer();
    }
}
