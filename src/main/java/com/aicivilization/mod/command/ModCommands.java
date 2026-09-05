package com.aicivilization.mod.command;

import com.aicivilization.mod.brain.BrainProfilePool;
import com.aicivilization.mod.brain.BrainProfilePoolProvider;
import com.aicivilization.mod.network.ModNetworking;
import com.aicivilization.mod.network.packet.OpenSpawnSetupPacket;
import com.aicivilization.mod.network.packet.SyncBrainPoolPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

/**
 * /aicivilization コマンド群。
 * <p>
 * /aicivilization spawn  … AI出現設定画面を開く（脳の管理もここから遷移できる）
 * /aicivilization brains … 脳（APIプロファイル）管理画面を開く
 * /aicivilization log    … 直近の文明ログをチャットに表示する
 */
public final class ModCommands {

    private ModCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("aicivilization")
                .then(Commands.literal("spawn").executes(ModCommands::openSpawnSetup))
                .then(Commands.literal("brains").executes(ModCommands::openBrainManagement))
                .then(Commands.literal("log").executes(ModCommands::showRecentLog))
        );
    }

    private static int openSpawnSetup(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        BrainProfilePool pool = BrainProfilePoolProvider.get(player.serverLevel());
        SyncBrainPoolPacket.sendTo(player, pool);
        ModNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenSpawnSetupPacket());
        return 1;
    }

    private static int openBrainManagement(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        BrainProfilePool pool = BrainProfilePoolProvider.get(player.serverLevel());
        SyncBrainPoolPacket.sendTo(player, pool);
        // 脳管理単体画面を開く指示は spawn 画面経由で「脳を管理する」ボタンからも遷移できるが、
        // コマンドから直接開けるよう専用パケットも用意する。
        ModNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new com.aicivilization.mod.network.packet.OpenBrainManagementPacket());
        return 1;
    }

    private static int showRecentLog(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        var entries = com.aicivilization.mod.memory.CivilizationLog.readRecent(player.serverLevel(), 15);
        if (entries.isEmpty()) {
            player.sendSystemMessage(Component.literal("§7まだ文明の出来事は記録されていません。"));
            return 1;
        }
        player.sendSystemMessage(Component.literal("§6=== 直近の文明の出来事 ==="));
        for (var entry : entries) {
            player.sendSystemMessage(Component.literal("§7[" + entry.category() + "] §f" + entry.message()));
        }
        return 1;
    }
}
