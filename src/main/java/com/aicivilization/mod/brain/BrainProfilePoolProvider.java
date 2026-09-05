package com.aicivilization.mod.brain;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.DimensionDataStorage;

/**
 * BrainProfilePool（脳プール）をワールドセーブから取得するためのヘルパー。
 * 常にオーバーワールドのデータストレージに紐付けて、ワールド全体で1つのプールを共有する。
 */
public final class BrainProfilePoolProvider {

    private BrainProfilePoolProvider() {
    }

    public static BrainProfilePool get(ServerLevel overworld) {
        DimensionDataStorage storage = overworld.getDataStorage();
        return storage.computeIfAbsent(BrainProfilePool::load, BrainProfilePool::new, BrainProfilePool.getDataName());
    }
}
