# BlockOffensive（方块攻势）— 项目上下文与代码索引

## 项目全局概览

基于 **FPSMatch 框架** 构建的 Minecraft Forge 1.20.1 模组，在 MC 中复刻 **反恐精英 (CS)** 完整对局流程。核心技术栈：Forge
47.4.6 + Mixin 0.8.5 + ModernUI 3.12 + TaCZ 1.1.7（枪械）。FPSMatch 作为 git submodule + composite build 协同开发。

## 核心业务逻辑与数据流

```
玩家加入地图 ──→ BaseMap.join() ──→ MapTeams 分配队伍 (CT/T)
                                    │
                                    ▼
                              开赛 ──→ 经济系统初始化 (ShopCapability)
                                    │
                    ┌───────────────┤
                    ▼               ▼
              回合进行中        暂停/等待阶段
              (CSGameMap.tick)   (PauseCapability)
                    │
        ┌───────────┼───────────┐
        ▼           ▼           ▼
    玩家击杀     C4 安放     回合超时
    (handleDeath) (CompositionC4) (tick 计时)
        │           │           │
        └───────────┼───────────┘
                    ▼
              victoryGoal() 判定胜负 ──→ 经济结算 / 队伍交换 / MVP 评选
                    │
                    ▼
            reset() ──→ 下一回合 || 比赛结束 (cleanupMap)
```

**数据通路**：服务端 `CSGameMap` / `CSMap`（核心逻辑层）→ `net/` 包（`SimpleChannel` 网络同步）→ 客户端 `CSClientData`（状态缓存）→
`client/screen/hud/`（HUD/Overlay/TAB 渲染）

**核心类层次**：`BaseMap`(FPSMatch) → `CSMap`(抽象，972行) → `CSGameMap`(爆破模式，1541行) / `CSDeathMatchMap`(
死亡竞赛，475行)

## 代码地图（核心）

### 根入口

| 路径                                         | 职责                                                   |
|--------------------------------------------|------------------------------------------------------|
| `src/main/java/.../BlockOffensive.java:46` | `@Mod` 入口，注册 items/entities/sounds/configs/packets   |
| `src/main/java/.../BOConfig.java`          | Client 配置（击杀消息HUD）+ Common 配置（脚步声/C4引信/游戏规则/web服务器）  |
| `src/main/templates/META-INF/mods.toml`    | Forge mod 元数据，声明依赖 forge/minecraft/modernui/fpsmatch |

### 对局核心 (`map/`)

| 文件                         | 行数   | 核心类/职责                                                                                             |
|----------------------------|------|----------------------------------------------------------------------------------------------------|
| `map/CSMap.java`           | 972  | `abstract class CSMap extends BaseMap` — CS 通用逻辑：队伍切换、经济系统、购买区域、暂停、友伤、出生点、死亡处理（含武器掉落/击杀消息构建/布娃娃兼容） |
| `map/CSGameMap.java`       | 1541 | `class CSGameMap extends CSMap` — 爆破模式核心：回合循环、C4 安放/引爆/拆除判定、MVP 评选、胜利条件、回合结束事件                     |
| `map/CSDeathMatchMap.java` | 475  | `class CSDeathMatchMap extends CSMap` — 死亡竞赛：无限重生、计分板、玩家移动检测                                       |
| `map/CSGameEvents.java`    | —    | `@EventBusSubscriber` 静态事件处理：玩家登录/离开/死亡/重生 hook                                                    |
| `map/shop/ItemType.java`   | —    | `enum ItemType implements INamedType` — 商店分类：装备/手枪/中阶/步枪/投掷物                                       |

### C4 系统

