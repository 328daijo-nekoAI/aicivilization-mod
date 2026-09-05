package com.aicivilization.mod.item;

import com.aicivilization.mod.brain.BrainProfilePool;
import com.aicivilization.mod.brain.BrainProfilePoolProvider;
import com.aicivilization.mod.network.ModNetworking;
import com.aicivilization.mod.network.packet.OpenSpawnSetupPacket;
import com.aicivilization.mod.network.packet.SyncBrainPoolPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

/**
 * 右クリックでAI出現設定画面を開くアイテム。
 * サーバーサイドで最新の脳プールをそのプレイヤーへ同期してから、画面を開かせる。
 */
public class CreationWandItem extends Item {

    public CreationWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<net.minecraft.world.item.ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BrainProfilePool pool = BrainProfilePoolProvider.get(serverPlayer.serverLevel());
            SyncBrainPoolPacket.sendTo(serverPlayer, pool);
            ModNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new OpenSpawnSetupPacket());
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}
