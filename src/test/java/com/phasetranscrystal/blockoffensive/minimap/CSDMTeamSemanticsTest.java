package com.phasetranscrystal.blockoffensive.minimap;

import com.phasetranscrystal.blockoffensive.map.CSDMTeamSemantics;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CSDMTeamSemanticsTest {
    @Test
    void ffaHasNoTeammateRelationEvenOnSameLabel() {
        assertFalse(CSDMTeamSemantics.areTeammates(false, "1", "1", false, false));
        assertFalse(CSDMTeamSemantics.areTeammates(false, "1", "2", false, false));
    }

    @Test
    void tdmComparesActualRuntimeTeamsAndObserversRemainDistinct() {
        assertTrue(CSDMTeamSemantics.areTeammates(true, "1", "1", false, false));
        assertFalse(CSDMTeamSemantics.areTeammates(true, "1", "2", false, false));
        assertFalse(CSDMTeamSemantics.areTeammates(true, "1", "1", true, false));
        assertFalse(CSDMTeamSemantics.areTeammates(true, "1", "1", false, true));
        assertFalse(CSDMTeamSemantics.areTeammates(true, "spectator", "spectator", false, false));
    }

    @Test
    void capacityAndAssignmentPoolDifferForFfaAndTdm() {
        assertEquals(1, CSDMTeamSemantics.teamCapacity(false));
        assertTrue(CSDMTeamSemantics.teamCapacity(true) > 1);
        assertEquals(List.of("1", "2"), CSDMTeamSemantics.tdmTeamPool());
        assertTrue(CSDMTeamSemantics.ffaBaseTeamPool().size() >= 5);
    }

    @Test
    void mapTeamsMustNotHardCodeCsdmAlwaysFalse() throws Exception {
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("FPSMatch/src/main/java/com/phasetranscrystal/fpsmatch/core/team/MapTeams.java")
        );
        // remove the unreachable provider-truth hardcode: if ("csdm".equals(...)) return false;
        assertFalse(source.contains("if (\"csdm\".equals(map.getGameType()))"));
        assertFalse(source.contains("if (\"csdm\".equals(map.getGameType())) {\n            return false;"));
    }

    @Test
    void isTdmSettingIsExposedInDeathMatchSettingsSource() throws Exception {
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/phasetranscrystal/blockoffensive/map/CSDeathMatchMap.java")
        );
        assertTrue(source.contains("isTDM = this.addSetting(\"team\", \"isTDM\", false);"));
        // settings() must include the isTDM setting for persistence/round-trip
        assertTrue(source.contains("isTDM,") || source.matches("(?s).*settings\\(\\)[^{]*\\{[^}]*isTDM.*"));
        // isTDM() must not hardcode false
        assertFalse(source.contains("public boolean isTDM() {\n        return false;\n    }"));
        assertTrue(source.contains("return isTDM != null && Boolean.TRUE.equals(isTDM.get())")
                || source.contains("return isTDM.get()")
                || source.contains("return Boolean.TRUE.equals(isTDM.get())"));
    }
}
