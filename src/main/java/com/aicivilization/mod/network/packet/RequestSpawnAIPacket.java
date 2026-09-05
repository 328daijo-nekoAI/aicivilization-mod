package com.aicivilization.mod.network.packet;

import com.aicivilization.mod.AICivilizationMod;
import com.aicivilization.mod.brain.BrainProfilePool;
import com.aicivilization.mod.brain.BrainProfilePoolProvider;
import com.aicivilization.mod.civilization.AISpawner;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * ワールド参加時 or 追加スポーン時に、GUIで選んだ脳プロファイル群を使って
 * AIエンティティを実際にスポーンさせるリクエスト。
 */
public class RequestSpawnAIPacket {

    private final List<UUID> selectedProfileIds;

    public RequestSpawnAIPacket(List<UUID> selectedProfileIds) {
        this.selectedProfileIds = selectedProfileIds;
    }

    public static void encode(RequestSpawnAIPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.selectedProfileIds.size());
        for (UUID id : packet.selectedProfileIds) {
            buf.writeUUID(id);
        }
    }

    public static RequestSpawnAIPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            ids.add(buf.readUUID());
        }
        return new RequestSpawnAIPacket(ids);
    }

    public static void handle(RequestSpawnAIPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }

            BrainProfilePool pool = BrainProfilePoolProvider.get(player.serverLevel());

            if (!pool.hasValidBrainCount(packet.selectedProfileIds.size())) {
                player.sendSystemMessage(Component.literal(
                        "§c脳の数が不正です。1〜" + BrainProfilePool.getMaxProfilesPerEntity() + "個の範囲で選択してください。"));
                return;
            }

            String result = AISpawner.spawnNewAI(player.serverLevel(), player.blockPosition(),
                    pool, packet.selectedProfileIds);

            if (result != null) {
                player.sendSystemMessage(Component.literal("§c" + result));
            } else {
                AICivilizationMod.LOGGER.info("[AICivilization] プレイヤー {} が新しいAIをスポーンさせました。",
                        player.getName().getString());
                SyncBrainPoolPacket.sendTo(player, pool);
            }
        });
        ctx.setPacketHandled(true);
    }
}
