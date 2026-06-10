# CS2 Deathmatch Flow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完善 BlockOffensive 的 `csdm` 死斗模式，使其核心流程与 CS2 死斗模式一致：限时对局、持续重生、击杀计分、短暂无敌购买/换装窗口、远离玩家的出生点选择、结束时按分数排序结算。

**Architecture:** 保持 `CSDeathMatchMap` 作为死斗模式权威状态与流程入口，死亡管线仍由 FPSMatch 统一触发，BlockOffensive 通过 `FPSMapEvent.PlayerEvent.KillEvent` 追加死斗得分逻辑。客户端 HUD/Tab 继续读取同步后的 `PlayerData`，服务端修复 living 状态、得分、保护窗口和出生点选择，避免新增独立状态系统。

**Tech Stack:** Java 21、Minecraft Forge 1.20.1、FPSMatch `BaseMap` / `PlayerData` / `FPSMapEvent`、Gradle `compileJava`。

---

## Current Evidence

- 死斗模式主类是 `src/main/java/com/phasetranscrystal/blockoffensive/map/CSDeathMatchMap.java`，游戏类型常量为 `TYPE = "csdm"`。
- `CSDeathMatchMap.setup()` 已有 `isTDM`、`matchTimeLimit`、`spawnProtectionTime` 三个设置，当前默认 `matchTimeLimit = 18000` tick、`spawnProtectionTime = 100`。
- `CSDeathMatchMap.start()` 已初始化倒计时、清理地图、重置玩家数据、收集出生点、发放初始装备。
- FPSMatch 死亡管线 `FPSMDeathPipelineEventHook.finalizeDeath()` 先调用 `map.handleDeath(context)`，然后写入击杀/爆头/助攻并广播 `KillEvent`。
- `BaseMap.handleDeath()` 会将死亡玩家 `PlayerData.living` 设为 `false` 并增加死亡数；`CSDeathMatchMap.respawnPlayer()` 当前没有把 living 设回 `true`。
- `CSDMOverlay` 与 `CSDMTabRenderer` 已按分数降序显示玩家，但 `CSDeathMatchMap.victory()` 当前使用升序 comparator。

## CS2 Deathmatch Alignment

### 已完成

- 已有独立 `csdm` 游戏类型与持久化注册。
- 已有固定时间倒计时，时间归零触发胜利结算。
- 已有死亡后立即重生流程。
- 已有全地图出生点池，不绑定攻守回合出生点。
- 已有出生保护/购买窗口雏形：玩家未移动、未开火时可以开店，受到伤害会被取消。
- 已有移动或开火取消保护逻辑。
- 已有死斗 HUD 和 Tab，显示时间、头像、K/D/A、爆头率、伤害与分数。
- 已有 TDM 开关并能阻止同队伤害。

### 未完成

- 击杀不会增加 `scores`，HUD/Tab 排名缺少真实得分来源。
- 重生后 `PlayerData.living` 不会恢复，客户端头像可能保持死亡灰度，后续流程也会误判玩家未存活。
- `spawnProtectionTime` 使用 `* 1000L` 按秒计算，默认 100 会形成约 100 秒保护；CS2 死斗更接近短暂保护/购买窗口。
- `DMPlayerData.needRespawnProtection` 被写入但不参与判断，保护状态语义不闭环。
- 出生点权重 `playerDistanceFactor / (distance + 1.0)` 会让靠近玩家的位置权重更高，方向与 CS2 尽量避开近距离交战点相反。
- 胜利结算按分数升序排序，和 HUD/Tab 的降序排名不一致。
- TDM 只有友伤限制，没有团队分数累计与团队胜者结算；本计划先对齐个人死斗，TDM 团队计分作为后续扩展。

## File Structure

### Modified files

- `src/main/java/com/phasetranscrystal/blockoffensive/map/CSDeathMatchMap.java`
  - 修复默认保护时间、重生后 living 状态、胜利排序、出生点权重和保护状态语义。
- `src/main/java/com/phasetranscrystal/blockoffensive/map/CSGameEvents.java`
  - 在死斗 `KillEvent` 中给有效击杀加分，避免改动 FPSMatch 通用死亡管线。

### Verification files

- 不新增测试文件。当前项目没有 BlockOffensive 单元测试/Gametest 样例，验证以 `gradlew compileJava` 和源码级 grep 检查为准。

---

## Task 1: 修复死斗击杀计分

