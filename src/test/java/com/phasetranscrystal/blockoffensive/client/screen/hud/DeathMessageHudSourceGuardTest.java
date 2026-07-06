package com.phasetranscrystal.blockoffensive.client.screen.hud;

import com.phasetranscrystal.blockoffensive.data.DeathMessageRules;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeathMessageHudSourceGuardTest {
    private static String readSource() throws IOException {
        return Files.readString(Path.of("src/main/java/com/phasetranscrystal/blockoffensive/client/screen/hud/CSDeathMessageHud.java"));
    }

    @Test
    void c4WeaponIsNotHardcodedAsSuicide() throws IOException {
        String source = readSource();

        assertFalse(source.contains("|| message.getWeapon().getItem() == BOItemRegister.C4.get()"));
        assertTrue(source.contains("DeathMessageRules.isSuicide(message.getDeadUUID(), message.getKillerUUID())"));
    }

    @Test
    void blindnessIconUsesConditionalRendererWithNullGuard() throws IOException {
        String source = readSource();

        assertFalse(source.contains("specialKillIcons.get(\"blindness\")"));
        assertTrue(source.contains("currentX = renderConditionalIcon(guiGraphics, \"blindness\", currentX, y);"));
    }

    @Test
    void flyingIconRendersAfterWeaponSection() throws IOException {
        String source = readSource();
        int weaponSection = source.indexOf("if(!isSuicide){");
        int iconLoop = source.indexOf("for (String iconKey : getPostWeaponIconKeys(message))");

        assertTrue(weaponSection >= 0);
        assertTrue(iconLoop > weaponSection);
    }

    @Test
    void specialKillFlagsUseSharedRenderAndWidthIconList() throws IOException {
        String source = readSource();
        String iconKeys = source.substring(source.indexOf("private static List<String> getPostWeaponIconKeys"));

        assertTrue(source.contains("for (String iconKey : getPostWeaponIconKeys(message))"));
        assertTrue(source.contains("width += getPostWeaponIconKeys(message).size() * 14;"));
        assertTrue(iconKeys.contains("DeathMessageRules.getPostWeaponIconKeys"));
        assertTrue(iconKeys.contains("message.isHeadShot()"));
        assertTrue(iconKeys.contains("message.isThroughSmoke()"));
        assertTrue(iconKeys.contains("message.isThroughWall()"));
    }

    @Test
    void specialKillFlagValuesProduceDeathMessageIconKeys() {
        assertEquals(
                List.of("headshot", "throw_smoke", "throw_wall"),
                DeathMessageRules.getPostWeaponIconKeys(false, true, true, true, false)
        );
    }
}
