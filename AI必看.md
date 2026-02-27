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

### 7. 炮塔发射位置偏移
**问题**: 子弹发射位置比炮塔实际位置偏高
**原因**:
- `pos` 已经是炮塔方块位置（炮塔可放置在基座6个面上）
- 原代码用 `pos.getCenter()` 再加 `facing方向*0.6`
- 对于朝上的炮塔：`y+0.5 + 0.6 = y+1.1`，比模型顶部（y+0.5）高出0.6
**解决**: 根据朝向分别计算炮口位置
```java
// 对于上下朝向：炮口在模型边缘向外延伸
if (facing == Direction.UP) {
    center = new Vec3(center.x, pos.getY() + 0.5 + outwardOffset, center.z);
} else if (facing == Direction.DOWN) {
    center = new Vec3(center.x, pos.getY() + 0.5 - outwardOffset, center.z);
} else {
    // 水平朝向：正常向面向方向延伸
    Vec3 outward = new Vec3(facing.getStepX(), 0, facing.getStepZ()).scale(outwardOffset);
    center = center.add(outward);
}
```

### 8. ItemStackHandler槽位越界
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

### 9. 子弹碰撞基座方块问题
**问题**: 特定角度下子弹穿过基座攻击目标
**原因**: 
- 视线检测起点在炮塔内部，可能击中炮塔自身后返回 true，忽略了后面的基座阻挡
- 子弹前几tick忽略碰撞，可能穿过基座
**解决**: 
1. 视线检测从炮塔外部开始（偏移0.6），避免击中炮塔自身
2. 如果击中基座，返回 false
```java
// canSeePoint 方法
Vec3 adjustedStart = start.add(outward.scale(0.6)); // 从炮塔外部开始

if (hitResult.getType() == HitResult.Type.MISS) return true;

BlockPos hitPos = hitResult.getBlockPos();
BlockPos basePos = pos.relative(facing.getOpposite());
if (hitPos.equals(basePos)) return false; // 被基座阻挡

return false; // 被其他方块阻挡
```
3. 子弹碰撞检测：基座不跳过
```java
// 只有炮塔自身方块可以跳过，基座方块必须触发碰撞
if (ignoreSourceBlockTicks > 0 && hitBlockPos.equals(sourcePos)) {
    // 检查是否是基座，基座不跳过
    if (basePos != null && hitBlockPos.equals(basePos)) {
        this.onHit(hitResult); // 击中基座，销毁
        return;
    }
    ignoreSourceBlockTicks--;
    return; // 跳过炮塔自身
}
```
**关键**: 
- `sourcePos` = 炮塔方块位置
- `basePos` = 炮塔位置 + 炮塔朝向的反方向 = 基座位置

### 10. 高速子弹碰撞检测问题
**问题**: 磁轨炮子弹（速度6.0）在特定角度下没有伤害
**原因**: 
- `ProjectileUtil.getHitResultOnMoveVector` 只返回一个碰撞结果
- 高速子弹在发射初期，边缘可能先碰到附近的方块边缘
- 方块碰撞优先级高于实体碰撞，导致子弹被方块销毁而错过实体
**解决**: 分离碰撞检测，优先检测实体
```java
// 1. 先用 ProjectileUtil.getEntityHitResult 检测实体碰撞
EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
    level, this, currentPos, nextPos, 
    boundingBox.expandTowards(movement).inflate(1.0),
    this::canHitEntity
);

// 2. 如果击中实体，处理实体碰撞
if (entityHit != null && entityHit.getType() == HitResult.Type.ENTITY) {
    this.onHit(entityHit);
    return;
}

// 3. 再用 level.clip 检测方块碰撞
BlockHitResult blockHit = level.clip(new ClipContext(...));

// 4. 特殊处理：基座销毁、炮塔自身跳过、高速子弹前几tick忽略方块
```
**关键**: 所有子弹子类都必须覆盖 tick() 方法，使用分离碰撞检测

