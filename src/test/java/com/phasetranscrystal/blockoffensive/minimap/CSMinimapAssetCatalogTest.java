package com.phasetranscrystal.blockoffensive.minimap;

import com.phasetranscrystal.fpsmatch.core.minimap.model.NamespacedId;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class CSMinimapAssetCatalogTest {
    private static final Pattern NAMESPACED = Pattern.compile("^[a-z0-9_.-]+:[a-z0-9/._-]+$");
    private static final Path RESOURCES = Path.of("src/main/resources");

    @Test
    void everyReferencedMarkerStyleAndTextureExistsWithBoundedPng() throws Exception {
        assertFalse(CSMinimapAssetCatalog.MARKERS.isEmpty());
        Set<String> styleIds = new HashSet<>();
        for (CSMinimapAssetCatalog.MarkerAsset asset : CSMinimapAssetCatalog.MARKERS) {
            assertTrue(styleIds.add(asset.styleId().toString()), "duplicate style " + asset.styleId());
            assertTrue(NAMESPACED.matcher(asset.styleId().toString()).matches());
            assertTrue(NAMESPACED.matcher(asset.typeId().toString()).matches());
            assertEquals(asset.styleId().namespace().toLowerCase(Locale.ROOT), asset.styleId().namespace());
            assertEquals(asset.styleId().path().toLowerCase(Locale.ROOT), asset.styleId().path());

            Path png = RESOURCES.resolve(asset.texturePath());
            assertTrue(Files.isRegularFile(png), "missing texture " + png);
            long size = Files.size(png);
            assertTrue(size > 0 && size <= CSMinimapAssetCatalog.MAX_MARKER_ICON_BYTES, "png size " + size);
            byte[] bytes = Files.readAllBytes(png);
            assertTrue(bytes.length >= 24, "png too small");
            // PNG signature
            assertArrayEquals(new byte[]{(byte)0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A},
                    java.util.Arrays.copyOf(bytes, 8));
            // IHDR width/height at offset 16
            int width = ((bytes[16] & 0xff) << 24) | ((bytes[17] & 0xff) << 16) | ((bytes[18] & 0xff) << 8) | (bytes[19] & 0xff);
            int height = ((bytes[20] & 0xff) << 24) | ((bytes[21] & 0xff) << 16) | ((bytes[22] & 0xff) << 8) | (bytes[23] & 0xff);
            assertTrue(width > 0 && width <= CSMinimapAssetCatalog.MAX_MARKER_ICON_EDGE);
            assertTrue(height > 0 && height <= CSMinimapAssetCatalog.MAX_MARKER_ICON_EDGE);
            // color type must be RGBA (6) at IHDR byte 25 (offset 25)
            assertEquals(6, bytes[25] & 0xff, "must be 8-bit RGBA");
            assertEquals(8, bytes[24] & 0xff, "must be 8-bit depth");
        }
    }

    @Test
    void styleCatalogJsonMatchesCatalogAndUsesLowercaseIds() throws Exception {
        Path catalog = RESOURCES.resolve("assets/blockoffensive/minimap/marker_styles.json");
        assertTrue(Files.isRegularFile(catalog));
        String json = Files.readString(catalog, StandardCharsets.UTF_8);
        for (CSMinimapAssetCatalog.MarkerAsset asset : CSMinimapAssetCatalog.MARKERS) {
            assertTrue(json.contains(asset.styleId().toString()), "catalog missing " + asset.styleId());
            String texId = asset.texturePath()
                    .replace("assets/blockoffensive/", "blockoffensive:")
                    .replace("textures/minimap/", "textures/minimap/");
            // catalog uses blockoffensive:textures/minimap/markers/*.png
            assertTrue(json.contains("blockoffensive:textures/minimap/markers/"), "catalog texture namespace");
        }
        assertFalse(json.toLowerCase(Locale.ROOT).contains("assets/blockoffensive/textures/minimap/source"),
                "client-only source assets must not enter runtime catalog");
    }

    @Test
    void languageFilesContainEveryMinimapKey() throws Exception {
        for (String lang : List.of("en_us.json", "zh_cn.json")) {
            Path path = RESOURCES.resolve("assets/blockoffensive/lang/" + lang);
            String text = Files.readString(path, StandardCharsets.UTF_8);
            for (String key : CSMinimapAssetCatalog.allTranslationKeys()) {
                assertTrue(text.contains("\"" + key + "\""), lang + " missing " + key);
            }
        }
    }

    @Test
    void objectiveProviderStyleIdsAreCoveredByCatalog() {
        Set<String> styles = CSMinimapAssetCatalog.MARKERS.stream()
                .map(a -> a.styleId().toString())
                .collect(Collectors.toSet());
        assertTrue(styles.contains(CSGameObjectiveMarkerProvider.STYLE_CARRIED.toString()));
        assertTrue(styles.contains(CSGameObjectiveMarkerProvider.STYLE_DROPPED.toString()));
        assertTrue(styles.contains(CSGameObjectiveMarkerProvider.STYLE_PLANTED.toString()));
        assertTrue(styles.contains(CSGameObjectiveMarkerProvider.STYLE_DEFUSING.toString()));
        // FM fallback player/death styles still valid when BO assets absent; BO provides overrides.
        assertDoesNotThrow(() -> NamespacedId.parse("fpsmatch:style/player"));
        assertDoesNotThrow(() -> NamespacedId.parse("fpsmatch:style/death"));
    }
}