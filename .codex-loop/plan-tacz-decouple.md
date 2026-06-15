# TACZ 解耦计划

## 目标

将 BlockOffensive 和 FPSMatch 两个项目中对 TACZ（Timeless and Classics Zero）模组的硬依赖完全解耦，通过抽象层使得：

1. TACZ 从编译时依赖变为可选运行时依赖
2. 支持未来替换为其他枪械模组实现
3. 核心逻辑不再直接依赖 TACZ API 类

## 分支状态

- **BlockOffensive**: `decouple-tacz` 分支（已创建）
- **FPSMatch**: `decouple-tacz` 分支（已创建）
- 两个分支均基于 `master` 创建，工作区干净

---

## 第一部分：FPSMatch TACZ 解耦

### 1.1 涉及文件清单（共 ~70 个文件）

#### 1.1.1 构建配置（3 个）
| 文件 | 改动 |
|------|------|
| `build.gradle` | TACZ/TACZ Tweaks/LR Tactical 依赖改为 compileOnly；移除 `modImplementation` |
| `gradle.properties` | 保留 `tacz_version` 作为编译参考 |
| `src/main/templates/META-INF/mods.toml` | 移除 `tacz` 依赖声明 |

#### 1.1.2 纯 TACZ 兼容层 — 保留为独立模块包（25 个文件）
这些文件本身就是 TACZ 专用的适配层，解耦后保留在 `compat.tacz` 和 `compat.spectate.tacz` 包内，但需要改为通过抽象接口调用而非直接 import TACZ 类。

**`compat/tacz/` 包（入口 + 客户端）**：
| 文件 | 说明 |
|------|------|
| `compat/tacz/TACZCompat.java` | TACZ 兼容层入口，当前为空壳 |
| `compat/tacz/client/animation/GunAnimationController.java` | 枪械动画控制 |
| `compat/tacz/client/event/SpectatorEventHandler.java` | 旁观者 TACZ 事件处理 |
| `compat/tacz/client/fakeitem/ClientFakeItemManager.java` | 假物品管理（仅含 `IGun` 检查） |
| `compat/tacz/client/shoot/SpecLocalShoot.java` | 旁观者射击模拟 |
| `compat/tacz/client/test/TaczSpecScreenShake.java` | 屏幕震动 |
| `compat/tacz/client/util/GunSpecUtils.java` | 工具类 |
| `compat/tacz/client/util/SpecRenderStacks.java` | 渲染栈 |

**`compat/spectate/tacz/` 包（旁观者 TACZ 适配）**：
| 文件 | 说明 |
|------|------|
| `compat/spectate/tacz/SpectatorGunItemMirror.java` | 物品镜像（仅 `IGun` 检查） |
| `compat/spectate/tacz/SpectatorGunItemMirrorTicker.java` | 镜像 ticker |
| `compat/spectate/tacz/SpectatorGunFireMirror.java` | 开火镜像 |
| `compat/spectate/tacz/SpectatorGunReloadMirror.java` | 换弹镜像 |
| `compat/spectate/tacz/SpectatorGunMovementMirror.java` | 移动镜像 |
| `compat/spectate/tacz/SpectatorGunRecoil.java` | 后坐力镜像 |
| `compat/spectate/tacz/SpectatorGunInspect.java` | 检视镜像 |
| `compat/spectate/tacz/SpectatorGunInspectNet.java` | 检视网络 |
| `compat/spectate/tacz/SpectatorGunShootSimulator.java` | 射击模拟 |
| `compat/spectate/tacz/SpectatorCameraRecoil.java` | 相机后坐力 |
| `compat/spectate/tacz/SpectatorShootSway.java` | 射击摇摆 |
| `compat/spectate/tacz/SpectatorGunStacks.java` | 枪械栈 |
| `compat/spectate/tacz/OtherPlayerReloadSound.java` | 换弹音效 |