**Files:**
- Modify: `src/main/java/com/phasetranscrystal/blockoffensive/map/CSGameEvents.java:64-80`
- Verify: `src/main/java/com/phasetranscrystal/blockoffensive/map/CSGameEvents.java`

- [ ] **Step 1: 确认当前没有死斗得分写入**

Run:

```bash
rg "addScore|setScores" src FPSMatch/src/main/java
```

Expected: 只看到 `PlayerData` 定义、MVP/回合结算等非死斗击杀得分路径，没有 `CSDeathMatchMap` 或 `CSGameEvents` 对有效击杀调用 `addScore`。

- [ ] **Step 2: 在 KillEvent 中追加死斗击杀得分**

Replace the current `onTeamKillPenalty` method with:

```java
    @SubscribeEvent
    public static void onTeamKillPenalty(FPSMapEvent.PlayerEvent.KillEvent event) {
        if (!(event.getMap() instanceof CSMap cs)) return;

        ServerPlayer killer = event.getPlayer();
        ServerPlayer dead = event.getDead();
        boolean teammateKill = cs.getMapTeams().isSameTeam(killer, dead) && !isC4Kill(event.getSource());
        if (teammateKill) {
            cs.getMapTeams().getPlayerData(killer).ifPresent(PlayerData::removeKill);
            return;
        }

        if (cs instanceof CSDeathMatchMap) {
            cs.getMapTeams().getPlayerData(killer).ifPresent(data -> data.addScore(1));
        }
    }
```

- [ ] **Step 3: 编译验证事件方法签名与导入**

Run:

```bash
.\gradlew compileJava
```

Expected: BUILD SUCCESSFUL。

- [ ] **Step 4: 检查得分路径只作用于死斗**

Run:

```bash
rg "data.addScore\(1\)|instanceof CSDeathMatchMap" src/main/java/com/phasetranscrystal/blockoffensive/map/CSGameEvents.java -n
```

Expected: `data.addScore(1)` 位于 `cs instanceof CSDeathMatchMap` 分支中，普通爆破模式不被击杀加分规则影响。

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/phasetranscrystal/blockoffensive/map/CSGameEvents.java
git commit -m "fix: score deathmatch kills"
```

---

## Task 2: 修复重生后玩家存活状态

**Files:**
- Modify: `src/main/java/com/phasetranscrystal/blockoffensive/map/CSDeathMatchMap.java:244-260`
- Verify: `src/main/java/com/phasetranscrystal/blockoffensive/map/CSDeathMatchMap.java`

- [ ] **Step 1: 确认死亡流程会将 living 设为 false**

Run:

```bash
rg "setLiving\(false\)|setLiving\(true\)" FPSMatch/src/main/java src/main/java -n
```

Expected: `BaseMap.handleDeath` 有 `setLiving(false)`，当前 `CSDeathMatchMap.respawnPlayer` 没有对应 `setLiving(true)`。

- [ ] **Step 2: 在 respawnPlayer 中恢复 PlayerData.living**

Replace `respawnPlayer` with:

```java
    public void respawnPlayer(ServerPlayer player) {
        player.heal(player.getMaxHealth());
        player.removeAllEffects();
        player.setGameMode(GameType.ADVENTURE);

        SpawnPointData spawnPoint = getRandomSpawnPoint();
        if (spawnPoint != null) {
            teleportToPoint(player, spawnPoint);
        }

        this.getMapTeams().getPlayerData(player).ifPresent(data -> data.setLiving(true));
        givePlayerKits(player);

        getDMPlayerData(player.getUUID()).ifPresent(DMPlayerData::respawn);
    }
```

- [ ] **Step 3: 编译验证 PlayerData API 可用**

Run:

```bash
.\gradlew compileJava
```

Expected: BUILD SUCCESSFUL。

- [ ] **Step 4: 检查客户端灰头像条件已有数据来源**

Run:

```bash
rg "isLiving\(\)|setLiving\(true\)|setLiving\(false\)" src/main/java FPSMatch/src/main/java -n
```

Expected: `CSDMOverlay` 仍读取 `data.isLiving()`，死斗重生路径现在能把该字段恢复为 true。

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/phasetranscrystal/blockoffensive/map/CSDeathMatchMap.java
git commit -m "fix: restore deathmatch living state on respawn"
```

---

## Task 3: 修复重生保护窗口语义

**Files:**
- Modify: `src/main/java/com/phasetranscrystal/blockoffensive/map/CSDeathMatchMap.java:101-106`
- Modify: `src/main/java/com/phasetranscrystal/blockoffensive/map/CSDeathMatchMap.java:402-472`
- Verify: `src/main/java/com/phasetranscrystal/blockoffensive/map/CSDeathMatchMap.java`

