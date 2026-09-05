package com.aicivilization.mod;

import com.aicivilization.mod.command.ModCommands;
import com.aicivilization.mod.entity.ModEntities;
import com.aicivilization.mod.item.ModItems;
import com.aicivilization.mod.network.ModNetworking;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * AI Civilization Mod のエントリポイント。
 * <p>
 * このMod単体で以下を提供する:
 * <ul>
 *     <li>Groq APIキーを「脳（Brain Profile）」として複数管理する仕組み</li>
 *     <li>AIエンティティへの脳の割り当て・検証</li>
 *     <li>ワールド参加時のAI出現GUI</li>
 *     <li>文明シミュレーション（結婚・離婚・出産・建築・経済・政治・宗教・戦争）</li>
 *     <li>ローカルHTML管理ダッシュボード</li>
 * </ul>
 * 導入は jar ファイルを mods フォルダに入れるだけで完結する。
 */
@Mod(AICivilizationMod.MOD_ID)
public class AICivilizationMod {

    public static final String MOD_ID = "aicivilization";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public AICivilizationMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModEntities.register(modEventBus);
        ModItems.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerAttributes);

        // Forgeイベントバス側のハンドラ（ワールドロード・GUI表示・ティック処理等）
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("[AICivilization] Mod初期化を開始します。");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModNetworking.register();
            LOGGER.info("[AICivilization] ネットワークチャンネルを登録しました。");
        });
        LOGGER.info("[AICivilization] 共通セットアップが完了しました。");
    }

    private void registerAttributes(final EntityAttributeCreationEvent event) {
        event.put(ModEntities.AI_CITIZEN.get(), com.aicivilization.mod.entity.AICitizenEntity.createAttributes().build());
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public void onRegisterCommands(final RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }
}
