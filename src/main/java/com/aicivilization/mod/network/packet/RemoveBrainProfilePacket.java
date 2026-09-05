package com.aicivilization.mod.network.packet;

import com.aicivilization.mod.brain.BrainProfilePool;
import com.aicivilization.mod.brain.BrainProfilePoolProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** ゲーム内GUIから脳プロファイルを削除するリクエスト。 */
public class RemoveBrainProfilePacket {

    private final UUID profileId;

    public RemoveBrainProfilePacket(UUID profileId) {
        this.profileId = profileId;
    }

    public static void encode(RemoveBrainProfilePacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.profileId);
    }

    public static RemoveBrainProfilePacket decode(FriendlyByteBuf buf) {
        return new RemoveBrainProfilePacket(buf.readUUID());
    }

    public static void handle(RemoveBrainProfilePacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }
            BrainProfilePool pool = BrainProfilePoolProvider.get(player.serverLevel());
            pool.removeProfile(packet.profileId);
            SyncBrainPoolPacket.sendTo(player, pool);
        });
        ctx.setPacketHandled(true);
    }
}
