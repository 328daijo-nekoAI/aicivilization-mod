package com.aicivilization.mod.network;

import com.aicivilization.mod.AICivilizationMod;
import com.aicivilization.mod.network.packet.AddBrainProfilePacket;
import com.aicivilization.mod.network.packet.OpenBrainManagementPacket;
import com.aicivilization.mod.network.packet.OpenSpawnSetupPacket;
import com.aicivilization.mod.network.packet.RemoveBrainProfilePacket;
import com.aicivilization.mod.network.packet.RequestSpawnAIPacket;
import com.aicivilization.mod.network.packet.SyncBrainPoolPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * クライアント(GUI)とサーバー(データ本体)の間でやり取りするパケットをまとめて登録する。
 * GUI操作（脳の追加/削除、AI出現リクエスト等）は全てここを経由してサーバーに送られ、
 * サーバー側の唯一のデータ（BrainProfilePool等）を更新する。
 */
public final class ModNetworking {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(AICivilizationMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private ModNetworking() {
    }

    public static void register() {
        int id = 0;

        CHANNEL.registerMessage(id++,
                AddBrainProfilePacket.class,
                AddBrainProfilePacket::encode,
                AddBrainProfilePacket::decode,
                AddBrainProfilePacket::handle);

        CHANNEL.registerMessage(id++,
                RemoveBrainProfilePacket.class,
                RemoveBrainProfilePacket::encode,
                RemoveBrainProfilePacket::decode,
                RemoveBrainProfilePacket::handle);

        CHANNEL.registerMessage(id++,
                RequestSpawnAIPacket.class,
                RequestSpawnAIPacket::encode,
                RequestSpawnAIPacket::decode,
                RequestSpawnAIPacket::handle);

        CHANNEL.registerMessage(id++,
                SyncBrainPoolPacket.class,
                SyncBrainPoolPacket::encode,
                SyncBrainPoolPacket::decode,
                SyncBrainPoolPacket::handle);

        CHANNEL.registerMessage(id++,
                OpenSpawnSetupPacket.class,
                OpenSpawnSetupPacket::encode,
                OpenSpawnSetupPacket::decode,
                OpenSpawnSetupPacket::handle);

        CHANNEL.registerMessage(id++,
                OpenBrainManagementPacket.class,
                OpenBrainManagementPacket::encode,
                OpenBrainManagementPacket::decode,
                OpenBrainManagementPacket::handle);
    }
}
