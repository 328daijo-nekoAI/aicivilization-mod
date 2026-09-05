package com.aicivilization.mod.gui;

import com.aicivilization.mod.brain.BrainProfilePool;
import com.aicivilization.mod.client.ClientBrainPoolCache;
import com.aicivilization.mod.network.ModNetworking;
import com.aicivilization.mod.network.packet.RequestSpawnAIPacket;
import com.aicivilization.mod.network.packet.SyncBrainPoolPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ワールドに入った際（または追加スポーン時）に、
 * 「何人のAIを出現させるか」「各AIにどの脳を何個割り当てるか」を選ぶ画面。
 * <p>
 * フェーズ1では、シンプルに「未割当の脳プロファイルから1〜5個を選んで1体スポーン」を
 * 繰り返す形にする。複数人まとめての一括セットアップUIはフェーズ2で拡張する。
 */
public class SpawnSetupScreen extends Screen {

    private final List<UUID> selected = new ArrayList<>();
    private static final int LIST_TOP = 50;
    private static final int ROW_HEIGHT = 20;

    public SpawnSetupScreen() {
        super(Component.literal("AI文明 - 新しいAIを出現させる"));
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;

        this.addRenderableWidget(Button.builder(Component.literal("この脳の組み合わせでスポーン"), btn -> onSpawn())
                .bounds(centerX - 100, this.height - 60, 200, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("脳を管理する"), btn -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new BrainManagementScreen());
                    }
                })
                .bounds(centerX - 100, this.height - 35, 200, 20)
                .build());
    }

    private void onSpawn() {
        if (selected.isEmpty()) {
            return;
        }
        ModNetworking.CHANNEL.sendToServer(new RequestSpawnAIPacket(new ArrayList<>(selected)));
        selected.clear();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        graphics.drawCenteredString(this.font, this.title, centerX, 10, 0xFFFFFF);
        graphics.drawString(this.font, String.format("選択中の脳: %d / %d 個（最低1個必要）",
                selected.size(), BrainProfilePool.getMaxProfilesPerEntity()), centerX - 150, 28, 0xAAAAAA);

        List<SyncBrainPoolPacket.Entry> entries = ClientBrainPoolCache.getEntries();
        int y = LIST_TOP;
        for (SyncBrainPoolPacket.Entry entry : entries) {
            if (entry.assigned()) {
                continue; // 既に他のAIに割り当て済みの脳は選べない
            }
            boolean isSelected = selected.contains(entry.profileId());
            String mark = isSelected ? "§a[x]" : "§7[ ]";
            String line = String.format("%s %s (%s)", mark, entry.profileName(), entry.modelName());
            graphics.drawString(this.font, line, centerX - 150, y, 0xFFFFFF);
            y += ROW_HEIGHT;
        }

        if (entries.stream().allMatch(SyncBrainPoolPacket.Entry::assigned)) {
            graphics.drawString(this.font,
                    "未割当の脳がありません。先に「脳を管理する」からAPIキーを追加してください。",
                    centerX - 150, y, 0xFF5555);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int centerX = this.width / 2;
        List<SyncBrainPoolPacket.Entry> entries = ClientBrainPoolCache.getEntries();

        int y = LIST_TOP;
        for (SyncBrainPoolPacket.Entry entry : entries) {
            if (entry.assigned()) {
                continue;
            }
            if (mouseX >= centerX - 150 && mouseX <= centerX + 150
                    && mouseY >= y && mouseY <= y + this.font.lineHeight) {
                toggleSelection(entry.profileId());
                return true;
            }
            y += ROW_HEIGHT;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void toggleSelection(UUID profileId) {
        if (selected.contains(profileId)) {
            selected.remove(profileId);
        } else if (selected.size() < BrainProfilePool.getMaxProfilesPerEntity()) {
            selected.add(profileId);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
