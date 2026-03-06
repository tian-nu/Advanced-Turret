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

### 13. 方块模型贴图引用错误
**问题**: T4/T5基座放置后显示T3的贴图
**原因**: 模型JSON文件中贴图路径引用错误
```json
// 错误：turret_base_t4.json 和 turret_base_t5.json 都引用了
"all": "advanced_turret:block/turret_base_t3"
```
**解决**: 修改为正确的贴图路径
```json
// turret_base_t4.json
"all": "advanced_turret:block/turret_base_t4"
// turret_base_t5.json
"all": "advanced_turret:block/turret_base_t5"
```

### 14. 新物品未添加到创造模式标签页
**问题**: 火箭炮塔等新物品在创造模式物品栏找不到
**原因**: `ModCreativeModeTabs.java` 中未调用 `output.accept()` 添加物品
**解决**: 在创造模式标签页注册中添加所有新物品
```java
// ModCreativeModeTabs.java 中添加
output.accept(ModBlocks.ROCKET_TURRET.get());
```
**注意**: 每次新增方块/物品后，必须检查是否已添加到创造模式标签页

### 15. TurretMenu 缺少插件物品检查
**问题**: 破坏插件无法通过shift-click放入插件槽
**原因**: `TurretMenu.isPluginItem()` 方法缺少对 `DESTRUCTION_PLUGIN` 的检查
**解决**: 添加破坏插件到检查列表
```java
private boolean isPluginItem(ItemStack stack) {
    return stack.getItem() instanceof SmartChipItem
            || stack.is(ModItems.CREATIVE_POWER_COMPONENT.get())
            || stack.is(ModItems.SOLAR_PLUGIN.get())
            || stack.is(ModItems.AMMO_RECYCLING_PLUGIN.get())
            || stack.is(ModItems.REDSTONE_CONVERSION_PLUGIN.get())
            || stack.is(ModItems.DESTRUCTION_PLUGIN.get());  // 添加这一行
}
```
**注意**: 新增插件物品后，需同时更新 `TurretMenu.isPluginItem()` 和 `TurretBaseBlockEntity.isPluginItem()`

### 16. 新炮塔缺少GeckoLib数据票注册
**问题**: 放置新炮塔时游戏崩溃，报错 `SerializableDataTicket.id() is null`
**原因**: 炮塔BlockEntity中声明了 `SerializableDataTicket` 但未在 `TurretMod.registerDataTickets()` 中注册
**崩溃日志**:
```
java.lang.NullPointerException: Cannot invoke "software.bernie.geckolib.network.SerializableDataTicket.id()" because "this.dataTicket" is null
    at RocketTurretBlockEntity.tick(RocketTurretBlockEntity.java:118)
```
**解决**: 在 `TurretMod.java` 的 `registerDataTickets()` 方法中注册所有炮塔的数据票
```java
private void registerDataTickets() {
    // 机枪炮塔
    MachineGunTurretBlockEntity.HAS_TARGET = GeckoLibUtil.addDataTicket(
        SerializableDataTicket.ofBoolean(location("has_target")));
    // ... 其他数据票
    
    // 磁轨炮塔
    RailgunTurretBlockEntity.HAS_TARGET = GeckoLibUtil.addDataTicket(
        SerializableDataTicket.ofBoolean(location("railgun_has_target")));
    // ... 其他数据票
    
    // 火箭炮塔 (必须添加！)
    RocketTurretBlockEntity.HAS_TARGET = GeckoLibUtil.addDataTicket(
        SerializableDataTicket.ofBoolean(location("rocket_has_target")));
    RocketTurretBlockEntity.TARGET_POS_X = GeckoLibUtil.addDataTicket(
        SerializableDataTicket.ofDouble(location("rocket_target_pos_x")));
    RocketTurretBlockEntity.TARGET_POS_Y = GeckoLibUtil.addDataTicket(
        SerializableDataTicket.ofDouble(location("rocket_target_pos_y")));
    RocketTurretBlockEntity.TARGET_POS_Z = GeckoLibUtil.addDataTicket(
        SerializableDataTicket.ofDouble(location("rocket_target_pos_z")));
}
```
**注意**: 新增任何使用GeckoLib动画的炮塔时，必须同步在 `TurretMod.registerDataTickets()` 中注册数据票