### 11. EnergyStorage.receiveEnergy 的 maxReceive 限制
**问题**: 太阳能/红石转化插件只能充1000FE，无法充更多
**原因**: 
- `EnergyStorage.receiveEnergy(maxReceive, simulate)` 返回值受内部 `this.maxReceive` 限制
- 构造 `EnergyStorage(capacity, maxReceive, maxExtract)` 时设置的 `maxReceive=1000`
- 即使调用 `receiveEnergy(18000, false)`，实际也只能充入1000
**解决**: 使用内部方法直接设置能量值，绕过限制
```java
// 错误写法：受maxReceive限制
energyStorage.receiveEnergy(18000, false);  // 只能充入1000！

// 正确写法：直接设置能量
private int addEnergyDirectly(int amount) {
    int maxEnergy = energyStorage.getMaxEnergyStored();
    int currentEnergy = energyStorage.getEnergyStored();
    int space = maxEnergy - currentEnergy;
    int toAdd = Math.min(amount, space);
    if (toAdd > 0) {
        energyStorage.setEnergyStored(currentEnergy + toAdd);  // 内部方法
        setChanged();
        syncToClient();
    }
    return toAdd;
}
```
**影响**: 
- 太阳能发电
- 红石转化（红石2000FE，红石块18000FE）
- 创造能量组件（直接满电）
- 任何需要单次充入大量能量的场景

### 12. canSeeSky 检测位置问题
**问题**: 太阳能插件在基座上方有炮塔时无法发电
**原因**: `level.canSeeSky(pos)` 检查的是基座位置，但基座上方可能有炮塔遮挡
**解决**: 检查基座上方一格是否能看到天空
```java
// 错误写法：检查基座位置
boolean canSeeSky = level.canSeeSky(pos);  // 炮塔在上方时永远false

// 正确写法：检查上方一格
boolean canSeeSky = level.canSeeSky(pos.above());  // 炮塔高度以上可以看到天空
```

---

## 📐 子弹系统设计

### 架构概览
```
TurretProjectileEntity (抽象基类)
├── TurretBulletEntity (机枪子弹 - 击中即销毁)
└── RailgunBulletEntity (磁轨炮子弹 - 穿透多目标)
```

### 基类属性 (TurretProjectileEntity)
| 属性 | 类型 | 说明 |
|------|------|------|
| damage | float | 子弹伤害（通过EntityData同步） |
| lifetime | int | 生命周期(tick)，默认100 |
| sourcePos | BlockPos | 炮塔方块位置（用于跳过自身碰撞） |
| basePos | BlockPos | 基座方块位置（基座必须销毁子弹） |
| ignoreSourceBlockTicks | int | 跳过炮塔自身碰撞的tick数，默认2 |

### 机枪子弹 (TurretBulletEntity)
- **行为**: 击中实体后销毁，击中方块后销毁
- **碰撞检测**: 分离检测，实体优先
- **特殊处理**: 基座销毁，炮塔自身跳过

### 磁轨炮子弹 (RailgunBulletEntity)
- **行为**: 穿透多个目标，击中方块后销毁
- **穿透次数**: 默认3次 (penetrationCount)
- **额外属性**:
  - `hitEntities`: Set<Integer> 已击中的实体ID，防止重复伤害
  - `ignoreBlockCollisionTicks`: int 前3tick忽略方块碰撞，避免高速子弹边缘碰撞问题
- **碰撞检测**: 分离检测，实体优先，穿透后继续飞行

### 炮塔参数对照表
| 参数 | 机枪炮塔 | 磁轨炮塔 |
|------|----------|----------|
| FIRE_RATE | 5 tick | 30 tick |
| SEARCH_RADIUS | 16.0 | 24.0 |
| BULLET_SPEED | 3.0 | 6.0 |
| BULLET_DAMAGE | 4.0F | 12.0F |
| 子弹类型 | TurretBulletEntity | RailgunBulletEntity |
| 穿透次数 | - | 3 |

### 发射流程
```java
// 1. 计算炮口位置（方块中心）
Vec3 muzzlePos = calculateMuzzlePosition(pos, facing); // 返回 pos.getCenter()

// 2. 计算目标位置（实体中心偏上）
Vec3 targetPos = target.position().add(0, target.getEyeHeight() * 0.5, 0);

// 3. 预判瞄准（如果启用）
if (base.isPredictiveAiming()) {
    double time = muzzlePos.distanceTo(targetPos) / BULLET_SPEED;
    targetPos = targetPos.add(target.getDeltaMovement().scale(time));
}

// 4. 创建子弹
BulletEntity bullet = new BulletEntity(level, muzzlePos.x, muzzlePos.y, muzzlePos.z, damage);
bullet.setOwner(null);
bullet.setSourcePos(pos);                              // 炮塔位置
bullet.setBasePos(pos.relative(facing.getOpposite())); // 基座位置
bullet.shoot(direction, (float) BULLET_SPEED);

// 5. 生成子弹
level.addFreshEntity(bullet);
```