| 文件                                | 行数  | 核心类/职责                                                                                                                           |
|-----------------------------------|-----|----------------------------------------------------------------------------------------------------------------------------------|
| `item/CompositionC4.java`         | 281 | `class CompositionC4 extends Item implements BlastBombItem` — C4 物品：右键使用、放置动画、区域校验、安放成功→生成实体                                     |
| `item/BombDisposalKit.java`       | —   | `class BombDisposalKit extends Item` — 拆弹套件物品                                                                                    |
| `entity/CompositionC4Entity.java` | 516 | `class CompositionC4Entity extends BlastBombEntity` — C4 实体：引信倒计时、拆除进度条、爆炸伤害（即死半径/爆炸半径）、粒子特效、状态同步(NONE→TICKING→DEFUSED/EXPLODED) |

### 网络层 (`net/`)

| 子包          | 关键类                               | 方向  | 职责                               |
|-------------|-----------------------------------|-----|----------------------------------|
| `net/bomb/` | `BombActionC2SPacket`             | C2S | 客户端安放/拆除按键状态                     |
|             | `BombActionS2CPacket`             | S2C | 触发客户端重读拆弹按键                      |
|             | `BombDemolitionProgressS2CPacket` | S2C | 拆除进度同步(0.0~1.0)                  |
| `net/shop/` | `ShopStatesS2CPacket`             | S2C | 商店可开/金钱/关闭时间                     |
| `net/spec/` | `KillCamS2CPacket`                | S2C | 击杀回放数据(双方位置、武器)                  |
|             | `CSGameWeaponDataS2CPacket`       | S2C | 观战时目标玩家武器数据                      |
|             | `BombFuseS2CPacket`               | S2C | C4 引信剩余时间                        |
|             | `SwitchSpectateC2SPacket`         | C2S | 观战切换上下一个队友                       |
|             | `RequestAttachTeammateC2SPacket`  | C2S | 请求附着观战队友                         |
| `net/mvp/`  | `MvpMessageS2CPacket`             | S2C | MVP 评选结果                         |
|             | `MvpHUDCloseS2CPacket`            | S2C | 关闭 MVP HUD                       |
| `net/dm/`   | `PlayerMoveC2SPacket`             | C2S | DM 模式玩家移动心跳                      |
| 根           | `DeathMessageS2CPacket`           | S2C | 死亡消息(killer/victim/weapon/flags) |
|             | `CSGameSettingsS2CPacket`         | S2C | 比赛设置(比分/时间/状态)                   |
|             | `CSTabRemovalS2CPacket`           | S2C | 从 TAB 移除玩家                       |
|             | `PxDeathCompatS2CPacket`          | S2C | PhysicsMod 布娃娃创建通知               |
|             | `PxRagdollRemovalCompatS2CPacket` | S2C | PhysicsMod 布娃娃移除                 |

数据包注册位置：`BlockOffensive.java:76-93` (`commonSetup` 方法)

### 客户端渲染 (`client/`)

