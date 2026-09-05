package com.aicivilization.mod.network.packet;

import com.aicivilization.mod.gui.SpawnSetupScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * サーバーからクライアントへ「AI出現設定画面を開いてください」と指示するパケット。
 * コマンド /aicivilization spawn 実行時に、最新の脳プール同期とセットで送られる。
 */
public class OpenSpawnSetupPacket {

    public static void encode(OpenSpawnSetupPacket packet, FriendlyByteBuf buf) {
        // ペイロードなし
    }

    public static OpenSpawnSetupPacket decode(FriendlyByteBuf buf) {
        return new OpenSpawnSetupPacket();
    }

    public static void handle(OpenSpawnSetupPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(OpenSpawnSetupPacket::openScreenClientSide);
        ctx.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void openScreenClientSide() {
        Minecraft.getInstance().setScreen(new SpawnSetupScreen());
    }
}
