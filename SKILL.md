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

---

# FPSMatch 框架 API 全面参考

> **关键原则**：BlockOffensive 的代码**严禁重复造轮子**。以下 FPSMatch 已提供的能力，直接复用而非重新实现。

---

## 1. 核心框架层 (Core Framework)

### 1.1 `FPSMCore` — 比赛管理器单例
**路径**：`com.phasetranscrystal.fpsmatch.core.FPSMCore`

| 方法 | 说明 |
|------|------|
| `FPSMCore.getInstance()` | 获取单例（未初始化时抛异常） |
| `FPSMCore.initialized()` | 检查是否已初始化 |
| `FPSMCore.getCurrentEnvironment()` | 返回 `FPSMDist(SERVER/LOCAL/LAN)` |
| `FPSMCore.checkAndLeaveTeam(ServerPlayer)` | 让玩家离开所有已加入的比赛 |
| `registerGameType(String, Function3<...>)` | 注册游戏类型工厂（BlockOffensive 通过 `RegisterFPSMapEvent` 调用） |
| `registerMap(String, BaseMap)` | 注册地图实例 |
| `isRegistered(BaseMap/String/String,String)` | 检查地图/类型是否注册 |
| `isInGame(Player)` | 玩家是否在任一比赛中 |
| `getMapByPlayer(Player/UUID)` | 获取玩家所在的地图 |
| `getMapByPlayerWithSpec(Player)` | 含观战者的地图查找 |
| `getMapByTypeWithName(String, String)` | 按类型+名称获取地图 |
| `getMapByName(String)` | 按名称获取地图 |
| `getMapByClass(Class<T>)` | 获取特定子类的地图列表 |
| `getMapByPosition(ServerLevel, BlockPos)` | 按坐标查找地图 |
| `getMapNames()/getMapNames(String)` | 获取所有/某类型地名称列表 |
| `getGameTypes()` | 获取所有注册的游戏类型 |
| `getPlayerByUUID(UUID)` | 获取在线玩家 Optional |
| `getServer()` | 获取 MinecraftServer |
| `getFPSMDataManager()` | 获取数据持久化管理器 |
| `getListenerModuleManager()` | 获取商店监听模块管理器 |
| `onServerTick()` | 每 tick 遍历所有地图 tick |
| `shutdown()` | 保存所有数据并清理 |

### 1.2 `FPSMatch` — 主模组类
**路径**：`com.phasetranscrystal.fpsmatch.FPSMatch`

| 静态方法/字段 | 说明 |
|--------------|------|
| `FPSMatch.MODID` = `"fpsmatch"` | 模组 ID |
| `FPSMatch.LOGGER` | Logger 实例 |
| `FPSMatch.INSTANCE` | `SimpleChannel` 网络通道 |
| `FPSMatch.sendToPlayer(ServerPlayer, M)` | 发送数据包到指定玩家 |
| `FPSMatch.sendToServer(M)` | 发送数据包到服务端 |
| `FPSMatch.sendTo(Player, M)` | 自动判断方向发送 |
| `FPSMatch.isDebugEnabled()` / `switchDebug()` | 调试模式 |
| `FPSMatch.pullGameInfo()` | [client] 请求服务端重新发送游戏信息 |

### 1.3 `FPSMDist` — 运行环境枚举
```java
enum FPSMDist { SERVER, LOCAL, LAN }
```

---

## 2. 地图系统 (Map System)

### 2.1 `BaseMap` — 地图基类（BlockOffensive 通过 CSMap 继承）
**路径**：`com.phasetranscrystal.fpsmatch.core.map.BaseMap`

