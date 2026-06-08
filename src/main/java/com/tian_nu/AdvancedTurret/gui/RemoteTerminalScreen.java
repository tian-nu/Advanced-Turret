package com.tian_nu.AdvancedTurret.gui;

import com.tian_nu.AdvancedTurret.client.RemoteTerminalClientHooks;
import com.tian_nu.AdvancedTurret.network.ModNetwork;
import com.tian_nu.AdvancedTurret.network.RemoteTerminalApplyPacket;
import com.tian_nu.AdvancedTurret.network.RemoteTerminalBaseInfo;
import com.tian_nu.AdvancedTurret.network.RemoteTerminalQueryPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 远程终端界面。
 */
public class RemoteTerminalScreen extends Screen {

    private static final int PANEL_W = 434;
    private static final int PANEL_H = 252;
    private static final int PAGE_SIZE = 5;

    private static final Direction[] FACE_ORDER = new Direction[]{
            Direction.UP, Direction.NORTH, Direction.SOUTH,
            Direction.WEST, Direction.EAST, Direction.DOWN
    };

    private static String lastSelectedBaseKey = "";
    private static int lastPageIndex = 0;

    public static void prepareNextSelectedBase(String dimensionId, BlockPos pos) {
        if (dimensionId == null || dimensionId.isBlank() || pos == null) {
            return;
        }
        lastSelectedBaseKey = dimensionId + "|" + pos.asLong();
        lastPageIndex = 0;
    }


    private final List<RemoteTerminalBaseInfo> baseEntries = new ArrayList<>();
    private final List<Integer> filteredIndices = new ArrayList<>();
    private final List<Button> rowButtons = new ArrayList<>();
    private final List<TechCheckbox> faceCheckboxes = new ArrayList<>();
    private final Map<String, ListDraft> listDraftByBaseKey = new HashMap<>();

    private int selectedIndex = -1;
    private int pageIndex = 0;

    private TechEditBox searchInput;

    private Button pagePrevButton;
    private Button pageNextButton;
    private Button refreshButton;
    private Button applyButton;
    private Button highlightToggleButton;
    private Button highlightSingleButton;
    private Button pinTopButton;
    private Button openListConfigButton;

    private String statusMessage = "";
    private boolean keepStatusOnNextSync = false;


    private int panelX;
    private int panelY;

    public RemoteTerminalScreen() {
        super(Component.translatable("gui.advanced_turret.remote_terminal.title"));
    }

