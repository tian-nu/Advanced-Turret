package com.tian_nu.AdvancedTurret.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.tian_nu.AdvancedTurret.ConfigManager;
import com.tian_nu.AdvancedTurret.network.ModNetwork;
import com.tian_nu.AdvancedTurret.network.TurretOpenFaceConfigPacket;
import com.tian_nu.AdvancedTurret.network.TurretRangeConfigPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 炮塔基座主界面
 */
public class TurretScreen extends AbstractContainerScreen<TurretMenu> {

    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath("advanced_turret", "textures/gui/container/turret_base.png");

    private static final int TEXTURE_WIDTH = 194;
    private static final int TEXTURE_HEIGHT = 166;
    private static final int AMMO_SLOT_COUNT = 9;
    private static final int PLAYER_SLOT_COUNT = 36;

    private static final int ENERGY_BAR_X = 169;
    private static final int ENERGY_BAR_Y = 19;
    private static final int ENERGY_BAR_WIDTH = 14;
    private static final int ENERGY_BAR_HEIGHT = 50;
    private static final int RANGE_INPUT_X = 80;
    private static final int RANGE_INPUT_Y = 22;
    private static final int RANGE_INPUT_WIDTH = 28;
    private static final int RANGE_INPUT_HEIGHT = 16;
    private static final int OWNER_LABEL_X = 8;
    private static final int OWNER_LABEL_Y = 13;

    private float backgroundAlpha = ConfigManager.getBackgroundAlpha();
    private float energyBarAlpha = ConfigManager.getEnergyBarAlpha();

    private Button smartConfigButton;
    private Button faceConfigButton;
    private Button personalConfigButton;
    private EditBox rangeInput;
    private double lastSubmittedRange = Double.NaN;

    public TurretScreen(TurretMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = TEXTURE_WIDTH;
        this.imageHeight = TEXTURE_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();

        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.titleLabelY = 2;
        this.inventoryLabelY = this.imageHeight - 94;

        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;

        this.smartConfigButton = Button.builder(Component.translatable("gui.advanced_turret.smart_config"), b -> {
            ItemStack stack = getPluginStack();
            if (!stack.isEmpty()) {
                Minecraft.getInstance().setScreen(new SmartChipConfigScreen(stack, menu.getBlockEntity().getBlockPos()));
            }
        }).bounds(guiLeft + this.imageWidth + 8, guiTop + 22, 84, 20).build();
        addRenderableWidget(this.smartConfigButton);

        this.faceConfigButton = Button.builder(Component.translatable("gui.advanced_turret.face_config"), b ->
            ModNetwork.CHANNEL.sendToServer(new TurretOpenFaceConfigPacket(menu.getBlockEntity().getBlockPos()))
        ).bounds(guiLeft + this.imageWidth + 8, guiTop + 47, 84, 20).build();
        addRenderableWidget(this.faceConfigButton);

        this.personalConfigButton = Button.builder(Component.translatable("gui.turret_personal_config.button"), b ->
            openConfigScreen()
        ).bounds(guiLeft + this.imageWidth + 8, guiTop + 72, 84, 20).build();
        addRenderableWidget(this.personalConfigButton);

        int rangeInputX = guiLeft + getRangeInputX();
        this.rangeInput = new EditBox(this.font, rangeInputX, guiTop + RANGE_INPUT_Y, getRangeInputWidth(), getRangeInputHeight(),
            Component.translatable("gui.advanced_turret.range_control"));
        this.rangeInput.setMaxLength(3);
        this.rangeInput.setFilter(this::isValidRangeInput);
        double manualLimit = menu.getBlockEntity().getManualRangeLimit();
        this.rangeInput.setValue(formatRangeValue(manualLimit));
        this.lastSubmittedRange = manualLimit;
        addRenderableWidget(this.rangeInput);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (rangeInput != null && !rangeInput.isFocused()) {
            commitRangeInput();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean clickedInput = rangeInput != null && rangeInput.isMouseOver(mouseX, mouseY);
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (rangeInput != null && !clickedInput) {
            if (rangeInput.isFocused()) {
                commitRangeInput();
            }
            rangeInput.setFocused(false);
        }
        return handled;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (rangeInput != null && rangeInput.isFocused() && (keyCode == 257 || keyCode == 335)) {
            commitRangeInput();
            rangeInput.setFocused(false);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean isValidRangeInput(String value) {
        return value.isEmpty() || value.matches("\\d{0,3}");
    }

    private String formatRangeValue(double value) {
        if (value <= 0.0D) {
            return "";
        }
        return Integer.toString((int) Math.round(value));
    }

    private void commitRangeInput() {
        if (rangeInput == null) {
            return;
        }
        String raw = rangeInput.getValue().trim();
        double parsed;
        if (raw.isEmpty()) {
            parsed = 0.0D;
        } else {
            try {
                parsed = Integer.parseInt(raw);
            } catch (NumberFormatException ignored) {
                return;
            }
        }
        parsed = parsed <= 0.0D ? 0.0D : Math.min(256.0D, Math.max(1.0D, parsed));
        if (Math.abs(parsed - lastSubmittedRange) < 0.001D) {
            return;
        }
        lastSubmittedRange = parsed;
        rangeInput.setValue(formatRangeValue(parsed));
        ModNetwork.CHANNEL.sendToServer(new TurretRangeConfigPacket(menu.getBlockEntity().getBlockPos(), parsed));
    }

    private ItemStack getPluginStack() {
        return menu.getBlockEntity().getPluginStack();
    }

    private void updateControlsVisibility() {
        ItemStack stack = getPluginStack();
        if (smartConfigButton != null) {
            smartConfigButton.visible = !stack.isEmpty();
        }
    }

    private void openConfigScreen() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new TurretPersonalConfigScreen(this, menu.getBlockEntity()));
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        updateControlsVisibility();
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);

        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;
        int barX = guiLeft + ENERGY_BAR_X;
        int barY = guiTop + ENERGY_BAR_Y;

        if (mouseX >= barX && mouseX < barX + ENERGY_BAR_WIDTH &&
            mouseY >= barY && mouseY < barY + ENERGY_BAR_HEIGHT) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(TurretUiTheme.tipTitle(Component.translatable("gui.advanced_turret.energy").getString()));
            tooltip.add(TurretUiTheme.tipInfo(menu.getEnergy() + " / " + menu.getMaxEnergy() + " FE"));
            guiGraphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, backgroundAlpha);
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.drawString(this.font, Component.translatable("gui.advanced_turret.range_control"), x + getRangeLabelX(), y + 13, TurretUiTheme.COLOR_TEXT_SUB, false);