| 文件                                             | 核心类                                 | 职责                                                                                   |
|------------------------------------------------|-------------------------------------|--------------------------------------------------------------------------------------|
| `client/BOClientBootstrap.java`                | —                                   | 客户端启动：注册按键绑定、TAB渲染器、HUD、实体渲染器                                                        |
| `client/BOClientEvent.java`                    | —                                   | 客户端 tick 事件：移动锁定、web服务器启停、GUI缩放控制                                                    |
| `client/data/CSClientData.java`                | `CSClientData`                      | 客户端全局状态单例：回合时间、比分、金钱、炸弹状态、武器数据Map                                                    |
| `client/data/WeaponData.java`                  | `record WeaponData`                 | 武器数据记录：weaponData(Map<String,List<String>>) + 护甲属性                                   |
| `client/key/`                                  | 4个 KeyBinding                       | `OpenShopKey(B)`, `DismantleBombKey(E)`, `SwitchSpectatorKey(A/D)`, `TeamChatKey(U)` |
| `client/screen/CSGameShopScreen.java`          | `CSGameShopScreen`                  | ModernUI 购买界面：5列×5行武器槽，购买/退款，团队色渲染                                                   |
| `client/screen/TeamChatScreen.java`            | `TeamChatScreen`                    | 队伍频道聊天界面                                                                             |
| `client/screen/hud/CSGameHud.java`             | `CSGameHud implements IHudRenderer` | **主HUD编排器**：自定义快捷栏、血量护甲、弹药信息；取消所有原版HUD                                               |
| `client/screen/hud/CSGameOverlay.java`         | `CSGameOverlay`                     | 爆破模式顶栏：回合计时、CT/T比分、存活玩家头像、金钱、拆弹进度条                                                   |
| `client/screen/hud/CSDMOverlay.java`           | `CSDMOverlay`                       | 死亡竞赛顶栏：回合计时、Top15玩家排行                                                                |
| `client/screen/hud/CSGameTabRenderer.java`     | `CSGameTabRenderer`                 | CS 计分板：Ping/头像/名字/金钱/击杀/死亡/助攻/爆头率/伤害                                                 |
| `client/screen/hud/CSDMTabRenderer.java`       | `CSDMTabRenderer`                   | DM 计分板：额外有 K/D 和 Score 列                                                             |
| `client/screen/hud/CSDeathMessageHud.java`     | `CSDeathMessageHud`                 | 击杀信息流：杀手+武器图标+受害者+特殊条件图标                                                             |
| `client/screen/hud/CSMvpHud.java`              | `CSMvpHud`                          | MVP 结算动画：胜利横幅 + 选手面板渐变过渡                                                             |
| `client/screen/hud/CSSpectatorHudOverlay.java` | `CSSpectatorHudOverlay`             | 观战信息栏：被观战者名片/头像/武器/血量/爆头率                                                            |
| `client/spec/KillCamManager.java`              | `KillCamManager`(1185行)             | **击杀回放系统**：相机拉远→灰度滤镜→击杀信息面板→黑屏过渡→自动附着队友                                              |
| `client/renderer/C4Renderer.java`              | `C4Renderer`                        | C4 实体渲染器（委托 ItemEntityRenderer）                                                      |
| `client/renderer/ShopSlotRenderer.java`        | `ShopSlotRenderer`                  | 商店图标渲染器（含购买力灰色处理）                                                                    |

### 兼容层 (`compat/`)

| 文件                                | 职责                                       |
|-----------------------------------|------------------------------------------|
| `compat/BOImpl.java`              | 可选模组存在性检测（isPhysicsModLoaded 等）          |
| `compat/BOMenuIntegration.java`   | ClothConfig 模组菜单集成                       |
| `compat/CSGrenadeCompat.java`     | CounterStrikeGrenade 模组兼容：击杀图标注册、投掷物类型映射 |
| `compat/HitIndicationCompat.java` | HitIndication 模组兼容：3D 方向命中标记渲染           |
| `compat/PhysicsModCompat.java`    | PhysicsMod 布娃娃兼容（292行）                   |
| `compat/IPassThroughEntity.java`  | 穿墙/穿烟标记接口                                |

### Mixin 注入 (`mixin/`)

| 文件                                               | 作用                             |
|--------------------------------------------------|--------------------------------|
| `mixin/StepSoundMixin.java`                      | 自定义脚步声（队友/敌人/蒙脚音量）             |
| `mixin/C4KitsItemEntityMixin.java`               | C4/拆弹套件掉落物实体处理                 |
| `mixin/ammo/AmmoEntityMixin.java`                | 弹药实体行为调整                       |
| `mixin/ammo/DefaultAmmoMixin.java`               | 默认弹药行为                         |
| `mixin/ammo/TweakAmmoMixin.java`                 | TaczTweaks 弹药适配（条件加载）          |
| `mixin/PlayerRendererMixin.java`                 | [client] 玩家渲染调整                |
| `mixin/compat/PhysicsModBlockifyGuardMixin.java` | PhysicsMod 布娃娃守卫               |
| `mixin/accessor/BOClipContextAccessor.java`      | 渲染裁剪上下文访问器                     |
| `BlockOffensiveMixinPlugin.java`                 | Mixin 条件加载插件（TaczTweaks 存在性检查） |