| 公共字段/方法 | 说明 |
|-------------|------|
| `final String mapName` | 地图名称 |
| `final AreaData mapArea` | 地图区域数据 |
| **生命周期** | |
| `load()` | 加载地图，注册到 FPSMCore |
| `start()` | 开始游戏（可被 StartEvent 取消） |
| `final mapTick()` | 每 tick 执行：checkForVictory → tick → teams.tick → capabilities.tick → syncToClient |
| `tick()` | 自定义 tick 逻辑（override） |
| `syncToClient()` | 同步数据到客户端（override） |
| `final checkForVictory()` | 检查胜利条件 |
| `victory()` | 触发 VictoryEvent |
| `abstract victoryGoal()` | 返回胜利条件是否满足（必须 override） |
| `startNewRound()` | 开始新回合（override） |
| `cleanupMap()` | 清理地图（可被 ClearEvent 取消） |
| `reset()` | 重置（触发 ResetEvent） |
| `reload()` | 重载（触发 ReloadEvent） |
| **队伍管理** | |
| `addTeam(TeamData)` | 创建并添加 ServerTeam |
| `getSpectatorTeam()` | 获取观战队伍 |
| `getMapTeams()` | 返回 MapTeams |
| **玩家管理** | |
| `join(ServerPlayer/String, ServerPlayer)` | 加入（自动分配到人数少的队伍） |
| `leave(ServerPlayer)` | 离开 |
| `checkGameHasPlayer(Player/UUID)` | 是否已加入 |
| `checkSpecHasPlayer(Player)` | 是否在观战 |
| **死亡/伤害** | |
| `handleDeath(DeathContext)` | 死亡处理（默认设置 isLiving=false, addDeath） |
| `resolveDeathItem(ServerPlayer, DamageSource)` | 解析死亡武器 |
| `isValidAttack(ServerPlayer, ServerPlayer)` | 验证攻击是否有效 |
| `recordHurtData(ServerPlayer, DamageSource, float)` | 记录伤害（助攻计算用） |
| `getAttackerFromDamageSource(DamageSource)` | 从 DamageSource 提取攻击者 |
| **工具方法** | |
| `sendPacketToAllPlayer(MSG)` | 发包给所有玩家（含观战者） |
| `sendPacketToSpecPlayer(MSG)` | 发包给观战者 |
| `sendPacketToTeamPlayer(ServerTeam, MSG, boolean)` | 发包给队伍（可按存活过滤） |
| `sendPacketToTeamLivingPlayer(ServerTeam, MSG)` | 发包给队伍存活玩家 |
| `sendPacketToJoinedPlayer(ServerPlayer, MSG, boolean)` | 发包给特定加入玩家 |
| `pullGameInfo(ServerPlayer)` | 同步游戏类型+地图名 |
| `teleportPlayerToReSpawnPoint(ServerPlayer)` | 传送到出生点 |
| `teleportToPoint(ServerPlayer, SpawnPointData)` | 直接传送到点 |
| `clearInventory(ServerPlayer/UUID, ...)` | 清空背包 |
| `syncInventory(ServerPlayer)` | 同步背包 |
| `getServerLevel()` / `getRandom()` | 获取服务端 Level |
| **配置系统** | |
| `addSetting(String, T)` | 添加配置项（int/long/float/double/byte/boolean/String） |
| `findSetting(String)` | 查找配置项 |
| `loadConfig()` / `saveConfig()` | 加载/保存配置 JSON |
| **Capability** | |
| `getCapabilityMap()` | 返回 `CapabilityMap<BaseMap, MapCapability>` |
| **状态** | |
| `final isStart()` | 是否已开始 |
| `isDebug()` / `switchDebugMode()` | 调试模式 |
| `getGameType()` | 返回游戏类型字符串（abstract, 必须 override） |

### 2.2 `MapTeams` — 队伍集合管理
**路径**：`com.phasetranscrystal.fpsmatch.core.team.MapTeams`

| 方法 | 说明 |
|------|------|
| `addTeam(TeamData)` | 创建普通队伍 |
| `addSpectatorTeam(TeamData)` | 创建观战队伍 |
| `joinTeam(String, ServerPlayer)` | 加入指定队伍（返回 JoinTeamResult） |
| `leaveTeam(ServerPlayer)` | 离开队伍 |
| `getTeamByName(String)` | 按名称获取队伍 |
| `getTeamByPlayer(Player)` | 获取玩家所在队伍 |
| `getNormalTeams()` | 获取非观战队伍列表 |
| `getSpectatorTeam()` | 获取观战队伍 |
| `getJoinedUUID()` | 所有已加入玩家的 UUID |
| `getJoinedPlayers()` | 所有已加入的玩家 |
| `getJoinedPlayersWithSpec()` | 含观战者的玩家列表 |
| `getSpecPlayers()` | 观战玩家 UUID 列表 |
| `getPlayerData(Player/UUID)` | 获取玩家数据 |
| `isSameTeam(Player, Player)` | 是否同队 |
| `tick()` | tick 所有队伍 |
| `addHurtData(...)` / `popHurtData(...)` | 伤害数据追踪 |
| `getAllPlayerData()` | 所有队伍的玩家数据 |
| `shutdown(Scoreboard)` | 清理所有 Scoreboard Team |

### 2.3 `DeathContext` — 死亡上下文
**路径**：`com.phasetranscrystal.fpsmatch.core.map.DeathContext`

| 方法 | 说明 |
|------|------|
| `getDeadPlayer()` | 死者 |
| `getAttacker()` | 攻击者 |
| `getSource()` | DamageSource |
| `isAttackerPresent()` | 是否有攻击者 |
| `getHurtDataEntries()` | 助攻者数据列表 |
| `getKillWeaponStack()` | 击杀武器 |
| `isHeadshot()` | 是否爆头 |
| `getPenetrateCount()` | 穿墙次数 |
| `isBlindKill()` | 是否致盲击杀 |
| `isThroughSmoke()` | 是否穿烟 |
| `isNoScope()` | 是否不开镜 |
| `isFlyKill()` | 是否跳杀 |

### 2.4 `VoteObj` — 投票对象
**路径**：`com.phasetranscrystal.fpsmatch.core.map.VoteObj`
- 提供玩家投票发起/统计功能

### 2.5 `BlastBombState` — 炸弹状态枚举
```java
enum BlastBombState { IDLE, TICKING, DEFUSED, EXPLODED }
```

---

## 3. 队伍系统 (Team System)

### 3.1 `BaseTeam` — 队伍基类
**路径**：`com.phasetranscrystal.fpsmatch.core.team.BaseTeam`

