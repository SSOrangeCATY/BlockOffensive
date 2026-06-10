# 方块攻势

[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/SSOrangeCATY/BlockOffensive)
[English Documentation](README.md)

方块攻势是面向 Minecraft 1.20.1 Forge 的战术竞技模组，基于 [FPSMatch](https://github.com/SSOrangeCATY/FPSMatch) 比赛框架构建，提供接近 Counter-Strike 的回合制对局、队伍经济、C4 目标、击杀反馈与客户端比赛展示能力，适合整合包与服务器玩法使用。

## 功能概览

| 模块 | 说明 |
| --- | --- |
| 对局流程 | 基于 FPSMatch 的 Counter-Strike 风格回合生命周期 |
| 队伍系统 | 队伍选择、队伍状态、比分流程与队伍商店支持 |
| 经济系统 | 可编辑队伍商店，并接入购买阶段玩法 |
| 目标玩法 | C4 放置、爆炸、拆弹工具与目标回合胜负判定 |
| 战斗反馈 | 击杀反馈、死亡信息、爆头反馈、HUD、Overlay 与 TAB 展示 |
| 兼容集成 | 围绕 FPSMatch、TaCZ、Modern UI、KubeJS 及相关 Forge 玩法模组集成 |
| 指令帮助 | 游戏内可通过 `/fpsm help` 查看指令帮助 |

## 版本兼容矩阵

| BlockOffensive | 分发来源 | Minecraft | Forge | FPSMatch | Modern UI | TaCZ | LR Tactical | CounterStrikeGrenade | KubeJS | Physics Mod | Hit Indication | GD656 Kill Icon | TaCZ Tweaks |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1.3.0 | QQ 群 / GitHub | 1.20.1 | 47.4.6 | 1.3.0+ | 3.12.0.1 | 1.1.7-hotfix | 0.3.0 | 1.4.1 | 2001.6.5-build.14 | 3.0.14 | 1.20.1-1.4 | 1.0.8-1.20.1-forge | 2.11.2 |
| 1.2.5.1 | Modrinth / CurseForge | 1.20.1 | 47.4.6 | 1.2.5 | 3.11.1.6 | 1.1.6-hotfix | 0.3.0 | 1.2.8 | - | 3.0.14 | 1.20.1-1.4 | 0.4.2-1.20.1 | - |
| 1.2.5 | Modrinth | 1.20.1 | 47.4.6 | 1.2.5 | 3.11.1.6 | 1.1.6-hotfix | 0.3.0 | 1.2.8 | - | 3.0.14 | 1.20.1-1.4 | 0.4.2-1.20.1 | - |

## 下载

| 平台 | 链接 |
| --- | --- |
| GitHub Releases | [Releases](https://github.com/SSOrangeCATY/BlockOffensive/releases) |
| Modrinth | [Modrinth 上的 BlockOffensive](https://modrinth.com/mod/blockoffensive) |
| CurseForge | [CurseForge 上的 BlockOffensive](https://www.curseforge.com/minecraft/mc-mods/blockoffensive) |

## 如何依赖方块攻势

方块攻势可以从公开的模组分发 Maven 仓库中拉取。根据你希望使用的分发平台，在 Gradle 中选择对应仓库和依赖坐标即可。

### CurseForge Maven

CurseForge Maven 通过 CurseForge 项目 ID 与文件 ID 解析产物。当前已确认的 CurseForge 项目 ID 为 `1332812`，`1.2.5.1` 对应的公开文件 ID 为 `7110162`。

```gradle
repositories {
    maven {
        name = "CurseMaven"
        url = "https://www.cursemaven.com"
    }
}

dependencies {
    modImplementation "curse.maven:blockoffensive-1332812:7110162"
}
```

### Modrinth Maven

Modrinth Maven 通过项目 slug 与 Modrinth 版本号解析产物。当前已确认的项目 slug 为 `blockoffensive`。

```gradle
repositories {
    maven {
        name = "Modrinth"
        url = "https://api.modrinth.com/maven"
    }
}

dependencies {
    modImplementation "maven.modrinth:blockoffensive:1.2.5.1"
}
```

如果使用项目自身 Maven 发布配置生成的源码构建产物，依赖坐标为 `com.phasetranscrystal:blockoffensive:<方块攻势版本>`。

## 社区与链接

| 资源 | 链接 |
| --- | --- |
| GitHub | [SSOrangeCATY/BlockOffensive](https://github.com/SSOrangeCATY/BlockOffensive) |
| FPSMatch | [SSOrangeCATY/FPSMatch](https://github.com/SSOrangeCATY/FPSMatch) |
| Bilibili | [作者主页](https://space.bilibili.com/21254202) |
| QQ 群 | 771884981 |

## 许可证

使用方块攻势即表示你接受 GPL v3 条款。完整许可证见 [LICENSE](LICENSE)。
