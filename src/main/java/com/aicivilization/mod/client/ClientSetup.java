package com.aicivilization.mod.client;

import com.aicivilization.mod.entity.ModEntities;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;

/**
 * クライアント側限定の初期化処理。
 * <p>
 * AICitizenEntity はバニラの Villager を継承しているが、Forge/Minecraft は
 * エンティティタイプごとに描画方法（EntityRenderer）を明示的に登録しないと
 * 描画時に NullPointerException でクラッシュする。
 * ここでバニラの村人と同じ見た目（VillagerRenderer）を割り当てる。
 * <p>
 * @Mod.EventBusSubscriber によるアノテーション自動登録が確実に効かない
 * 環境があったため、Modのコンストラクタから明示的にイベントバスへ
 * 登録する方式に切り替えている（AICivilizationModから呼び出す）。
 */
public final class ClientSetup {

    private ClientSetup() {
    }

    /** Modのコンストラクタから呼び出す。クライアント環境でのみ実際に登録される。 */
    public static void register(IEventBus modEventBus) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                modEventBus.addListener(ClientSetup::onRegisterRenderers));
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.AI_CITIZEN.get(), VillagerRenderer::new);
    }
}