**`mixin/compat/spectate/tacz/` 包（TACZ Mixin 注入）**：
| 文件 | 说明 |
|------|------|
| `CameraSetupEventMixin.java` | 相机设置 Mixin |
| `FirstPersonRenderGunEventMixin.java` | 第一人称渲染 Mixin |
| `MixinGunAnimationStateContext.java` | 动画状态 Mixin |
| `MixinLocalPlayerAimSpectator.java` | 瞄准 Mixin |
| `MixinLocalPlayerAimSpectatorTick.java` | 瞄准 Tick Mixin |
| `MixinLocalPlayerInspectSync.java` | 检视同步 Mixin |
| `MixinLocalPlayerShootSpectator.java` | 射击 Mixin |
| `MixinTaczTickAnimationEvent.java` | 动画 Tick Mixin |
| `MixinClientPacketListenerSpectatorItemMirror.java` | 客户端包监听 Mixin |
| `MixinGameRendererSpectatorHands.java` | 渲染器 Mixin |
| `MixinLocalPlayerSpectatorReload.java` | 换弹 Mixin |

#### 1.1.3 弹药相关 Mixin（4 个）— 需条件加载
| 文件 | 说明 |
|------|------|
| `mixin/ammo/AmmoEntityMixin.java` | 弹药实体 Mixin（target: `EntityKineticBullet`） |
| `mixin/ammo/ModernKineticGunItemMixin.java` | 现代枪械物品 Mixin |
| `mixin/ammo/DefaultAmmoMixin.java` | 默认弹药 Mixin（target: `BlockRayTrace`） |
| `mixin/ammo/TweakAmmoMixin.java` | TACZ Tweaks 弹药 Mixin |

#### 1.1.4 其他 TACZ Mixin（2 个）
| 文件 | 说明 |
|------|------|
| `mixin/LivingEntityIsDeadOrDyingMixin.java` | target: `EntityKineticBullet` |
| `mixin/collisiobox/MixinRenderHeadShotAABB.java` | target: `RenderHeadShotAABB` |

#### 1.1.5 核心代码中散布的 TACZ 引用（20+ 个文件）

| 文件 | 使用的 TACZ API | 分类 |
|------|----------------|------|
| `FPSMatch.java` | `ClothConfigScreen`（仅用于无 Cloth Config 时的 fallback） | 配置 |
| `FPSMatchMixinPlugin.java` | `FPSMImpl.findTaczTweaks()` 条件判断 | 条件加载 |
| `compat/impl/FPSMImpl.java` | `TACZ`/`TACZ_TWEAKS` modId 常量 | 模组检测 |
| `util/FPSMUtil.java` | `TimelessAPI`, `GunTabType`, `IGun`, `CommonGunIndex` | 核心工具 |
| `util/FPSMFormatUtil.java` | `IGun`, `"tacz."` 翻译键前缀 | 格式化 |
| `util/BlockRayTraceReflector.java` | 反射调用 `BlockRayTrace`，`ModBlocks` | 工具 |
| `mixin/ItemDropSoundMixin.java` | `GunTabType`, `IGun` | 音效 |
| `common/sound/FPSMSoundRegister.java` | `GunTabType`, `IGun` | 音效注册 |
| `common/shop/functional/ChangeShopItemModule.java` | `IGun` | 商店 |
| `common/event/FPSMDeathPipelineEventHook.java` | `EntityKillByGunEvent`, `IGun` | 事件 |
| `common/entity/MatchDropEntity.java` | `GunTabType`, `IGun` | 实体 |
| `common/effect/FPSMEffectRegister.java` | `SyncConfig` | 效果 |
| `common/command/FPSMBaseCommand.java` | `TimelessAPI`, `IGun`, `CommonGunIndex`, `GunData`（`fpsm tacz dummy` 命令） | 命令 |
| `common/client/screen/EditShopSlotMenu.java` | `IGun` | 客户端 UI |
| `common/client/renderer/MatchDropRenderer.java` | `IGun` | 客户端渲染 |
| `common/client/net/SpectatorClientPacketHandlers.java` | `SpectatorGunInspectNet`（内部引用 compat 层） | 客户端网络 |
| `common/capability/team/ShopCapability.java` | `IGun` | 能力 |
| `common/attributes/ammo/GunDamageHandler.java` | `EntityHurtByGunEvent` | 属性 |
| `core/shop/skin/SkinHandler.java` | `IGun` | 商店皮肤 |
| `core/shop/slot/ShopSlot.java` | `IGun` | 商店槽位 |
| `core/shop/ShopData.java` | `IGun` | 商店数据 |