- [ ] **Step 1: 调整默认保护时长为 CS2 风格短窗口**

Replace the `spawnProtectionTime` default in `setup()` with:

```java
        spawnProtectionTime = this.addSetting("spawnProtectionTime", 10);
```

- [ ] **Step 2: 让保护状态显式依赖 needRespawnProtection**

Replace `isInSpawnProtection` with:

```java
    public boolean isInSpawnProtection(UUID uuid) {
        return this.getDMPlayerData(uuid)
                .map(d -> d.isSpawning() && System.currentTimeMillis() - d.lastProtectionTime < spawnProtectionTime.get() * 1000L)
                .orElse(false);
    }
```

Replace `DMPlayerData.isSpawning`, `setFired`, and `setMoved` with:

```java
        public boolean isSpawning(){
            return needRespawnProtection && !isFired && !isMoved;
        }

        public void setFired(){
            isFired = true;
            needRespawnProtection = false;
        }

        public void setMoved(){
            isMoved = true;
            needRespawnProtection = false;
        }
```

- [ ] **Step 3: 编译验证内部类字段访问无误**

Run:

```bash
.\gradlew compileJava
```

Expected: BUILD SUCCESSFUL。

- [ ] **Step 4: 检查开火/移动取消保护链仍然存在**

Run:

```bash
rg "handlePlayerFire|handlePlayerMove|PlayerMoveC2SPacket|GunShootEvent" src/main/java -n
```

Expected: 客户端移动包和服务端开火事件仍分别调用 `handlePlayerMove` / `handlePlayerFire`。

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/phasetranscrystal/blockoffensive/map/CSDeathMatchMap.java
git commit -m "fix: tighten deathmatch spawn protection"
```

---

## Task 4: 修复出生点权重方向

**Files:**
- Modify: `src/main/java/com/phasetranscrystal/blockoffensive/map/CSDeathMatchMap.java:276-308`
- Verify: `src/main/java/com/phasetranscrystal/blockoffensive/map/CSDeathMatchMap.java`

- [ ] **Step 1: 将权重改为偏好远离在线玩家的位置**

Replace `calculateSpawnPointWeight` with:

```java
    public double calculateSpawnPointWeight(SpawnPointData spawnPoint) {
        double weight = 1.0;
        List<ServerPlayer> onlinePlayers = this.getMapTeams().getOnline();

        for (Player player : onlinePlayers) {
            if (player.isSpectator() || player.isDeadOrDying()) {
                continue;
            }

            double distance = player.distanceToSqr(spawnPoint.getX(), spawnPoint.getY(), spawnPoint.getZ());
            weight += Math.min(distance, 4096.0) / 4096.0;
        }

        return weight;
    }
```

- [ ] **Step 2: 改用地图随机源避免每次创建 Random**

Replace `selectWeightedRandomSpawnPoint` with:

```java
    public SpawnPointData selectWeightedRandomSpawnPoint(Map<SpawnPointData, Double> weightMap) {
        double totalWeight = weightMap.values().stream().mapToDouble(Double::doubleValue).sum();
        double randomValue = this.getRandom().nextDouble() * totalWeight;

        double currentWeight = 0.0;
        for (Map.Entry<SpawnPointData, Double> entry : weightMap.entrySet()) {
            currentWeight += entry.getValue();
            if (currentWeight >= randomValue) {
                return entry.getKey();
            }
        }

        return weightMap.keySet().iterator().next();
    }
```

- [ ] **Step 3: 编译验证 Math 与随机源使用无误**

Run:

```bash
.\gradlew compileJava
```

Expected: BUILD SUCCESSFUL。

- [ ] **Step 4: 检查 `java.util.Random` 是否还能移除**

Run:

```bash
rg "new Random|java.util\.\*|Random" src/main/java/com/phasetranscrystal/blockoffensive/map/CSDeathMatchMap.java -n
```

Expected: `new Random` 不再出现；如果 `java.util.*` 仍覆盖所需类型，可不拆分 import。

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/phasetranscrystal/blockoffensive/map/CSDeathMatchMap.java
git commit -m "fix: prefer safer deathmatch spawns"
```

---

## Task 5: 修复死斗结束排名顺序

