# AI必看 - 开发关键要点

## 🔧 核心架构

### 项目结构
```
com.tian_nu.AdvancedTurret/
├── TurretMod.java              # 主类，注册所有DeferredRegister
├── blocks/
│   ├── TurretBaseBlock.java    # 基座方块，放置逻辑
│   ├── MachineGunTurretBlock.java
│   └── entitys/
│       └── TurretBaseBlockEntity.java  # 核心：能量/物品/配置管理
├── items/
│   └── SmartChipItem.java      # 智能芯片，NBT配置存储
├── gui/
│   ├── TurretScreen.java       # 主GUI
│   └── TurretFaceConfigScreen.java  # 面配置GUI
└── network/
    └── ModNetwork.java         # 网络包
```

---

## ⚠️ 踩过的坑

### 1. 无敌帧问题
**问题**: 多炮塔同时攻击时伤害丢失
**解决**: 子弹击中时先清除目标无敌帧
```java
livingEntity.invulnerableTime = 0;
livingEntity.hurtTime = 0;
livingEntity.hurt(damageSource, damage);
```

### 2. 子弹自撞
**问题**: 子弹发射后立即撞到炮塔自身方块
**解决**: 子弹前2tick忽略炮塔源方块碰撞
```java
if (hitResult.getBlockPos().equals(sourcePos) && ignoreSourceBlockTicks > 0) {
    ignoreSourceBlockTicks--;
    return; // 跳过碰撞
}
```

### 3. 紫黑块贴图
**问题**: 破坏方块时显示紫黑块
**解决**: 在blockstate JSON中指定 `particle` 纹理
```json
"particle": "advanced_turret:block/gun_turret"
```

### 4. GeckoLib动画同步
**问题**: 客户端不显示瞄准动画
**解决**: 使用 `SerializableDataTicket` 同步目标位置
```java
// 在TurretMod中注册
HAS_TARGET = GeckoLibUtil.addDataTicket(SerializableDataTicket.ofBoolean(location("has_target")));
// 在BlockEntity中设置
setAnimData(HAS_TARGET, true);
setAnimData(TARGET_POS_X, targetPos.x);
```

### 5. 槽位索引计算
**问题**: 面配置槽位索引混乱
**解决**: 统一索引公式
```java
// 6面各3槽，共18槽
int slotIndex = faceIndex * 3 + upgradeIndex;
// faceIndex: DOWN=0, UP=1, NORTH=2, SOUTH=3, WEST=4, EAST=5
```

### 6. 旧存档兼容
**问题**: 旧版本存档无法加载
**解决**: 在 `load()` 中添加迁移逻辑
```java
if (tag.contains("UpgradeSlots")) {
    // 迁移到DOWN面
    ItemStackHandler legacy = new ItemStackHandler(2);
    legacy.deserializeNBT(tag.getCompound("UpgradeSlots"));
    // 复制到新槽位...
}
```

### 7. ItemStackHandler槽位越界
**问题**: 修改 `ItemStackHandler` 的大小后，旧存档中的handler仍是旧大小，访问新槽位时崩溃
```
java.lang.RuntimeException: Slot 1 not in valid range - [0,1)
```
**原因**: `ItemStackHandler` 在创建时确定大小，NBT加载不会改变已创建的handler大小
**解决**: 访问槽位时使用 `Math.min(理论槽数量, handler.getSlots())` 确保不越界
```java
// 错误写法：直接用理论槽数量
int slotCount = getPluginSlotCount();  // T5返回2
for (int i = 0; i < slotCount; i++) {
    basePluginSlot.getStackInSlot(i);  // 旧存档只有1个槽，访问槽位1崩溃！
}

// 正确写法：使用实际槽数量
int slotCount = Math.min(getPluginSlotCount(), basePluginSlot.getSlots());
for (int i = 0; i < slotCount; i++) {
    basePluginSlot.getStackInSlot(i);  // 安全！
}
```
**影响范围**: 所有访问动态大小 `ItemStackHandler` 的地方：
- `TurretBaseBlockEntity`: `hasCreativePowerComponent()`, `getPluginStack()`, `getAllPluginStacks()`
- `TurretMenu`: 构造函数, `addPluginSlots()`, `quickMoveStack()`
- `TurretScreen`: 渲染方法

---

## 📌 关键实现

### 基座等级判断
```java
public int getTier() {
    BlockState state = getBlockState();
    if (state.is(ModBlocks.TURRET_BASE_T5.get())) return 5;
    if (state.is(ModBlocks.TURRET_BASE_T4.get())) return 4;
    // ...
}
```

### 升级组件效果计算
```java
// 每面独立计算
public float getDamageForFace(Direction face, float baseDamage) {
    int count = countUpgradeItems(face, ModItems.ATTACK_BOOST_COMPONENT.get());
    return (float)(baseDamage * Math.min(3.0, 1.0 + count * 0.10));
}
```

### 目标识别流程
```java
1. 黑名单检查 → 强制攻击
2. 白名单检查 → 强制跳过
3. 目标模式匹配（flags位运算）
4. 友伤保护检查（主人/驯服生物）
5. 范围检查
6. 视线检测（三点：头/身/脚）
```

### 插件配置存储
**关键**: 所有配置存储在插件物品的NBT中，基座动态读取
```java
// SmartChipItem
public static void setFriendlyFire(ItemStack stack, boolean enabled) {
    stack.getOrCreateTag().putBoolean(KEY_FRIENDLY_FIRE, enabled);
}
// TurretBaseBlockEntity
public boolean isFriendlyFire() {
    ItemStack stack = getPluginStack();
    return !stack.isEmpty() && SmartChipItem.isFriendlyFire(stack);
}
```

---

## 🎯 待实现功能优先级

1. **高优先级**
   - T4/T5双功能槽支持
   - 无电量低头动画
   - 各类型子弹实体

2. **中优先级**
   - 激光/火箭/导弹/榴弹炮塔
   - 破坏插件
   - 垃圾炮塔

3. **低优先级**
   - 立场炮塔（相位/谐振）
   - 太阳能/弹药回收/红石插件
   - 精度组件

---

## 🔍 常用调试

### 检查能量是否充足
```java
int energyCost = base.getEnergyCostForFace(facing, FIRE_RATE);
if (base.getEnergyStored() < energyCost) return;
```

### 检查面是否启用
```java
byte mask = SmartChipItem.getEnabledFaces(pluginStack);
boolean enabled = (mask & (1 << face.get3DDataValue())) != 0;
```

### 检查目标是否可见
```java
// 三点检测
Vec3[] points = {head, center, feet};
for (Vec3 point : points) {
    if (canSeePoint(start, point)) return true;
}
```

---

## 📚 参考文档

- 功能规格: `MOD_FEATURES.md`
- 项目计划: `project_plan.task`
- Forge文档: `forge文档/`
- 参考代码: `代码参考文档/`

---

**最后更新**: 2026-02-26