#### 1.1.6 资源文件
| 文件 | 说明 |
|------|------|
| `fpsmatch.mixins.json` | 8 个 TACZ compat Mixin 入口 |
| `assets/fpsmatch/lang/en_us.json` | `fpsm.tacz.*` 翻译键 |
| `assets/fpsmatch/lang/zh_cn.json` | `fpsm.tacz.*` 翻译键 |
| `data/tacz/tags/entity_types/interact_key/whitelist.json` | TACZ 数据标签 |

---

### 1.2 FPSMatch 抽象层设计

#### 核心思想：引入 `IGunProvider` 接口体系

```
com.phasetranscrystal.fpsmatch.compat.gun (新增)
├── IGunProvider.java              # 枪械提供者接口（顶层抽象）
├── GunDataDTO.java                # 枪械数据 DTO（纯 POJO，无 TACZ 依赖）
├── GunTabTypeEnum.java            # 枪械分类枚举（替代 GunTabType）
├── GunEventType.java              # 枪械事件类型枚举
├── GunCompatManager.java          # 枪械兼容管理器（ServiceLoader / 注册制）
├── NoGunProvider.java             # 默认空实现（无枪械模组时使用）
└── impl/
    └── tacz/
        ├── TACZGunProvider.java   # TACZ 实现（Adapter 模式）
        ├── TACZGunEventBridge.java # TACZ 事件桥接
        └── TACZBootstrap.java     # TACZ 启动引导
```

#### IGunProvider 接口设计

```java
public interface IGunProvider {
    String getModId();                              // 标识模组
    boolean isAvailable();                          // 模组是否已加载

    // 枪械识别
    boolean isGun(ItemStack stack);                 // 是否为枪械
    ResourceLocation getGunId(ItemStack stack);     // 获取枪械 ID
    GunTabType getGunTabType(ItemStack stack);      // 获取枪械类别

    // 弹药
    int getDummyAmmo(ItemStack stack);              // 获取备弹
    void setDummyAmmo(ItemStack stack, int amount); // 设置备弹
    int getMaxAmmo(ItemStack stack);                // 获取最大弹药
    int getCurrentAmmo(ItemStack stack);            // 获取当前弹药
    void setCurrentAmmo(ItemStack stack, int count);// 设置当前弹药

    // 枪械数据
    GunDataDTO getGunData(ItemStack stack);         // 获取枪械数据 DTO
    ResourceLocation getGunHUDTexture(ItemStack stack); // 获取 HUD 纹理

    // 音效
    SoundEvent getGunPickupSound(ItemStack stack);  // 获取拾取音效
    SoundEvent getGunDropSound(ItemStack stack);    // 获取丢弃音效
}
```

#### GunDataDTO（数据传输对象）

```java
public class GunDataDTO {
    private int ammoAmount;          // 弹匣容量
    private int maxDummyAmmo;        // 最大备弹
    private float damage;            // 伤害
    private float fireRate;          // 射速
    private FireMode fireMode;       // 射击模式
    private ResourceLocation gunId;  // 枪械 ID
    // ... 其他必要字段
}
```

#### GunCompatManager（服务发现）

```java
public class GunCompatManager {
    private static IGunProvider activeProvider = NoGunProvider.INSTANCE;

    public static void register(IGunProvider provider) {
        if (provider.isAvailable()) {
            activeProvider = provider;
        }
    }

    public static IGunProvider getProvider() {
        return activeProvider;
    }
}
```

---

### 1.3 FPSMatch 逐文件改造方案

#### A. 构建配置改造

