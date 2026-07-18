package com.phasetranscrystal.blockoffensive.minimap;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CSGameMinimapRegionSourceGuardTest {
    @Test
    void providerDoesNotExposeAreaDataOrInferSiteAB() throws Exception {
        String provider = Files.readString(Path.of(
                "src/main/java/com/phasetranscrystal/blockoffensive/minimap/CSGameMinimapRegionProvider.java"
        ));
        assertFalse(provider.contains("import com.phasetranscrystal.fpsmatch.core.data.AreaData"));
        assertFalse(provider.contains("site_a"));
        assertFalse(provider.contains("site_b"));
        assertTrue(provider.contains("BombSiteDefinition"));
        assertTrue(provider.contains("site.id()"));
    }

    @Test
    void sourcesUseCapabilityPureProjections() throws Exception {
        String sources = Files.readString(Path.of(
                "src/main/java/com/phasetranscrystal/blockoffensive/minimap/CSGameMinimapRegionSources.java"
        ));
        assertTrue(sources.contains("minimapMapBoundary"));
        assertTrue(sources.contains("getBombSites"));
        assertTrue(sources.contains("minimapSpawnEnvelope"));
        assertTrue(sources.contains("minimapShopBounds"));
        assertFalse(sources.contains("import com.phasetranscrystal.fpsmatch.core.data.AreaData"));
        assertFalse(sources.contains("getBombAreaData"));
        assertFalse(sources.contains("getSpawnPointsData"));
        assertFalse(sources.contains("getAreas()"));
    }
}