        String ownerName = menu.getBlockEntity().getResolvedOwnerName();
        if (!ownerName.isBlank()) {
            guiGraphics.drawString(this.font,
                Component.translatable("gui.advanced_turret.owner_tooltip", ownerName),
                x + OWNER_LABEL_X,
                y + OWNER_LABEL_Y,
                TurretUiTheme.COLOR_TEXT_SUB,
                false);
        }

        Rect ammoRect = calcSlotRect(0, AMMO_SLOT_COUNT, 3);
        if (ammoRect != null) {
            TurretUiTheme.drawSection(guiGraphics, x + ammoRect.x, y + ammoRect.y, ammoRect.w, ammoRect.h, backgroundAlpha);
            guiGraphics.drawString(this.font, Component.translatable("gui.advanced_turret.ammo"), x + ammoRect.x, y + ammoRect.y - 9, TurretUiTheme.COLOR_TEXT_SUB, false);
            drawSlotFrames(guiGraphics, x, y, 0, AMMO_SLOT_COUNT);
        }

        Rect pluginRect = getPluginRect();
        if (pluginRect != null) {
            TurretUiTheme.drawSection(guiGraphics, x + pluginRect.x, y + pluginRect.y, pluginRect.w, pluginRect.h, backgroundAlpha);
            guiGraphics.drawString(this.font, Component.translatable("gui.advanced_turret.plugin"), x + pluginRect.x, y + pluginRect.y - 10, TurretUiTheme.COLOR_TEXT_SUB, false);
            drawSlotFrames(guiGraphics, x, y, AMMO_SLOT_COUNT,
                Math.min(menu.getBlockEntity().getPluginSlotCount(), menu.getBlockEntity().getBasePluginSlot().getSlots()));
        }

        int playerStart = menu.slots.size() - PLAYER_SLOT_COUNT;
        if (playerStart >= 0) {
            Rect invRect = calcSlotRect(playerStart, 27, 3);
            if (invRect != null) {
                TurretUiTheme.drawSection(guiGraphics, x + invRect.x, y + invRect.y, invRect.w, invRect.h, backgroundAlpha);
            }

            Rect hotbarRect = calcSlotRect(playerStart + 27, 9, 3);
            if (hotbarRect != null) {
                TurretUiTheme.drawSection(guiGraphics, x + hotbarRect.x, y + hotbarRect.y, hotbarRect.w, hotbarRect.h, backgroundAlpha);
            }
        }

