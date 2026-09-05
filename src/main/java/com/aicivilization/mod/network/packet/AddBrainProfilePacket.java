package com.aicivilization.mod.network.packet;

import com.aicivilization.mod.brain.BrainProfilePool;
import com.aicivilization.mod.brain.BrainProfilePoolProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * ゲーム内GUIから「新しい脳（APIプロファイル）を追加」した時にサーバーへ送るパケット。
 * config直接編集をせずに、GUIだけでAPIキー登録が完結するようにするための仕組み。
 */
public class AddBrainProfilePacket {

    private final String profileName;
    private final String apiKey;
    private final String modelName;

    public AddBrainProfilePacket(String profileName, String apiKey, String modelName) {
        this.profileName = profileName;
        this.apiKey = apiKey;
        this.modelName = modelName;
    }

    public static void encode(AddBrainProfilePacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.profileName);
        buf.writeUtf(packet.apiKey);
        buf.writeUtf(packet.modelName);
    }

    public static AddBrainProfilePacket decode(FriendlyByteBuf buf) {
        String name = buf.readUtf();
        String key = buf.readUtf();
        String model = buf.readUtf();
        return new AddBrainProfilePacket(name, key, model);
    }

    public static void handle(AddBrainProfilePacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }
            BrainProfilePool pool = BrainProfilePoolProvider.get(player.serverLevel());
            pool.addProfile(packet.profileName, packet.apiKey, packet.modelName);

            // 追加後、そのプレイヤーへ最新プールを同期して画面に反映させる
            SyncBrainPoolPacket.sendTo(player, pool);
        });
        ctx.setPacketHandled(true);
    }
}
