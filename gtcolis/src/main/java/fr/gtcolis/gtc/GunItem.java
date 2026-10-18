package fr.gtcolis.gtc;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Arme a feu a tir instantane (consomme des Munitions). */
public class GunItem extends Item {
    private final float damage;
    private final int range, cooldown, pellets;
    private final float spread, pitch;
    private final boolean auto;

    public GunItem(Properties p, float damage, int range, int cooldown, int pellets, float spread, boolean auto, float pitch) {
        super(p);
        this.damage = damage; this.range = range; this.cooldown = cooldown; this.pellets = pellets;
        this.spread = spread; this.auto = auto; this.pitch = pitch;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand hand) {
        ItemStack st = p.getItemInHand(hand);
        if (!level.isClientSide) fire(level, p);
        if (auto) {
            p.startUsingItem(hand);
            return InteractionResultHolder.consume(st);
        }
        return InteractionResultHolder.sidedSuccess(st, level.isClientSide);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return auto ? 72000 : 0;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity e, ItemStack stack, int remaining) {
        if (!level.isClientSide && e instanceof Player p) fire(level, p);
    }

    private void fire(Level level, Player p) {
        if (p.getVehicle() instanceof VoitureEntity) return;
        if (p.getCooldowns().isOnCooldown(this)) return;
        if (!p.getAbilities().instabuild) {
            if (Util.ammo(p) <= 0) {
                Util.bar((net.minecraft.server.level.ServerPlayer) p, "\u00a7cPlus de munitions ! Va a l'armurerie.");
                p.getCooldowns().addCooldown(this, 20);
                return;
            }
            p.getInventory().clearOrCountMatchingItems(s -> s.is(ModRegistry.MUNITIONS.get()), 1, p.inventoryMenu.getCraftSlots());
        }
        p.getCooldowns().addCooldown(this, cooldown);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.0F, pitch);
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        for (int i = 0; i < pellets; i++) {
            Vec3 dir = look.add((level.random.nextDouble() - 0.5) * spread * 2, (level.random.nextDouble() - 0.5) * spread * 2, (level.random.nextDouble() - 0.5) * spread * 2).normalize();
            Vec3 end = eye.add(dir.scale(range));
            BlockHitResult bhr = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
            Vec3 stop = bhr.getType() == HitResult.Type.MISS ? end : bhr.getLocation();
            AABB box = new AABB(eye, stop).inflate(1.0);
            EntityHitResult ehr = ProjectileUtil.getEntityHitResult(level, p, eye, stop, box,
                    t -> !t.isSpectator() && t.isPickable() && t != p && !t.isPassengerOfSameVehicle(p));
            if (ehr != null) {
                Entity t = ehr.getEntity();
                t.hurt(level.damageSources().playerAttack(p), damage);
                stop = ehr.getLocation();
            }
            if (level instanceof ServerLevel sl) {
                double len = stop.distanceTo(eye);
                for (double d = 1.5; d < len; d += 2.0) {
                    Vec3 pt = eye.add(dir.scale(d));
                    sl.sendParticles(ParticleTypes.CRIT, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
                }
            }
        }
    }
}
