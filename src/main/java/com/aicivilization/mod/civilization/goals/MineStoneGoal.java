package com.aicivilization.mod.civilization.goals;

import com.aicivilization.mod.entity.AICitizenEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;
import java.util.Optional;

/**
 * AIが地表付近の石ブロックを見つけて採掘する行動。
 * GatherWoodGoalと同じ探索→接近→破壊→インベントリ追加のパターンを踏襲する。
 */
public class MineStoneGoal extends Goal {

    private static final int SEARCH_RADIUS = 6;
    private static final int BREAK_TICKS_REQUIRED = 40;

    private final AICitizenEntity citizen;
    private BlockPos targetStone;
    private int breakProgressTicks;

    public MineStoneGoal(AICitizenEntity citizen) {
        this.citizen = citizen;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (citizen.isAIChild()) {
            return false;
        }
        Optional<BlockPos> found = findNearbyStone();
        found.ifPresent(pos -> targetStone = pos);
        return found.isPresent();
    }

    @Override
    public boolean canContinueToUse() {
        return targetStone != null && isMinableStone(citizen.level().getBlockState(targetStone).getBlock());
    }

    @Override
    public void start() {
        breakProgressTicks = 0;
    }

    @Override
    public void stop() {
        targetStone = null;
        breakProgressTicks = 0;
    }

    @Override
    public void tick() {
        if (targetStone == null) {
            return;
        }

        double distSq = citizen.distanceToSqr(
                targetStone.getX() + 0.5, targetStone.getY() + 0.5, targetStone.getZ() + 0.5);

        if (distSq > 4.0) {
            citizen.getNavigation().moveTo(
                    targetStone.getX() + 0.5, targetStone.getY(), targetStone.getZ() + 0.5, 0.5);
            citizen.getLookControl().setLookAt(
                    targetStone.getX() + 0.5, targetStone.getY() + 0.5, targetStone.getZ() + 0.5);
            return;
        }

        citizen.getLookControl().setLookAt(
                targetStone.getX() + 0.5, targetStone.getY() + 0.5, targetStone.getZ() + 0.5);
        breakProgressTicks++;

        if (breakProgressTicks >= BREAK_TICKS_REQUIRED) {
            harvestStone();
        }
    }

    private void harvestStone() {
        Level level = citizen.level();
        if (level.isClientSide) {
            return;
        }
        BlockState state = level.getBlockState(targetStone);
        if (isMinableStone(state.getBlock())) {
            level.destroyBlock(targetStone, false);
            citizen.getInventory().addItem(new ItemStack(Items.COBBLESTONE, 1));
        }
        targetStone = null;
        breakProgressTicks = 0;
    }

    private Optional<BlockPos> findNearbyStone() {
        BlockPos origin = citizen.blockPosition();
        Level level = citizen.level();

        BlockPos closest = null;
        double closestDist = Double.MAX_VALUE;

        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dy = -3; dy <= 1; dy++) {
                for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (isMinableStone(level.getBlockState(pos).getBlock())) {
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

    private boolean isMinableStone(Block block) {
        return block == Blocks.STONE || block == Blocks.COBBLESTONE || block == Blocks.ANDESITE
                || block == Blocks.DIORITE || block == Blocks.GRANITE;
    }
}
