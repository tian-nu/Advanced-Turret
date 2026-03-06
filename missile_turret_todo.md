# 导弹炮塔开发任务清单 (TODO)

> **创建时间**: 2026-03-05
> **状态**: ✅ 完成

---

## ✅ 所有前置检查已通过
- [x] 检查 AI必看.md（已读）
- [x] 检查 炮塔数值与机制.md（已读）
- [x] 检查 炮塔制作流程.md（已读）
- [x] 检查现有代码（MachineGunTurret, RocketTurret, RocketEntity等）
- [x] 创建开发任务规划文件 (`missile_turret_development.task`)
- [x] 更新 AI必看.md 添加制导机制说明

---

## TODO 列表

### 阶段 1: 核心代码文件

- [x] TODO-1: 创建 `MissileTurretBlock.java` ✅
- [x] TODO-2: 创建 `MissileTurretBlockEntity.java` ✅

### 阶段 2: 注册

- [x] TODO-3: 在 `ModBlocks.java` 中注册炮塔 ✅
- [x] TODO-4: 在 `ModBlockEntities.java` 中注册方块实体 ✅
- [x] TODO-5: 在 `TurretMod.java` 中注册 GeckoLib 数据票⚠️ ✅
- [x] TODO-6: 在 `TurretBaseBlockEntity.java` 的 hasTurretOnFace() 中添加检测⚠️ ✅

### 阶段 3: 弹药和物品

- [x] TODO-7: 在 `ModItems.java` 中添加导弹弹药 ✅
- [x] TODO-8: 在 `ModCreativeModeTabs.java` 中添加到创造模式标签页 ✅

### 阶段 4: 子弹实体

- [x] TODO-9: 创建 `MissileEntity.java` (制导追踪 + 越飞越快) ✅
- [x] TODO-10: 在 `ModEntities.java` 中注册导弹实体 ✅
- [x] TODO-11: 在 `TurretProjectileEntity.java` 的 getBaseFromBlockEntity() 中添加支持 ✅

### 阶段 5: 渲染

- [x] TODO-12: 创建 `MissileTurretGeoModel.java` ✅
- [x] TODO-13: 创建 `MissileTurretGeoRenderer.java` ✅
- [x] TODO-14: 创建 `MissileRenderer.java` (子弹渲染器) ✅
- [x] TODO-15: 在 `ClientEvents.java` 中注册渲染器 ✅

### 阶段 6: 资源文件

- [x] TODO-16: 创建方块模型 JSON (`missile_turret.json`) ✅
- [x] TODO-17: 创建物品模型 JSON (`missile_turret.json`, `missile.json`) ✅
- [x] TODO-18: 创建 Blockstate JSON (`missile_turret.json`) ✅
- [x] TODO-19: 创建 GeckoLib 模型文件 (`missile_turret.geo.json`) ✅
- [x] TODO-20: 创建 GeckoLib 动画文件 (`missile_turret.animation.json`) ✅
- [x] TODO-21: 创建贴图文件 (`missile_turret.png`, `missile.png`) ✅
- [x] TODO-22: 添加中文翻译 ✅
- [x] TODO-23: 添加英文翻译 ✅

### 阶段 7: 验证和测试

- [x] TODO-24: 运行编译测试 ✅ BUILD SUCCESSFUL
- [ ] TODO-25: 游戏内测试功能（待进行）

---

## 📊 统计

- **总任务数**: 25个
- **已完成**: 24个 (96%)
- **待游戏测试**: 1个
- **文件总数**: 24个（6个Java新建 + 9个资源新建 + 9个修改）
- **编译状态**: ✅ BUILD SUCCESSFUL

---

## 📝 实现说明

### 导弹制导机制关键代码
见 AI必看.md 第 21 节"导弹制导机制实现"

### 导弹越飞越快机制关键代码
见 AI必看.md 第 22 节"导弹越飞越快机制"

---

## 📚 相关文档

- [导弹炮塔开发完成总结.md](./导弹炮塔开发完成总结.md)
- [导弹炮塔对比文档.md](./导弹炮塔对比文档.md)

---

**状态**: ✅ 完成
**完成时间**: 2026-03-05
