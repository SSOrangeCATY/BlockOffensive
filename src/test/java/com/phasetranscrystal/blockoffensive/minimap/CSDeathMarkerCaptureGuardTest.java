package com.phasetranscrystal.blockoffensive.minimap;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CSDeathMarkerCaptureGuardTest {
    @Test
    void csCapturesDeathBeforeSpectatorTransition() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/phasetranscrystal/blockoffensive/map/CSGameMap.java"
        ));
        int capture = source.indexOf("CSDeathMarkerCapture.captureFromWorldPose");
        int spectator = source.indexOf("dead.setGameMode(GameType.SPECTATOR)");
        assertTrue(capture >= 0 && spectator > capture);
        assertTrue(source.contains("CS_DEATH_TTL_TICKS"));
    }

    @Test
    void csdmCapturesDeathBeforeImmediateRespawn() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/phasetranscrystal/blockoffensive/map/CSDeathMatchMap.java"
        ));
        int capture = source.indexOf("CSDeathMarkerCapture.captureFromWorldPose");
        int respawn = source.indexOf("respawnPlayer(context.getDeadPlayer())");
        assertTrue(capture >= 0 && respawn > capture);
        assertTrue(source.contains("CSDM_DEATH_TTL_TICKS"));
    }
}