### 17. 新炮塔缺少渲染器和GeoModel
**问题**: 放置新炮塔后模型不显示
**原因**: 新炮塔缺少渲染器和GeoModel类，且未在ClientEvents中注册
**解决**: 新增炮塔需要创建以下文件并注册：

1. **创建GeoModel类** (`client/models/XxxTurretGeoModel.java`)
```java
public class RocketTurretGeoModel extends GeoModel<RocketTurretBlockEntity> {
    @Override
    public ResourceLocation getModelResource(RocketTurretBlockEntity animatable) {
        return TurretMod.location("geo/block/rocket_turret.geo.json");
    }
    @Override
    public ResourceLocation getTextureResource(RocketTurretBlockEntity animatable) {
        return TurretMod.location("textures/block/rocket_turret.png");
    }
    @Override
    public ResourceLocation getAnimationResource(RocketTurretBlockEntity animatable) {
        return TurretMod.location("animations/block/rocket_turret.animation.json");
    }
    // ... 瞄准动画逻辑
}
```

2. **创建渲染器类** (`client/XxxTurretGeoRenderer.java`)
```java
public class RocketTurretGeoRenderer extends GeoBlockRenderer<RocketTurretBlockEntity> {
    public RocketTurretGeoRenderer(BlockEntityRendererProvider.Context context) {
        super(new RocketTurretGeoModel());
    }
    // ... rotateBlock方法处理不同朝向
}
```

3. **在ClientEvents.java中注册渲染器**
```java
@SubscribeEvent
public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
    event.registerBlockEntityRenderer(
        ModBlockEntities.ROCKET_TURRET.get(),
        RocketTurretGeoRenderer::new
    );
}
```

4. **在BlockEntity中添加旋转角度字段**
```java
public float yRot0 = 0.0f;  // yaw角度
public float xRot0 = 0.0f;  // pitch角度
```

**检查清单**: 新增炮塔时必须完成：
- [ ] `TurretMod.registerDataTickets()` - 注册GeckoLib数据票
- [ ] `XxxTurretGeoModel.java` - 创建GeoModel类
- [ ] `XxxTurretGeoRenderer.java` - 创建渲染器类
- [ ] `ClientEvents.registerBlockEntityRenderers()` - 注册渲染器
- [ ] `ModCreativeModeTabs` - 添加到创造模式标签页

### 18. 新炮塔缺少弹药检查和消耗
**问题**: 新炮塔放置后不攻击目标
**原因**: 炮塔的 `canShoot` 和 `shoot` 方法缺少弹药检查和消耗逻辑
**现象**: 
- 炮塔能找到目标并瞄准
- 但不会发射子弹
- 控制台无错误日志
**解决**: 添加弹药检查和消耗方法
```java
// 1. 在ModItems中添加弹药物品
public static final RegistryObject<Item> ROCKET =
    ITEMS.register("rocket", () -> new Item(new Item.Properties().stacksTo(64)));

// 2. 在炮塔BlockEntity中添加弹药检查
private boolean hasAmmo(TurretBaseBlockEntity base) {
    IItemHandler ammoInv = base.getAmmoInventory();
    for (int i = 0; i < ammoInv.getSlots(); i++) {
        ItemStack stack = ammoInv.getStackInSlot(i);
        if (!stack.isEmpty() && stack.is(ModItems.ROCKET.get())) {
            return true;
        }
    }
    return false;
}

// 3. 在炮塔BlockEntity中添加弹药消耗
private void consumeAmmo(TurretBaseBlockEntity base) {
    IItemHandler ammoInv = base.getAmmoInventory();
    for (int i = 0; i < ammoInv.getSlots(); i++) {
        ItemStack stack = ammoInv.getStackInSlot(i);
        if (!stack.isEmpty() && stack.is(ModItems.ROCKET.get())) {
            stack.shrink(1);
            return;
        }
    }
}

// 4. 在canShoot中检查弹药
private boolean canShoot(TurretBaseBlockEntity base) {
    // 检查能量
    if (base.getEnergyStored() < energyCost) return false;
    // 检查弹药
    return hasAmmo(base);
}

// 5. 在shoot中消耗弹药（支持弹药回收插件）
private void shoot(...) {
    // ... 发射逻辑
    
    // 消耗弹药
    if (base.hasAmmoRecyclingPlugin()) {
        if (base.getLevel().random.nextFloat() >= Config.ammoRecycleChance) {
            consumeAmmo(base);
        }
    } else {
        consumeAmmo(base);
    }
}
```
**注意**: 根据 `MOD_FEATURES.md`，不同炮塔使用不同弹药：
- 机枪: `MACHINE_GUN_BULLET`
- 磁轨炮: 专用磁轨（待实现）
- 激光: 无需弹药（能量驱动）
- 火箭/导弹/榴弹: 专用弹药物品