### 视线检测 (canSeePoint)
```java
// 1. 从炮塔外部开始检测（偏移0.6），避免击中炮塔自身
Vec3 adjustedStart = start.add(outward.scale(0.6));

// 2. 如果被基座阻挡，返回false（不锁定目标）
BlockPos basePos = pos.relative(facing.getOpposite());
if (hitPos.equals(basePos)) return false;

// 3. 如果击中其他方块，返回false
return false;
```

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

## 🔌 插件系统

### 插件列表
| 插件 | 注册名 | 功能 | 配置项 |
|------|--------|------|--------|
| 创造能量组件 | CREATIVE_POWER_COMPONENT | 直接满电（非1000/t充电） | - |
| 太阳能插件 | SOLAR_PLUGIN | 白天露天发电 | `solarEnergyGeneration` (10 FE/t) |
| 弹药回收插件 | AMMO_RECYCLING_PLUGIN | 攻击时20%概率不消耗弹药 | `ammoRecycleChance` (20%) |
| 红石转化插件 | REDSTONE_CONVERSION_PLUGIN | 弹药槽红石/红石块转能量 | 红石2000FE/个，红石块18000FE/个 |
| 破坏插件 | DESTRUCTION_PLUGIN | 爆炸破坏方块 | - |
| 智能芯片 | SMART_CHIP | 目标配置、友伤保护、预判瞄准 | NBT存储 |

### 插件检查方法
```java
// TurretBaseBlockEntity 中提供的方法
public boolean hasSolarPlugin();           // 太阳能插件
public boolean hasAmmoRecyclingPlugin();   // 弹药回收插件
public boolean hasRedstoneConversionPlugin(); // 红石转化插件
public boolean hasDestructionPlugin();     // 破坏插件
public boolean hasCreativePowerComponent(); // 创造能量组件
```

### 插件功能实现位置
- **创造能量组件**: `TurretBaseBlockEntity.setEnergyFull()` 直接设置满电
- **太阳能插件**: 白天（0-12000 tick）+ 露天（`canSeeSky`）
- **弹药回收**: `MachineGunTurretBlockEntity.shoot()` 中，攻击前检查，20%概率跳过弹药消耗
- **红石转化**: 弹药槽消耗，优先红石块(18000FE)，再红石粉(2000FE)
- **破坏插件**: 预留给爆炸类子弹使用（火箭/导弹等）

---

## 🔫 弹药系统

### 机枪子弹
- **物品**: `ModItems.MACHINE_GUN_BULLET`
- **贴图**: 复用 `turret_bullet.png`
- **堆叠**: 64个/组
- **消耗**: 机枪每次射击消耗1个子弹

### 机枪发射流程
```java
// 1. 检查弹药
if (!hasAmmo(base)) return;

// 2. 消耗弹药
consumeAmmo(base);

// 3. 消耗能量
base.consumeEnergy(energyCost);

// 4. 生成子弹实体
level.addFreshEntity(bullet);
```

### 弹药检查方法
```java
// MachineGunTurretBlockEntity 中
private boolean hasAmmo(TurretBaseBlockEntity base) {
    IItemHandler ammoInv = base.getAmmoInventory();
    for (int i = 0; i < ammoInv.getSlots(); i++) {
        ItemStack stack = ammoInv.getStackInSlot(i);
        if (!stack.isEmpty() && stack.is(ModItems.MACHINE_GUN_BULLET.get())) {
            return true;
        }
    }
    return false;
}
```

---

## 🎯 待实现功能优先级

1. **高优先级**
   - ✅ T4/T5双功能槽支持
   - ✅ 无电量低头动画
   - ✅ 插件功能完整实现
   - 各类型子弹实体（火箭/导弹/榴弹等）

2. **中优先级**
   - 激光/火箭/导弹/榴弹炮塔
   - 垃圾炮塔

3. **低优先级**
   - 立场炮塔（相位/谐振）
   - 精度组件效果

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