    @Override
    protected void init() {
        this.rowButtons.clear();
        this.faceCheckboxes.clear();

        panelX = (this.width - PANEL_W) / 2;
        panelY = (this.height - PANEL_H) / 2;

        this.searchInput = new TechEditBox(this.font, panelX + 14, panelY + 36, 140, 18,
                Component.translatable("gui.advanced_turret.remote_terminal.search"));
        this.searchInput.setResponder(value -> {
            pageIndex = 0;
            rebuildFilteredEntries();
            refreshFormBySelection();
        });
        addRenderableWidget(searchInput);

        this.refreshButton = addRenderableWidget(TechButton.builder(Component.translatable("gui.advanced_turret.remote_terminal.refresh"), b -> requestBaseList())
                .bounds(panelX + 158, panelY + 36, 46, 18)
                .build());

        this.highlightSingleButton = addRenderableWidget(TechButton.builder(Component.empty(), b -> toggleSingleHighlight())
                .bounds(panelX + 218, panelY + 36, 98, 18)
                .build());

        this.highlightToggleButton = addRenderableWidget(TechButton.builder(Component.empty(), b -> {
            RemoteTerminalClientHooks.toggleHighlightEnabled();
            statusMessage = Component.translatable(RemoteTerminalClientHooks.isHighlightEnabled()
                    ? "gui.advanced_turret.remote_terminal.highlight_on"
                    : "gui.advanced_turret.remote_terminal.highlight_off").getString();
            refreshToggleButtons();
        }).bounds(panelX + 320, panelY + 36, 98, 18).build());

        this.pinTopButton = addRenderableWidget(TechButton.builder(Component.empty(), b -> togglePinSelected())
                .bounds(panelX + 218, panelY + 58, 98, 18)
                .build());

        this.openListConfigButton = addRenderableWidget(TechButton.builder(Component.translatable("gui.advanced_turret.remote_terminal.list_config"), b -> openListConfig())
                .bounds(panelX + 320, panelY + 58, 98, 18)
                .build());

        int rowStartY = panelY + 60;
        for (int i = 0; i < PAGE_SIZE; i++) {
            final int row = i;
            Button rowButton = addRenderableWidget(TechButton.builder(Component.literal("-"), b -> selectByVisibleRow(row))
                    .bounds(panelX + 14, rowStartY + i * 24, 190, 20)
                    .build());
            rowButtons.add(rowButton);
        }

        this.pagePrevButton = addRenderableWidget(TechButton.builder(Component.translatable("gui.advanced_turret.entity_analyzer.prev"), b -> switchPage(-1))
                .bounds(panelX + 14, panelY + 182, 66, 18)
                .build());
        this.pageNextButton = addRenderableWidget(TechButton.builder(Component.translatable("gui.advanced_turret.entity_analyzer.next"), b -> switchPage(1))
                .bounds(panelX + 138, panelY + 182, 66, 18)
                .build());

        int faceY = panelY + 168;
        for (int i = 0; i < FACE_ORDER.length; i++) {
            Direction face = FACE_ORDER[i];
            TechCheckbox checkbox = new TechCheckbox(
                    panelX + 218 + (i % 3) * 66,
                    faceY + (i / 3) * 20,
                    64,
                    20,
                    Component.translatable(directionLangKey(face)),
                    true
            );
            faceCheckboxes.add(checkbox);
            addRenderableWidget(checkbox);
        }

        this.applyButton = addRenderableWidget(TechButton.builder(Component.translatable("gui.advanced_turret.remote_terminal.apply"), b -> sendApply())
                .bounds(panelX + 10, panelY + PANEL_H - 28, 100, 18)
                .build());

        addRenderableWidget(TechButton.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(panelX + PANEL_W - 70, panelY + PANEL_H - 28, 60, 18)
                .build());

        requestBaseList();
        rebuildFilteredEntries();
        refreshFormBySelection();
    }

    @Override
    public void onClose() {
        super.onClose();
    }

    public void applyServerData(List<RemoteTerminalBaseInfo> entries) {
        String selectedKeyBefore = getSelectedBase() == null ? "" : buildBaseKey(getSelectedBase());
        if (selectedKeyBefore.isBlank() && !lastSelectedBaseKey.isBlank()) {
            selectedKeyBefore = lastSelectedBaseKey;
        }

        this.baseEntries.clear();
        this.baseEntries.addAll(entries);

        if (baseEntries.isEmpty()) {
            selectedIndex = -1;
            statusMessage = Component.translatable("gui.advanced_turret.remote_terminal.no_base").getString();
        } else {
            selectedIndex = -1; // 未找到则默认为 -1（无选中）
            if (!selectedKeyBefore.isBlank()) {
                for (int i = 0; i < baseEntries.size(); i++) {
                    if (selectedKeyBefore.equals(buildBaseKey(baseEntries.get(i)))) {
                        selectedIndex = i;
                        break;
                    }
                }
            }
            if (selectedIndex == -1 && !lastSelectedBaseKey.isBlank()) {
               // 上次选中的基座可能已被拆除，不选中任何项
               selectedIndex = -1;
            } else if (selectedIndex == -1 && selectedKeyBefore.isBlank()) {
               // 初次打开，不自动选中第 0 个基座
               selectedIndex = -1;
            }
            if (!keepStatusOnNextSync) {
                statusMessage = Component.translatable("gui.advanced_turret.remote_terminal.synced", baseEntries.size()).getString();
            }
            keepStatusOnNextSync = false;
        }

        pruneDrafts();
        rebuildFilteredEntries();
        this.pageIndex = Math.min(lastPageIndex, getTotalPages() - 1);
        this.pageIndex = Math.max(0, this.pageIndex);
        refreshFormBySelection();
    }

    public void applyOperationResult(String messageKey, int arg1, int arg2) {
        if (arg1 >= 0 && arg2 >= 0) {
            statusMessage = Component.translatable(messageKey, arg1, arg2).getString();
        } else {
            statusMessage = Component.translatable(messageKey).getString();
        }
        keepStatusOnNextSync = true;


        if ("gui.advanced_turret.remote_terminal.op_apply_success".equals(messageKey)) {
            RemoteTerminalBaseInfo selected = getSelectedBase();
            if (selected != null) {
                listDraftByBaseKey.remove(buildBaseKey(selected));
            }
        }
    }

