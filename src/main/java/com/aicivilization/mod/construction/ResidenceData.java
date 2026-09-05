package com.aicivilization.mod.construction;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/**
 * AI 1体分の居住地情報。
 * AICitizenEntityのNBTに保持され、「家を持っているか」の判断材料になる。
 */
public class ResidenceData {

    private boolean hasHome = false;
    private BlockPos homePos = null;

    public boolean hasHome() {
        return hasHome;
    }

    public BlockPos getHomePos() {
        return homePos;
    }

    public void setHome(BlockPos pos) {
        this.hasHome = true;
        this.homePos = pos;
    }

    public void clearHome() {
        this.hasHome = false;
        this.homePos = null;
    }

    /** 既存インスタンスの状態を、保存されていたデータで上書きする（final参照を維持するため）。 */
    public void restoreFrom(ResidenceData other) {
        this.hasHome = other.hasHome;
        this.homePos = other.homePos;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("HasHome", hasHome);
        if (homePos != null) {
            tag.putInt("HomeX", homePos.getX());
            tag.putInt("HomeY", homePos.getY());
            tag.putInt("HomeZ", homePos.getZ());
        }
        return tag;
    }

    public static ResidenceData load(CompoundTag tag) {
        ResidenceData data = new ResidenceData();
        data.hasHome = tag.getBoolean("HasHome");
        if (data.hasHome && tag.contains("HomeX")) {
            data.homePos = new BlockPos(tag.getInt("HomeX"), tag.getInt("HomeY"), tag.getInt("HomeZ"));
        }
        return data;
    }
}
