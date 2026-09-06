package com.aicivilization.mod.entity;

import com.aicivilization.mod.civilization.goals.GatherWoodGoal;
import com.aicivilization.mod.civilization.goals.MineStoneGoal;
import com.aicivilization.mod.construction.ResidenceData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AICitizenEntity extends Villager {

    private List<UUID> brainProfileIds = new ArrayList<>();

    private String citizenName = "";
    private boolean isChild = false;
    private int ageTicks = 0;

    private float happiness = 50f;
    private UUID partnerId = null;
    private UUID motherId = null;
    private UUID fatherId = null;
    private final ResidenceData residenceData = new ResidenceData();

    public ResidenceData getResidenceData() {
        return residenceData;
    }

    public AICitizenEntity(EntityType<? extends Villager> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(6, new GatherWoodGoal(this));
        this.goalSelector.addGoal(7, new MineStoneGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Villager.createAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    public List<UUID> getBrainProfileIds() {
        return brainProfileIds;
    }

    public void setBrainProfileIds(List<UUID> ids) {
        this.brainProfileIds = new ArrayList<>(ids);
    }

    public String getCitizenName() {
        return citizenName;
    }

    public void setCitizenName(String name) {
        this.citizenName = name;
        this.setCustomName(Component.literal(name));
        this.setCustomNameVisible(true);
    }

    public boolean isAIChild() {
        return isChild;
    }

    public void setAIChild(boolean child) {
        this.isChild = child;
        this.setBaby(child);
    }

    public float getHappiness() {
        return happiness;
    }

    public void setHappiness(float happiness) {
        this.happiness = Math.max(0f, Math.min(100f, happiness));
    }

    public UUID getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(UUID partnerId) {
        this.partnerId = partnerId;
    }

    public UUID getMotherId() {
        return motherId;
    }

    public void setMotherId(UUID motherId) {
        this.motherId = motherId;
    }

    public UUID getFatherId() {
        return fatherId;
    }

    public void setFatherId(UUID fatherId) {
        this.fatherId = fatherId;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("CitizenName", citizenName);
        tag.putBoolean("IsAIChild", isChild);
        tag.putInt("AgeTicks", ageTicks);
        tag.putFloat("Happiness", happiness);

        ListTag brainList = new ListTag();
        for (UUID id : brainProfileIds) {
            CompoundTag idTag = new CompoundTag();
            idTag.putUUID("Id", id);
            brainList.add(idTag);
        }
        tag.put("BrainProfileIds", brainList);

        if (partnerId != null) tag.putUUID("PartnerId", partnerId);
        if (motherId != null) tag.putUUID("MotherId", motherId);
        if (fatherId != null) tag.putUUID("FatherId", fatherId);

        tag.put("Residence", residenceData.save());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        citizenName = tag.getString("CitizenName");
        isChild = tag.getBoolean("IsAIChild");
        ageTicks = tag.getInt("AgeTicks");
        happiness = tag.getFloat("Happiness");

        brainProfileIds.clear();
        ListTag brainList = tag.getList("BrainProfileIds", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < brainList.size(); i++) {
            brainProfileIds.add(brainList.getCompound(i).getUUID("Id"));
        }

        partnerId = tag.hasUUID("PartnerId") ? tag.getUUID("PartnerId") : null;
        motherId = tag.hasUUID("MotherId") ? tag.getUUID("MotherId") : null;
        fatherId = tag.hasUUID("FatherId") ? tag.getUUID("FatherId") : null;

        if (tag.contains("Residence")) {
            residenceData.restoreFrom(ResidenceData.load(tag.getCompound("Residence")));
        }
    }
}