    public void saveListDraftAndApply(RemoteTerminalBaseInfo base, String blacklistCsv, String whitelistCsv) {
        if (base == null) {
            return;
        }
        String key = buildBaseKey(base);
        listDraftByBaseKey.put(key, new ListDraft(blacklistCsv, whitelistCsv));

        byte faceMask = base.enabledFacesMask();
        RemoteTerminalBaseInfo selected = getSelectedBase();
        if (selected != null && key.equals(buildBaseKey(selected))) {
            faceMask = collectFaceMask();
        }

        sendApplyForBase(base, blacklistCsv, whitelistCsv, faceMask);
        refreshFormBySelection();
    }

    private void requestBaseList() {
        ModNetwork.CHANNEL.sendToServer(new RemoteTerminalQueryPacket());
        statusMessage = Component.translatable("gui.advanced_turret.remote_terminal.querying").getString();
    }

    private void switchPage(int step) {
        int totalPages = getTotalPages();
        if (totalPages <= 1) {
            pageIndex = 0;
        } else {
            pageIndex = Math.max(0, Math.min(pageIndex + step, totalPages - 1));
        }
        lastPageIndex = pageIndex;
        refreshFormBySelection();
    }

    private void selectByVisibleRow(int row) {
        int idxInFiltered = pageIndex * PAGE_SIZE + row;
        if (idxInFiltered < 0 || idxInFiltered >= filteredIndices.size()) {
            return;
        }
        selectedIndex = filteredIndices.get(idxInFiltered);
        RemoteTerminalBaseInfo selected = getSelectedBase();
        if (selected != null) {
            lastSelectedBaseKey = buildBaseKey(selected);
        }
        refreshFormBySelection();
    }

    private void toggleSingleHighlight() {
        RemoteTerminalBaseInfo selected = getSelectedBase();
        if (selected == null) {
            return;
        }
        String selectedKey = buildBaseKey(selected);
        boolean enabled = RemoteTerminalClientHooks.toggleHighlightedBaseKey(selectedKey);
        statusMessage = Component.translatable(enabled
                ? "gui.advanced_turret.remote_terminal.highlight_single_on"
                : "gui.advanced_turret.remote_terminal.highlight_single_off").getString();
        refreshToggleButtons();
    }

    private void togglePinSelected() {
        RemoteTerminalBaseInfo selected = getSelectedBase();
        if (selected == null) {
            return;
        }
        String selectedKey = buildBaseKey(selected);
        boolean pinnedNow = RemoteTerminalClientHooks.togglePinnedBaseKey(selectedKey);
        statusMessage = Component.translatable(pinnedNow
                ? "gui.advanced_turret.remote_terminal.pinned"
                : "gui.advanced_turret.remote_terminal.unpinned").getString();
        rebuildFilteredEntries();
        refreshFormBySelection();
    }

    private void openListConfig() {
        RemoteTerminalBaseInfo selected = getSelectedBase();
        if (selected == null) {
            return;
        }
        if (!selected.loaded()) {
            statusMessage = Component.translatable("gui.advanced_turret.remote_terminal.op_fail_unloaded").getString();
            return;
        }
        if (!selected.hasSmartChip()) {
            statusMessage = Component.translatable("gui.advanced_turret.remote_terminal.op_fail_no_chip").getString();
            return;
        }

        if (this.minecraft != null) {
            this.minecraft.setScreen(new RemoteTerminalListConfigScreen(
                    this,
                    selected,
                    getBlacklistCsv(selected),
                    getWhitelistCsv(selected)
            ));
        }
    }

    private void rebuildFilteredEntries() {
        filteredIndices.clear();
        String keyword = searchInput == null ? "" : searchInput.getValue().trim().toLowerCase(Locale.ROOT);

        for (int i = 0; i < baseEntries.size(); i++) {
            RemoteTerminalBaseInfo entry = baseEntries.get(i);
            if (keyword.isEmpty() || matchesKeyword(entry, keyword)) {
                filteredIndices.add(i);
            }
        }

        filteredIndices.sort((a, b) -> {
            String keyA = buildBaseKey(baseEntries.get(a));
            String keyB = buildBaseKey(baseEntries.get(b));
            boolean pinnedA = RemoteTerminalClientHooks.isPinned(keyA);
            boolean pinnedB = RemoteTerminalClientHooks.isPinned(keyB);
            return Boolean.compare(!pinnedA, !pinnedB);
        });

        int totalPages = getTotalPages();
        pageIndex = Math.max(0, Math.min(pageIndex, totalPages - 1));
        lastPageIndex = pageIndex;

        // 如果搜索过滤掉了当前选中项，不重置为 0，保持或清除选中
    }