| 字段/方法 | 说明 |
|----------|------|
| `final String name` | 队伍名 |
| `final String gameType` / `mapName` | 所属游戏/地图 |
| `getPlayerLimit()` / `getRemainingLimit()` | 人数限制 |
| `getScores()` / `setScores(int)` | 得分 |
| `getColor()` / `getColorVec3f()` / `setColor(Vector3f)` | 颜色 |
| `getFixedName()` | `gameType_mapName_name` |
| `getPlayerTeam()` | Scoreboard PlayerTeam（仅服务端有效） |
| `join(Player)` / `leave(Player)` | 加入/离开（触发 JoinEvent/LeaveEvent） |
| `getCapabilityMap()` | `CapabilityMap<BaseTeam, TeamCapability>` |
| `isSpectator()` / `isNormal()` | 是否观战队伍 |
| `resetCapabilities()` / `clean()` | 清理 |
| **抽象方法** | |
| `abstract delPlayer(UUID)` | 删除玩家 |
| `abstract resetLiving()` | 重置存活 |
| `abstract getPlayerData(UUID)` | 获取玩家数据 |
| `abstract getPlayersData()` | 获取所有玩家数据列表 |
| `abstract getPlayerList()` | 获取玩家 UUID 列表 |
| `abstract hasPlayer(UUID)` | 是否有玩家 |
| `abstract getPlayerCount()` | 玩家数量 |
| `abstract isEmpty()` | 是否为空 |
| `abstract getPlayers()` | 获取玩家 Map |
| `abstract clearAndPutPlayers(Map)` | 替换全部玩家 |
| `abstract sendMessage(Component, boolean)` | 发送消息 |
| `abstract isClientSide()` | 是否客户端侧 |

### 3.2 `ServerTeam` — 服务端队伍
**路径**：`com.phasetranscrystal.fpsmatch.core.team.ServerTeam`

- 实现所有 `BaseTeam` 抽象方法
- 额外提供：
  - `getLivingPlayers()` — 存活玩家 UUID
  - `getOnlinePlayers()` / `getOfflinePlayers()` — 在线/离线玩家
  - `hasNoOnlinePlayers()` — 是否有在线玩家
  - `getOnline()` — 在线玩家实体列表
  - `sync(ServerPlayer)` — 同步队伍信息到客户端
  - `syncCapabilities(ServerPlayer/Collection)` — 同步 capabilities
  - `setEnableRounds(boolean)` — 启用回合模式
  - `getMap()` — 获取所属 BaseMap
  - `tick()` — tick 所有 capability

### 3.3 `ClientTeam` — 客户端队伍
**路径**：`com.phasetranscrystal.fpsmatch.core.team.ClientTeam`
- 客户端侧队伍数据镜像

### 3.4 `TeamData` — 队伍创建数据
**路径**：`com.phasetranscrystal.fpsmatch.core.team.TeamData`
- Builder 模式：`TeamData.builder().name(...).playerLimit(...).color(...).build()`

---

## 4. 玩家数据系统 (Player Data)

### 4.1 `PlayerData` — 玩家数据类（核心）
**路径**：`com.phasetranscrystal.fpsmatch.core.data.PlayerData`

| 方法 | 说明 |
|------|------|
| `getOwner()` / `name()` | UUID / 名字 |
| `getScores()` / `addScore(int)` / `setScores(int)` | 得分 |
| `getKills()` / `addKill()` / `removeKill()` / `setKills(int)` | 击杀 |
| `getDeaths()` / `addDeath()` / `setDeaths(int)` | 死亡 |
| `getAssists()` / `addAssist()` / `setAssists(int)` | 助攻 |
| `getDamage()` / `addDamage(float)` / `setDamage(float)` | 伤害 |
| `getMvpCount()` / `addMvpCount(int)` / `setMvpCount(int)` | MVP 次数 |
| `getHeadshotRate()` | 爆头率 |
| `getKD()` | K/D 比率 |
| `getHeadshotKills()` / `addHeadshotKill()` | 爆头击杀 |
| `isLiving()` / `setLiving(boolean)` | 存活状态 |
| `isLivingOnServer()` | 存活且在线 |
| `isOnline()` | 是否在线（仅服务端） |
| `getPlayer()` | Optional<ServerPlayer>（仅服务端） |
| **回合制字段** | |
| `getTempKills()` / `getTempDeaths()` / `getTempAssists()` / `getTempDamage()` | 回合临时数据 |
| `saveRoundData()` | 回合临时数据合并到基础字段 |
| `reset()` / `resetWithSpawnPoint()` | 重置所有数据 |
| **伤害/助攻** | |
| `addDamageData(UUID, float)` | 记录伤害明细 |
| `getDamageData()` / `getDamages()` | 获取伤害数据 |
| `isHurtTo(UUID)` | 是否伤害过某玩家 |
| `clearDamageData()` | 清空伤害数据 |
| **其他** | |
| `copy(Player)` / `merge(PlayerData)` / `info()` / `mappedInfo()` | 复制/合并/序列化 |
| `getSpawnPointsData()` / `setSpawnPointsData(SpawnPointData)` | 出生点 |
| `getHealthPercent()` | [client only] HP 百分比 |
| `getTabString()` | [client only] K/D/A 字符串 |
| **内部类 `Damage`** | `count` + `damage` + `addDamage(float)` + `merge(Damage)` |

### 4.2 `Setting<T>` — 泛型配置项
**路径**：`com.phasetranscrystal.fpsmatch.core.data.Setting`
- 支持类型：int/long/float/double/byte/boolean/String
- 提供 `get()`, `set(T)`, `addChangeListener(...)`
- JSON 序列化/反序列化：`toJson()`, `fromJson(JsonElement)`
- 静态工厂：`Setting.of(String configName, T defaultValue)`