### 19. 基座hasTurretOnFace缺少新炮塔检测
**问题**: 新炮塔放置后不攻击，基座GUI看不到该面的升级槽
**原因**: `TurretBaseBlockEntity.hasTurretOnFace()` 方法没有检查新炮塔类型
**现象**:
- 炮塔能放置、能显示模型、能瞄准目标
- 但不发射子弹
- 基座GUI中该面的升级槽不显示
**根因分析**:
```java
// isFaceEnabled() 调用 hasTurretOnFace()
public boolean isFaceEnabled(Direction face) {
    if (!hasTurretOnFace(face)) return false;  // 返回false！
    return (getEnabledFacesMask() & (1 << face.get3DDataValue())) != 0;
}

// hasTurretOnFace() 没有检查 RocketTurretBlockEntity
public boolean hasTurretOnFace(Direction face) {
    // ...
    if (be instanceof MachineGunTurretBlockEntity turret) { ... }
    if (be instanceof RailgunTurretBlockEntity turret) { ... }
    // 缺少: if (be instanceof RocketTurretBlockEntity turret) { ... }
    return false;  // 永远返回false！
}
```
**解决**: 在 `hasTurretOnFace()` 方法中添加所有新炮塔的检测
```java
public boolean hasTurretOnFace(Direction face) {
    Level level = getLevel();
    if (level == null) return false;
    BlockPos turretPos = getBlockPos().relative(face);
    BlockEntity be = level.getBlockEntity(turretPos);
    
    if (be instanceof MachineGunTurretBlockEntity turret) {
        TurretBaseBlockEntity base = turret.getBaseEntity();
        return base != null && base.getBlockPos().equals(getBlockPos());
    }
    if (be instanceof RailgunTurretBlockEntity turret) {
        TurretBaseBlockEntity base = turret.getBaseEntity();
        return base != null && base.getBlockPos().equals(getBlockPos());
    }
    // 新炮塔必须添加！
    if (be instanceof RocketTurretBlockEntity turret) {
        TurretBaseBlockEntity base = turret.getBaseEntity();
        return base != null && base.getBlockPos().equals(getBlockPos());
    }
    return false;
}
```
**影响范围**: 所有新增炮塔都必须在 `hasTurretOnFace()` 中添加检测，否则：
1. `isFaceEnabled()` 返回 false
2. 炮塔tick中检查面启用状态失败，跳过攻击逻辑
3. 基座GUI不显示该面的升级槽

### 20. getSearchRadiusForFace 硬编码范围限制
**问题**: 火箭炮塔索敌范围定义48格，实际只有32格
**原因**: `TurretBaseBlockEntity.getSearchRadiusForFace()` 中硬编码了 `Math.min(32.0, ...)` 限制
**根因分析**:
```java
public double getSearchRadiusForFace(Direction face, double baseRadius) {
    int count = countUpgradeItems(face, ModItems.RANGE_COMPONENT.get());
    return Math.min(32.0, baseRadius + count);  // 最大只能32格！
}
```
**解决**: 修改范围限制逻辑，允许更大范围
```java
public double getSearchRadiusForFace(Direction face, double baseRadius) {
    int count = countUpgradeItems(face, ModItems.RANGE_COMPONENT.get());
    // 每个范围组件增加8格，上限为基础范围+32格
    return Math.min(baseRadius + 32.0, baseRadius + count * 8);
}
```
**注意**: 范围组件的加成应该与基础范围挂钩，不能硬编码固定上限

---

## 📐 子弹系统设计