    private boolean matchesKeyword(RemoteTerminalBaseInfo entry, String keyword) {
        String baseName = entry.baseName().toLowerCase(Locale.ROOT);
        String dimension = entry.dimensionId().toLowerCase(Locale.ROOT);
        String pos = entry.pos().getX() + "," + entry.pos().getY() + "," + entry.pos().getZ();
        return baseName.contains(keyword) || dimension.contains(keyword) || pos.contains(keyword);
    }


    private void refreshFormBySelection() {
        RemoteTerminalBaseInfo selected = getSelectedBase();
        boolean hasSelection = selected != null;
        boolean editableChip = hasSelection && selected.loaded() && selected.hasSmartChip();
        boolean editableFaces = hasSelection && selected.loaded();

        if (hasSelection) {
            applyFaceMask(selected.enabledFacesMask());
        } else {
            applyFaceMask((byte) 0b111111);
        }

        applyButton.active = editableChip;
        openListConfigButton.active = editableChip;

        for (TechCheckbox checkbox : faceCheckboxes) {
            checkbox.active = editableFaces;
        }

        pagePrevButton.active = pageIndex > 0;
        pageNextButton.active = pageIndex < getTotalPages() - 1;

        refreshRowButtons();
        refreshToggleButtons();
    }

    private void refreshToggleButtons() {
        boolean highlighting = RemoteTerminalClientHooks.isHighlightEnabled();
        highlightToggleButton.setMessage(Component.translatable(highlighting
                ? "gui.advanced_turret.remote_terminal.highlight_disable"
                : "gui.advanced_turret.remote_terminal.highlight_enable"));

        RemoteTerminalBaseInfo selected = getSelectedBase();
        boolean hasSelection = selected != null;
        if (!hasSelection) {
            highlightSingleButton.active = false;
            highlightSingleButton.setMessage(Component.translatable("gui.advanced_turret.remote_terminal.highlight_single"));
            pinTopButton.active = false;
            pinTopButton.setMessage(Component.translatable("gui.advanced_turret.remote_terminal.pin_top"));
            return;
        }

        String selectedKey = buildBaseKey(selected);

        highlightSingleButton.active = true;
        boolean singleHighlighted = selectedKey.equals(RemoteTerminalClientHooks.getHighlightedBaseKey());
        highlightSingleButton.setMessage(Component.translatable(singleHighlighted
                ? "gui.advanced_turret.remote_terminal.unhighlight_single"
                : "gui.advanced_turret.remote_terminal.highlight_single"));

        pinTopButton.active = true;
        boolean pinned = RemoteTerminalClientHooks.isPinned(selectedKey);
        pinTopButton.setMessage(Component.translatable(pinned
                ? "gui.advanced_turret.remote_terminal.unpin_top"
                : "gui.advanced_turret.remote_terminal.pin_top"));
    }

    private void refreshRowButtons() {
        int start = pageIndex * PAGE_SIZE;
        for (int i = 0; i < rowButtons.size(); i++) {
            Button button = rowButtons.get(i);
            int idxInFiltered = start + i;
            if (idxInFiltered >= 0 && idxInFiltered < filteredIndices.size()) {
                int realIndex = filteredIndices.get(idxInFiltered);
                RemoteTerminalBaseInfo entry = baseEntries.get(realIndex);
                button.visible = true;
                button.active = true;
                
                String key = buildBaseKey(entry);
                boolean isPinned = RemoteTerminalClientHooks.isPinned(key);
                boolean isSelected = realIndex == selectedIndex;
                
                if (button instanceof TechButton techBtn) {
                    techBtn.setSelected(isSelected);
                }

                String prefix = isSelected ? "▶ " : (isPinned ? "★ " : "  ");
                String baseName = entry.baseName();
                String label = prefix + (realIndex + 1) + ". " + baseName + " [T" + entry.tier() + "]";
                
                if (isPinned) {
                    button.setMessage(Component.literal(label).withStyle(style -> style.withColor(TurretUiTheme.COLOR_WARN)));
                } else {
                    button.setMessage(Component.literal(label));
                }
            } else {
                button.visible = false;
                button.active = false;
            }
        }
    }