### 4.3 `SpawnPointData` — 出生点数据
**路径**：`com.phasetranscrystal.fpsmatch.core.data.SpawnPointData`
- 坐标/维度/朝向/优先级

### 4.4 `AreaData` — 区域数据
**路径**：`com.phasetranscrystal.fpsmatch.core.data.AreaData`
- `isBlockPosInArea(BlockPos)` — 判断坐标是否在区域内

### 4.5 `HashData` — 哈希数据
**路径**：`com.phasetranscrystal.fpsmatch.core.data.HashData`
- 文件哈希校验

---

## 5. 事件系统 (Event System)

### 5.1 `FPSMapEvent` — 地图事件体系
**路径**：`com.phasetranscrystal.fpsmatch.common.event.FPSMapEvent`

| 内部类 | 可取消 | 说明 |
|--------|--------|------|
| `StartEvent` | ✅ | 地图开始时触发 |
| `VictoryEvent` | ❌ | 胜利时触发（含 scoreboard + team summaries） |
| `ClearEvent` | ✅ | 清理地图时触发 |
| `ResetEvent` | ❌ | 重置时触发 |
| `ReloadEvent` | ✅ | 重载时触发 |
| `LoadEvent` | ❌ | 加载时触发 |
| `PlayerEvent` | — | 玩家事件基类 |
| `PlayerEvent.JoinEvent` | ✅ | 玩家加入 |
| `PlayerEvent.LeaveEvent` | ✅ | 玩家离开 |
| `PlayerEvent.HurtEvent` | ✅ | 玩家受伤（可修改 amount） |
| `PlayerEvent.DeathEvent` | ✅ | 玩家死亡 |
| `PlayerEvent.KillEvent` | ❌ | 玩家击杀（不可取消） |
| `PlayerEvent.KillRecordEvent` | ✅ | 击杀统计写入前触发 |
| `PlayerEvent.LoggedInEvent` | ❌ | 离线玩家重新登录 |
| `PlayerEvent.LoggedOutEvent` | ✅ | 玩家下线 |
| `PlayerEvent.PickupItemEvent` | ✅ | 拾取物品 |
| `PlayerEvent.TossItemEvent` | ✅ | 丢弃物品 |
| `PlayerEvent.ChatEvent` | ✅ | 玩家聊天 |

VictoryEvent 额外数据：
- `getScoreboard()` → `Map<UUID, PlayerScoreSnapshot>`（按得分排序）
- `getTeamSummaries()` → `Map<String, TeamScoreSummary>`
- `PlayerScoreSnapshot` record: player, name, team, scores, kills, deaths, assists, damage, headshotRate
- `TeamScoreSummary` record: team, roundScores, playerCount, totalPlayerScores, totalKills, totalDeaths, totalAssists, totalDamage, averageHeadshotRate

### 5.2 `FPSMTeamEvent` — 队伍事件
**路径**：`com.phasetranscrystal.fpsmatch.common.event.FPSMTeamEvent`

| 内部类 | 可取消 | 说明 |
|--------|--------|------|
| `JoinEvent` | ❌ | 玩家加入队伍 |
| `LeaveEvent` | ❌ | 玩家离开队伍 |

### 5.3 `FPSMReloadEvent` — 重载事件
**路径**：`com.phasetranscrystal.fpsmatch.common.event.FPSMReloadEvent`
- 不可取消

### 5.4 注册事件
**路径**：`com.phasetranscrystal.fpsmatch.common.event.register`

| 事件 | 用途 |
|------|------|
| `RegisterFPSMapEvent` | `registerGameType(String, Function3)` — 注册游戏类型工厂 |
| `RegisterFPSMSaveDataEvent` | `registerData(Class, String, SaveHolder)` — 注册持久化数据 |

---

## 6. Capability 系统

### 6.1 框架类

| 类 | 说明 |
|----|------|
| `FPSMCapability` | 基类，定义 `CapabilityFactory`、`CapabilitySynchronizable`、`CapabilityGsonSerializable` 接口 |
| `FPSMCapabilityManager` | 注册/获取/创建 capability 实例 |
| `CapabilityMap<H, C>` | 持有者-能力映射，提供 add/get/all/tick/resetAll |
| `MapCapability` | 继承 FPSMCapability，关联 BaseMap |
| `TeamCapability` | 继承 FPSMCapability，关联 BaseTeam |

**Capability 生命周期**：init → tick（每 tick）→ reset → serialize/deserialize

### 6.2 FPSMatch 内置 Capability

| Capability | 类型 | 作用 |
|-----------|------|------|
| `ShopCapability` | Team | 团队商店系统（BlockOffensive 直接使用） |
| `SpawnPointCapability` | Team | 出生点管理，`assignNextSpawnPoint(UUID)` |
| `StartKitsCapability` | Team | 初始装备发放，`addKit(ItemStack, amount)`, `giveStartKits(player)` |
| `GameEndTeleportCapability` | Map | 游戏结束传送点 |
| `DemolitionModeCapability` | Map | 爆破模式标记 |

