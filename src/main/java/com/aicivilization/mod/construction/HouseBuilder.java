package com.aicivilization.mod.construction;

import com.aicivilization.mod.entity.AICitizenEntity;
import com.aicivilization.mod.memory.CivilizationLog;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Random;

/**
 * AIが「家を持っていない」と判断した場合に、自律的に簡易な家を建てる処理（仕様6章）。
 * <p>
 * フェーズ1〜4の実装では、Groqに詳細な間取りを設計させる代わりに、
 * 周辺の空きスペースを探して固定パターンの小屋をブロック配置コマンド的に組み立てる。
 * AIの思考結果テキストに「家を建てたい」等の意図が含まれた場合にこれを呼び出す
 * トリガー判定は CivilizationTickHandler / ActionInterpreter 側が行う。
 */
public final class HouseBuilder {

    private static final Random RANDOM = new Random();
    private static final int HOUSE_WIDTH = 5;
    private static final int HOUSE_DEPTH = 5;
    private static final int HOUSE_HEIGHT = 4;

    private HouseBuilder() {
    }

    /**
     * citizenの現在位置周辺に空きスペースを探し、簡易な家を建てる。
     *
     * @return 成功時は家の基準座標、空きスペースが見つからない場合はnull
     */
    public static BlockPos tryBuildHouse(ServerLevel level, AICitizenEntity citizen) {
        BlockPos origin = findBuildableSpot(level, citizen.blockPosition());
        if (origin == null) {
            return null;
        }

        buildSimpleHouse(level, origin);
        citizen.getResidenceData().setHome(origin);

        CivilizationLog.record(level, "construction",
                citizen.getCitizenName() + " が新しい家を建てました。(" + origin.getX() + ", "
                        + origin.getY() + ", " + origin.getZ() + ")");

        return origin;
    }

    /** citizen周辺のランダムな方向を何箇所か試し、平坦で空いている場所を探す。 */
    private static BlockPos findBuildableSpot(ServerLevel level, BlockPos center) {
        for (int attempt = 0; attempt < 8; attempt++) {
            int dx = RANDOM.nextInt(21) - 10; // -10 ~ +10
            int dz = RANDOM.nextInt(21) - 10;
            BlockPos candidate = center.offset(dx, 0, dz);
            BlockPos ground = findGroundLevel(level, candidate);
            if (ground != null && isAreaClear(level, ground)) {
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
            return null; // 地面が不安定（水上や崖など）は避ける
        }
        return ground;
    }

    private static boolean isAreaClear(ServerLevel level, BlockPos origin) {
        for (int x = 0; x < HOUSE_WIDTH; x++) {
            for (int z = 0; z < HOUSE_DEPTH; z++) {
                for (int y = 0; y < HOUSE_HEIGHT; y++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (!level.getBlockState(pos).isAir() && !level.getBlockState(pos).getFluidState().isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * 固定パターンの簡易な家を組み立てる。
     * 木材の壁・床・屋根・ドア・窓穴のシンプルな1LDK的構造。
     */
    private static void buildSimpleHouse(ServerLevel level, BlockPos origin) {
        BlockState wall = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState floor = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState roof = Blocks.OAK_SLAB.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState door = Blocks.OAK_DOOR.defaultBlockState();

        // 床
        for (int x = 0; x < HOUSE_WIDTH; x++) {
            for (int z = 0; z < HOUSE_DEPTH; z++) {
                level.setBlockAndUpdate(origin.offset(x, 0, z), floor);
            }
        }

        // 壁（外周のみ、高さ1〜3）
        for (int y = 1; y <= 3; y++) {
            for (int x = 0; x < HOUSE_WIDTH; x++) {
                for (int z = 0; z < HOUSE_DEPTH; z++) {
                    boolean isEdge = x == 0 || x == HOUSE_WIDTH - 1 || z == 0 || z == HOUSE_DEPTH - 1;
                    if (isEdge) {
                        level.setBlockAndUpdate(origin.offset(x, y, z), wall);
                    } else if (y != 1) {
                        level.setBlockAndUpdate(origin.offset(x, y, z), air); // 内部は空洞
                    }
                }
            }
        }

        // ドア（正面中央）
        int doorX = HOUSE_WIDTH / 2;
        level.setBlockAndUpdate(origin.offset(doorX, 1, 0), door);
        level.setBlockAndUpdate(origin.offset(doorX, 2, 0), air);

        // 窓穴（左右の壁に1つずつ）
        level.setBlockAndUpdate(origin.offset(0, 2, HOUSE_DEPTH / 2), air);
        level.setBlockAndUpdate(origin.offset(HOUSE_WIDTH - 1, 2, HOUSE_DEPTH / 2), air);

        // 屋根
        for (int x = 0; x < HOUSE_WIDTH; x++) {
            for (int z = 0; z < HOUSE_DEPTH; z++) {
                level.setBlockAndUpdate(origin.offset(x, 4, z), roof);
            }
        }

        // ベッド（内部中央付近、簡易的に配置。向き調整はせず単一ブロックのみ設置）
        level.setBlockAndUpdate(origin.offset(2, 1, 2), Blocks.RED_BED.defaultBlockState());
    }
}
