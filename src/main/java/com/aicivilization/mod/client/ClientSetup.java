package com.aicivilization.mod.client;

import com.aicivilization.mod.entity.ModEntities;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * クライアント側限定の初期化処理。
 * <p>
 * AICitizenEntity はバニラの Villager を継承しているが、Forge/Minecraft は
 * エンティティタイプごとに描画方法（EntityRenderer）を明示的に登録しないと
 * 描画時に NullPointerException でクラッシュする。
 * ここでバニラの村人と同じ見た目（VillagerRenderer）を割り当てる。
 */
@Mod.EventBusSubscriber(modid = com.aicivilization.mod.AICivilizationMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class ClientSetup {

    private ClientSetup() {
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.AI_CITIZEN.get(), VillagerRenderer::new);
    }
}