1. **`build.gradle`**:
   - `tacz`/`tacz-tweaks`/`lrtactical` 从 `modImplementation` 改为 `modCompileOnly`
   - 新增 `compat/` 子模块或 sourceSet 隔离（可选，推荐）
   - 保留版本属性用于编译

2. **`mods.toml`**:
   - 移除 `tacz` 的硬依赖声明

#### B. 抽象接口优先改造（核心层）

对每个散布 TACZ 引用的文件，按优先级改造：

**优先级 P0 — 必须最先改造（被最多文件依赖）**：

| 文件 | 改造方式 |
|------|---------|
| `FPSMUtil.java` | 所有 `TimelessAPI`/`IGun`/`GunTabType` 调用改为 `GunCompatManager.getProvider()` |
| `FPSMFormatUtil.java` | `IGun` 判断改为 `getProvider().isGun()`；翻译键改为 `fpsmatch.gun.` 前缀 |
| `FPSMImpl.java` | 移除 `TACZ`/`TACZ_TWEAKS` 常量；改为通用 `findMod("tacz")` |
| `FPSMatchMixinPlugin.java` | `findTaczTweaks()` 改为通用条件判断 |
| `FPSMSoundRegister.java` | `GunTabType` 改为 `GunTabTypeEnum`；注册/查询从 `GunCompatManager` 走 |

**优先级 P1 — 事件依赖（需特殊处理）**：

| 文件 | TACZ 事件 | 改造方式 |
|------|----------|---------|
| `FPSMDeathPipelineEventHook.java` | `EntityKillByGunEvent` | 改为监听 FPSMatch 自定义事件 `FPSMGunKillEvent`，由 TACZ 兼容层桥接 |
| `GunDamageHandler.java` | `EntityHurtByGunEvent` | 同上，改为 `FPSMGunDamageEvent` |
| `SpectatorEventHandler.java` | `GunFireEvent`, `GunReloadEvent` | 改为监听 FPSMatch 自定义事件，由 TACZ 兼容层桥接 |

**事件桥接方案**：
```
TACZ GunFireEvent ──→ TACZGunEventBridge ──→ FPSMGunFireEvent (FPSMatch 自定义)
                                                  ↓
                                          SpectatorEventHandler
```

**优先级 P2 — 商店/物品/实体**：

| 文件 | 改造方式 |
|------|---------|
| `ShopData.java` | `IGun` 检查 → `getProvider().isGun()` |
| `ShopSlot.java` | `IGun` 检查 → `getProvider().isGun()` |
| `SkinHandler.java` | `IGun` 检查 → `getProvider().isGun()` |
| `ChangeShopItemModule.java` | `IGun` 检查 → `getProvider().isGun()` |
| `ShopCapability.java` | `IGun` 检查 → `getProvider().isGun()` |
| `MatchDropEntity.java` | `GunTabType`/`IGun` → `getProvider()` |
| `MatchDropRenderer.java` | `IGun` → `getProvider()` |
| `EditShopSlotMenu.java` | `IGun` → `getProvider()` |

**优先级 P3 — 命令/效果/工具**：

| 文件 | 改造方式 |
|------|---------|
| `FPSMBaseCommand.java` | `fpsm tacz dummy` 命令：新增 `fpsm gun dummy` 通用命令，`tacz` 子命令保留但标记为 deprecated |
| `FPSMEffectRegister.java` | `SyncConfig` 引用需要评估：如果只是配置读取，可通过反射或条件判断 |
| `BlockRayTraceReflector.java` | 已使用反射，改为通过 `IGunProvider` 接口暴露或保持反射 |
| `ItemDropSoundMixin.java` | `GunTabType`/`IGun` → `getProvider()` |
| `FPSMatch.java` | `ClothConfigScreen` 引用 → 改为 `BOImpl` 风格的条件判断 |
| `SpectatorClientPacketHandlers.java` | `SpectatorGunInspectNet` 引用 → 通过事件系统解耦 |

#### C. 兼容层保留但隔离

