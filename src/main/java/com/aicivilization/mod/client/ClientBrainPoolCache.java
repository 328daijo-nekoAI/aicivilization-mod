package com.aicivilization.mod.client;

import com.aicivilization.mod.network.packet.SyncBrainPoolPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * サーバーから同期された脳プールの情報をクライアント側で一時保持するキャッシュ。
 * GUI画面はここを参照して一覧を描画する。APIキーはマスク済みの文字列のみ保持し、
 * 生のキーはクライアントに送らない（表示上の安全のため）。
 */
@OnlyIn(Dist.CLIENT)
public final class ClientBrainPoolCache {

    private static List<SyncBrainPoolPacket.Entry> cachedEntries = new ArrayList<>();

    private ClientBrainPoolCache() {
    }

    public static void update(List<SyncBrainPoolPacket.Entry> entries) {
        cachedEntries = entries;
    }

    public static List<SyncBrainPoolPacket.Entry> getEntries() {
        return cachedEntries;
    }
}
