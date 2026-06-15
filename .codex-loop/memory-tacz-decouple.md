# TACZ 解耦 - 工作记忆

## 当前状态
- **阶段**: 全部完成
- **分支**: BlockOffensive `decouple-tacz`, FPSMatch `decouple-tacz` (均已创建)

## 已完成
- [x] 创建两个项目的解耦分支
- [x] 全面搜索 TACZ 引用
- [x] 编写解耦计划文档
- [x] 阶段 1: 创建抽象层 (IGunProvider, GunDataDTO, GunTabTypeEnum, NoGunProvider, GunCompatManager)
- [x] 阶段 2: TACZ 实现 (TACZGunProvider, TACZGunEventBridge, TACZBootstrap)
- [x] 阶段 3: FPSMatch 迁移 (20+ 文件，62 调用点)
- [x] 阶段 4: BlockOffensive 迁移 (7 文件)
- [x] 阶段 5: 构建配置 (modCompileOnly, mandatory=false, Mixin 条件注入)
- [x] 两个项目编译通过 (BUILD SUCCESSFUL)
- [x] 多 Provider 注册制改造 (GunCompatManager 从单 Provider 改为 LinkedHashMap 多 Provider)
- [x] 事件桥接安全加固：TACZGunEventBridge 移除 @Mod.EventBusSubscriber 自动注册，改为 TACZBootstrap 判断模组加载后手动注册

## 最新改动 (2026-06-16)

### TACZGunEventBridge 注册方式优化
- **之前**: 使用 `@Mod.EventBusSubscriber` 自动注册 + 每个方法内 `isTaczLoaded()` 运行时检查
- **现在**: 移除 `@Mod.EventBusSubscriber`，由 `TACZBootstrap` 在确认 TACZ 已加载后手动调用 `MinecraftForge.EVENT_BUS.register(TACZGunEventBridge.class)`
- **优势**: TACZ 未加载时，EventBridge 类不会被加载，事件处理器不会被注册到总线，从根源上避免问题
- **模式**: 其他枪械模组的 EventBridge 应遵循相同模式

### 当前架构
```
GunCompatManager (多 Provider 注册制)
├── providers: LinkedHashMap<String, IGunProvider>
├── findProvider(ItemStack) → 按物品路由到正确 Provider
├── isGun(ItemStack) → 检查所有 Provider
├── getGunData(ResourceLocation) → 遍历所有 Provider
└── isInGame() → 检查所有 Provider

TACZBootstrap (MOD bus)
├── if (tacz loaded):
│   ├── GunCompatManager.register(new TACZGunProvider())
│   └── MinecraftForge.EVENT_BUS.register(TACZGunEventBridge.class)

TACZGunEventBridge (普通类，不再使用 @Mod.EventBusSubscriber)
├── onGunFire → FPSMGunFireEvent
├── onGunReload → FPSMGunReloadEvent
├── onGunShoot → FPSMGunShootEvent
├── onGunKill → FPSMGunKillEvent
└── onEntityHurtByGun → FPSMGunDamageEvent
```

## 关键决策
- 抽象层名称：`IGunProvider` + `GunCompatManager`（多 Provider 注册制）
- FPSMatch 新增包：`com.phasetranscrystal.fpsmatch.compat.gun`
- BlockOffensive 复用 FPSMatch 的抽象层
- TACZ 兼容层保留在现有包内，通过条件加载控制
- 事件桥接：TACZ 事件 → FPSM 自定义事件（TACZGunEventBridge）
- 事件注册：判断模组加载后再注册，不依赖 @Mod.EventBusSubscriber 自动注册

## 约束
- 两个项目都跑在同一个 workspace，BlockOffensive 依赖 FPSMatch
- 先改 FPSMatch，再改 BlockOffensive
- 每个阶段完成后需要编译验证