**Files:**
- Modify: `src/main/java/com/phasetranscrystal/blockoffensive/map/CSDeathMatchMap.java:177-184`
- Verify: `src/main/java/com/phasetranscrystal/blockoffensive/map/CSDeathMatchMap.java`

- [ ] **Step 1: 将胜利结算排序改为分数降序**

Replace `victory()` with:

```java
    @Override
    public void victory(){
        this.sendVictoryMessage(
                Component.translatable("map.deathmatch.message.victory.head").withStyle(ChatFormatting.GOLD).withStyle(ChatFormatting.BOLD),
                Comparator.comparingInt(PlayerData::getScores).reversed());
        super.victory();
        this.reset();
    }
```

- [ ] **Step 2: 编译验证 comparator 类型推断**

Run:

```bash
.\gradlew compileJava
```

Expected: BUILD SUCCESSFUL。

- [ ] **Step 3: 检查 HUD、Tab、结算均为降序**

Run:

```bash
rg "comparingInt\(.*getScores|reversed\(\)" src/main/java/com/phasetranscrystal/blockoffensive -n
```

Expected: `CSDeathMatchMap.victory`、`CSDMOverlay`、`CSDMTabRenderer` 都对分数使用降序排序。

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/phasetranscrystal/blockoffensive/map/CSDeathMatchMap.java
git commit -m "fix: rank deathmatch results by high score"
```

---

## Task 6: 做全链路回归检查

**Files:**
- Verify: `src/main/java/com/phasetranscrystal/blockoffensive/map/CSDeathMatchMap.java`
- Verify: `src/main/java/com/phasetranscrystal/blockoffensive/map/CSGameEvents.java`
- Verify: `src/main/java/com/phasetranscrystal/blockoffensive/client/screen/hud/CSDMOverlay.java`
- Verify: `src/main/java/com/phasetranscrystal/blockoffensive/client/screen/hud/CSDMTabRenderer.java`

- [ ] **Step 1: 编译整个主源码**

Run:

```bash
.\gradlew compileJava
```

Expected: BUILD SUCCESSFUL。

- [ ] **Step 2: 检查死斗核心流程引用**

Run:

```bash
rg "CSDeathMatchMap|csdm|isInSpawnProtection|handlePlayerFire|handlePlayerMove|addScore" src/main/java FPSMatch/src/main/java -n
```

Expected: 关键引用仍指向 `CSDeathMatchMap`、`CSGameEvents`、`PlayerMoveC2SPacket`、HUD/Tab 注册路径，没有遗漏旧方法名。

- [ ] **Step 3: 检查未完成项是否仍为刻意保留**

Run:

```bash
rg "isTDM\(|matchTimeLimit|spawnProtectionTime|getClientTime|victoryGoal" src/main/java/com/phasetranscrystal/blockoffensive/map -n
```

Expected: `isTDM` 仍只控制友伤，`victoryGoal` 仍按时间结束；团队死斗得分未在本计划内实现。

- [ ] **Step 4: 更新最终交付说明**

Record this verification matrix in the final response:

```text
已验证：compileJava 通过；死斗击杀计分、重生 living、保护窗口、出生点权重、胜利排序均有源码证据。
未验证：真实多人服务器内 CS2 手感；需要本地或测试服创建 csdm 地图后进行实机验证。
刻意未做：TDM 团队分数与团队胜者结算，作为后续扩展。
```

- [ ] **Step 5: Commit**

```bash
git status --short
git add src/main/java/com/phasetranscrystal/blockoffensive/map/CSDeathMatchMap.java src/main/java/com/phasetranscrystal/blockoffensive/map/CSGameEvents.java
git commit -m "fix: align deathmatch flow with CS2"
```

---

## Self-Review

- Spec coverage: 计划覆盖死斗限时流程、持续重生、有效击杀计分、重生保护、购买窗口、出生点选择、HUD/Tab 排名数据来源和结算排序；TDM 团队计分明确列为未完成后续扩展。
- Placeholder scan: 未发现占位式任务；每个代码变更步骤均给出具体替换代码或命令。
- Type consistency: 使用现有 `FPSMapEvent.PlayerEvent.KillEvent`、`CSDeathMatchMap`、`PlayerData.addScore(int)`、`PlayerData.setLiving(boolean)`、`BaseMap.getRandom()`，签名与已检查源码一致。
- Risk notes: 保护时长默认从 100 改为 10 会改变旧地图体验；如旧存档已有 setting，保存值可能覆盖默认值。出生点权重仅基于距离，没有做视线、阵营、敌我火力方向判断，属于最小可验证改进。
