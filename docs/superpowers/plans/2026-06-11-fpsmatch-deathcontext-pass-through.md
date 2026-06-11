# FPSMatch Pass-Through DeathContext Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 BlockOffensive 里的穿墙/穿烟击杀标记迁移到 FPSMatch 通用枪械死亡管线，并在 DeathContext 中新增大狙击杀前是否开镜状态。

**Architecture:** FPSMatch 负责从 TaCZ 子弹实体与枪械击杀事件补全 DeathContext，BlockOffensive 只消费 DeathContext 生成死亡消息。子弹穿墙/穿烟状态通过 FPSMatch mixin 写入通用接口，死亡管线在 EntityKillByGunEvent 中读取接口并写入 DeathContext；狙击枪开镜状态通过 TaCZ 的 IGunOperator 同步瞄准状态与 GunTabType.SNIPER 判定写入 DeathContext。

**Tech Stack:** Java 21、Minecraft Forge 1.20.1、Mixin、MixinExtras、TaCZ API、FPSMatch DeathContext/FPSMDeathPipelineEventHook。

---

## Current Evidence

- BlockOffensive 原穿墙/穿烟标记来自 `src/main/java/com/phasetranscrystal/blockoffensive/mixin/ammo/AmmoEntityMixin.java`、`DefaultAmmoMixin.java`、`TweakAmmoMixin.java` 与 `compat/IPassThroughEntity.java`。
- BlockOffensive 原消费点在 `src/main/java/com/phasetranscrystal/blockoffensive/map/CSMap.java`，从 `context.getGunBullet()` 读取 `IPassThroughEntity` 后再写回 `DeathContext`。
- FPSMatch 的 `DeathContext` 已有 `passWall/passSmoke` 字段，但没有通用写入点。
- FPSMatch 的 `FPSMDeathPipelineEventHook.onPlayerKillEvent(EntityKillByGunEvent)` 是枪械击杀补全 DeathContext 的正确入口。
- TaCZ API 可用方法：`EntityKillByGunEvent#getBullet()`、`#getGunId()`、`IGunOperator#fromLivingEntity(attacker).getSynAimingProgress()`、`FPSMUtil.getGunTypeByGunId(...)`。

## Modified Files

- `FPSMatch/src/main/java/com/phasetranscrystal/fpsmatch/core/map/DeathContext.java`
  - 新增 `scopedKill` 字段、getter/setter。
- `FPSMatch/src/main/java/com/phasetranscrystal/fpsmatch/common/event/FPSMDeathPipelineEventHook.java`
  - 在枪械击杀事件补全 `passWall/passSmoke/scopedKill`。
- `FPSMatch/src/main/java/com/phasetranscrystal/fpsmatch/compat/IPassThroughEntity.java`
  - 从 BlockOffensive 迁移通用子弹穿透接口。
- `FPSMatch/src/main/java/com/phasetranscrystal/fpsmatch/mixin/accessor/FPSMClipContextAccessor.java`
  - 从 BlockOffensive 迁移 ClipContext collisionContext accessor。
- `FPSMatch/src/main/java/com/phasetranscrystal/fpsmatch/mixin/ammo/AmmoEntityMixin.java`
  - 从 BlockOffensive 迁移 TaCZ 子弹穿烟状态记录。
- `FPSMatch/src/main/java/com/phasetranscrystal/fpsmatch/mixin/ammo/DefaultAmmoMixin.java`
  - 从 BlockOffensive 迁移默认 TaCZ 穿墙记录。
- `FPSMatch/src/main/java/com/phasetranscrystal/fpsmatch/mixin/ammo/TweakAmmoMixin.java`
  - 从 BlockOffensive 迁移 TaCZ Tweaks 穿墙记录。
- `FPSMatch/src/main/java/com/phasetranscrystal/fpsmatch/mixin/FPSMatchMixinPlugin.java`
  - 为默认/TaCZ Tweaks 穿墙 mixin 增加互斥加载条件。
- `FPSMatch/src/main/resources/fpsmatch.mixins.json`
  - 注册迁移后的 ammo/accessor mixin。
- `src/main/java/com/phasetranscrystal/blockoffensive/map/CSMap.java`
  - 删除原有从子弹实体读取穿墙/穿烟并写入 DeathContext 的重复功能。
- `src/main/resources/blockoffensive.mixins.json`
  - 删除原有 ammo/accessor mixin 注册。

## Tasks

