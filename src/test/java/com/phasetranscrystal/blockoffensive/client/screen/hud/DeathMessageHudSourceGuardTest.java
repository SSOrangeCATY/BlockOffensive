package com.phasetranscrystal.blockoffensive.client.screen.hud;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
        int flyingIcon = source.indexOf("if (message.isFlying()) currentX = renderConditionalIcon(guiGraphics, \"fly\", currentX, y);");

        assertTrue(weaponSection >= 0);
        assertTrue(flyingIcon > weaponSection);
    }
}
