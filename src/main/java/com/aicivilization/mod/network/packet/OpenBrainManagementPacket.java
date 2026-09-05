package com.aicivilization.mod.network.packet;

import com.aicivilization.mod.gui.BrainManagementScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** サーバーからクライアントへ「脳管理画面を開いてください」と指示するパケット。 */
public class OpenBrainManagementPacket {

    public static void encode(OpenBrainManagementPacket packet, FriendlyByteBuf buf) {
        // ペイロードなし
    }

    public static OpenBrainManagementPacket decode(FriendlyByteBuf buf) {
        return new OpenBrainManagementPacket();
    }

    public static void handle(OpenBrainManagementPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(OpenBrainManagementPacket::openScreenClientSide);
        ctx.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void openScreenClientSide() {
        Minecraft.getInstance().setScreen(new BrainManagementScreen());
    }
}
