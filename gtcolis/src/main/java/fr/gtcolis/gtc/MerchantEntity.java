package fr.gtcolis.gtc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

/** Commercant / PNJ a menu (armurerie, concessionnaire, casino, garage, friperie, indic, flic corrompu). */
public class MerchantEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> SHOP = SynchedEntityData.defineId(MerchantEntity.class, EntityDataSerializers.STRING);

    public MerchantEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setInvulnerable(true);
    }

    public static AttributeSupplier.Builder attrs() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SHOP, "armurier");
    }

    public String getShop() {
        return this.entityData.get(SHOP);
    }

    public void setShop(String s) {
        this.entityData.set(SHOP, s);
        Shops.Shop sh = Shops.ALL.get(s);
        if (sh != null) {
            setCustomName(Component.literal("\u00a7a$ \u00a7f" + sh.title()));
            setCustomNameVisible(true);
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (src.getEntity() instanceof ServerPlayer sp) {
            GtcData.addHeat(sp, 0.5F);
        }
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide && player instanceof ServerPlayer sp) {
            Net.to(sp, new Net.OpenShop(getShop()));
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putString("gtcShop", getShop());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        if (t.contains("gtcShop")) setShop(t.getString("gtcShop"));
    }
}