### 6.3 如何在 BlockOffensive 中扩展 Capability
1. 实现 `MapCapability` 或 `TeamCapability`
2. 在 `FPSMCapabilityManager` 注册
3. 通过 `BaseMap.getCapabilityMap().add(YourCap.class)` 挂载
4. 若需同步：实现 `FPSMCapability.CapabilitySynchronizable`

---

## 7. 商店系统 (Shop System)

### 7.1 `FPSMShop<T extends INamedType>` — 商店系统核心
**路径**：`com.phasetranscrystal.fpsmatch.core.shop.FPSMShop`
- 泛型商店系统，BlockOffensive 使用 `ItemType` 作为 T
- **关键方法**：
  - `FPSMShop<Void> registerShop(String name, INamedType type)` — 注册商店类型
  - `FPSMShop<Void> setShopData(String shopTypeName, String teamName, ShopData)` — 设置队伍商店数据
  - `void initShop(ShopCapability)` — 初始化商店
  - `void handleButton(ServerPlayer, T, int slotIndex, ShopAction)` — 处理购买/退款
  - 支持默认商店配置

### 7.2 `ShopData` — 玩家商店数据
- 金钱管理：`getMoney()`, `setMoney(int)`, `addMoney(int)`, `spendMoney(int)`
- 槽位管理：`getSlot(int)`, `setSlot(int, ShopSlot)`, `getSlots()`
- 操作：`buy(ShopSlot)`, `returnItem(ShopSlot)`
- 同步：广播 `ShopDataSlotS2CPacket` / `ShopMoneyS2CPacket`

### 7.3 `ShopSlot` — 商品槽位
- `getItemSupplier()` — 物品提供器
- `getPrice()` / `getGroupId()` — 价格/分组
- `getBoughtCount()` / `setBoughtCount(int)` — 已购买数量
- `isLocked()` — 是否锁定
- `addListener(ListenerModule)` — 添加购买/退款监听

### 7.4 `ShopAction` — 商店操作枚举
```java
enum ShopAction { BUY, RETURN }
```

### 7.5 `INamedType` — 商店类型接口
```java
interface INamedType {
    String name();          // 类型名称
    int slotCount();        // 槽位数量
    boolean needUnLock();   // 是否需要解锁
    List<ShopSlot> getDefaultSlots(); // 默认槽位
}
```

### 7.6 `ListenerModule` — 商店事件监听
```java
interface ListenerModule {
    String getName();
    int getPriority();     // 优先级，越小越先执行
    void onSlotChanged(ShopSlot slot, ShopAction action, Player player);
    void onCostCheck(ShopSlot slot, Player player, int cost);
    void onReset();
}
```

### 7.7 内置 ListenerModule
| 类 | 说明 |
|----|------|
| `BulletproofArmorWithHelmetListenerModule` | 购买防弹衣+头盔 |
| `BulletproofArmorWithoutHelmetListenerModule` | 仅购买防弹衣 |
| `ChangeShopItemModule` | 动态替换商店物品 |
| `ReturnGoodsModule` | 退款处理 |

### 7.8 商店皮肤
| 类 | 说明 |
|----|------|
| `SkinHandler` | 皮肤应用/取消 |
| `SkinType` enum: `GUN_ID`, `GUN_DISPLAY_ID`, `ITEM`, `DEFAULT` |

---

## 8. 网络系统 (Network)

### 8.1 核心发包方法（通过 `FPSMatch` 或 `BaseMap`）
```java
// 全局
FPSMatch.sendToPlayer(ServerPlayer player, M message);  // 发给特定玩家
FPSMatch.sendToServer(M message);                        // 发给服务端
FPSMatch.sendTo(Player player, M message);               // 自动判断

// BaseMap 级别（推荐使用）
map.sendPacketToAllPlayer(MSG);                          // 发给所有玩家+观战者
map.sendPacketToSpecPlayer(MSG);                         // 发给观战者
map.sendPacketToTeamPlayer(ServerTeam, MSG, boolean);    // 发给队伍
map.sendPacketToTeamLivingPlayer(ServerTeam, MSG);       // 发给存活队员
map.sendPacketToJoinedPlayer(ServerPlayer, MSG, boolean);// 发给特定玩家
```

### 8.2 FPSMatch 内置数据包（BlockOffensive 可直接使用）

| 数据包 | 方向 | 用途 |
|--------|------|------|
| `FPSMatchGameTypeS2CPacket` | S2C | 同步游戏类型+地图名 |
| `FPSMatchStatsResetS2CPacket` | S2C | 重置客户端统计 |
| `FPSMatchRespawnS2CPacket` | S2C | 重生通知 |
| `FPSMSoundPlayC2SPacket` | C2S | 请求播放音效 |
| `FPSMSoundPlayS2CPacket` | S2C | 服务端播放音效 |
| `FPSMusicPlayS2CPacket` / `FPSMusicStopS2CPacket` | S2C | 音乐控制 |
| `ShopActionC2SPacket` | C2S | 商店操作 |
| `ShopDataSlotS2CPacket` | S2C | 商店槽位数据 |
| `ShopMoneyS2CPacket` | S2C | 金钱同步 |
| `SaveSlotDataC2SPacket` | C2S | 保存商店槽位 |
| `OpenEditorC2SPacket` | C2S | 打开编辑器 |
| `SpectateModeS2CPacket` | S2C | 观战模式同步 |
| `TeamPlayerStatsS2CPacket` | S2C | 队伍玩家统计 |
| `TeamPlayerLeaveS2CPacket` | S2C | 队伍玩家离开 |
| `FPSMAddTeamS2CPacket` | S2C | 添加队伍 |
| `TeamCapabilitiesS2CPacket` | S2C | 同步 capability |
| `TeamChatMessageC2SPacket` | C2S | 队伍聊天 |
| `FPSMInventorySelectedS2CPacket` | S2C | 物品栏选中 |
| `BulletproofArmorAttributeS2CPacket` | S2C | 防弹衣属性 |
| `FlashBombAddonS2CPacket` | S2C | 闪光弹效果 |
| `ThrowEntityC2SPacket` | C2S | 投掷实体 |