### 架构概览
```
TurretProjectileEntity (抽象基类)
├── TurretBulletEntity (机枪子弹 - 击中即销毁)
├── RailgunBulletEntity (磁轨炮子弹 - 穿透多目标)
└── RocketEntity (火箭弹 - 爆炸范围伤害)
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

### 火箭弹 (RocketEntity)
- **行为**: 击中实体或方块后爆炸
- **伤害类型**:
  - 直击伤害 (directDamage): 直接命中目标
  - 爆炸伤害 (explosionDamage): 范围AOE，随距离衰减
- **爆炸半径**: 默认4格 (explosionRadius)
- **破坏插件**: `destroyBlocks` 字段控制是否破坏方块
- **视觉效果**: 烟雾尾迹粒子
- **碰撞检测**: 分离检测，实体优先，击中后爆炸

### 炮塔参数对照表
| 参数 | 机枪炮塔 | 磁轨炮塔 | 火箭炮塔 |
|------|----------|----------|----------|
| FIRE_RATE | 5 tick | 30 tick | 100 tick (5秒) |
| SEARCH_RADIUS | 16.0 | 24.0 | 48.0 |
| BULLET_SPEED | 3.0 | 6.0 | 2.0 |
| BULLET_DAMAGE | 4.0F | 12.0F | 10.0F (直击) |
| 爆炸伤害 | - | - | 10.0F |
| 爆炸半径 | - | - | 4.0F |
| 能量消耗 | 100 FE | 1000 FE | 5000 FE |
| 子弹类型 | TurretBulletEntity | RailgunBulletEntity | RocketEntity |
| 特殊能力 | - | 穿透3目标 | 爆炸范围伤害 |

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

## 📦 资源文件复用记录

以下文件是临时复用，后续需要制作专属资源：

### 火箭炮塔 (Rocket Turret)
| 文件类型 | 复用源 | 文件路径 |
|---------|--------|----------|
| Geo模型 | `machine_gun_turret.geo.json` | `geo/block/rocket_turret.geo.json` |
| 动画文件 | `machine_gun_turret.animation.json` | `animations/block/rocket_turret.animation.json` |
| 方块贴图 | `gun_turret.png` | `textures/block/rocket_turret.png` |
| 方块模型 | `models/block/machine_gun_turret.json` | `models/block/rocket_turret.json` |
| Blockstate | `blockstates/machine_gun_turret.json` | `blockstates/rocket_turret.json` |
| 物品模型 | `models/item/machine_gun_turret.json` | `models/item/rocket_turret.json` |

**待制作**: 火箭炮塔专属模型（可设计为多管火箭发射器造型）

---

### 20. 磁轨炮子弹穿透次数硬编码
**问题**: 磁轨炮无法配置穿透次数
**原因**: `RailgunBulletEntity` 中 `PENETRATION_COUNT` 硬编码为 3
**解决**: 可以通过升级组件控制，或保持硬编码

---

### 21. 导弹制导机制实现 🔥新增
**用途**: 导弹炮塔需要实时追踪目标移动
**关键参数**:
- `turnRate`: 转向速率 (0.3) - 控制导弹转向的平滑度
- `acceleration`: 加速度 (0.03) - 每tick速度增加
- `targetEntity`: 追踪的目标实体

**实现逻辑**:
```java
// 在 MissileEntity.tick() 中
@Override
public void tick() {
    super.tick();
    
    // 生命周期检查
    if (this.lifetime-- <= 0) {
        this.discard();
        return;
    }
    
    // 越飞越快
    Vec3 movement = this.getDeltaMovement();
    double currentSpeed = movement.length();
    if (currentSpeed > 0.001) {
        double newSpeed = currentSpeed * (1.0 + acceleration);
        // 或线性加速: newSpeed = currentSpeed + acceleration;
        Vec3 direction = movement.normalize();
        this.setDeltaMovement(direction.scale(newSpeed));
    }
    
    // 制导追踪（每tick调整朝向）
    if (targetEntity != null && targetEntity.isAlive()) {
        Vec3 currentDir = this.getDeltaMovement().normalize();
        Vec3 toTarget = targetEntity.position().subtract(this.position()).normalize();
        
        // 转向插值：新方向 = 当前方向 + (目标方向 - 当前方向) * 转向速率
        Vec3 newDir = currentDir.add(toTarget.subtract(currentDir).scale(turnRate)).normalize();
        
        // 应用新方向（保持当前速度）
        this.setDeltaMovement(newDir.scale(currentSpeed));
    }
    
    // 碰撞检测（分离检测，实体优先）
    // ... 参考 RocketEntity.tick()
}
```

**制导原理**:
1. 每tick计算导弹当前位置到目标位置的方向向量 `toTarget`
2. 使用插值公式 `newDir = currentDir + (toTarget - currentDir) * turnRate` 计算新方向
3. 转向速率 `turnRate` 控制转向灵敏度：
   - `turnRate = 0.01` → 转向很慢，几乎不追踪
   - `turnRate = 0.3` → 适中转向，能追踪移动目标
   - `turnRate = 1.0` → 瞬间转向，激光般追踪

**目标设置**:
```java
// 在 MissileTurretBlockEntity.shoot() 中创建导弹时
MissileEntity missile = new MissileEntity(level, muzzlePos.x, muzzlePos.y, muzzlePos.z, directDamage);
missile.setTargetEntity(target); // 设置追踪目标
missile.setTurnRate(TURN_RATE);
missile.setAcceleration(ACCELERATION);
// ...
```

**注意事项**:
- 目标死亡后（targetEntity.isAlive() == false）继续朝最后方向飞行
- 目标超出追踪范围继续飞行直到碰撞或生命周期结束
- 转向速率过高可能导致导弹绕圈子（方向计算问题）
- 分离碰撞检测必须先检测实体，再检测方块（避免目标被方块遮挡时无法追踪）

---

### 22. 导弹越飞越快机制 🔥新增
**用途**: 导弹发射后加速，增加打击距离和威力
**实现方式**（两种）:
```java
// 方式1：指数增长（推荐，火箭炮塔使用）
double newSpeed = currentSpeed * (1.0 + acceleration);
// acceleration = 0.047 → 20tick后速度从2.0增长到5.0