    private void sendApply() {
        RemoteTerminalBaseInfo selected = getSelectedBase();
        if (selected == null) {
            return;
        }
        if (!selected.loaded()) {
            statusMessage = Component.translatable("gui.advanced_turret.remote_terminal.op_fail_unloaded").getString();
            return;
        }
        if (!selected.hasSmartChip()) {
            statusMessage = Component.translatable("gui.advanced_turret.remote_terminal.op_fail_no_chip").getString();
            return;
        }

        sendApplyForBase(selected, getBlacklistCsv(selected), getWhitelistCsv(selected), collectFaceMask());
    }

    private void sendApplyForBase(RemoteTerminalBaseInfo base, String blacklistCsv, String whitelistCsv, byte enabledFacesMask) {
        List<String> blacklist = parseCommaSeparated(blacklistCsv);
        List<String> whitelist = parseCommaSeparated(whitelistCsv);

        ModNetwork.CHANNEL.sendToServer(new RemoteTerminalApplyPacket(
                base.dimensionId(),
                base.pos(),
                blacklist,
                whitelist,
                enabledFacesMask
        ));
        statusMessage = Component.translatable("gui.advanced_turret.remote_terminal.applying").getString();
    }

    private byte collectFaceMask() {
        byte mask = 0;
        for (int i = 0; i < FACE_ORDER.length; i++) {
            if (faceCheckboxes.get(i).selected()) {
                mask |= (byte) (1 << FACE_ORDER[i].get3DDataValue());
            }
        }
        return mask;
    }

    private void applyFaceMask(byte mask) {
        for (int i = 0; i < FACE_ORDER.length; i++) {
            boolean enabled = (mask & (1 << FACE_ORDER[i].get3DDataValue())) != 0;
            faceCheckboxes.get(i).setChecked(enabled);
        }
    }