### 事件系统 (`event/`)

| 文件                                   | 职责                           |
|--------------------------------------|------------------------------|
| `event/CSGameMapEvent.java`          | 地图事件基类：队伍交换、玩家放置C4           |
| `event/CSGamePlayerGetMvpEvent.java` | 玩家获得 MVP                     |
| `event/CSGameRoundEndEvent.java`     | 回合结束（含 `WinnerReason` 枚举）    |
| `event/CSHUDRenderEvent.java`        | HUD 渲染事件（含 MVP Pre/Post 子事件） |

### 其他关键文件

| 文件                                                 | 职责                                                    |
|----------------------------------------------------|-------------------------------------------------------|
| `entity/BOEntityRegister.java`                     | 实体类型注册（C4）                                            |
| `item/BOItemRegister.java`                         | 物品/创造标签页注册（C4、拆弹套件）                                   |
| `sound/BOSoundRegister.java`                       | 42 种自定义音效注册                                           |
| `sound/MVPMusicManager.java`                       | MVP 音乐管理                                              |
| `command/CSCommand.java`                           | `/cs2 <action>` 命令，委托 `CSGameMap.handleChatCommand()` |
| `spectator/BOSpecManager.java`                     | 观战管理：自由/附着模式、击杀回放、队友轮换                                |
| `spectator/DamagePosTracker.java`                  | 全局受伤位置追踪，供 KillCam fallback 使用                        |
| `data/DeathMessage.java`                           | 死亡消息数据模型（Builder模式）                                   |
| `data/MvpReason.java`                              | MVP 理由数据模型                                            |
| `data/persistence/CSGameMapFixer.java`             | 旧版地图 JSON → 新 capability 格式迁移                         |
| `util/BOUtil.java`                                 | 通用工具：投掷物注册、队伍聊天消息构建                                   |
| `util/ThrowableType.java`                          | 投掷物类型枚举                                               |
| `map/team/capability/ColoredPlayerCapability.java` | 队伍颜色 Capability                                       |
| `web/BOClientWebServer.java`                       | 客户端 HTTP 服务器：`GET /api/data` 返回 JSON 比赛状态             |

### 资源

| 路径                                                | 内容                             |
|---------------------------------------------------|--------------------------------|
| `resources/assets/blockoffensive/sounds.json`     | 42 种音效定义                       |
| `resources/assets/blockoffensive/sounds/`         | 105 个 .ogg 音频文件                |
| `resources/assets/blockoffensive/lang/zh_cn.json` | 中文翻译 (~166行)                   |
| `resources/blockoffensive.mixins.json`            | Mixin 配置 (6 common + 1 client) |

### FPSMatch 框架依赖关系

- **`FPSMCore`** — 单例比赛管理器：`getInstance()`, `getMapByPlayer()`, `registerGameType()`
- **`BaseMap`** — 抽象地图基类：生命周期 (`start/tick/victory/reset/cleanup`)、队伍管理、网络广播
- **`MapTeams` / `ServerTeam` / `TeamData`** — 队伍系统
- **`FPSMShop<T>`** — 泛型商店系统：`handleButton(player, type, index, action)`
- **`CapabilityMap / MapCapability / TeamCapability`** — 可插拔功能系统
- 内置 Capability：`SpawnPointCapability`, `ShopCapability`, `StartKitsCapability`, `DemolitionModeCapability`,
  `GameEndTeleportCapability`
- **`NetworkPacketRegister`** — 反射式数据包注册器

## 关键 API 与定位线索

### 服务端核心入口