- `compat/tacz/` 和 `compat/spectate/tacz/` 包保留，但所有 TACZ 直接引用限于此包内
- Mixin 条件加载：`FPSMatchMixinPlugin` 中检查 TACZ 是否加载
- `fpsmatch.mixins.json` 中的 TACZ mixin 通过 `shouldApplyMixin` 条件控制

---

## 第二部分：BlockOffensive TACZ 解耦

### 2.1 涉及文件清单（共 ~15 个文件）

#### 2.1.1 构建配置（2 个）
| 文件 | 改动 |
|------|------|
| `build.gradle` | TACZ/TACZ Tweaks 改为 `modCompileOnly` |
| `gradle.properties` | 保留 `tacz_version` 作为编译参考 |

#### 2.1.2 散布 TACZ 引用的文件

| 文件 | 使用的 TACZ API | 分类 |
|------|----------------|------|
| `BlockOffensive.java` | `GunTabType`（用于音效注册），`ClothConfigScreen` | 主入口 |
| `map/CSGameMap.java` | `EntityKineticBullet` import（仅 import，未使用） | 地图 |
| `map/CSMap.java` | `GunTabType`, `IGun`（武器分类、弹夹模式） | 地图 |
| `map/CSGameEvents.java` | `TimelessAPI`, `GunReloadEvent`, `GunShootEvent`, `IGun`（弹夹模式核心逻辑） | 事件 |
| `data/DeathMessage.java` | `TimelessAPI`, `IGun`, `ClientGunIndex`（武器图标获取） | 数据 |
| `client/screen/hud/CSGameHud.java` | 大量 TACZ API（弹药 HUD 渲染核心） | HUD |
| `client/screen/CSGameShopScreen.java` | `TimelessAPI`, `GunTabType`, `IGun`, `GunDisplayInstance` | 商店 UI |
| `client/renderer/ShopSlotRenderer.java` | `TimelessAPI`, `GunTabType`, `IGun`, `GunDisplayInstance` | 渲染 |
| `client/key/TeamChatKey.java` | `InputExtraCheck.isInGame` | 按键 |
| `client/key/OpenShopKey.java` | `InputExtraCheck.isInGame` | 按键 |
| `client/key/DismantleBombKey.java` | `InputExtraCheck.isInGame` | 按键 |
| `client/BOClientEvent.java` | `IGun`, `InputExtraCheck` | 客户端事件 |

---

### 2.2 BlockOffensive 抽象层设计

BlockOffensive 复用 FPSMatch 的 `IGunProvider` 接口（因为 BO 依赖 FPSMatch）。

```
com.phasetranscrystal.blockoffensive.compat.gun (新增)
├── BOGunCompat.java     # BO 枪械兼容工具（封装 FPSMatch 的 IGunProvider）
```

#### 关键设计决策

1. **BlockOffensive 不自行实现 IGunProvider**，而是通过 FPSMatch 的 `GunCompatManager` 获取
2. **弹夹模式（magazine mode）** 是 TACZ 特有功能，改为通过 `IGunProvider` 的扩展接口 `IMagazineGunProvider` 提供
3. **InputExtraCheck.isInGame** 需要抽象为 FPSMatch 提供的 `IInputCheck` 接口

#### IMagazineGunProvider（扩展接口）

```java
public interface IMagazineGunProvider extends IGunProvider {
    // 弹夹模式支持
    int getMagazineCapacity(ItemStack stack);     // 弹匣容量
    int getMagazineCount(ItemStack stack);        // 弹匣数量
    void setMagazineCount(ItemStack stack, int count); // 设置弹匣数量
    void reloadFromMagazine(ItemStack stack);     // 从弹匣换弹
}
```

---

### 2.3 BlockOffensive 逐文件改造方案

#### 优先级 P0 — 最关键文件

**`CSGameEvents.java`** — 弹夹模式核心逻辑：
- `GunReloadEvent`、`GunShootEvent` 监听 → 改为监听 FPSMatch 自定义事件 `FPSMGunReloadEvent`、`FPSMGunShootEvent`
- `TimelessAPI.getCommonGunIndex()` → `GunCompatManager.getProvider().getGunData()`
- `IGun` 接口调用 → `GunCompatManager.getProvider()`