// 方式2：线性增长（更简单）
double newSpeed = currentSpeed + acceleration;
// acceleration = 0.1 → 每tick增加0.1速度
```

**参数选择**:
- `acceleration = 0.03` (指数) → 适合远距离制导
- 指数增长初期慢后期快，模拟真实导弹加速
- 线性增长恒定加速，更容易控制

---

### 23. 导弹炮塔开发完成 🚀新增
**完成日期**: 2026-03-05

**特点：**
- 制导追踪：导弹自动追踪目标移动（turnRate=0.3）
- 越飞越快：每tick速度线性增长（acceleration=0.03）
- 爆炸伤害：直击10 + 爆炸15（半径4格）
- 远程攻击：64格搜索范围
- 高能耗：10000 FE/发
- 低射速：6.7秒/发

**已创建文件：**
- Java文件（6个）：`MissileTurretBlock.java`, `MissileTurretBlockEntity.java`, `MissileEntity.java`, `MissileTurretGeoModel.java`, `MissileTurretGeoRenderer.java`, `MissileRenderer.java`
- 资源文件（9个）：模型、动画、贴图
- 修改文件（9个）：注册、翻译

**关键实现：**
```java
// 制导追踪（MissileEntity.tick()）
Vec3 currentDir = movement.normalize();
Vec3 toTarget = targetEntity.position().subtract(this.position()).normalize();
Vec3 newDir = currentDir.add(toTarget.subtract(currentDir).scale(turnRate)).normalize();
this.setDeltaMovement(newDir.scale(currentSpeed));

// 越飞越快
double newSpeed = currentSpeed + acceleration;
this.setDeltaMovement(movement.normalize().scale(newSpeed));
```

**与火箭炮塔对比：**
- 制导功能（独有）
- 线性加速度 vs 火箭的指数加速度
- 搜索范围 64 vs 48
- 爆炸伤害 15 vs 10
- 能量消耗 10000 vs 5000

**编译状态**: ✅ BUILD SUCCESSFUL

**详细文档**: 见 `导弹炮塔开发完成总结.md` 和 `导弹炮塔对比文档.md`

---

### 24. 厉行节约功能BUG：预约伤害覆盖问题 🔥修复
**问题**: 多炮塔攻击同一高血量目标时全部停火
**修复日期**: 2026-03-06

**原因**: 
`TurretBaseBlockEntity.reserveDamage()` 方法直接覆盖预约值，而不是累积
```java
// 错误写法（覆盖）
reservedDamage.put(entityId, damage);

// 正确写法（累积）
float existing = reservedDamage.getOrDefault(entityId, 0.0f);
float totalReserved = existing + damage;
reservedDamage.put(entityId, totalReserved);
```

**问题场景**:
假设100血僵尸，3个机枪炮塔（每个伤害4）：
1. 炮塔A锁定 → 预约4
2. 炮塔B锁定 → **覆盖为4**（错误！应该是累积为8）
3. 炮塔C锁定 → **又覆盖为4**（错误！应该是累积为12）
4. 所有炮塔检查目标 → 发现预约值被改 → **全部停火**

**修复**: 
修改 `reserveDamage()` 方法为累积预约值，支持多炮塔同时瞄准同一目标

**编译状态**: ✅ BUILD SUCCESSFUL

---

### 25. 火箭炮塔贴图路径错误 🔥修复
**问题**: 火箭炮塔方块模型贴图路径错误
**修复日期**: 2026-03-06

**原因**: 
`rocket_turret.json` 中贴图路径使用了错误的命名空间
```json
// 错误写法
"0": "Blockbench:block/rocket_turret"

