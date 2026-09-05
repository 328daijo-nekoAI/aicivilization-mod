package com.aicivilization.mod.network.packet;

import com.aicivilization.mod.brain.BrainProfile;
import com.aicivilization.mod.brain.BrainProfilePool;
import com.aicivilization.mod.client.ClientBrainPoolCache;
import com.aicivilization.mod.network.ModNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * サーバー側の脳プール（真のデータ）を、GUIを開いているクライアントへ同期する。
 * クライアント側はこれを受け取ってキャッシュし、GUI描画に使う。
 */
public class SyncBrainPoolPacket {

    public record Entry(UUID profileId, String profileName, String maskedApiKey,
                         String modelName, boolean assigned) {
    }

    private final List<Entry> entries;

    public SyncBrainPoolPacket(List<Entry> entries) {
        this.entries = entries;
    }

    public static void sendTo(ServerPlayer player, BrainProfilePool pool) {
        List<Entry> entries = new ArrayList<>();
        for (BrainProfile p : pool.getAllProfiles()) {
            entries.add(new Entry(p.getProfileId(), p.getProfileName(), p.getMaskedApiKey(),
                    p.getModelName(), p.isAssigned()));
        }
        ModNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncBrainPoolPacket(entries));
    }

    public static void encode(SyncBrainPoolPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entries.size());
        for (Entry e : packet.entries) {
            buf.writeUUID(e.profileId());
            buf.writeUtf(e.profileName());
            buf.writeUtf(e.maskedApiKey());
            buf.writeUtf(e.modelName());
            buf.writeBoolean(e.assigned());
        }
    }

    public static SyncBrainPoolPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            UUID id = buf.readUUID();
            String name = buf.readUtf();
            String maskedKey = buf.readUtf();
            String model = buf.readUtf();
            boolean assigned = buf.readBoolean();
            entries.add(new Entry(id, name, maskedKey, model, assigned));
        }
        return new SyncBrainPoolPacket(entries);
    }

    public static void handle(SyncBrainPoolPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> handleClientSide(packet));
        ctx.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClientSide(SyncBrainPoolPacket packet) {
        ClientBrainPoolCache.update(packet.entries);
    }
}
