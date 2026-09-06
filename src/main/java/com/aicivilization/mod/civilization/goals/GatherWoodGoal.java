package com.aicivilization.mod.civilization.goals;

import com.aicivilization.mod.entity.AICitizenEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;
import java.util.Optional;

/**
 * AIが周辺の原木ブロックを見つけて伐採する行動（仕様6章・経済システムの資材確保）。
 * <p>
 * バニラのMinecraft AIが使う Goal システムを利用する。毎ティック呼ばれる
 * canUse/tick 等のメソッドで、探索→接近→破壊→インベントリ追加という
 * 一連の採取行動を段階的に実行する。
 */
public class GatherWoodGoal extends Goal {

    private static final int SEARCH_RADIUS = 8;
    private static final int BREAK_TICKS_REQUIRED = 30;

    private final AICitizenEntity citizen;
    private BlockPos targetLog;
    private int breakProgressTicks;

    public GatherWoodGoal(AICitizenEntity citizen) {
        this.citizen = citizen;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (citizen.isAIChild()) {
            return false;
        }
        Optional<BlockPos> found = findNearbyLog();
        found.ifPresent(pos -> targetLog = pos);
        return found.isPresent();
    }

    @Override
    public boolean canContinueToUse() {
        return targetLog != null && isLog(citizen.level().getBlockState(targetLog));
    }

    @Override
    public void start() {
        breakProgressTicks = 0;
    }

    @Override
    public void stop() {
        targetLog = null;
        breakProgressTicks = 0;
    }

    @Override
    public void tick() {
        if (targetLog == null) {
            return;
        }

        double distSq = citizen.distanceToSqr(
                targetLog.getX() + 0.5, targetLog.getY() + 0.5, targetLog.getZ() + 0.5);

        if (distSq > 4.0) {
            citizen.getNavigation().moveTo(
                    targetLog.getX() + 0.5, targetLog.getY(), targetLog.getZ() + 0.5, 0.5);
            citizen.getLookControl().setLookAt(
                    targetLog.getX() + 0.5, targetLog.getY() + 0.5, targetLog.getZ() + 0.5);
            return;
        }

        citizen.getLookControl().setLookAt(
                targetLog.getX() + 0.5, targetLog.getY() + 0.5, targetLog.getZ() + 0.5);
        breakProgressTicks++;

        if (breakProgressTicks >= BREAK_TICKS_REQUIRED) {
            harvestLog();
        }
    }

    private void harvestLog() {
        Level level = citizen.level();
        if (level.isClientSide) {
            return;
        }
        BlockState state = level.getBlockState(targetLog);
        if (isLog(state)) {
            level.destroyBlock(targetLog, false);
            citizen.getInventory().add(new ItemStack(Items.OAK_LOG, 1));
        }
        targetLog = null;
        breakProgressTicks = 0;
    }

    private Optional<BlockPos> findNearbyLog() {
        BlockPos origin = citizen.blockPosition();
        Level level = citizen.level();

        BlockPos closest = null;
        double closestDist = Double.MAX_VALUE;

        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (isLog(level.getBlockState(pos))) {
                        double dist = origin.distSqr(pos);
                        if (dist < closestDist) {
                            closestDist = dist;
                            closest = pos;
                        }
                    }
                }
            }
        }
        return Optional.ofNullable(closest);
    }

    private boolean isLog(BlockState state) {
        return state.is(BlockTags.LOGS);
    }
}