// 正确写法
"0": "advanced_turret:block/rocket_turret"
```

**修复**: 
修改 `models/block/rocket_turret.json` 中的贴图路径为正确的命名空间

**编译状态**: ✅ BUILD SUCCESSFUL

---

### 26. 厉行节约功能BUG：发射后立即清除预约 🔥修复
**问题**: 炮塔发射后立即清除预约，导致其他炮塔瞬间发射，实际效果大打折扣
**修复日期**: 2026-03-06

**原因**: 
所有炮塔的 `shoot()` 方法中都错误地调用了 `cancelReservation()`
```java
// 错误逻辑（发射后立即清除预约）
if (base.isThriftyMode() && target != null) {
    base.cancelReservation(target.getId());  // ← 导致其他炮塔误判
}
```

**问题场景**:
1. 炮塔A锁定目标，预约伤害4
2. 炮塔B也锁定目标，累积预约为8
3. 炮塔A发射，**立即清除预约**（错误！）
4. 炮塔B发现预约值为0，认为目标未被攻击，**立即发射**
5. 所有炮塔瞬间发射，厉行节约失效

**修复**: 
删除所有炮塔 `shoot()` 方法中的 `cancelReservation()` 调用。预约应该：
- 在目标锁定时累积
- 在目标丢失/死亡时清除（在 `updateTarget` 中）
- 在10秒后自动超时清除（在 `clearExpiredReservations` 中）

**影响文件**:
- `MachineGunTurretBlockEntity.java`
- `RailgunTurretBlockEntity.java`
- `RocketTurretBlockEntity.java`
- `MissileTurretBlockEntity.java`

**编译状态**: ✅ BUILD SUCCESSFUL

---

### 27. 方块模型格式版本不兼容 🔥修复
**问题**: 火箭炮塔和磁轨炮塔的物品模型/方块形态显示错误
**修复日期**: 2026-03-06

**原因**: 
Blockbench 导出的新版 JSON 模型格式不被 Minecraft 1.20.1 支持
```json
// 错误格式（Blockbench 新版）
"format_version": "1.21.11"

// 正确格式（Minecraft 1.20.1 支持）
无 format_version 字段，或使用 "format_version": "1.9.0"
```

**问题影响**:
- `models/block/rocket_turret.json` 格式错误 → 物品模型显示错误
- `models/block/railgun_turret.json` 格式错误 → 物品模型显示错误
- `models/block/missile_turret.json` 格式正确 ✅
- `models/block/machine_gun_turret.json` 格式正确 ✅

**修复方案**:
移除 `format_version` 字段，保留 `texture_size` 字段

**修复文件**:
- `models/block/rocket_turret.json`
- `models/block/railgun_turret.json`

**编译状态**: ✅ BUILD SUCCESSFUL

---

### 28. 模型贴图键名不一致导致贴图错误 🔥修复
**问题**: 火箭炮塔的物品/方块形态贴图显示错误
**修复日期**: 2026-03-06

**原因**: 
火箭炮塔模型使用了 `"0"` 作为贴图键名，而其他炮塔都用 `"1"`
```json
// 错误写法
"textures": {
    "0": "advanced_turret:block/rocket_turret",  // ❌ 键名是 "0"
    ...
}
"faces": {
    "north": {"uv": [...], "texture": "#0"},  // ❌ 引用 #0
    ...
}

// 正确写法（与其他炮塔一致）
"textures": {
    "1": "advanced_turret:block/rocket_turret",  // ✅ 键名是 "1"
    ...
}
"faces": {
    "north": {"uv": [...], "texture": "#1"},  // ✅ 引用 #1
    ...
}
```

**修复**: 
统一火箭炮塔模型的贴图键名为 `"1"`，所有 `"texture": "#0"` 改为 `"texture": "#1"`

**影响文件**: `models/block/rocket_turret.json`

**编译状态**: ✅ BUILD SUCCESSFUL

---

**最后更新**: 2026-03-06
