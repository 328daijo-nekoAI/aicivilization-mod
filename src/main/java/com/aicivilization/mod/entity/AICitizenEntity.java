package com.aicivilization.mod.entity;

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

/**
 * AI文明の住人となるエンティティ。
 * <p>
 * バニラの村人(Villager)をベースに拡張する。理由:
 * - 見た目・アニメーション・取引UI等の基盤を流用でき、独自モデルを1から作る必要がない
 * - AI自身が「見た目の傾向（職業・年齢に応じた服装など）」を選べる余地が
 *   VillagerDataの職業・タイプ設定を通じて自然に持たせられる
 * <p>
 * このクラス自体は「NBT上のフラグと基礎ステータス」のみを保持し、
 * 実際の思考ロジック（Groq呼び出し・行動決定）は brain/civilization パッケージ側が扱う。
 */
public class AICitizenEntity extends Villager {

    // このエンティティが使っている脳（APIプロファイル）のIDリスト。1〜5個。
    private List<UUID> brainProfileIds = new ArrayList<>();

    // ライフサイクル関連
    private String citizenName = "";
    private boolean isChild = false;
    private int ageTicks = 0;

    // 人間関係・感情パラメータ（8章の感情システムの基礎値）
    private float happiness = 50f;   // 0-100
    private UUID partnerId = null;    // 結婚相手
    private UUID motherId = null;
    private UUID fatherId = null;
    private final ResidenceData residenceData = new ResidenceData();

    public ResidenceData getResidenceData() {
        return residenceData;
    }

    public AICitizenEntity(EntityType<? extends Villager> type, Level level) {
        super(type, level);
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

    // Villagerのランダムな取引生成等、村人特有の初期化を一部無効化したい場合はここでオーバーライドする。
    // フェーズ1では基礎データ構造のみを用意し、細かい挙動制御はフェーズ2以降で拡張する。
}