### Task 1: 迁移穿透接口与 mixin 到 FPSMatch

- [ ] 创建 `FPSMatch/src/main/java/com/phasetranscrystal/fpsmatch/compat/IPassThroughEntity.java`。
- [ ] 创建 `FPSMatch/src/main/java/com/phasetranscrystal/fpsmatch/mixin/accessor/FPSMClipContextAccessor.java`。
- [ ] 创建 `FPSMatch/src/main/java/com/phasetranscrystal/fpsmatch/mixin/ammo/AmmoEntityMixin.java`，包名与接口 import 改为 FPSMatch。
- [ ] 创建 `FPSMatch/src/main/java/com/phasetranscrystal/fpsmatch/mixin/ammo/DefaultAmmoMixin.java`，accessor 改为 `FPSMClipContextAccessor`。
- [ ] 创建 `FPSMatch/src/main/java/com/phasetranscrystal/fpsmatch/mixin/ammo/TweakAmmoMixin.java`，接口 import 改为 FPSMatch。

### Task 2: 注册 FPSMatch mixin 并删除 BlockOffensive 原 mixin 注册

- [ ] 在 `fpsmatch.mixins.json` 的 `mixins` 中加入 `accessor.FPSMClipContextAccessor`、`ammo.AmmoEntityMixin`、`ammo.DefaultAmmoMixin`、`ammo.TweakAmmoMixin`。
- [ ] 在 `FPSMatchMixinPlugin.shouldApplyMixin` 中用 `mixinClassName` 判断 `ammo.DefaultAmmoMixin` 与 `ammo.TweakAmmoMixin`，按 `FPSMImpl.findTaczTweaks()` 互斥加载。
- [ ] 从 `blockoffensive.mixins.json` 删除 `accessor.BOClipContextAccessor`、`ammo.AmmoEntityMixin`、`ammo.DefaultAmmoMixin`、`ammo.TweakAmmoMixin`。

### Task 3: DeathContext 新增狙击开镜状态

- [ ] 在 `DeathContext` 增加 `private boolean scopedKill;`。
- [ ] 增加 `public boolean isScopedKill()` 与 `public void setScopedKill(boolean scopedKill)`。

### Task 4: FPSMatch 死亡管线写入穿墙/穿烟/开镜

- [ ] 在 `FPSMDeathPipelineEventHook.onPlayerKillEvent` 中读取 `context.getGunBullet()` 或 `event.getBullet()` 的 `IPassThroughEntity`。
- [ ] 将子弹的穿墙/穿烟状态写入 `context.setPassWall(...)` 与 `context.setPassSmoke(...)`。
- [ ] 对 `FPSMUtil.getGunTypeByGunId(event.getGunId()) == GunTabType.SNIPER` 的枪写入 `context.setScopedKill(IGunOperator.fromLivingEntity(attacker).getSynAimingProgress() > 0.5f)`。

### Task 5: 删除 BlockOffensive 原有重复消费逻辑

- [ ] 从 `CSMap.java` 删除 `IPassThroughEntity` import。
- [ ] 删除 `handleDeath` 中从 `context.getGunBullet()` 读取 `IPassThroughEntity` 并写回 `DeathContext` 的代码块。
- [ ] 保留 `BOUtil.buildDeathMessagePacket(...)` 读取 `context.isPassWall()` 和 `context.isPassSmoke()`。

### Task 6: 验证与审查

- [ ] 运行 `rg "blockoffensive\$|IPassThroughEntity|setPassWall|setPassSmoke|isScopedKill|setScopedKill" FPSMatch/src/main/java src/main/java -n`。
- [ ] 运行 `gradlew.bat compileJava --console=plain`。
- [ ] 运行 `gradlew.bat build --console=plain`。
- [ ] 审查 mixin 条件加载，确保未装 TaCZ Tweaks 时不加载 `TweakAmmoMixin`，装了 TaCZ Tweaks 时不加载 `DefaultAmmoMixin`。

## Self-Review

- Spec coverage: 覆盖穿墙、穿烟迁移到 FPSMatch，删除 BlockOffensive 原有重复功能，变量写入 DeathContext，大狙击杀前开镜状态写入 DeathContext。
- Placeholder scan: 无占位任务；每个任务有明确文件和检查命令。
- Type consistency: 使用已确认的 TaCZ API 方法与现有 FPSMatch 工具 `FPSMUtil.getGunTypeByGunId`。