**`CSGameHud.java`** — 弹药 HUD（最复杂的 TACZ 依赖）：
- 所有 TACZ 客户端 API → 通过 `IGunProvider` + `IClientGunProvider` 扩展接口
- `ResourceLocation("tacz", ...)` → 改为 `fpsmatch` 命名空间下的资源，或由 provider 提供
- `GunDisplayInstance`、`ClientGunIndex` → 由 `IClientGunProvider` 提供替代

**`CSMap.java`** — 武器分类和弹夹模式：
- `GunTabType` → `FPSMatch` 的 `GunTabTypeEnum`
- `IGun` → `GunCompatManager.getProvider()`

#### 优先级 P1 — 客户端渲染

**`CSGameShopScreen.java`**：
- `TimelessAPI`、`GunDisplayInstance` → `GunCompatManager.getProvider()`
- `GunTabType` → `GunTabTypeEnum`

**`ShopSlotRenderer.java`**：
- 同上

**`DeathMessage.java`**：
- `IGun`、`TimelessAPI`、`ClientGunIndex` → `GunCompatManager.getProvider()`

#### 优先级 P2 — 按键和事件

**`TeamChatKey.java`、`OpenShopKey.java`、`DismantleBombKey.java`**：
- `InputExtraCheck.isInGame` → FPSMatch 提供的 `IInputCheck.isInGame()`

**`BOClientEvent.java`**：
- `IGun` → `GunCompatManager.getProvider()`
- `InputExtraCheck` → `IInputCheck`

**`BlockOffensive.java`**：
- `GunTabType` 音效注册 → 通过 `GunTabTypeEnum` 和 `GunCompatManager` 走
- `ClothConfigScreen` → 条件判断

**`CSGameMap.java`**：
- `EntityKineticBullet` import — 当前未使用，可直接移除

---

## 第三部分：事件依赖特殊处理

### 3.1 需要桥接的 TACZ 事件清单

| TACZ 事件 | 使用位置 | FPSMatch 自定义事件 | 桥接方向 |
|-----------|---------|-------------------|---------|
| `GunFireEvent` | `SpectatorEventHandler` | `FPSMGunFireEvent` | TACZ → FPSM |
| `GunReloadEvent` | `CSGameEvents`（弹夹模式）, `SpectatorEventHandler` | `FPSMGunReloadEvent` | TACZ → FPSM |
| `GunShootEvent` | `CSGameEvents`（DM 射击追踪） | `FPSMGunShootEvent` | TACZ → FPSM |
| `EntityKillByGunEvent` | `FPSMDeathPipelineEventHook` | `FPSMGunKillEvent` | TACZ → FPSM |
| `EntityHurtByGunEvent` | `GunDamageHandler` | `FPSMGunDamageEvent` | TACZ → FPSM |
| `TickAnimationEvent` | `MixinTaczTickAnimationEvent` | 保留在 Mixin 中，条件加载 | — |
| `CameraSetupEvent` | `CameraSetupEventMixin` | 保留在 Mixin 中，条件加载 | — |
| `FirstPersonRenderGunEvent` | `FirstPersonRenderGunEventMixin` | 保留在 Mixin 中，条件加载 | — |

### 3.2 事件桥接实现方案

在 `compat/tacz/TACZGunEventBridge.java` 中：

```java
@Mod.EventBusSubscriber
public class TACZGunEventBridge {
    @SubscribeEvent
    public static void onGunFire(GunFireEvent event) {
        if (GunCompatManager.getProvider() instanceof TACZGunProvider) {
            FPSMGunFireEvent fpsmEvent = new FPSMGunFireEvent(/* 转换 */);
            MinecraftForge.EVENT_BUS.post(fpsmEvent);
        }
    }
    // ... 其他事件同理
}
```

### 3.3 新增 FPSMatch 自定义事件

在 `common/event/` 下新增：