### 8.3 数据包注册（BlockOffensive 模式）
BlockOffensive 已有自己的 `NetworkPacketRegister`，在 `BlockOffensive.commonSetup()` 中注册：
```java
PACKET_REGISTER.registerPacket(YourPacket.class);
```

### 8.4 客户端数据包执行
- `ClientPacketExecutor` — 分发数据包到客户端处理
- `ClientPacketRegistry` — 维护 packet 类型 → handler 映射

### 8.5 HTTP 网络模块（FPSMatch 内置，BlockOffensive 可复用）
**路径**：`com.phasetranscrystal.fpsmatch.core.network`
- `NetworkModule` — HTTP 客户端核心（支持拦截器、异步）
- `RequestBuilder` — 链式构建 HTTP 请求
- `RequestMethod` enum: GET/POST/PUT/DELETE
- `ApiResponse<T>` / `ApiError` — 响应封装
- `Interceptor` — 拦截器接口（参考 `LoggingInterceptor`）
- `Downloader` — 文件下载系统

---

## 9. 持久化系统 (Persistence)

### 9.1 `FPSMDataManager` — 数据管理器
**路径**：`com.phasetranscrystal.fpsmatch.core.persistence.FPSMDataManager`
- `registerData(Class<T>, String folderName, SaveHolder<T>)` — 注册数据类型
- `readAllData()` / `saveAllData()` / `saveData(Class)` — 读写所有数据
- `getSaveFolder(BaseMap)` — 获取地图存档目录

### 9.2 `SaveHolder<T>` — 保存包装器
```java
class SaveHolder<T> {
    Class<T> clazz;
    String folder;        // 存档文件夹
    SaveHolder<T> addDataFixer(DataFixer<T> fixer);
    SaveHolder<T> addAfterLoad(BiConsumer<...> hook);
    SaveHolder<T> withDefaultData(Supplier<T> supplier);
    SaveHolder<T> shouldBackup(boolean flag);
}
```

### 9.3 其他
- `ISavePort` — 地图实现此接口后自动拥有持久化能力
- `PersistenceUtils` — 文件/目录创建工具
- `DataFixer<T>` — 数据版本迁移

### 9.4 注册方式（BlockOffensive 已在 `MapRegister` 中使用）
```java
@SubscribeEvent
public static void onRegisterFPSMSaveData(RegisterFPSMSaveDataEvent event) {
    event.registerData(YourClass.class, "folder", saveHolder);
}
```

---

## 10. 客户端系统 (Client)

### 10.1 `FPSMClient` — 客户端全局类
**路径**：`com.phasetranscrystal.fpsmatch.common.client.FPSMClient`
- `getGlobalData()` → `FPSMClientGlobalData` — 客户端全局数据
- 键位注册：`registerKeyBinding(...)`
- 实体渲染注册
- `reset()` — 重置所有客户端状态

### 10.2 `FPSMClientGlobalData` — 客户端全局数据
**路径**：`com.phasetranscrystal.fpsmatch.common.client.data.FPSMClientGlobalData`
- **BlockOffensive 大量使用此类的数据**
- `getGameType()` / `setGameType(String)` — 当前游戏类型
- `getMapName()` / `setMapName(String)` — 当前地图名
- `getShopData(String teamName)` — 获取商店数据
- `getTeamsData()` — 获取队伍数据 Map
- `getPlayerData(UUID)` — 获取玩家数据
- `getSpecTarget()` — 获取观战目标
- `addTeam(...)` / `removeTeam(...)` — 队伍增删
- `reset()` — 清空所有数据

### 10.3 `SpectateMode` — 观战模式枚举
```java
enum SpectateMode { ATTACH, FREE }
```

---

## 11. 兼容层 (Compat Layer)

### 11.1 `FPSMImpl` — 模组存在性检测
**路径**：`com.phasetranscrystal.fpsmatch.compat.impl.FPSMImpl`

| 方法 | 说明 |
|------|------|
| `findLrtacticalMod()` | lrtactical 是否存在 |
| `findCounterStrikeGrenadesMod()` | csgrenades 是否存在 |
| `findMohist()` | Mohist 是否存在 |
| `findClothConfig()` | Cloth Config 是否存在 |
| `findTaczTweaks()` | TaCZ Tweaks 是否存在 |
| `findPhysicsMod()` | PhysicsMod 是否存在 |
| `findKubeJS()` | KubeJS 是否存在 |
| `withVersion(String modId, String version)` | 版本检查 |

