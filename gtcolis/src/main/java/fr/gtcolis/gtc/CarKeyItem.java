package fr.gtcolis.gtc;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

import java.util.UUID;

/** Cle de voiture : appelle (ou fait apparaitre) ta voiture sur le bloc vise. */
public class CarKeyItem extends Item {
    public CarKeyItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getLevel() instanceof ServerLevel sl && ctx.getPlayer() instanceof ServerPlayer sp) {
            BlockPos at = ctx.getClickedPos().relative(ctx.getClickedFace());
            ItemStack key = ctx.getItemInHand();
            VoitureEntity car = null;
            if (key.hasTag() && key.getTag().hasUUID("car")) {
                Entity e = sl.getEntity(key.getTag().getUUID("car"));
                if (e instanceof VoitureEntity v && v.isAlive()) car = v;
            }
            if (car != null) {
                car.stopRiding();
                car.ejectPassengers();
                car.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, sp.getYRot(), 0F);
                Util.bar(sp, "\u00a7aTa voiture arrive");
            } else {
                car = ModRegistry.VOITURE.get().create(sl);
                if (car == null) return InteractionResult.FAIL;
                car.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, sp.getYRot(), 0F);
                sl.addFreshEntity(car);
                key.getOrCreateTag().putUUID("car", car.getUUID());
                Util.bar(sp, "\u00a7aVoiture sortie. Clic droit pour monter, ZQSD pour conduire.");
            }
        }
        return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
    }
}