        TurretUiTheme.drawSection(guiGraphics, x + ENERGY_BAR_X - 2, y + ENERGY_BAR_Y - 2, ENERGY_BAR_WIDTH + 4, ENERGY_BAR_HEIGHT + 4, backgroundAlpha);
        renderEnergyBar(guiGraphics, x, y);
        RenderSystem.disableBlend();
    }

    private void drawSlotFrames(GuiGraphics guiGraphics, int guiX, int guiY, int startIndex, int count) {
        for (int i = 0; i < count; i++) {
            int idx = startIndex + i;
            if (idx < 0 || idx >= menu.slots.size()) {
                continue;
            }
            Slot slot = menu.slots.get(idx);
            TurretUiTheme.drawSlotFrame(guiGraphics, guiX + slot.x - 1, guiY + slot.y - 1, backgroundAlpha);
        }
    }

    private Rect getPluginRect() {
        int pluginSlotCount = Math.min(menu.getBlockEntity().getPluginSlotCount(), menu.getBlockEntity().getBasePluginSlot().getSlots());
        if (pluginSlotCount <= 0) {
            return null;
        }
        return calcSlotRect(AMMO_SLOT_COUNT, pluginSlotCount, 1);
    }

    private int getRangeInputX() {
        Rect pluginRect = getPluginRect();
        if (pluginRect != null) {
            return pluginRect.x;
        }
        return RANGE_INPUT_X;
    }

    private int getRangeLabelX() {
        Rect pluginRect = getPluginRect();
        if (pluginRect != null) {
            return pluginRect.x;
        }
        return getRangeInputX() - 6;
    }


    private int getRangeInputWidth() {
        Rect pluginRect = getPluginRect();
        if (pluginRect != null) {
            return pluginRect.w;
        }
        return RANGE_INPUT_WIDTH;
    }

    private int getRangeInputHeight() {
        return RANGE_INPUT_HEIGHT;
    }
    private Rect calcSlotRect(int startIndex, int count, int pad) {
        if (count <= 0) {
            return null;
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (int i = 0; i < count; i++) {
            int idx = startIndex + i;
            if (idx < 0 || idx >= menu.slots.size()) {
                continue;
            }
            Slot slot = menu.slots.get(idx);
            minX = Math.min(minX, slot.x - 1);
            minY = Math.min(minY, slot.y - 1);
            maxX = Math.max(maxX, slot.x + 16);
            maxY = Math.max(maxY, slot.y + 16);
        }

        if (minX == Integer.MAX_VALUE) {
            return null;
        }

        return new Rect(minX - pad, minY - pad, (maxX - minX + 1) + pad * 2, (maxY - minY + 1) + pad * 2);
    }

    private void renderEnergyBar(GuiGraphics guiGraphics, int x, int y) {
        int maxEnergy = menu.getMaxEnergy();
        int fillHeight = maxEnergy > 0 ? (int) (((float) menu.getEnergy() / maxEnergy) * ENERGY_BAR_HEIGHT) : 0;
        int barLeft = x + ENERGY_BAR_X;
        int barTop = y + ENERGY_BAR_Y;
        int barBottom = barTop + ENERGY_BAR_HEIGHT;

        guiGraphics.fill(barLeft, barTop, barLeft + ENERGY_BAR_WIDTH, barBottom, 0xAA2A1010);

        if (fillHeight > 0) {
            int alpha = Math.max(0, Math.min(255, (int) (energyBarAlpha * 255.0F)));
            int fillColor = (alpha << 24) | 0x00D63838;
            int glowColor = (alpha << 24) | 0x00FF6B6B;
            guiGraphics.fill(barLeft, barBottom - fillHeight, barLeft + ENERGY_BAR_WIDTH, barBottom, fillColor);
            guiGraphics.fill(barLeft, barBottom - fillHeight, barLeft + ENERGY_BAR_WIDTH, barBottom - fillHeight + 1, glowColor);
        }

        TurretUiTheme.drawBorder(guiGraphics, barLeft, barTop, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT, 0xFF8A2B2B);
    }

    public void setBackgroundAlpha(float alpha) {
        this.backgroundAlpha = Math.max(0.0F, Math.min(1.0F, alpha));
        ConfigManager.setBackgroundAlpha(alpha);
        ConfigManager.saveConfig();
    }

    public void setEnergyBarAlpha(float alpha) {
        this.energyBarAlpha = Math.max(0.0F, Math.min(1.0F, alpha));
        ConfigManager.setEnergyBarAlpha(alpha);
        ConfigManager.saveConfig();
    }

    public float getBackgroundAlpha() {
        return this.backgroundAlpha;
    }

    public float getEnergyBarAlpha() {
        return this.energyBarAlpha;
    }

    private record Rect(int x, int y, int w, int h) {
    }
}