> **注意**：BlockOffensive 也在 `BOImpl` 中定义了类似方法。应优先使用 `FPSMImpl`，或在 `BOImpl` 中委托给 `FPSMImpl`。

### 11.2 其他兼容类
| 类 | 说明 |
|----|------|
| `CounterStrikeGrenadesCompat` | CS手雷模组兼容（投掷物映射） |
| `LrtacticalCompat` | LR Tactical 兼容 |
| `TACZCompat` | TaCZ 枪械兼容 |

---

## 12. 工具类 (Utilities)

### 12.1 `FPSMUtil` — 核心工具类
**路径**：`com.phasetranscrystal.fpsmatch.util.FPSMUtil`

| 方法 | 说明 |
|------|------|
| `sortPlayerInventory(ServerPlayer)` | 排序玩家背包 |
| `getOwnerIfTraceable(Entity, Entity)` | 追踪弹头实体获取真正的攻击者 |
| `setGunAmmo(ItemStack, int, int)` | 设置枪械弹药（通过 NBT） |
| 掉落/拾取处理相关方法 | — |

### 12.2 `FPSMFormatUtil` — 格式化工具
| 方法 | 说明 |
|------|------|
| `formatNumber(int/float)` | 数字格式化 |
| `formatBoolean(boolean)` | 布尔格式化 |
| `getLocalizedName(ItemStack)` | 物品名本地化 |

### 12.3 `RenderUtil` — 渲染工具（BlockOffensive 已在使用）
**路径**：`com.phasetranscrystal.fpsmatch.util.RenderUtil`
- `color(Vector3f)` → int color
- 纹理渲染、字符串绘制等

### 12.4 `FPSMCodec` — 编解码器
**路径**：`com.phasetranscrystal.fpsmatch.util.FPSMCodec`
- 通用 Gson 序列化/反序列化工具

### 12.5 其他工具类
| 类 | 说明 |
|----|------|
| `BlockRayTraceReflector` | 反射调用 TACZ 的射线追踪 |
| `PreviewColorUtil` | 地图预览颜色生成 |
| `ItemKey` | 物品键（带 NBT 对比） |
| `FileHashUtil` + `HashAlgorithm` | 文件哈希计算 |

---

## 13. 实体/物品框架

### 13.1 实体
| 类 | 说明 |
|----|------|
| `BaseProjectileEntity` | 投射物实体基类 |
| `BaseProjectileLifeTimeEntity` | 带生命周期的投射物 |
| `BlastBombEntity` | 爆炸物实体基类（C4 继承此） |

### 13.2 物品
| 类 | 说明 |
|----|------|
| `BlastBombItem` | 爆炸物品基类接口 |
| `IThrowEntityAble` | 可投掷实体接口 |

---

## 14. 音效系统

### `FPSMSoundRegister` — 音效注册
**路径**：`com.phasetranscrystal.fpsmatch.common.sound.FPSMSoundRegister`
- 注册 FPSMatch 内置音效资源

---

## 15. KubeJS 集成

**路径**：`com.phasetranscrystal.fpsmatch.compat.kubejs`
- `FPSMatchKubeJSPlugin` — 注册 FPSMatch 事件到 KubeJS
- `FPSMatchCommonEvents` — 转发 Forge 事件到 KubeJS
- `FPSMatchKubeJSEvents` — 供 KubeJS 使用的事件定义

---

# BlockOffensive 模块速查

## 代码地图（核心）

### 根入口

| 路径 | 职责 |
|------|------|
| `src/main/java/.../BlockOffensive.java:46` | `@Mod` 入口，注册 items/entities/sounds/configs/packets |
| `src/main/java/.../BOConfig.java` | Client 配置（击杀消息HUD）+ Common 配置（脚步声/C4引信/游戏规则/web服务器） |
| `src/main/templates/META-INF/mods.toml` | Forge mod 元数据，声明依赖 forge/minecraft/modernui/fpsmatch |

### 对局核心 (`map/`)

| 文件 | 行数 | 核心类/职责 |
|------|------|-----------|
| `map/CSMap.java` | 972 | `abstract class CSMap extends BaseMap` — CS 通用逻辑：队伍切换、经济系统、购买区域、暂停、友伤、出生点、死亡处理（含武器掉落/击杀消息构建/布娃娃兼容） |
| `map/CSGameMap.java` | 1541 | `class CSGameMap extends CSMap` — 爆破模式核心：回合循环、C4 安放/引爆/拆除判定、MVP 评选、胜利条件、回合结束事件 |
| `map/CSDeathMatchMap.java` | 475 | `class CSDeathMatchMap extends CSMap` — 死亡竞赛：无限重生、计分板、玩家移动检测 |
| `map/CSGameEvents.java` | — | `@EventBusSubscriber` 静态事件处理：玩家登录/离开/死亡/重生 hook |
| `map/shop/ItemType.java` | — | `enum ItemType implements INamedType` — 商店分类 |

### C4 系统

| 文件 | 行数 | 核心类/职责 |
|------|------|-----------|
| `item/CompositionC4.java` | 281 | `class CompositionC4 extends Item implements BlastBombItem` — C4 物品 |
| `item/BombDisposalKit.java` | — | 拆弹套件物品 |
| `entity/CompositionC4Entity.java` | 516 | `class CompositionC4Entity extends BlastBombEntity` — C4 实体 |