| 需求         | 定位                                                                                                 |
|------------|----------------------------------------------------------------------------------------------------|
| 对局创建/开始/结束 | `FPSMCore.getInstance().registerMap()` → `BaseMap.start()` → `victoryGoal()/victory()` → `reset()` |
| 回合逻辑主循环    | `CSGameMap.tick()`（每 tick 调用）                                                                      |
| 玩家击杀处理     | `CSGameMap.handleDeath(DeathContext)`                                                              |
| C4 安放流程    | `CompositionC4.use()` → `finishUsingItem()` 生成 `CompositionC4Entity`                               |
| C4 拆除流程    | `CompositionC4Entity` 监听 `BombActionC2SPacket` → `handleDefuseKey()`                               |
| 回合胜利判定     | `CSGameMap.victoryGoal()` 返回 boolean                                                               |
| 商店交互       | `CSGameShopScreen` → `ShopActionC2SPacket` → `FPSMShop.handleButton()`                             |
| 聊天指令       | `/cs2 <action>` → `CSCommand.onRegisterCommands()` → `CSGameMap.handleChatCommand()`               |

### 客户端核心入口

| 需求      | 定位                                                              |
|---------|-----------------------------------------------------------------|
| HUD 渲染  | `CSGameHud` 实现 `IHudRenderer`，取消原版 HUD                          |
| 击杀消息显示  | `CSDeathMessageHud`，接收 `DeathMessageS2CPacket`                  |
| 计分板     | `CSGameTabRenderer` / `CSDMTabRenderer`，注册于 `BOClientBootstrap` |
| 购买界面    | `OpenShopKey` → `CSGameShopScreen`                              |
| 击杀回放    | `KillCamManager`，接收 `KillCamS2CPacket`                          |
| 观战面板    | `CSSpectatorHudOverlay`                                         |
| 状态缓存    | `CSClientData` (static fields)，由各 S2C 数据包更新                     |
| web API | `BOClientWebServer` → `GET /api/data`                           |

### 扩展点

- **新增游戏模式**：继承 `CSMap`（或直接 `BaseMap`），实现 `victoryGoal()`/`getGameType()`，通过
  `FPSMCore.registerGameType()` 注册
- **新增网络包**：在 `net/` 下新建类，于 `BlockOffensive.commonSetup()` 中 `PACKET_REGISTER.registerPacket()`
- **新增 HUD 组件**：实现 `IHudRenderer`，通过 `BOClientBootstrap` 注册
- **新增 Capability**：实现 `MapCapability`/`TeamCapability`，通过 `CapabilityMap` 挂载
- **兼容层**：在 `compat/` 下添加新类，在 `BOImpl` 添加检测方法

## 当前开发状态与下一步计划

**版本**：1.3.0-snapshot（Forge 1.20.1）
**最新提交**：`b186043` — 添加 KubeJS 兼容 + 修复不显示爆头 bug

**近期已完成的重构/修复**：

- 死亡管线逻辑重构（`538a256`）
- 修复 C4 爆炸在友伤关闭时不伤队友、击杀统计错误、CT 胜利音效错误（`184e58b`）
- 修复客户端实体意外断连（`1c49716`）
- 发包注册结构重构，隔离客户端 S2C 处理逻辑（`a673840`）
- PhysicsMod 布娃娃注入失败修复（`71c0a8b`）
- 队伍逻辑优化（`1354608`）

**已知状态/待完善**：

1. `isSnapshot=true` — 当前为快照开发版本，尚未发布正式版
2. `compat/kubejs/` 目录存在但为空 — KubeJS 兼容初步添加，功能端可能仍待实现
3. FPSMatch 作为 submodule 紧密耦合 — 两仓库 commit 同步前进，需关注子模块版本对齐
4. `CSGameMap` (1541行) 较长，存在进一步模块化拆分的重构空间
5. 客户端 `KillCamManager` (1185行) 同样较长，灰度滤镜有性能回退机制
6. `CSGameMapFixer` 负责旧版地图数据迁移，新版本正式发布后可考虑移除
