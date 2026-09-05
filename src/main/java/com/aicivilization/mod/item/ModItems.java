package com.aicivilization.mod.item;

import com.aicivilization.mod.AICivilizationMod;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * アイテム登録。
 * 「創造の杖」は右クリックでAI出現設定画面(SpawnSetupScreen)を開くための道具。
 * コマンドが苦手なプレイヤーでも直感的にAIを追加できるようにする。
 */
public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, AICivilizationMod.MOD_ID);

    public static final RegistryObject<Item> CREATION_WAND = ITEMS.register("creation_wand",
            () -> new CreationWandItem(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
