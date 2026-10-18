package fr.gtcolis.gtc;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Voiture conduisible (ZQSD : avancer/reculer, Q/D : tourner). Renverse les passants. */
public class VoitureEntity extends Mob {
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(VoitureEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BODY = SynchedEntityData.defineId(VoitureEntity.class, EntityDataSerializers.INT);
    private double lx, lz;

    public VoitureEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setMaxUpStep(1.1F);
    }

    public static AttributeSupplier.Builder attrs() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.MOVEMENT_SPEED, 0.40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(COLOR, 0);
        this.entityData.define(BODY, 1);
    }

    public int getColor() { return entityData.get(COLOR); }
    public void setColor(int c) { entityData.set(COLOR, Math.floorMod(c, 6)); }
    public int getBody() { return entityData.get(BODY); }
    public void setBody(int b) { entityData.set(BODY, Math.floorMod(b, 3)); }

    @Override
    protected void registerGoals() { }

    @Override
    public double getPassengersRidingOffset() {
        return 0.5D;
    }

    @Override
    protected boolean canAddPassenger(Entity e) {
        return getPassengers().isEmpty();
    }

    @Override
    public LivingEntity getControllingPassenger() {
        Entity e = getFirstPassenger();
        return e instanceof LivingEntity le ? le : null;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack st = player.getItemInHand(hand);
        if (st.is(Items.IRON_INGOT) && getHealth() < getMaxHealth()) {
            if (!level().isClientSide) {
                heal(15F);
                if (!player.getAbilities().instabuild) st.shrink(1);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (!level().isClientSide) player.startRiding(this);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void travel(Vec3 v) {
        LivingEntity p = getControllingPassenger();
        if (isAlive() && isVehicle() && p != null) {
            double sp = getDeltaMovement().horizontalDistance();
            float turn = p.xxa * Math.min(4.5F, 1.2F + (float) sp * 9F);
            setYRot(getYRot() - turn);
            setXRot(0F);
            this.yRotO = getYRot();
            this.yBodyRot = getYRot();
            this.yHeadRot = getYRot();
            float f = p.zza;
            if (f < 0) f *= 0.5F;
            setSpeed((float) getAttributeValue(Attributes.MOVEMENT_SPEED));
            super.travel(new Vec3(0.0D, v.y, f));
        } else {
            super.travel(new Vec3(0.0D, v.y, 0.0D));
        }
    }

    @Override
    public void tick() {
        super.tick();
        double speed = Math.sqrt((getX() - lx) * (getX() - lx) + (getZ() - lz) * (getZ() - lz));
        lx = getX();
        lz = getZ();
        LivingEntity driver = getControllingPassenger();
        if (!level().isClientSide && driver != null && speed > 0.33) {
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.35),
                    t -> t != this && t != driver && !(t instanceof VoitureEntity) && !t.isPassengerOfSameVehicle(this))) {
                DamageSource src = driver instanceof Player pl ? level().damageSources().playerAttack(pl) : level().damageSources().mobAttack(this);
                if (e.hurt(src, Math.min(14F, (float) speed * 28F))) {
                    Vec3 push = getDeltaMovement().scale(1.4D);
                    e.setDeltaMovement(e.getDeltaMovement().add(push.x, 0.35D, push.z));
                    e.hurtMarked = true;
                    if (driver instanceof ServerPlayer sp && (e instanceof ContactEntity || e instanceof MerchantEntity || e instanceof Player)) {
                        GtcData.addHeat(sp, 0.6F);
                    }
                }
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide && getHealth() < getMaxHealth() * 0.4F && random.nextInt(3) == 0) {
            level().addParticle(ParticleTypes.LARGE_SMOKE, getX() + (random.nextDouble() - 0.5), getY() + 1.1, getZ() + (random.nextDouble() - 0.5), 0, 0.03, 0);
        }
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putInt("gtcColor", getColor());
        t.putInt("gtcBody", getBody());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        setColor(t.getInt("gtcColor"));
        setBody(t.contains("gtcBody") ? t.getInt("gtcBody") : 1);
    }
}