```
FPSMGunFireEvent.java        # 枪械开火事件
FPSMGunReloadEvent.java      # 枪械换弹事件
FPSMGunShootEvent.java       # 枪械射击事件
FPSMGunKillEvent.java        # 枪械击杀事件
FPSMGunDamageEvent.java      # 枪械伤害事件
```

这些事件纯 POJO，不依赖 TACZ，数据通过 `GunDataDTO` 传递。

---

## 第四部分：实施顺序

### 阶段 1：FPSMatch 抽象层（基础）
1. 创建 `IGunProvider` 接口和 `GunDataDTO`
2. 创建 `GunTabTypeEnum` 枚举
3. 创建 `GunCompatManager` 管理器
4. 创建 `NoGunProvider` 空实现
5. 创建 FPSMatch 自定义事件（`FPSMGunFireEvent` 等）

### 阶段 2：FPSMatch 核心层改造
6. 改造 `FPSMUtil.java`
7. 改造 `FPSMSoundRegister.java`
8. 改造 `FPSMFormatUtil.java`
9. 改造 `FPSMImpl.java` 和 `FPSMatchMixinPlugin.java`
10. 改造商店相关（`ShopData`, `ShopSlot`, `SkinHandler`, `ShopCapability`）
11. 改造实体相关（`MatchDropEntity`, `MatchDropRenderer`）
12. 改造事件相关（`FPSMDeathPipelineEventHook`, `GunDamageHandler`）
13. 改造命令（`FPSMBaseCommand`）
14. 改造其他（`FPSMatch.java`, `FPSMEffectRegister`, `BlockRayTraceReflector`）

### 阶段 3：FPSMatch TACZ 兼容层隔离
15. 创建 `TACZGunProvider` 实现
16. 创建 `TACZGunEventBridge` 事件桥接
17. 改造 `compat/tacz/` 内文件使用 `IGunProvider` 而非直接 TACZ API
18. 改造 `compat/spectate/tacz/` 同理
19. 更新 `fpsmatch.mixins.json` 和 `FPSMatchMixinPlugin` 条件加载
20. 修改 `build.gradle` 依赖为 `compileOnly`

### 阶段 4：BlockOffensive 改造
21. 改造 `BlockOffensive.java`（音效注册）
22. 改造 `CSGameEvents.java`（弹夹模式）
23. 改造 `CSGameHud.java`（弹药 HUD）
24. 改造 `CSMap.java`（武器分类）
25. 改造 `CSGameShopScreen.java`, `ShopSlotRenderer.java`
26. 改造 `DeathMessage.java`
27. 改造按键文件（`TeamChatKey`, `OpenShopKey`, `DismantleBombKey`）
28. 改造 `BOClientEvent.java`
29. 修改 `build.gradle` 依赖为 `compileOnly`

### 阶段 5：验证
30. 编译验证（`./gradlew build`）
31. 无 TACZ 环境运行测试
32. 有 TACZ 环境回归测试

---

## 第五部分：风险与注意事项

1. **Mixin 条件加载**：TACZ 相关 Mixin 必须在 TACZ 未加载时完全不触发，`FPSMatchMixinPlugin.shouldApplyMixin` 需要正确配置
2. **ClientGunIndex / GunDisplayInstance**：这些是客户端渲染相关的 TACZ 类，在 `IClientGunProvider` 中需要提供替代方案
3. **弹夹模式（Magazine Mode）**：高度依赖 TACZ 的弹药系统，需要 `IMagazineGunProvider` 扩展接口
4. **InputExtraCheck.isInGame**：需要 FPSMatch 提供等价的 `IInputCheck` 实现
5. **LR Tactical 兼容**：`compat/spectate/lrt/` 中的 Mixin 也间接依赖 TACZ，需要一并处理
6. **TACZ Tweaks**：`TweakAmmoMixin` 和 `FPSMatchMixinPlugin` 中的 taczTweaks 检查需要保留条件逻辑
7. **资源文件**：`data/tacz/tags/` 下的数据文件需要通过 TACZ 兼容层按需加载