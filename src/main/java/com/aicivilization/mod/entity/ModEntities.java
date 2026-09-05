package com.aicivilization.mod.entity;

import com.aicivilization.mod.AICivilizationMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AICivilizationMod.MOD_ID);

    public static final RegistryObject<EntityType<AICitizenEntity>> AI_CITIZEN =
            ENTITY_TYPES.register("ai_citizen", () -> EntityType.Builder.of(AICitizenEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(10)
                    .build(AICivilizationMod.MOD_ID + ":ai_citizen"));

    private ModEntities() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
