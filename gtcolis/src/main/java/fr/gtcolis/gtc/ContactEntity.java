package fr.gtcolis.gtc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * PNJ generique. role : 0 = contact (missions, point d'exclamation), 1 = VIP (escorte),
 * 2 = cible (filature), 3 = danseur / decor.
 */
public class ContactEntity extends PathfinderMob {
    public int role = 0;
    public int cd = 0;
    public UUID owner;

    public ContactEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        applyRole();
    }

    public static AttributeSupplier.Builder attrs() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    public void setRole(int r) {
        role = r;
        applyRole();
    }

    private void applyRole() {
        if (role == 0) {
            setPersistenceRequired();
            setCustomName(Component.literal("\u00a7e\u2757 \u00a7fContact"));
            setCustomNameVisible(true);
        }
    }

    /** true = marque "etoile" (mission en cours), false = point d'exclamation. */
    public void setMark(boolean star) {
        if (role == 0) {
            setCustomName(Component.literal(star ? "\u00a76\u2605 \u00a7fContact" : "\u00a7e\u2757 \u00a7fContact"));
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new Goal() {
            @Override
            public boolean canUse() {
                return role == 1 && owner != null && getOwnerPlayer() != null && distanceToSqr(getOwnerPlayer()) > 25;
            }

            @Override
            public boolean canContinueToUse() {
                return role == 1 && getOwnerPlayer() != null && distanceToSqr(getOwnerPlayer()) > 9 && !getNavigation().isDone();
            }

            @Override
            public void tick() {
                Player o = getOwnerPlayer();
                if (o != null) getNavigation().moveTo(o, 1.3D);
            }

            @Override
            public void start() {
                Player o = getOwnerPlayer();
                if (o != null) getNavigation().moveTo(o, 1.3D);
            }
        });
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9D) {
            @Override
            public boolean canUse() {
                return (role == 0 || role == 2) && super.canUse();
            }
        });
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F) {
            @Override
            public boolean canUse() {
                return role != 2 && super.canUse();
            }
        });
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    private Player getOwnerPlayer() {
        return owner == null ? null : level().getPlayerByUUID(owner);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (cd > 0) cd--;
        if (role == 3 && !level().isClientSide && tickCount % 24 == 0 && onGround()) jumpFromGround();
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide && player instanceof ServerPlayer sp && role == 0) {
            MissionManager.talk(sp, this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return role != 0;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putInt("gtcRole", role);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        role = t.getInt("gtcRole");
        applyRole();
    }
}
