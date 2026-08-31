# Advanced Turret Mod — 完整学习路线图

> **适用对象**：有基础 Java 编程知识，但**从未接触过 Minecraft Mod 开发**的新手。
> **学习目标**：按本路线图逐文件阅读代码，理解一个完整 MC Mod 的骨架、核心机制、渲染、网络通信和 GUI 系统。
> **预计耗时**：新手约 8-12 小时（分 3-4 天完成），熟手约 2-3 小时。
> **项目信息**：Minecraft 1.20.1 + Forge 47.4.10 + GeckoLib 4.4.4

---

## 学习方法说明

1. **按顺序阅读**：后面的文件依赖于前面的概念，跳读会卡住。
2. **每个文件读完后**：问自己三个问题：
   - 这个文件**做了什么**？（一句话概括）
   - 它**依赖**哪些其他文件？（import 列表）
   - 它**被谁依赖**？（搜索引用它的代码）
3. **不要只看代码**：对照本文的"新手常见疑问"部分，确认自己理解了。
4. **碰到不懂的术语**：查看本文末尾的术语附录。

---

## 第一阶段：项目骨架与环境（约 1 小时）

理解"一个 MC Mod 是怎么被 Forge 加载起来的"。

### 步骤 1：`gradle.properties` — 构建配置的变量表

