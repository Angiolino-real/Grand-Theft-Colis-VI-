package fr.gtcolis.gtc;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Point Relais : livre tous les colis portes. */
public class PointRelaisBlock extends Block {
    public PointRelaisBlock(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            if (!GtcData.deliver(sp)) Util.bar(sp, "\u00a7eTu n'as aucun colis a livrer. Ramasse les colis dans la rue !");
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