### 网络层 (`net/`)

| 子包 | 关键类 | 方向 | 职责 |
|------|--------|------|------|
| `net/bomb/` | `BombActionC2SPacket`/`BombActionS2CPacket`/`BombDemolitionProgressS2CPacket` | C2S/S2C | C4 安放/拆除/进度 |
| `net/shop/` | `ShopStatesS2CPacket` | S2C | 商店状态 |
| `net/spec/` | `KillCamS2CPacket`/`CSGameWeaponDataS2CPacket`/`BombFuseS2CPacket`/`SwitchSpectateC2SPacket`/`RequestAttachTeammateC2SPacket` | S2C/C2S | 观战数据 |
| `net/mvp/` | `MvpMessageS2CPacket`/`MvpHUDCloseS2CPacket` | S2C | MVP |
| `net/dm/` | `PlayerMoveC2SPacket` | C2S | DM 移动检测 |
| 根 | `DeathMessageS2CPacket`/`CSGameSettingsS2CPacket`/`CSTabRemovalS2CPacket`/`PxDeathCompatS2CPacket`/`PxRagdollRemovalCompatS2CPacket` | S2C | 死亡/设置/物理兼容 |

### 客户端渲染 (`client/`)

| 文件 | 核心类 | 职责 |
|------|--------|------|
| `client/BOClientBootstrap.java` | — | 客户端启动注册 |
| `client/BOClientEvent.java` | — | 客户端 tick 事件 |
| `client/data/CSClientData.java` | `CSClientData` | 客户端全局状态单例 |
| `client/data/WeaponData.java` | `record WeaponData` | 武器数据 |
| `client/key/` | 4个 KeyBinding | 按键绑定 |
| `client/screen/CSGameShopScreen.java` | `CSGameShopScreen` | 购买界面 |
| `client/screen/TeamChatScreen.java` | `TeamChatScreen` | 队伍聊天 |
| `client/screen/hud/CSGameHud.java` | `CSGameHud` | 主 HUD |
| `client/screen/hud/CSGameOverlay.java` | `CSGameOverlay` | 爆破模式顶栏 |
| `client/screen/hud/CSDMOverlay.java` | `CSDMOverlay` | DM 顶栏 |
| `client/screen/hud/CSGameTabRenderer.java` | `CSGameTabRenderer` | CS 计分板 |
| `client/screen/hud/CSDMTabRenderer.java` | `CSDMTabRenderer` | DM 计分板 |
| `client/screen/hud/CSDeathMessageHud.java` | `CSDeathMessageHud` | 击杀信息流 |
| `client/screen/hud/CSMvpHud.java` | `CSMvpHud` | MVP 结算 |
| `client/screen/hud/CSSpectatorHudOverlay.java` | `CSSpectatorHudOverlay` | 观战信息栏 |
| `client/spec/KillCamManager.java` | `KillCamManager`(1185行) | 击杀回放 |
| `client/renderer/C4Renderer.java` | `C4Renderer` | C4 渲染 |
| `client/renderer/ShopSlotRenderer.java` | `ShopSlotRenderer` | 商店图标 |

### 兼容层 (`compat/`)

| 文件 | 职责 |
|------|------|
| `compat/BOImpl.java` | 可选模组存在性检测 |
| `compat/BOMenuIntegration.java` | ClothConfig 集成 |
| `compat/CSGrenadeCompat.java` | CSGrenade 兼容 |
| `compat/HitIndicationCompat.java` | HitIndication 3D标记 |
| `compat/PhysicsModCompat.java` | PhysicsMod 布娃娃(292行) |
| `compat/IPassThroughEntity.java` | 穿墙/穿烟接口 |

---

## 扩展点与最佳实践

- **新增游戏模式**：继承 `CSMap`（或直接 `BaseMap`），实现 `victoryGoal()`/`getGameType()`，通过 `RegisterFPSMapEvent` 注册
- **新增网络包**：在 `net/` 下新建类，于 `BlockOffensive.commonSetup()` 中 `PACKET_REGISTER.registerPacket()`
- **新增 HUD 组件**：实现 `IHudRenderer`，通过 `BOClientBootstrap` 注册
- **新增 Capability**：实现 `MapCapability`/`TeamCapability`，通过 `CapabilityMap` 挂载
- **新增商店兼容模块**：实现 `ListenerModule`，通过 `LMManager` 注册
- **数据持久化**：通过 `RegisterFPSMSaveDataEvent` 注册，使用 `SaveHolder` 包装
- **模组检测**：优先使用 `FPSMImpl` 的检测方法，避免重复实现
- **发包**：优先使用 `BaseMap.sendPacketToXxx()` 方法，其次 `FPSMatch.sendToXxx()`

---

## 当前开发状态

**版本**：1.3.0-snapshot（Forge 1.20.1）
**依赖**：Minecraft 1.20.1 | Forge 47.3.11 | FPSMatch 1.2.3.7 | ModernUI 3.11.1.6

**已知状态**：
- 当前为快照开发版本
- FPSMatch 作为 submodule 紧密耦合
- `CSGameMap` (1541行)、`KillCamManager` (1185行) 较长，存在重构空间
- `CSGameMapFixer` 负责旧版数据迁移，正式版后可考虑移除