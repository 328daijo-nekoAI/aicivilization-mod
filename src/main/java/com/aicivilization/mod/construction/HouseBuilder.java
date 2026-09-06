package com.aicivilization.mod.construction;

import com.aicivilization.mod.entity.AICitizenEntity;
import com.aicivilization.mod.memory.CivilizationLog;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Random;

/**
 * AIが「家を持っていない」と判断した場合に、自律的に簡易な家を建てる処理（仕様6章）。
 * <p>
 * サバイバル的な仕様: AIは実際に自分のインベントリにある材料（丸太・板材等）
 * だけを使って家を建てる。材料を無から生成することはなく、
 * 必要量が足りない場合は建築を行わない（＝資源採取Goalが集めるのを待つ）。
 */
public final class HouseBuilder {

    private static final Random RANDOM = new Random();

    private static final int[] SIZE_OPTIONS = {3, 5};
    private static final int WALL_HEIGHT = 3;
    private static final int PLANKS_PER_LOG = 4;

    private HouseBuilder() {
    }

    public static BlockPos tryBuildHouse(ServerLevel level, AICitizenEntity citizen) {
        int availablePlanks = countAvailablePlanks(citizen);

        int chosenSize = -1;
        for (int i = SIZE_OPTIONS.length - 1; i >= 0; i--) {
            if (availablePlanks >= requiredPlanksFor(SIZE_OPTIONS[i])) {
                chosenSize = SIZE_OPTIONS[i];
                break;
            }
        }

        if (chosenSize < 0) {
            return null;
        }

        BlockPos origin = findBuildableSpot(level, citizen.blockPosition(), chosenSize);
        if (origin == null) {
            return null;
        }

        int planksNeeded = requiredPlanksFor(chosenSize);
        if (!consumePlanks(citizen, planksNeeded)) {
            return null;
        }

        buildSimpleHouse(level, origin, chosenSize);
        citizen.getResidenceData().setHome(origin);

        CivilizationLog.record(level, "construction",
                citizen.getCitizenName() + " が持っている材料（丸太換算" + planksNeeded / PLANKS_PER_LOG
                        + "個相当）を使って家を建てました。(" + origin.getX() + ", "
                        + origin.getY() + ", " + origin.getZ() + ")");

        return origin;
    }

    private static int requiredPlanksFor(int size) {
        int floor = size * size;
        int wallPerimeter = (size - 1) * 4;
        int wall = wallPerimeter * WALL_HEIGHT;
        int roof = size * size;
        return floor + wall + roof;
    }

    private static int countAvailablePlanks(AICitizenEntity citizen) {
        SimpleContainer inventory = citizen.getInventory();
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            Item item = stack.getItem();
            if (isLogItem(item)) {
                total += stack.getCount() * PLANKS_PER_LOG;
            } else if (isPlankItem(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static boolean consumePlanks(AICitizenEntity citizen, int planksNeeded) {
        SimpleContainer inventory = citizen.getInventory();
        int remaining = planksNeeded;

        for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !isPlankItem(stack.getItem())) {
                continue;
            }
            int take = Math.min(stack.getCount(), remaining);
            stack.shrink(take);
            remaining -= take;
        }

        for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !isLogItem(stack.getItem())) {
                continue;
            }
            int logsNeeded = (int) Math.ceil(remaining / (double) PLANKS_PER_LOG);
            int take = Math.min(stack.getCount(), logsNeeded);
            stack.shrink(take);
            remaining -= take * PLANKS_PER_LOG;
        }

        return remaining <= 0;
    }

    private static boolean isLogItem(Item item) {
        return item == Items.OAK_LOG || item == Items.SPRUCE_LOG || item == Items.BIRCH_LOG
                || item == Items.JUNGLE_LOG || item == Items.ACACIA_LOG || item == Items.DARK_OAK_LOG;
    }

    private static boolean isPlankItem(Item item) {
        return item == Items.OAK_PLANKS || item == Items.SPRUCE_PLANKS || item == Items.BIRCH_PLANKS
                || item == Items.JUNGLE_PLANKS || item == Items.ACACIA_PLANKS || item == Items.DARK_OAK_PLANKS;
    }

    private static BlockPos findBuildableSpot(ServerLevel level, BlockPos center, int size) {
        for (int attempt = 0; attempt < 8; attempt++) {
            int dx = RANDOM.nextInt(21) - 10;
            int dz = RANDOM.nextInt(21) - 10;
            BlockPos candidate = center.offset(dx, 0, dz);
            BlockPos ground = findGroundLevel(level, candidate);
            if (ground != null && isAreaClear(level, ground, size)) {
                return ground;
            }
        }
        return null;
    }

    private static BlockPos findGroundLevel(ServerLevel level, BlockPos columnPos) {
        int topY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                columnPos.getX(), columnPos.getZ());
        BlockPos ground = new BlockPos(columnPos.getX(), topY, columnPos.getZ());
        BlockState state = level.getBlockState(ground.below());
        if (state.isAir()) {
            return null;
        }
        return ground;
    }

    private static boolean isAreaClear(ServerLevel level, BlockPos origin, int size) {
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                for (int y = 0; y < WALL_HEIGHT + 1; y++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (!level.getBlockState(pos).isAir() && !level.getBlockState(pos).getFluidState().isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static void buildSimpleHouse(ServerLevel level, BlockPos origin, int size) {
        BlockState wall = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState floor = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState roof = Blocks.OAK_SLAB.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState door = Blocks.OAK_DOOR.defaultBlockState();

        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                level.setBlockAndUpdate(origin.offset(x, 0, z), floor);
            }
        }

        for (int y = 1; y <= WALL_HEIGHT; y++) {
            for (int x = 0; x < size; x++) {
                for (int z = 0; z < size; z++) {
                    boolean isEdge = x == 0 || x == size - 1 || z == 0 || z == size - 1;
                    if (isEdge) {
                        level.setBlockAndUpdate(origin.offset(x, y, z), wall);
                    } else if (y != 1) {
                        level.setBlockAndUpdate(origin.offset(x, y, z), air);
                    }
                }
            }
        }

        int doorX = size / 2;
        level.setBlockAndUpdate(origin.offset(doorX, 1, 0), door);
        level.setBlockAndUpdate(origin.offset(doorX, 2, 0), air);

        if (size >= 5) {
            level.setBlockAndUpdate(origin.offset(0, 2, size / 2), air);
            level.setBlockAndUpdate(origin.offset(size - 1, 2, size / 2), air);
        }

        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                level.setBlockAndUpdate(origin.offset(x, WALL_HEIGHT + 1, z), roof);
            }
        }

        if (size >= 3) {
            level.setBlockAndUpdate(origin.offset(size / 2, 1, size / 2), Blocks.RED_BED.defaultBlockState());
        }
    }
}