**路径**：[gradle.properties](file:///d:/Project/MCMOD-炮塔/turret-defense-mod/gradle.properties)

**你在这里学什么**：
- Minecraft 版本号、Forge 版本号、Mod ID、Mod 名称等**核心常量**的定义位置
- 这些变量会被 `build.gradle` 和 `mods.toml` 引用，做到"改一处，处处生效"
- 认识 `mapping_channel=parchment`（Parchment 映射提供人类可读的参数名）

**关键代码行**：
```properties
minecraft_version=1.20.1          # 你的 Mod 运行在哪个 MC 版本
forge_version=47.4.10             # 对应的 Forge 版本
mod_id=advanced_turret            # Mod 的唯一标识符（全小写）
mod_name=炮塔                      # 显示名称
geckolib_version=4.4.4            # 动画库版本
```

**新手常见疑问**：
- ❓ "为什么 mod_id 和 mod_name 不一样？" → mod_id 是程序内部用的标识符（只能英文小写+下划线），mod_name 是玩家看到的显示名。
- ❓ "Parchment 是什么？" → Minecraft 源码是混淆过的（参数名都是 `p_12345`），Parchment 提供了人类可读的映射。比如 `p_12345_` 变成 `blockPos`。

---

### 步骤 2：`settings.gradle` — 插件仓库配置

**路径**：[settings.gradle](file:///d:/Project/MCMOD-炮塔/turret-defense-mod/settings.gradle)

**你在这里学什么**：
- Gradle 的 `pluginManagement` 块，告诉 Gradle 去哪找插件
- 三个关键仓库：MinecraftForge、ParchmentMC、SpongePowered（Mixin 插件）

**新手常见疑问**：
- ❓ "这个文件里的 URL 是干什么的？" → 它们是 Gradle 插件的下载地址，类似 Maven 仓库。Gradle 构建时会自动从这些 URL 下载编译所需的插件。

---

### 步骤 3：`build.gradle` — 构建脚本（核心）

**路径**：[build.gradle](file:///d:/Project/MCMOD-炮塔/turret-defense-mod/build.gradle)

**你在这里学什么**：
- **Plugins 块**：`net.minecraftforge.gradle`（Forge 编译插件）、`org.parchmentmc.librarian.forgegradle`（Parchment 映射）
- **Minecraft 块**：`mappings`（映射类型）、`runs`（四种运行配置：client、server、gameTestServer、data）
- **Dependencies 块**：依赖声明 — Forge 本体、GeckoLib 动画库、可选的 Mekanism 和 JEI 测试依赖
- **ProcessResources 任务**：将 `gradle.properties` 中的变量注入到 `mods.toml` 和 `pack.mcmeta` 中

**关键代码行**：
```groovy
minecraft "net.minecraftforge:forge:${minecraft_version}-${forge_version}"
implementation fg.deobf("software.bernie.geckolib:geckolib-forge-${minecraft_version}:${geckolib_version}")
```

**新手常见疑问**：
- ❓ "`fg.deobf` 是什么？" → ForgeGradle 的特殊语法。Minecraft 源码是混淆的，引用的库也需要用 Forge 的映射来反混淆。`fg.deobf` 告诉 Gradle 在编译时做这个映射。
- ❓ "四种 runs 分别是什么？" → `client` 启动 Minecraft 客户端（用于测试）；`server` 启动专用服务器；`gameTestServer` 运行自动化测试；`data` 运行数据生成器（自动生成 JSON 配方等）。

---

### 步骤 4：`mods.toml` — Mod 元数据

**路径**：[src/main/resources/META-INF/mods.toml](file:///d:/Project/MCMOD-炮塔/turret-defense-mod/src/main/resources/META-INF/mods.toml)

**你在这里学什么**：
- TOML 格式的 Mod 声明文件，是 Forge 识别 Mod 的入口
- `modLoader="javafml"` — 声明使用 Java FML 加载器
- `[[mods]]` — 声明一个 Mod 块（可以有多个）
- `[[dependencies.${mod_id}]]` — 声明依赖（forge、minecraft、geckolib）
- 变量 `${mod_id}` 等由 `build.gradle` 的 `processResources` 任务从 `gradle.properties` 注入

**新手常见疑问**：
- ❓ "为什么这个文件用 `${}` 变量？" → 这是 Gradle 的资源处理占位符。构建时 Gradle 会把这些变量替换成 `gradle.properties` 中的实际值。这样做的好处是改版本号时只需要改一处。
- ❓ "`displayTest` 是什么？" → 控制服务器连接时的版本检查。`MATCH_VERSION` 表示客户端和服务端版本必须完全一致，否则会显示红色 X。

---

### 步骤 5：`pack.mcmeta` — 资源包元数据

**路径**：[src/main/resources/pack.mcmeta](file:///d:/Project/MCMOD-炮塔/turret-defense-mod/src/main/resources/pack.mcmeta)

**你在这里学什么**：
- Minecraft 资源包的声明文件，`pack_format` 必须与 MC 版本匹配（1.20.1 对应 15）
- 这个文件告诉 Minecraft："我提供的 assets 和 data 目录是一个合法的资源包"

---

### 阶段一自检清单

- [ ] 我能说出 `gradle.properties`、`build.gradle`、`mods.toml` 三者之间的关系吗？
- [ ] 我知道 `mod_id` 在整个项目中出现在哪些地方吗？
- [ ] 我理解 Forge 是如何通过 `@Mod` 注解找到 Mod 主类的吗？

---

## 第二阶段：Mod 主类与注册系统（约 1.5 小时）

理解"Mod 启动时发生了什么"。

### 步骤 6：`TurretMod.java` — Mod 主入口（必读！）

**路径**：[src/main/java/com/tian_nu/AdvancedTurret/TurretMod.java](file:///d:/Project/MCMOD-炮塔/turret-defense-mod/src/main/java/com/tian_nu/AdvancedTurret/TurretMod.java)

**你在这里学什么**：
- `@Mod(TurretMod.MOD_ID)` 注解 — 这是 Forge 识别 Mod 主类的唯一方式。`MOD_ID` 必须与 `mods.toml` 中的一致
- `MOD_ID = "advanced_turret"` — 全局唯一的 Mod ID 常量
- `location(path)` 方法 — 创建 `ResourceLocation`（MC 中标识资源的"路径"）：`advanced_turret:xxx`
- **构造函数** — Mod 加载时执行的第一段代码：
  1. `ModItems.register(modEventBus)` — 注册物品
  2. `ModCreativeModeTabs.register(modEventBus)` — 注册创造模式标签页
  3. `ModBlocks.register(modEventBus)` — 注册方块
  4. `ModBlockEntities.register(modEventBus)` — 注册方块实体
  5. `ModMenuTypes.register(modEventBus)` — 注册 GUI 菜单类型
  6. `ModEntities.register(modEventBus)` — 注册实体类型
  7. `modEventBus.addListener(this::commonSetup)` — 注册公共初始化事件
  8. `modEventBus.addListener(ModCreativeModeTabs::addCreative)` — 注册创造模式标签页填充事件
  9. `MinecraftForge.EVENT_BUS.register(this)` — 注册事件监听器
  10. `context.registerConfig(ModConfig.Type.COMMON, Config.SPEC)` — 注册配置文件
  11. `registerDataTickets()` — 注册 GeckoLib 动画数据同步票
- **`registerDataTickets()` 方法** — 为每种炮塔注册"动画数据通道"（布尔值：是否有目标、坐标值：目标位置等）。这些数据会自动在客户端/服务端间同步，GeckoLib 根据它们驱动动画。
- **`commonSetup` 方法** — 在 Mod 初始化阶段执行，注册网络通道
- **事件监听器**：`onServerStarting`（服务端启动）、`onLivingFall`（处理手榴弹免伤逻辑）

**新手常见疑问**：
- ❓ "`modEventBus` 和 `MinecraftForge.EVENT_BUS` 有什么区别？" → `modEventBus` 是 Mod 总线，只在 Mod 初始化阶段使用，注册物品/方块/实体等。`MinecraftForge.EVENT_BUS` 是游戏事件总线，在整个游戏生命周期中触发，用于监听服务器启动、实体受伤等游戏事件。
- ❓ "`DeferredRegister` 是什么？" → Forge 提供的延迟注册工具。它收集所有要注册的内容，然后在合适的时机一次性提交给 Forge 注册表。比老式的 `RegistryEvent` 更简洁。
- ❓ "`DataTicket` 是什么？" → GeckoLib 的数据同步机制。类似于 Minecraft 的 `EntityDataAccessor`，但专门用于方块实体动画。标记为 `public static` 是因为它们是全局唯一的标识符，所有同类型炮塔共享。

---

### 步骤 7：`ModItems.java` — 物品注册

**路径**：[src/main/java/com/tian_nu/AdvancedTurret/items/ModItems.java](file:///d:/Project/MCMOD-炮塔/turret-defense-mod/src/main/java/com/tian_nu/AdvancedTurret/items/ModItems.java)

**你在这里学什么**：
- `DeferredRegister<Item> ITEMS` — 创建一个物品注册器，绑定到 `advanced_turret` 命名空间
- `RegistryObject<Item>` — 注册后得到的"引用句柄"，通过 `.get()` 获取实际物品实例
- `describedItem(String... tooltipKeys)` — 自定义工厂方法，创建带描述提示（Tooltip）的普通物品
- 特殊物品（有自定义行为）使用 `new XXXItem(...)` 直接实例化
- **物品分类**：
  - **基础材料**：`turret_template`、`iron_gear`、`precision_gear_set`、`circuit_board`
  - **弹药**：`machine_gun_bullet`、`railgun_bullet`、`rocket`、`missile`、`grenade`
  - **工具**：`hand_grenade`（手榴弹）、`ammunition_launcher`（弹药发射器）、`radar`（雷达）、`remote_terminal`（远程终端）、`entity_analyzer`（实体分析器）
  - **插件**：`creative_power_component`、`solar_plugin`、`ammo_recycling_plugin`、`redstone_conversion_plugin`、`destruction_plugin`
  - **升级组件**：`attack_boost_component`、`energy_efficiency_component`、`range_component`、`accuracy_component`、`fire_rate_component`、`precision_component_t1~t5`
  - **智能芯片**：`smart_chip`

**新手常见疑问**：
- ❓ "`DeferredRegister` 的 `register` 方法返回值是什么？" → `RegistryObject<T>`，它是一个"懒加载"的引用。在注册完成前调用 `.get()` 会返回 null，注册完成后才返回实际对象。
- ❓ "为什么 `HAND_GRENADE` 不用 `describedItem` 方法？" → 因为手榴弹有自己的类 `HandGrenadeItem`，需要自定义右键行为（投掷爆炸）。`describedItem` 只适用于没有特殊逻辑的普通物品。

---

### 步骤 8：`ModBlocks.java` — 方块注册

**路径**：[src/main/java/com/tian_nu/AdvancedTurret/blocks/ModBlocks.java](file:///d:/Project/MCMOD-炮塔/turret-defense-mod/src/main/java/com/tian_nu/AdvancedTurret/blocks/ModBlocks.java)

**你在这里学什么**：
- `DeferredRegister<Block> BLOCKS` — 方块注册器
- `registerBlock(name, supplier)` — 同时注册方块和对应的 BlockItem（方块物品，即放在背包里的方块形式）
- **BlockBehaviour.Properties.of()** — 方块的属性设置：
  - `.mapColor(MapColor.STONE)` — 小地图上的颜色
  - `.requiresCorrectToolForDrops()` — 需要正确的工具才能掉落
  - `.noOcclusion()` — 方块不遮挡视线（炮塔模型需要渲染）
  - `.strength(hardness, blastResistance)` — 硬度和爆炸抗性
- **方块类型**：
  - **炮塔基座**：T1-T5（石头→绿宝石，等级越高能量越多）
  - **炮塔**：机枪、磁轨炮、火箭、导弹、激光、相位立场、共振立场、榴弹发射器、垃圾炮塔

**新手常见疑问**：
- ❓ "为什么注册方块时还要注册 BlockItem？" → 方块被放置在世界中时是 `Block`，但放在玩家背包里时是 `BlockItem`（一种特殊的 `Item`）。两者是分开注册的，但通过 `registerBlockItems` 自动关联。
- ❓ "`.noOcclusion()` 为什么重要？" → 默认情况下 Minecraft 使用"方块遮挡剔除"优化渲染——如果相邻方块完全遮挡了某个面，就不渲染那个面。炮塔模型不是完整的方块，所以必须关闭遮挡剔除。

---

### 步骤 9：`ModBlockEntities.java` — 方块实体注册

**路径**：[src/main/java/com/tian_nu/AdvancedTurret/blocks/entitys/ModBlockEntities.java](file:///d:/Project/MCMOD-炮塔/turret-defense-mod/src/main/java/com/tian_nu/AdvancedTurret/blocks/entitys/ModBlockEntities.java)

**你在这里学什么**：
- `BlockEntityType` 的概念 — 方块实体（Block Entity）是"有逻辑的方块"，可以存储数据、每 tick 更新、响应事件
- `BlockEntityType.Builder.of(T::new, blocks...)` — 创建方块实体类型，指定它适用于哪些方块
- 每个炮塔类型都有自己的方块实体类（因为它们的行为不同）

**新手常见疑问**：
- ❓ "方块实体和方块有什么区别？" → 方块（Block）是静态的"类型定义"（硬度、材质、掉落物）。方块实体（BlockEntity）是动态的"实例数据"（能量存储、物品栏、逻辑更新）。类比：方块是"模具"，方块实体是"用模具造出来的活物"。
- ❓ "为什么 `TURRET_BASE` 的 builder 里传了 5 个方块？" → 因为 T1-T5 五个基座方块共享同一个方块实体类型 `TurretBaseBlockEntity`。在代码中通过 `getTier()` 区分等级。

---

### 步骤 10：`ModEntities.java` — 实体注册

**路径**：[src/main/java/com/tian_nu/AdvancedTurret/entity/ModEntities.java](file:///d:/Project/MCMOD-炮塔/turret-defense-mod/src/main/java/com/tian_nu/AdvancedTurret/entity/ModEntities.java)

**你在这里学什么**：
- `EntityType.Builder.of(T::new, MobCategory.MISC)` — 创建实体类型
- `.sized(width, height)` — 碰撞箱大小
- `.clientTrackingRange(64)` — 客户端追踪范围（64 格内渲染）
- `.updateInterval(1)` — 更新频率（每 tick 更新）
- `.fireImmune()` — 免疫火焰伤害
- **实体类型**：`turret_bullet`、`railgun_bullet`、`rocket`、`missile`、`grenade`、`launcher_grenade`、`junk_projectile`

**新手常见疑问**：
- ❓ "实体和方块实体有什么区别？" → 实体（Entity）是**可以移动**的（子弹、生物、掉落物）。方块实体（BlockEntity）是**固定在方块位置**的。子弹是实体，炮塔是方块实体。
- ❓ "`MobCategory.MISC` 是什么？" → 实体分类。MISC 表示"杂项"，因为子弹不是生物，不适合用 CREATURE、MONSTER 等分类。

---

### 步骤 11：`ModCreativeModeTabs.java` — 创造模式标签页

**路径**：[src/main/java/com/tian_nu/AdvancedTurret/items/ModCreativeModeTabs.java](file:///d:/Project/MCMOD-炮塔/turret-defense-mod/src/main/java/com/tian_nu/AdvancedTurret/items/ModCreativeModeTabs.java)

**你在这里学什么**：
- `CreativeModeTab.builder()` — 创建自定义创造模式标签页
- `.icon(() -> ...)` — 标签页的图标
- `.displayItems((params, output) -> { ... })` — 定义标签页中显示哪些物品
- `addCreative(BuildCreativeModeTabContentsEvent)` — 将物品添加到原版的"工具"和"战斗"标签页

**新手常见疑问**：
- ❓ "为什么要把物品添加到原版标签页？" → 方便玩家在创造模式中快速找到。比如实体分析器既是工具又是武器，所以同时出现在"工具"和"战斗"标签页。

---

### 阶段二自检清单

- [ ] 我能画出 `TurretMod` 构造函数中注册顺序的流程图吗？
- [ ] 我理解 `DeferredRegister` 的"先收集后提交"机制吗？
- [ ] 我能区分"方块 → 方块实体 → 实体"这三个概念吗？

---

## 第三阶段：配置系统（约 0.5 小时）

### 步骤 12：`Config.java` — 服务端配置

**路径**：[src/main/java/com/tian_nu/AdvancedTurret/Config.java](file:///d:/Project/MCMOD-炮塔/turret-defense-mod/src/main/java/com/tian_nu/AdvancedTurret/Config.java)

**你在这里学什么**：
- `ForgeConfigSpec.Builder` — Forge 配置系统构建器
- `defineInRange(name, defaultValue, min, max)` — 定义带范围的配置项
- 配置分为两类：
  1. **静态声明**（`public static final ForgeConfigSpec.XXX`）— 定义配置的"元数据"（名称、默认值、范围）
  2. **运行时缓存**（`public static XXX`）— 配置加载后从这里读取值，避免每次读文件
- `@SubscribeEvent static void onLoad(ModConfigEvent)` — 配置文件加载/重载时，将配置值复制到缓存变量
- `SPEC = BUILDER.build()` — 构建最终的配置规范

**配置项分类**：
- 基座能量：`turretBaseMaxEnergyT1~T5`、`maxTransferRate`
- 每种炮塔的属性：伤害、射程、射速、能量消耗、子弹速度
- 插件参数：太阳能产电量、弹药回收概率、红石转换比
- 立场炮塔参数：范围、能耗、效果持续时间

**新手常见疑问**：
- ❓ "为什么 Config 有两套变量？" → `TURRET_BASE_MAX_ENERGY_T1`（大写）是 `ForgeConfigSpec.IntValue`，用于定义配置。`turretBaseMaxEnergyT1`（小写）是 `int`，用于运行时读取。前者只在加载配置时使用，后者在游戏逻辑中频繁读取，性能更好。
- ❓ "配置文件在哪里？" → `config/advanced_turret-common.toml`（在 Minecraft 游戏目录下）。Forge 自动生成。

---

### 步骤 13：`ConfigManager.java` — 客户端配置

**路径**：[src/main/java/com/tian_nu/AdvancedTurret/ConfigManager.java](file:///d:/Project/MCMOD-炮塔/turret-defense-mod/src/main/java/com/tian_nu/AdvancedTurret/ConfigManager.java)

**你在这里学什么**：
- 客户端专属配置（不通过 Forge 配置系统，直接用 JSON 文件读写）
- `FMLPaths.CONFIGDIR.get()` — 获取 Minecraft 配置目录
- Gson 库的 JSON 序列化/反序列化
- 配置内容：GUI 背景透明度、能量条透明度、最近扫描的实体列表

**新手常见疑问**：
- ❓ "为什么不用 Forge Config 而用 JSON？" → Forge Config 是服务端配置，需要在服务端和客户端之间同步。这个 ConfigManager 管理的是纯客户端 UI 设置（背景透明度等），不需要同步，所以直接用 JSON 更简单。

---

## 第四阶段：核心系统 — 炮塔基座（约 2.5 小时，最重要！）

这是整个 Mod 的核心，理解了它就理解了 70% 的项目。

### 步骤 14：`TurretBaseBlock.java` — 基座方块类

**路径**：[src/main/java/com/tian_nu/AdvancedTurret/blocks/TurretBaseBlock.java](file:///d:/Project/MCMOD-炮塔/turret-defense-mod/src/main/java/com/tian_nu/AdvancedTurret/blocks/TurretBaseBlock.java)

**你在这里学什么**：
- `extends BaseEntityBlock` — 所有有方块实体的方块都必须继承这个类
- `newBlockEntity(pos, state)` — 创建方块实体实例
- `getTicker(...)` — 返回 tick 更新函数（服务端每 tick 调用 `TurretBaseBlockEntity::tick`）
- `use(state, level, pos, player, hand, hit)` — 右键交互逻辑：
  - 服务端检查权限
  - **Shift+右键** → 打开面配置 GUI（`TurretFaceConfigMenu`）
  - **普通右键** → 打开主 GUI（`TurretMenu`）
- `setPlacedBy(...)` — 方块被放置时设置所有者
- `onRemove(...)` — 方块被破坏时掉落物品、清理远程终端索引

**新手常见疑问**：
- ❓ "`getTicker` 返回 null 是什么意思？" → 客户端不需要 tick 更新（客户端不处理游戏逻辑），所以客户端返回 null。服务端返回 `TurretBaseBlockEntity::tick`。
- ❓ "`NetworkHooks.openScreen` 做了什么？" → 打开一个 GUI，并在客户端和服务端之间建立同步通道。`buf -> buf.writeBlockPos(pos)` 是向客户端传递额外数据（方块坐标）。

---

### 步骤 15：`TurretBaseBlockEntity.java` — 基座方块实体（核心，约 1300 行）

**路径**：[src/main/java/com/tian_nu/AdvancedTurret/blocks/entitys/TurretBaseBlockEntity.java](file:///d:/Project/MCMOD-炮塔/turret-defense-mod/src/main/java/com/tian_nu/AdvancedTurret/blocks/entitys/TurretBaseBlockEntity.java)

**这是整个项目最重要的文件！** 建议分 5 个部分阅读：

#### 第一部分：能量系统（行 265-310）

- `getTier()` — 根据方块状态判断基座等级（T1-T5）
- `getMaxEnergyForTier()` — 等级 → 最大能量
- `getMaxTransferRateForTier()` — 等级 → 传输速率
- `BaseEnergyStorage`（内部类）— 继承 `ForgeEnergyStorage`，封装能量存取逻辑，并自动同步到客户端

#### 第二部分：物品栏系统（行 126-250）

- **弹药槽**（ammoInventory）：9 格，存储不同类型的弹药
- **插件槽**（basePluginSlot）：最多 2 格，存储智能芯片、太阳能插件等
- **面升级槽**（faceUpgradeSlots）：6 个面 × 最多 3 格，每个面独立存储升级组件
- **组合物品栏**（combinedInventory）：将所有槽合并为一个接口，供外部访问（如管道）

#### 第三部分：所有权与面管理（行 97-110, 452-542）

- `owner` — 所有者 UUID
- `enabledFacesMask` — 位掩码控制每个面是否启用（bit 0=DOWN, 1=UP, 2=NORTH, 3=SOUTH, 4=WEST, 5=EAST）
- `hasTurretOnFace(face)` — 检查指定面是否安装了炮塔（遍历所有炮塔类型）
- `isFaceEnabled(face)` — 检查面是否启用且安装了炮塔

#### 第四部分：伤害预留系统（行 112-118, 771-888）

- `reserveDamage(entityId, damage, health, time)` — 为实体预留伤害
- `tryReserveDamage(...)` — 节约模式下的伤害预留（如果已有预留覆盖了目标生命值，拒绝新的预留）
- `confirmDamage(...)` — 确认实际造成的伤害，从预留中扣除
- `clearExpiredReservations(...)` — 清理过期的预留
- **目的**：防止多个炮塔同时攻击同一目标造成伤害浪费

#### 第五部分：升级组件加成（行 1167-1209）

- `getDamageForFace(face, baseDamage)` — 根据攻击增强组件数量计算最终伤害
- `getSearchRadiusForFace(face, baseRadius)` — 根据射程组件计算最终搜索范围
- `getFireRateForFace(face, baseFireRate)` — 根据射速组件计算最终射击间隔
- `getEnergyCostForFace(face, baseEnergyCost)` — 根据能量效率组件计算最终能耗

**NBT 序列化**（行 979-1092）：
- `saveAdditional(CompoundTag)` — 将方块实体的所有数据保存到 NBT
- `load(CompoundTag)` — 从 NBT 恢复数据（包含大量旧版兼容代码）

**网络同步**（行 1211-1246）：
- `getUpdateTag()` / `handleUpdateTag()` — 方块实体数据同步
- `syncToClient()` — 主动触发同步

**新手常见疑问**：
- ❓ "位掩码是什么？为什么用位掩码控制面？" → 6 个方向，每个方向只有启用/禁用两种状态，用 1 个 byte（8 位）就能存储。`(mask & (1 << bit)) != 0` 检查某位是否为 1。这比用 6 个 boolean 字段节省 NBT 空间。
- ❓ "伤害预留系统是干什么的？" → 假设目标有 20 点生命值，炮塔 A 准备发射（伤害 10），炮塔 B 也准备发射（伤害 10）。如果不预留，两个炮塔都会开枪，但第二个炮塔的伤害可能浪费。预留系统让炮塔 A 先"预约"10 点伤害，炮塔 B 看到目标只剩 10 点有效生命值后，只需要再预约 10 点。
- ❓ "`ContainerData` 是什么？" → 用于在 GUI 和服务端之间同步数据（如能量值）。客户端 GUI 通过 `data.get(0)` 读取当前能量，`data.get(1)` 读取最大能量。

---

### 步骤 16：`TurretBaseBlockEntity.tick()` — 每 tick 逻辑

**在 TurretBaseBlockEntity.java 第 632-698 行**

**你在这里学什么**：
- `static void tick(Level, BlockPos, BlockState, TurretBaseBlockEntity)` — Forge 的标准 tick 方法签名
- 每 tick 执行：
  1. 确保能量容量正确（等级可能变了）
  2. 创造电源组件 → 能量回满
  3. 太阳能插件 → 白天 + 露天 → 产出能量
  4. 红石转换插件 → 消耗弹药槽中的红石/红石块 → 转换为能量
  5. 清理过期的伤害预留

---

## 第五阶段：炮塔行为（约 2 小时）

### 步骤 17：`MachineGunTurretBlockEntity.java` — 机枪炮塔（范例）

**路径**：[src/main/java/com/tian_nu/AdvancedTurret/blocks/entitys/MachineGunTurretBlockEntity.java](file:///d:/Project/MCMOD-炮塔/turret-defense-mod/src/main/java/com/tian_nu/AdvancedTurret/blocks/entitys/MachineGunTurretBlockEntity.java)

**你在这里学什么**：
- 炮塔方块实体的标准结构（所有炮塔都遵循这个模式）：
  1. `getBaseEntity()` — 获取连接的基座
  2. `tick()` — 每 tick 更新
  3. `updateTarget()` — 搜索/更新目标
  4. `canShoot()` — 检查是否满足射击条件（能量 + 弹药）
  5. `shoot()` — 执行射击
- `getFireRate()` / `getSearchRadius()` / `getBulletDamage()` / `getBulletSpeed()` — 从 Config 读取配置值
- **GeckoLib 动画数据同步**：`HAS_TARGET`、`TARGET_POS_X/Y/Z` — 通过 DataTicket 同步到客户端，驱动炮塔模型的旋转动画
- **目标搜索**：`findTarget()` — 在搜索范围内找到最近的敌对生物
- **可见性检测**：`getVisibleTargetPoint()` — 检测头部/身体/脚部是否可见（射线检测）
- **预判瞄准**：`isPredictiveAiming()` — 如果启用了预判瞄准，计算子弹飞行时间后偏移目标位置

**关键代码行**：
```java
// 无电量时低头动画
if (base.getEnergyStored() == 0) {
    blockEntity.setAnimData(HAS_TARGET, true);
    blockEntity.setAnimData(TARGET_POS_Y, pos.getY() - 2.0);  // 向下看
}

// 创建子弹
TurretBulletEntity bullet = new TurretBulletEntity(level, muzzlePos.x, muzzlePos.y, muzzlePos.z, damage);
bullet.shoot(direction, getBulletSpeed());
level.addFreshEntity(bullet);
```

**新手常见疑问**：
- ❓ "`setAnimData` 是什么？" → GeckoLib 提供的方法。它设置方块实体的动画数据（如目标位置），这些数据会自动同步到客户端，客户端渲染器读取它们来旋转炮塔模型。
- ❓ "为什么无电量时设置 `TARGET_POS_Y = pos.getY() - 2.0`？" → 这是一个巧妙的设计：炮塔没电时"低头"表示没电了。通过设置目标位置在炮塔下方，炮塔的旋转动画会指向下方。

---

### 步骤 18：`LinearTurretTargetingHelper.java` — 直线炮塔瞄准工具

**路径**：[src/main/java/com/tian_nu/AdvancedTurret/blocks/entitys/LinearTurretTargetingHelper.java](file:///d:/Project/MCMOD-炮塔/turret-defense-mod/src/main/java/com/tian_nu/AdvancedTurret/blocks/entitys/LinearTurretTargetingHelper.java)

**你在这里学什么**：
- `isTargetInRange(entity, pos, radius)` — 检查目标是否在射程内
- `findVisibleTargetPoint(level, pos, facing, muzzlePos, entity)` — 射线检测找到可见的瞄准点（头部 > 身体 > 脚部）
- `canSeePoint(level, pos, facing, start, end)` — 从炮口到目标点之间是否有方块阻挡

---

### 步骤 19：`TurretTargetFilterHelper.java` — 目标过滤工具

**路径**：[src/main/java/com/tian_nu/AdvancedTurret/blocks/entitys/TurretTargetFilterHelper.java](file:///d:/Project/MCMOD-炮塔/turret-defense-mod/src/main/java/com/tian_nu/AdvancedTurret/blocks/entitys/TurretTargetFilterHelper.java)

**你在这里学什么**：
- `passesCommonChecks(entity, base, pos, radius)` — 所有炮塔共用的目标过滤：
  1. 实体是否存活且可攻击？
  2. 是否符合智能芯片的规则？（白名单/黑名单/目标类型标志）
  3. 是否是友伤保护的对象？（所有者、驯服的宠物）
  4. 是否在射程内？
- `shouldSkipForThrifty(entity, base)` — 节约模式检查
- `matchesSmartChipRules(entity, base)` — 智能芯片的复杂规则：
  - 白名单中的实体 → 跳过（不攻击）
  - 黑名单中的实体 → 总是攻击
  - 目标类型标志位（FLAG_HOSTILE/NEUTRAL/FRIENDLY/PLAYERS）→ 匹配则攻击

---

### 步骤 20：`AbstractFieldTurretBlockEntity.java` — 立场炮塔基类

**路径**：[src/main/java/com/tian_nu/AdvancedTurret/blocks/entitys/AbstractFieldTurretBlockEntity.java](file:///d:/Project/MCMOD-炮塔/turret-defense-mod/src/main/java/com/tian_nu/AdvancedTurret/blocks/entitys/AbstractFieldTurretBlockEntity.java)

**你在这里学什么**：
- 立场炮塔（相位立场、共振立场）的公共逻辑：不用发射子弹，而是对范围内的实体施加药水效果
- `tickServer()` — 每 tick 检查能量、搜索目标、施加效果
- `getFieldRange()` → 范围 = 基础范围 + 射程组件加成
- `getEffectDuration()` → 效果持续时间 = 基础持续时间 + 射速组件加成
- `getEffectAmplifierBonus()` → 攻击增强组件 ≥ 4 个时效果等级 +1
- `refreshEffect()` — 智能刷新效果（只在效果即将消失时刷新，避免浪费）

**新手常见疑问**：
- ❓ "为什么要检查 `shouldApplyEffect` 而不是每次都刷新？" → 如果每 tick 都施加效果，效果持续时间会一直被重置，浪费能量。`shouldApplyEffect` 只在效果快消失（剩余时间 ≤ 刷新间隔）时才重新施加。

---

## 第六阶段：弹道与实体（约 1 小时）

### 步骤 21：`TurretProjectileEntity.java` — 子弹抽象基类

**路径**：[src/main/java/com/tian_nu/AdvancedTurret/entity/TurretProjectileEntity.java](file:///d:/Project/MCMOD-炮塔/turret-defense-mod/src/main/java/com/tian_nu/AdvancedTurret/entity/TurretProjectileEntity.java)

**你在这里学什么**：
- `extends Projectile` — 继承 Minecraft 的投射物基类
- 核心属性：`damage`（伤害）、`lifetime`（生命周期）、`sourcePos`（炮塔位置）、`basePos`（基座位置）、`maxTravelDistance`（最大飞行距离）
- `shoot(direction, speed)` — 设置子弹速度方向
- `dealDamage(target, damage)` — 清除无敌帧后造成伤害（解决多炮塔伤害丢失问题）
- `shouldIgnoreDamage(entity)` — 白名单检查（智能芯片白名单中的实体不受伤害）
- `shouldDiscardForFlightLimits(nextPos)` — 飞行限制检查（越界、超距、区块未加载）
- `getBaseFromBlockEntity(be)` — 从方块实体获取基座（支持从炮塔方块或基座方块获取）

**关键代码行**：
```java
// 清除无敌帧，解决多炮塔伤害丢失
target.invulnerableTime = 0;
target.hurtTime = 0;
```

**新手常见疑问**：
- ❓ "为什么要清除无敌帧？" → Minecraft 中实体受伤后有短暂的无敌时间（invulnerableTime），在此期间其他伤害会被忽略。如果两个炮塔同时命中同一个目标，第二个炮塔的伤害可能被忽略。清除无敌帧确保每个炮塔的伤害都能生效。
- ❓ "为什么有 `lifetime` 限制？" → 防止子弹无限飞行（比如射向天空后永远不掉落，占用内存）。

---

### 步骤 22：`TurretBulletEntity.java` — 机枪子弹

**路径**：[src/main/java/com/tian_nu/AdvancedTurret/entity/TurretBulletEntity.java](file:///d:/Project/MCMOD-炮塔/turret-defense-mod/src/main/java/com/tian_nu/AdvancedTurret/entity/TurretBulletEntity.java)

**你在这里学什么**：
- 自定义 `tick()` 方法，实现**分离碰撞检测**：
  1. 先检测实体碰撞（`ProjectileUtil.getEntityHitResult`）
  2. 再检测方块碰撞（`level.clip`）
  3. 基座方块 → 销毁子弹
  4. 炮塔自身 → 前 2 tick 跳过（避免子弹刚生成就撞到自己）
  5. 其他方块 → 销毁子弹
- `onHitEntity()` — 击中实体，造成伤害后销毁

**新手常见疑问**：
- ❓ "为什么需要分离碰撞检测？" → Minecraft 默认的碰撞检测是"先检测方块再检测实体"，但子弹应该"优先检测实体"（否则子弹会穿过薄墙后面的实体）。分离检测让你自己控制顺序。
- ❓ "为什么前 2 tick 要跳过自身碰撞？" → 子弹从炮口生成时，起点在炮塔方块内部。如果不跳过，子弹会立即撞到炮塔方块并销毁。

---

## 第七阶段：智能芯片与物品系统（约 1 小时）

### 步骤 23：`SmartChipItem.java` — 智能芯片

**路径**：[src/main/java/com/tian_nu/AdvancedTurret/items/SmartChipItem.java](file:///d:/Project/MCMOD-炮塔/turret-defense-mod/src/main/java/com/tian_nu/AdvancedTurret/items/SmartChipItem.java)

**你在这里学什么**：
- 使用 **NBT（Named Binary Tag）** 在物品上存储持久化数据
- NBT 键：`TargetFlags`（目标类型标志）、`FriendlyFire`（友伤）、`PredictiveAiming`（预判瞄准）、`Blacklist`/`Whitelist`（黑/白名单）、`ThriftyMode`（厉行节约）
- **位标志组合**：`FLAG_HOSTILE | FLAG_NEUTRAL | FLAG_FRIENDLY | FLAG_PLAYERS` 表示"攻击所有类型"
- `use(level, player, hand)` — 右键打开配置 GUI
- `useOn(context)` — Shift+右键基座 → 将芯片插入插件槽
- `getReadOnlyTag(stack)` — 纯读取标签，避免意外创建空 NBT

**新手常见疑问**：
- ❓ "NBT 是什么？" → Minecraft 的数据存储格式，类似于 JSON。物品的 NBT 存储在 `ItemStack` 的 `tag` 字段中。`stack.getOrCreateTag()` 获取或创建 NBT，`stack.getTag()` 只读取不创建。
- ❓ "为什么用位标志而不是枚举？" → 位标志可以组合：`FLAG_HOSTILE | FLAG_NEUTRAL` 表示"同时攻击敌对和中立生物"。用枚举做不到这种组合。

---

### 步骤 24：`HandGrenadeItem.java` — 手榴弹

**路径**：[src/main/java/com/tian_nu/AdvancedTurret/items/HandGrenadeItem.java](file:///d:/Project/MCMOD-炮塔/turret-defense-mod/src/main/java/com/tian_nu/AdvancedTurret/items/HandGrenadeItem.java)

**你在这里学什么**：
- 自定义物品的右键行为（投掷手榴弹）
- 创建实体（`GrenadeEntity`）并添加到世界

---

### 步骤 25：`EntityAnalyzerItem.java` — 实体分析器

**路径**：[src/main/java/com/tian_nu/AdvancedTurret/items/EntityAnalyzerItem.java](file:///d:/Project/MCMOD-炮塔/turret-defense-mod/src/main/java/com/tian_nu/AdvancedTurret/items/EntityAnalyzerItem.java)

**你在这里学什么**：
- 右键实体 → 获取实体类型 ID → 添加到智能芯片的黑名单/白名单
- 与 `ConfigManager` 联动（记录最近扫描的实体）

---

## 第八阶段：网络通信（约 1 小时）

### 步骤 26：`ModNetwork.java` — 网络通道注册

**路径**：[src/main/java/com/tian_nu/AdvancedTurret/network/ModNetwork.java](file:///d:/Project/MCMOD-炮塔/turret-defense-mod/src/main/java/com/tian_nu/AdvancedTurret/network/ModNetwork.java)

**你在这里学什么**：
- `NetworkRegistry.newSimpleChannel(...)` — 创建一个网络通道
- `CHANNEL.registerMessage(packetId++, PacketClass, encode, decode, handle)` — 注册数据包：
  - `encode` — 序列化（写入 `FriendlyByteBuf`）
  - `decode` — 反序列化（从 `FriendlyByteBuf` 读取）
  - `handle` — 处理收到的数据包
- 9 个数据包类型：
  - `SmartChipConfigPacket` — 智能芯片配置同步
  - `TurretOpenFaceConfigPacket` — 打开面配置 GUI
  - `TurretFaceSelectPacket` — 选择面
  - `TurretRangeConfigPacket` — 射程配置
  - `TurretFaceEnableConfigPacket` — 面启用/禁用
  - `RemoteTerminalQueryPacket` — 远程终端查询
  - `RemoteTerminalApplyPacket` — 远程终端应用配置
  - `RemoteTerminalBaseListPacket` — 远程终端基座列表
  - `RemoteTerminalOperationResultPacket` — 远程终端操作结果
  - `EnergySyncPacket` — 能量同步

**新手常见疑问**：
- ❓ "为什么需要网络包？" → Minecraft 是客户端-服务端架构。客户端操作（如修改智能芯片配置）需要发送到服务端，服务端更新数据后需要通知客户端。网络包就是这种通信的载体。
- ❓ "`packetId++` 是什么意思？" → 每个数据包在通道中有一个唯一的 ID。`packetId++` 从 0 开始自增，自动分配 ID。

---

### 步骤 27：`SmartChipConfigPacket.java` — 数据包范例

**路径**：[src/main/java/com/tian_nu/AdvancedTurret/network/SmartChipConfigPacket.java](file:///d:/Project/MCMOD-炮塔/turret-defense-mod/src/main/java/com/tian_nu/AdvancedTurret/network/SmartChipConfigPacket.java)

**你在这里学什么**：
- 数据包的标准结构：
  1. 字段（要传输的数据）
  2. 构造函数（创建数据包）
  3. `encode(FriendlyByteBuf)` — 写入数据
  4. `decode(FriendlyByteBuf)` — 读取数据并创建数据包
  5. `handle(Supplier<NetworkEvent.Context>)` — 处理逻辑
- `.enqueueWork(() -> { ... })` — 确保在主线程执行
- `.setPacketHandled(true)` — 标记数据包已处理

---

## 第九阶段：GUI 与菜单（约 1 小时）

### 步骤 28：`ModMenuTypes.java` — 菜单类型注册

**路径**：[src/main/java/com/tian_nu/AdvancedTurret/gui/ModMenuTypes.java](file:///d:/Project/MCMOD-炮塔/turret-defense-mod/src/main/java/com/tian_nu/AdvancedTurret/gui/ModMenuTypes.java)

**你在这里学什么**：
- `IForgeMenuType.create(TurretMenu::new)` — 创建菜单类型（Forge 扩展版本，支持从 `FriendlyByteBuf` 读取额外数据）
- 两个菜单类型：`TURRET_BASE`（主界面）、`TURRET_FACE_CONFIG`（面配置界面）

---

### 步骤 29：`TurretMenu.java` — 基座 GUI 菜单

**路径**：[src/main/java/com/tian_nu/AdvancedTurret/gui/TurretMenu.java](file:///d:/Project/MCMOD-炮塔/turret-defense-mod/src/main/java/com/tian_nu/AdvancedTurret/gui/TurretMenu.java)

**你在这里学什么**：
- `AbstractContainerMenu` 的子类，定义 GUI 的槽位布局
- `addSlot(new SlotItemHandler(...))` — 添加方块实体的物品槽
- `addSlot(new Slot(playerInventory, ...))` — 添加玩家背包槽
- `quickMoveStack(...)` — Shift+点击物品的快速移动逻辑
- `addDataSlots(data)` — 添加同步数据（能量值等）

---

### 步骤 30：`TurretScreen.java` / `TurretFaceConfigScreen.java` — 客户端 GUI 渲染

**路径**：
- [src/main/java/com/tian_nu/AdvancedTurret/gui/TurretScreen.java](file:///d:/Project/MCMOD-炮塔/turret-defense-mod/src/main/java/com/tian_nu/AdvancedTurret/gui/TurretScreen.java)
- [src/main/java/com/tian_nu/AdvancedTurret/gui/TurretFaceConfigScreen.java](file:///d:/Project/MCMOD-炮塔/turret-defense-mod/src/main/java/com/tian_nu/AdvancedTurret/gui/TurretFaceConfigScreen.java)

**你在这里学什么**：
- `AbstractContainerScreen<TurretMenu>` 的子类，在客户端渲染 GUI
- `renderBg(...)` — 渲染背景
- `render(...)` — 渲染前景（包括能量条、能量数值）
- 自定义 UI 组件：`TechButton`、`TechCheckbox`、`TechEditBox`

---

## 第十阶段：客户端渲染（约 1 小时）

### 步骤 31：`ClientEvents.java` — 客户端初始化

**路径**：[src/main/java/com/tian_nu/AdvancedTurret/client/ClientEvents.java](file:///d:/Project/MCMOD-炮塔/turret-defense-mod/src/main/java/com/tian_nu/AdvancedTurret/client/ClientEvents.java)

**你在这里学什么**：
- `@Mod.EventBusSubscriber(value = Dist.CLIENT)` — 只在客户端执行的代码
- `onClientSetup` — 注册 GUI 屏幕、加载客户端配置
- `registerBlockEntityRenderers` — 注册方块实体渲染器（每个炮塔对应一个 GeoRenderer）
- `registerEntityRenderers` — 注册实体渲染器（子弹、火箭、导弹等）

---

### 步骤 32：`MachineGunTurretGeoRenderer.java` — 炮塔渲染器（范例）

**路径**：[src/main/java/com/tian_nu/AdvancedTurret/client/MachineGunTurretGeoRenderer.java](file:///d:/Project/MCMOD-炮塔/turret-defense-mod/src/main/java/com/tian_nu/AdvancedTurret/client/MachineGunTurretGeoRenderer.java)

**你在这里学什么**：
- `extends GeoBlockRenderer<T>` — GeckoLib 的方块实体渲染器基类
- GeckoLib 会根据 `GeoModel` 和动画数据自动旋转炮塔模型
- 渲染器本身代码很少，因为 GeckoLib 做了大部分工作

---

### 步骤 33：`MachineGunTurretGeoModel.java` — 炮塔模型定义

**路径**：[src/main/java/com/tian_nu/AdvancedTurret/client/models/MachineGunTurretGeoModel.java](file:///d:/Project/MCMOD-炮塔/turret-defense-mod/src/main/java/com/tian_nu/AdvancedTurret/client/models/MachineGunTurretGeoModel.java)

**你在这里学什么**：
- `GeoModel<T>` 的子类，指定模型文件（`.geo.json`）、贴图文件（`.png`）、动画文件（`.animation.json`）
- `ResourceLocation` 引用 `assets/advanced_turret/` 下的资源

---

## 第十一阶段：数据文件（约 0.5 小时）

### 步骤 34：配方、掉落表、标签

**路径**：
- `src/main/resources/data/advanced_turret/recipes/` — 合成配方 JSON
- `src/main/resources/data/advanced_turret/loot_tables/` — 方块掉落表
- `src/main/resources/data/advanced_turret/tags/` — 物品/方块标签

**你在这里学什么**：
- 配方 JSON 的标准格式（`type`、`pattern`、`key`、`result`）
- 掉落表的标准格式（`type`、`pools`、`rolls`、`entries`）
- 标签的作用：`base_items.json` 标记"基础物品"、`turret_types.json` 标记"炮塔类型"、`components.json` 标记"组件"

---

### 步骤 35：语言文件、模型、贴图

**路径**：
- `assets/advanced_turret/lang/zh_cn.json` — 中文翻译
- `assets/advanced_turret/lang/en_us.json` — 英文翻译
- `assets/advanced_turret/models/` — 物品/方块模型 JSON
- `assets/advanced_turret/textures/` — 贴图 PNG
- `assets/advanced_turret/geo/` — GeckoLib 模型文件
- `assets/advanced_turret/animations/` — GeckoLib 动画文件

**你在这里学什么**：
- 语言文件使用 `Component.translatable("key")` 的 key 进行翻译
- 模型 JSON 指定物品/方块的显示形状
- `.geo.json` 是 Blockbench 导出的 GeckoLib 模型文件

---

## 完整文件依赖图（从上到下）

```
gradle.properties  ──→  build.gradle  ──→  mods.toml  +  pack.mcmeta
                                              │
                                              ▼
                                       TurretMod.java  (主入口)
                                     /    |    |    |    \
                                    ▼     ▼    ▼    ▼     ▼
                               ModItems  ModBlocks  ModEntities  ModBlockEntities  ModMenuTypes
                                  │        │          │               │              │
                                  │        ▼          │               ▼              ▼
                                  │   TurretBaseBlock  │    TurretBaseBlockEntity  TurretMenu
                                  │        │           │         (核心)           (GUI菜单)
                                  │        ▼           │         /  |  \
                                  │   TurretBaseBlockEntity  ◄──┘   │   └──►  Config.java
                                  │                                │
                                  ▼                                ▼
                          SmartChipItem            MachineGunTurretBlockEntity
                          HandGrenadeItem          RailgunTurretBlockEntity
                          EntityAnalyzerItem       LaserTurretBlockEntity
                          RemoteTerminalItem       RocketTurretBlockEntity
                          AmmunitionLauncherItem   MissileTurretBlockEntity
                          RadarItem                GrenadeLauncherTurretBlockEntity
                                                   JunkTurretBlockEntity
                                                   PhaseFieldTurretBlockEntity
                                                   ResonanceFieldTurretBlockEntity
                                                           │
                                                           ▼
                                                   TurretProjectileEntity
                                                   ├── TurretBulletEntity
                                                   ├── RailgunBulletEntity
                                                   ├── RocketEntity
                                                   ├── MissileEntity
                                                   ├── GrenadeEntity
                                                   └── JunkProjectileEntity
                                                           │
                                                           ▼
                                                   ModNetwork (网络包)
                                                   ├── SmartChipConfigPacket
                                                   ├── TurretFaceConfigPacket
                                                   ├── RemoteTerminalQueryPacket
                                                   └── ...
                                                           │
                                                           ▼
                                                   ClientEvents (渲染)
                                                   ├── GeoRenderer × 9
                                                   ├── EntityRenderer × 7
                                                   └── GUI Screens
```

---

## 术语附录

| 术语 | 解释 | 出处在哪 |
|------|------|----------|
| **Mod ID** | Mod 的唯一标识符，全小写英文+下划线。如 `advanced_turret` | gradle.properties, TurretMod.java |
| **DeferredRegister** | Forge 的延迟注册工具。先收集所有要注册的内容，然后在合适时机一次性提交 | ModItems.java, ModBlocks.java |
| **RegistryObject** | 注册后的"引用句柄"，通过 `.get()` 获取实际对象 | 所有注册文件 |
| **BlockEntity（方块实体）** | 有逻辑的方块，可以存储数据、每 tick 更新 | TurretBaseBlockEntity.java |
| **Entity（实体）** | 可以移动的游戏对象（子弹、生物、掉落物） | TurretProjectileEntity.java |
| **NBT（Named Binary Tag）** | Minecraft 的数据存储格式，类似于 JSON | SmartChipItem.java, TurretBaseBlockEntity.java |
| **DataTicket** | GeckoLib 的数据同步机制，用于在客户端/服务端间同步动画数据 | TurretMod.java, MachineGunTurretBlockEntity.java |
| **GeckoLib** | 第三方 Minecraft 动画库，支持 Blockbench 模型和骨骼动画 | build.gradle, GeoRenderer |
| **Parchment** | 社区维护的 Minecraft 映射，提供人类可读的参数名和 Javadoc | gradle.properties |
| **Capability** | Forge 的能力系统，用于给方块实体添加能量/物品/流体接口 | TurretBaseBlockEntity.java |
| **ContainerData** | GUI 和服务端之间的数据同步通道 | TurretBaseBlockEntity.java, TurretMenu.java |
| **位掩码（Bitmask）** | 用整数的每个 bit 表示一个布尔值，节省存储空间 | TurretBaseBlockEntity.java (enabledFacesMask) |
| **Tooltip** | 物品的悬浮提示文字 | ModItems.java (describedItem) |
| **ResourceLocation** | Minecraft 的资源路径标识符，格式 `namespace:path` | TurretMod.java (location方法) |
| **EventBus** | Forge 的事件总线，用于注册事件监听器 | TurretMod.java |
| **FriendlyFire** | 友伤保护，防止炮塔攻击所有者及其宠物 | SmartChipItem.java, TurretTargetFilterHelper.java |
| **ThriftyMode（厉行节约）** | 防止多个炮塔浪费火力在同一目标上 | SmartChipItem.java, TurretBaseBlockEntity.java |
| **PredictiveAiming** | 预判瞄准，根据目标移动速度提前偏移瞄准点 | SmartChipItem.java, MachineGunTurretBlockEntity.java |

---

## 全局术语索引

以上术语已同步至全局术语索引。如需在其他教程中引用，请使用本表。

---

## 学习建议

1. **不要一次读完**：每天完成 2-3 个阶段，给自己消化时间。
2. **动手实验**：在 IDE 中打开项目，用"Find Usages"功能追踪函数调用链。例如：右键 `getBaseEntity()` → "Find Usages" → 看看哪些地方调用了它。
3. **修改小参数**：尝试修改 `Config.java` 中的某个数值（如把机枪伤害从 4.0 改成 40.0），运行游戏看看效果。这能帮你理解"配置 → 代码 → 游戏"的完整链路。
4. **对比学习**：读完 `MachineGunTurretBlockEntity` 后，对比阅读 `LaserTurretBlockEntity`（激光是持续伤害，不是发射子弹）和 `GrenadeLauncherTurretBlockEntity`（榴弹有抛物线弹道），理解不同炮塔的差异。
5. **重点关注**：如果你将来想自己写 Mod，最需要理解的部分是：
   - 注册系统（ModItems/ModBlocks/ModBlockEntities/ModEntities 的模式）
   - 方块实体（TurretBaseBlockEntity 的完整结构）
   - 网络通信（一个数据包的标准写法）
   - Gui 系统（Menu + Screen 的配合）

---

> **本路线图编写时间**：2026-06-07
> **基于项目版本**：advanced_turret v1.0.0
> **编写方式**：逐文件通读源码后手工编写，每个文件都经过实际代码确认。