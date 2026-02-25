package com.tian_nu.AdvancedTurret.gui;

import com.tian_nu.AdvancedTurret.items.SmartChipItem;
import com.tian_nu.AdvancedTurret.items.SmartChipItem.TargetMode;
import com.tian_nu.AdvancedTurret.network.ModNetwork;
import com.tian_nu.AdvancedTurret.network.SmartChipConfigPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SmartChipConfigScreen extends Screen {
    private final ItemStack stack;
    private final BlockPos pos; // Null if handheld

    private int targetFlags;
    private boolean friendlyFire;
    private boolean predictiveAiming;
    private byte enabledFacesMask;
    
    private EditBox blacklistInput;
    private EditBox whitelistInput;
    
    private Checkbox friendlyFireCheckbox;
    private Checkbox predictiveAimingCheckbox;
    
    // Flag Toggles
    private Checkbox hostileCheckbox;
    private Checkbox neutralCheckbox;
    private Checkbox friendlyCheckbox;
    private Checkbox playersCheckbox;
    
    private final Map<Direction, Button> faceButtons = new HashMap<>();

    public SmartChipConfigScreen(ItemStack stack, BlockPos pos) {
        super(Component.translatable("gui.advanced_turret.smart_config"));
        this.stack = stack;
        this.pos = pos;
        
        // Load initial values
        this.targetFlags = SmartChipItem.getTargetFlags(stack);
        this.friendlyFire = SmartChipItem.isFriendlyFire(stack);
        this.predictiveAiming = SmartChipItem.isPredictiveAiming(stack);
        this.enabledFacesMask = SmartChipItem.getEnabledFaces(stack);
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = 30;
        
        // Target Mode Flags
        int flagsStartX = centerX - 150;
        this.hostileCheckbox = new Checkbox(flagsStartX, startY, 70, 20, 
                Component.translatable("gui.advanced_turret.target_mode.hostile"), (targetFlags & SmartChipItem.FLAG_HOSTILE) != 0);
        addRenderableWidget(hostileCheckbox);
        
        this.neutralCheckbox = new Checkbox(flagsStartX + 75, startY, 70, 20, 
                Component.translatable("gui.advanced_turret.target_mode.neutral"), (targetFlags & SmartChipItem.FLAG_NEUTRAL) != 0);
        addRenderableWidget(neutralCheckbox);
        
        this.friendlyCheckbox = new Checkbox(flagsStartX + 150, startY, 70, 20, 
                Component.translatable("gui.advanced_turret.target_mode.friendly"), (targetFlags & SmartChipItem.FLAG_FRIENDLY) != 0);
        addRenderableWidget(friendlyCheckbox);
        
        this.playersCheckbox = new Checkbox(flagsStartX + 225, startY, 70, 20, 
                Component.translatable("gui.advanced_turret.target_mode.players"), (targetFlags & SmartChipItem.FLAG_PLAYERS) != 0);
        addRenderableWidget(playersCheckbox);
        
        // Toggles
        startY += 25;
        this.friendlyFireCheckbox = new Checkbox(centerX - 100, startY, 100, 20, 
                Component.translatable("gui.advanced_turret.friendly_fire"), friendlyFire);
        addRenderableWidget(friendlyFireCheckbox);
        
        this.predictiveAimingCheckbox = new Checkbox(centerX + 10, startY, 100, 20, 
                Component.translatable("gui.advanced_turret.predictive_aiming"), predictiveAiming);
        addRenderableWidget(predictiveAimingCheckbox);
        
        // Face Selection (3x2 grid)
        startY += 30;
        int btnSize = 20;
        int faceStartX = centerX - (btnSize * 3) / 2;
        
        Direction[] directions = {Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        String[] labels = {"U", "D", "N", "S", "W", "E"};
        
        for (int i = 0; i < directions.length; i++) {
            Direction dir = directions[i];
            int x = faceStartX + (i % 3) * (btnSize + 5);
            int y = startY + (i / 3) * (btnSize + 5);
            
            Button btn = Button.builder(Component.literal(labels[i]), b -> {
                boolean enabled = (enabledFacesMask & (1 << dir.get3DDataValue())) != 0;
                if (enabled) {
                    enabledFacesMask &= ~(1 << dir.get3DDataValue());
                } else {
                    enabledFacesMask |= (1 << dir.get3DDataValue());
                }
            }).bounds(x, y, btnSize, btnSize).build();
            
            faceButtons.put(dir, btn);
            addRenderableWidget(btn);
        }
        
        // Lists
        startY += 60;
        addRenderableWidget(new Button.Builder(Component.translatable("gui.advanced_turret.blacklist_label"), b -> {}).bounds(centerX - 150, startY - 12, 300, 10).build()).active = false;
        
        this.blacklistInput = new EditBox(this.font, centerX - 150, startY, 300, 20, Component.translatable("gui.advanced_turret.blacklist_label"));
        this.blacklistInput.setMaxLength(1024);
        this.blacklistInput.setValue(String.join(",", SmartChipItem.getBlacklist(stack)));
        addRenderableWidget(blacklistInput);
        
        startY += 40;
        addRenderableWidget(new Button.Builder(Component.translatable("gui.advanced_turret.whitelist_label"), b -> {}).bounds(centerX - 150, startY - 12, 300, 10).build()).active = false;
        
        this.whitelistInput = new EditBox(this.font, centerX - 150, startY, 300, 20, Component.translatable("gui.advanced_turret.whitelist_label"));
        this.whitelistInput.setMaxLength(1024);
        this.whitelistInput.setValue(String.join(",", SmartChipItem.getWhitelist(stack)));
        addRenderableWidget(whitelistInput);
        
        // Save Button
        startY += 30;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> {
            saveAndClose();
        }).bounds(centerX - 50, this.height - 30, 100, 20).build());
    }
    
    private void saveAndClose() {
        // Parse lists
        List<String> blacklist = Arrays.stream(blacklistInput.getValue().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
                
        List<String> whitelist = Arrays.stream(whitelistInput.getValue().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        
        // Calculate flags
        int flags = 0;
        if (hostileCheckbox.selected()) flags |= SmartChipItem.FLAG_HOSTILE;
        if (neutralCheckbox.selected()) flags |= SmartChipItem.FLAG_NEUTRAL;
        if (friendlyCheckbox.selected()) flags |= SmartChipItem.FLAG_FRIENDLY;
        if (playersCheckbox.selected()) flags |= SmartChipItem.FLAG_PLAYERS;
        
        ModNetwork.CHANNEL.sendToServer(new SmartChipConfigPacket(
                pos,
                TargetMode.HOSTILE, // Deprecated, but keeping for compatibility if packet requires enum
                friendlyFireCheckbox.selected(),
                predictiveAimingCheckbox.selected(),
                enabledFacesMask,
                blacklist,
                whitelist,
                flags
        ));
        
        this.onClose();
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);
        
        // Render face button colors
        faceButtons.forEach((dir, btn) -> {
            boolean enabled = (enabledFacesMask & (1 << dir.get3DDataValue())) != 0;
            int color = enabled ? 0x4000FF00 : 0x40FF0000;
            guiGraphics.fill(btn.getX(), btn.getY(), btn.getX() + btn.getWidth(), btn.getY() + btn.getHeight(), color);
        });
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