    private List<String> parseCommaSeparated(String text) {
        return Arrays.stream(text.split("[,\\r\\n]+"))

                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    private RemoteTerminalBaseInfo getSelectedBase() {
        if (baseEntries.isEmpty() || selectedIndex < 0 || selectedIndex >= baseEntries.size()) {
            return null;
        }
        if (filteredIndices.isEmpty()) {
            return baseEntries.get(selectedIndex);
        }
        if (!filteredIndices.contains(selectedIndex)) {
            return null; // 不匹配时不要自动选中过滤结果的第一项
        }
        return baseEntries.get(selectedIndex);
    }


    private String getBlacklistCsv(RemoteTerminalBaseInfo base) {
        ListDraft draft = listDraftByBaseKey.get(buildBaseKey(base));
        if (draft != null) {
            return draft.blacklistCsv;
        }
        return String.join(",", base.blacklist());
    }

    private String getWhitelistCsv(RemoteTerminalBaseInfo base) {
        ListDraft draft = listDraftByBaseKey.get(buildBaseKey(base));
        if (draft != null) {
            return draft.whitelistCsv;
        }
        return String.join(",", base.whitelist());
    }

    private void pruneDrafts() {
        if (listDraftByBaseKey.isEmpty()) {
            return;
        }
        List<String> aliveKeys = baseEntries.stream().map(RemoteTerminalScreen::buildBaseKey).toList();
        listDraftByBaseKey.keySet().removeIf(key -> !aliveKeys.contains(key));
    }

    private int getTotalPages() {
        int size = filteredIndices.size();
        int pages = (size + PAGE_SIZE - 1) / PAGE_SIZE;
        return Math.max(1, pages);
    }

    private static String buildBaseKey(RemoteTerminalBaseInfo info) {
        return info.dimensionId() + "|" + info.pos().asLong();
    }

    private static String directionLangKey(Direction direction) {
        return switch (direction) {
            case UP -> "gui.advanced_turret.remote_terminal.face.up";
            case DOWN -> "gui.advanced_turret.remote_terminal.face.down";
            case NORTH -> "gui.advanced_turret.remote_terminal.face.north";
            case SOUTH -> "gui.advanced_turret.remote_terminal.face.south";
            case WEST -> "gui.advanced_turret.remote_terminal.face.west";
            case EAST -> "gui.advanced_turret.remote_terminal.face.east";
        };
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);

        TurretUiTheme.drawPanel(guiGraphics, panelX, panelY, PANEL_W, PANEL_H);

        TurretUiTheme.drawSection(guiGraphics, panelX + 10, panelY + 30, 198, 178);
        TurretUiTheme.drawSection(guiGraphics, panelX + 214, panelY + 30, 210, 114);
        TurretUiTheme.drawSection(guiGraphics, panelX + 214, panelY + 150, 210, 58);

        guiGraphics.drawCenteredString(this.font, this.title, panelX + PANEL_W / 2, panelY + 10, TurretUiTheme.COLOR_TEXT);
        guiGraphics.drawString(this.font, Component.translatable("gui.advanced_turret.remote_terminal.base_list"), panelX + 14, panelY + 20, TurretUiTheme.COLOR_TEXT_SUB, false);

        int totalPages = getTotalPages();
        guiGraphics.drawCenteredString(this.font,
                Component.literal((pageIndex + 1) + " / " + totalPages),
                panelX + 104,
                panelY + 187,
                TurretUiTheme.COLOR_TEXT_SUB);

        RemoteTerminalBaseInfo selected = getSelectedBase();
        if (selected == null) {
            Component emptyText = baseEntries.isEmpty()
                    ? Component.translatable("gui.advanced_turret.remote_terminal.no_base")
                    : Component.translatable("gui.advanced_turret.remote_terminal.no_match");
            guiGraphics.drawString(this.font, emptyText, panelX + 218, panelY + 86, TurretUiTheme.COLOR_WARN, false);
        } else {
            Component baseName = Component.literal(selected.baseName());
            Component baseLabel = Component.translatable("gui.advanced_turret.remote_terminal.base_entry",
                    selectedIndex + 1,
                    baseEntries.size(),
                    baseName,
                    selected.tier());
            Component dimension = Component.translatable("gui.advanced_turret.remote_terminal.dimension", selected.dimensionId());
            String posText = formatPos(selected.pos());

        guiGraphics.drawString(this.font, baseLabel, panelX + 218, panelY + 84, TurretUiTheme.COLOR_TEXT, false);
        guiGraphics.drawString(this.font, dimension, panelX + 218, panelY + 96, TurretUiTheme.COLOR_TEXT_SUB, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.advanced_turret.remote_terminal.position", posText), panelX + 218, panelY + 108, TurretUiTheme.COLOR_ACCENT, false);

        Component loadState = selected.loaded()
                ? Component.translatable("gui.advanced_turret.remote_terminal.loaded")
                : Component.translatable("gui.advanced_turret.remote_terminal.unloaded");
        int loadColor = selected.loaded() ? TurretUiTheme.COLOR_OK : TurretUiTheme.COLOR_WARN;
        guiGraphics.drawString(this.font, loadState, panelX + 218, panelY + 120, loadColor, false);

        Component chipState = selected.hasSmartChip()
                ? Component.translatable("gui.advanced_turret.remote_terminal.chip_ready")
                : Component.translatable("gui.advanced_turret.remote_terminal.no_chip");
        int chipColor = selected.hasSmartChip() ? TurretUiTheme.COLOR_OK : TurretUiTheme.COLOR_WARN;
        guiGraphics.drawString(this.font, chipState, panelX + 218, panelY + 132, chipColor, false);
        }

        guiGraphics.drawString(this.font, Component.translatable("gui.advanced_turret.remote_terminal.face_config"), panelX + 218, panelY + 156, TurretUiTheme.COLOR_TEXT_SUB, false);

        if (!statusMessage.isEmpty()) {
            guiGraphics.drawString(this.font, Component.literal(statusMessage), panelX + 120, panelY + PANEL_H - 23, TurretUiTheme.COLOR_TEXT_SUB, false);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private String formatPos(BlockPos pos) {
        return "[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]";
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean clickedSearch = searchInput != null && searchInput.isMouseOver(mouseX, mouseY);
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (!clickedSearch && searchInput != null) {
            searchInput.setFocused(false);
        }
        return handled;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }


    private static final class ListDraft {
        private final String blacklistCsv;
        private final String whitelistCsv;

        private ListDraft(String blacklistCsv, String whitelistCsv) {
            this.blacklistCsv = blacklistCsv == null ? "" : blacklistCsv;
            this.whitelistCsv = whitelistCsv == null ? "" : whitelistCsv;
        }
    }
}
