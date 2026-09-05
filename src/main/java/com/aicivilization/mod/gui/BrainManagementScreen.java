package com.aicivilization.mod.gui;

import com.aicivilization.mod.brain.GroqModels;
import com.aicivilization.mod.client.ClientBrainPoolCache;
import com.aicivilization.mod.network.ModNetworking;
import com.aicivilization.mod.network.packet.AddBrainProfilePacket;
import com.aicivilization.mod.network.packet.RemoveBrainProfilePacket;
import com.aicivilization.mod.network.packet.SyncBrainPoolPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 「脳（Groq APIプロファイル）」をゲーム内GUIから追加・編集・削除する画面。
 * config直接編集を不要にするための、フェーズ1の中心的なUI。
 * <p>
 * レイアウト:
 * - 上部: 新規プロファイル追加フォーム（名前・APIキー・モデル名）
 * - 中央: 登録済みプロファイル一覧（名前・マスク済みキー・モデル・割当状況・削除ボタン）
 */
public class BrainManagementScreen extends Screen {

    private EditBox nameField;
    private EditBox apiKeyField;
    private EditBox modelField;

    private int scrollOffset = 0;
    private static final int ROW_HEIGHT = 22;
    private static final int LIST_TOP = 90;

    public BrainManagementScreen() {
        super(Component.literal("AI文明 - 脳（API）管理"));
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;

        nameField = new EditBox(this.font, centerX - 150, 30, 90, 20, Component.literal("プロファイル名"));
        nameField.setHint(Component.literal("名前(例: 脳1)"));
        this.addRenderableWidget(nameField);

        apiKeyField = new EditBox(this.font, centerX - 55, 30, 140, 20, Component.literal("APIキー"));
        apiKeyField.setHint(Component.literal("gsk_ から始まるAPIキー"));
        apiKeyField.setMaxLength(256);
        this.addRenderableWidget(apiKeyField);

        modelField = new EditBox(this.font, centerX + 90, 30, 150, 20, Component.literal("モデル名"));
        modelField.setValue(GroqModels.DEFAULT_MODEL);
        modelField.setHint(Component.literal("例: " + GroqModels.DEFAULT_MODEL));
        this.addRenderableWidget(modelField);

        this.addRenderableWidget(Button.builder(Component.literal("追加"), btn -> onAddProfile())
                .bounds(centerX + 90, 55, 150, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("おすすめモデルを見る"), btn -> {
                    // シンプルにチャット風に候補を提示する（詳細なドロップダウンはフェーズ2で拡張）
                    modelField.setValue(GroqModels.RECOMMENDED.get(
                            (GroqModels.RECOMMENDED.indexOf(modelField.getValue()) + 1) % GroqModels.RECOMMENDED.size()));
                })
                .bounds(centerX - 150, 55, 90, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("閉じる"), btn -> this.onClose())
                .bounds(centerX - 40, this.height - 30, 80, 20)
                .build());
    }

    private void onAddProfile() {
        String name = nameField.getValue().isBlank() ? "無題の脳" : nameField.getValue();
        String key = apiKeyField.getValue();
        String model = modelField.getValue().isBlank() ? GroqModels.DEFAULT_MODEL : modelField.getValue();

        if (key.isBlank()) {
            return; // APIキーは必須
        }

        ModNetworking.CHANNEL.sendToServer(new AddBrainProfilePacket(name, key, model));

        nameField.setValue("");
        apiKeyField.setValue("");
        modelField.setValue(GroqModels.DEFAULT_MODEL);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;

        graphics.drawCenteredString(this.font, this.title, centerX, 10, 0xFFFFFF);
        graphics.drawString(this.font, "登録済みの脳（1つのAIにつき1〜5個割り当てられます）", centerX - 150, LIST_TOP - 15, 0xAAAAAA);

        List<SyncBrainPoolPacket.Entry> entries = ClientBrainPoolCache.getEntries();

        int y = LIST_TOP;
        for (SyncBrainPoolPacket.Entry entry : entries) {
            String status = entry.assigned() ? "§a[割当済]" : "§7[未割当]";
            String line = String.format("%s  %s  (%s)  %s",
                    entry.profileName(), entry.maskedApiKey(), entry.modelName(), status);
            graphics.drawString(this.font, line, centerX - 150, y, 0xFFFFFF);

            // 削除ボタン相当（クリック判定は簡略化し、mouseClickedで処理）
            graphics.drawString(this.font, "§c[削除]", centerX + 140, y, 0xFF5555);

            y += ROW_HEIGHT;
        }

        if (entries.isEmpty()) {
            graphics.drawString(this.font, "まだ脳（APIプロファイル）が登録されていません。", centerX - 150, y, 0xAAAAAA);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int centerX = this.width / 2;
        List<SyncBrainPoolPacket.Entry> entries = ClientBrainPoolCache.getEntries();

        int y = LIST_TOP;
        for (SyncBrainPoolPacket.Entry entry : entries) {
            if (mouseX >= centerX + 140 && mouseX <= centerX + 170
                    && mouseY >= y && mouseY <= y + this.font.lineHeight) {
                ModNetworking.CHANNEL.sendToServer(new RemoveBrainProfilePacket(entry.profileId()));
                return true;
            }
            y += ROW_HEIGHT;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
