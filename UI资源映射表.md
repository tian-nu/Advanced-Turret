# UI资源映射表.md

## TurretScreen
- Texture:
  - `textures/gui/container/turret_base.png`（主背景 + 能量条纹理区）
  - `textures/gui/container/personal_config_button.png`（个人配置按钮）
- Widgets:
  - `ImageButton` 个人配置入口 -> `TurretPersonalConfigScreen`
  - `Button` 智能配置入口 -> `SmartChipConfigScreen`
  - `Button` 面配置入口 -> `TurretFaceConfigScreen`（发包打开）
- Slot Regions（由 `menu.slots` 自动计算）:
  - 弹药槽(0~8)
  - 插件槽(9~9+pluginCount-1)
  - 玩家背包(最后36槽中的前27)
  - 快捷栏(最后36槽中的后9)

## TurretFaceConfigScreen
- Texture:
  - `textures/gui/container/turret_base.png`
- Widgets:
  - 无额外按钮（容器槽位交互为主）
- Slot Regions（由 `menu.slots` 自动计算）:
  - 升级槽(0~17，按 `slot.isActive()` 过滤显示)
  - 玩家背包(最后36槽中的前27)
  - 快捷栏(最后36槽中的后9)

## SmartChipConfigScreen
- Texture:
  - 无独立贴图（统一主题绘制）
- Widgets:
  - 目标类型复选框：敌对/中立/友善/玩家
  - 行为开关：友伤/预判/厉行节约
  - 作用面按钮：U/D/N/S/W/E
  - 黑白名单输入框 + 保存按钮

## TurretPersonalConfigScreen
- Texture:
  - 无独立贴图（已移除错误引用 `personal_config.png`）
- Widgets:
  - 背景透明度输入
  - 能量条透明度输入
  - 自动装填开关（界面层）
  - 确认/重置/取消按钮

## 备注
- 所有“槽位框/分区框”统一通过 `TurretUiTheme` 绘制。
- 主界面与面配置界面的分区边界均由 `menu.slots` 坐标推导，避免硬编码像素偏移。
