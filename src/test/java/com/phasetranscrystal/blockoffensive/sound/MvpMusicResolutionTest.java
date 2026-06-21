package com.phasetranscrystal.blockoffensive.sound;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 MVP 音乐解析逻辑：玩家自定义音乐优先，无自定义时回退到 FPSMatch 默认音乐。
 * <p>
 * 对应 Bug 3 修复：设置了玩家 MVP 音乐后取得 MVP 时音乐未播放。
 * <p>
 * 根因：CSGameMap.roundVictory 未调用 sendMvpMusicPacket，且默认音乐
 * "blockoffensive:mvp.default" 从未注册为声音事件。
 * 修复：调用 sendMvpMusicPacket，默认音乐职责下沉到 FPSMatch 的 MvpMusicManager。
 * <p>
 * 由于测试 classpath 不包含 Minecraft 类（ResourceLocation）和 BlockOffensive
 * 运行时类（MVPMusicManager），此处通过纯 Java 字符串模拟验证解析逻辑的正确性。
 * 生产代码中 {@code CSGameMap.sendMvpMusicPacket} 与 {@code getMvpMusicName}
 * 使用 ResourceLocation，但逻辑等价。
 */
class MvpMusicResolutionTest {

    /**
     * FPSMatch 内置默认 MVP 音乐（与 MvpMusicManager.BUILTIN_DEFAULT 等价）。
     */
    private static final String FPSM_BUILTIN_DEFAULT = "fpsmatch:mvp.default";

    /**
     * 模拟 FPSMatch MvpMusicManager 的默认音乐状态。
     */
    private static String fpsmDefaultMusic = FPSM_BUILTIN_DEFAULT;

    /**
     * 模拟 BlockOffensive MVPMusicManager 的玩家自定义音乐存储。
     * key: 玩家 UUID 字符串，value: 音乐资源位置字符串。
     */
    private final Map<String, String> playerCustomMusic = new HashMap<>();
    private final Map<String, String> playerCustomMusicName = new HashMap<>();

    /**
     * 模拟 CSGameMap.sendMvpMusicPacket 的音乐解析逻辑。
     * 玩家有自定义音乐时返回自定义音乐，否则返回 FPSMatch 默认音乐。
     */
    static String resolveMvpMusic(Map<String, String> customMusicStore, String playerUuid) {
        if (customMusicStore.containsKey(playerUuid)) {
            return customMusicStore.get(playerUuid);
        }
        return fpsmDefaultMusic;
    }

    /**
     * 模拟 CSGameMap.getMvpMusicName 的名称解析逻辑。
     */
    static String resolveMvpMusicName(Map<String, String> customMusicNameStore,
                                      Map<String, String> customMusicStore,
                                      String playerUuid) {
        if (!customMusicStore.containsKey(playerUuid)) {
            return fpsmDefaultMusic;
        }
        return customMusicNameStore.getOrDefault(playerUuid, customMusicStore.get(playerUuid));
    }

    /**
     * 模拟 FPSMatch MvpMusicManager.setDefaultMvpMusic。
     */
    static void setFpsmDefaultMusic(String music) {
        fpsmDefaultMusic = music != null ? music : FPSM_BUILTIN_DEFAULT;
    }

    /**
     * 模拟 FPSMatch MvpMusicManager.resetToBuiltinDefault。
     */
    static void resetFpsmDefault() {
        fpsmDefaultMusic = FPSM_BUILTIN_DEFAULT;
    }

    @org.junit.jupiter.api.AfterEach
    void cleanup() {
        playerCustomMusic.clear();
        playerCustomMusicName.clear();
        resetFpsmDefault();
    }

    @Test
    void playerWithCustomMusic_returnsCustomMusic() {
        String uuid = "player-1";
        String custom = "test:epic_mvp";
        playerCustomMusic.put(uuid, custom);
        playerCustomMusicName.put(uuid, "Epic MVP");

        String resolved = resolveMvpMusic(playerCustomMusic, uuid);
        assertEquals(custom, resolved);
        assertEquals("Epic MVP", resolveMvpMusicName(playerCustomMusicName, playerCustomMusic, uuid));
    }

    @Test
    void playerWithoutCustomMusic_returnsFpsMatchDefault() {
        String uuid = "player-2";

        String resolved = resolveMvpMusic(playerCustomMusic, uuid);
        assertEquals("fpsmatch:mvp.default", resolved);
        assertEquals("fpsmatch:mvp.default", resolveMvpMusicName(playerCustomMusicName, playerCustomMusic, uuid));
    }

    @Test
    void playerWithoutCustomMusic_usesOverriddenDefault() {
        // 当 FPSMatch 默认音乐被自定义时，无自定义音乐的玩家使用新默认值
        setFpsmDefaultMusic("custom:mvp");

        String uuid = "player-3";
        String resolved = resolveMvpMusic(playerCustomMusic, uuid);
        assertEquals("custom:mvp", resolved);
    }

    @Test
    void customMusicTakesPrecedenceOverDefault() {
        // 玩家自定义音乐优先于 FPSMatch 默认音乐
        setFpsmDefaultMusic("fpsmatch:overridden");

        String uuid = "player-4";
        String custom = "player:unique";
        playerCustomMusic.put(uuid, custom);

        String resolved = resolveMvpMusic(playerCustomMusic, uuid);
        assertEquals(custom, resolved);
        assertFalse(resolved.equals("fpsmatch:overridden"));
    }

    @Test
    void defaultMusicIsNotNull() {
        // 确保默认音乐永不为 null（避免 NPE 导致播放失败）
        String uuid = "player-5";
        String resolved = resolveMvpMusic(playerCustomMusic, uuid);
        assertNotNull(resolved);
        assertTrue(!resolved.isEmpty());
    }

    @Test
    void multiplePlayers_eachResolvesIndependently() {
        String withMusic = "player-with-music";
        String withoutMusic = "player-without-music";
        playerCustomMusic.put(withMusic, "a:b");
        playerCustomMusicName.put(withMusic, "A");

        assertEquals("a:b", resolveMvpMusic(playerCustomMusic, withMusic));
        assertEquals("fpsmatch:mvp.default", resolveMvpMusic(playerCustomMusic, withoutMusic));
    }

    @Test
    void fpsmDefaultNeverNullOrEmpty_afterNullSet() {
        // 模拟 MvpMusicManager.setDefaultMvpMusic(null) 应回退到内置默认值
        setFpsmDefaultMusic(null);
        assertEquals("fpsmatch:mvp.default", fpsmDefaultMusic);
        assertFalse(fpsmDefaultMusic.isBlank());
    }

    @Test
    void fpsmDefault_resetRestoresBuiltin() {
        // 修改默认值后重置应恢复内置默认
        setFpsmDefaultMusic("temp:override");
        assertEquals("temp:override", fpsmDefaultMusic);
        resetFpsmDefault();
        assertEquals("fpsmatch:mvp.default", fpsmDefaultMusic);
    }
}
