package com.phasetranscrystal.blockoffensive.minimap;

import com.phasetranscrystal.fpsmatch.core.minimap.model.MapKey;
import com.phasetranscrystal.fpsmatch.core.minimap.region.BombSiteDefinition;
import com.phasetranscrystal.fpsmatch.core.minimap.region.BombSiteIdAssigner;
import com.phasetranscrystal.fpsmatch.core.minimap.region.RuntimeRegionDescriptor;
import com.phasetranscrystal.fpsmatch.core.minimap.region.WorldAxisAlignedBounds;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CSGameMinimapRegionProviderTest {
    @Test
    void emitsBoundarySitesSpawnsShopsWithStableSiteIds() {
        WorldAxisAlignedBounds map = new WorldAxisAlignedBounds(-50, 0, -50, 50, 20, 50);
        List<BombSiteDefinition> sites = BombSiteIdAssigner.assignAnonymous(List.of(
                new WorldAxisAlignedBounds(1, 0, 1, 4, 3, 4),
                new WorldAxisAlignedBounds(20, 0, 20, 24, 3, 24)
        ));
        CSGameMinimapRegionProvider provider = new CSGameMinimapRegionProvider(
                () -> Optional.of(map),
                () -> sites,
                () -> List.of(
                        new CSGameMinimapRegionProvider.TeamRegionSource("ct", new WorldAxisAlignedBounds(-40, 0, -40, -30, 3, -30)),
                        new CSGameMinimapRegionProvider.TeamRegionSource("t", new WorldAxisAlignedBounds(30, 0, 30, 40, 3, 40))
                ),
                () -> List.of(
                        new CSGameMinimapRegionProvider.TeamRegionSource("ct", new WorldAxisAlignedBounds(-20, 0, -20, -15, 3, -15))
                )
        );
        List<RuntimeRegionDescriptor> regions = provider.collect(new MapKey("cs", "dust2"), "ground");
        assertEquals(1, regions.stream().filter(r -> r.id().equals("map_boundary")).count());
        assertEquals(List.of("site_1", "site_2"),
                regions.stream().filter(r -> r.semanticType().equals(CSGameMinimapRegionProvider.SEMANTIC_BOMB_SITE))
                        .map(RuntimeRegionDescriptor::id).toList());
        assertTrue(regions.stream().anyMatch(r -> r.id().equals("spawn_ct")));
        assertTrue(regions.stream().anyMatch(r -> r.id().equals("spawn_t")));
        assertTrue(regions.stream().anyMatch(r -> r.id().equals("shop_ct")));
        assertTrue(regions.stream().allMatch(r -> r.floorId().equals("ground")));
        assertTrue(regions.stream().filter(r -> r.id().startsWith("site_"))
                .allMatch(r -> r.gameplayReference().isPresent()));
        // missing capability / wrong game type
        assertTrue(provider.collect(new MapKey("csdm", "aim"), "ground").isEmpty());
    }

    @Test
    void missingBoundaryAndEmptySourcesStillDeterministic() {
        CSGameMinimapRegionProvider provider = new CSGameMinimapRegionProvider(
                Optional::empty,
                List::of,
                List::of,
                List::of
        );
        assertTrue(provider.collect(new MapKey("cs", "dust2"), "ground").isEmpty());
        assertEquals(
                provider.collect(new MapKey("cs", "dust2"), "ground"),
                provider.collect(new MapKey("cs", "dust2"), "ground")
        );
    }

    @Test
    void siteIdsDoNotDependOnDisplayNames() {
        List<BombSiteDefinition> renamed = List.of(
                BombSiteDefinition.of("site_1", "Banana", new WorldAxisAlignedBounds(0, 0, 0, 1, 1, 1)),
                BombSiteDefinition.of("site_2", "A Site", new WorldAxisAlignedBounds(2, 0, 2, 3, 1, 3))
        );
        CSGameMinimapRegionProvider provider = new CSGameMinimapRegionProvider(
                Optional::empty,
                () -> renamed,
                List::of,
                List::of
        );
        List<String> ids = provider.collect(new MapKey("cs", "dust2"), "ground").stream()
                .map(RuntimeRegionDescriptor::id).toList();
        assertEquals(List.of("site_1", "site_2"), ids);
        assertEquals(
                provider.collect(new MapKey("cs", "dust2"), "ground"),
                provider.collect(new MapKey("cs", "dust2"), "upper")
                        .stream().map(r -> new RuntimeRegionDescriptor(
                                r.id(), "ground", r.label(), r.semanticType(), r.tags(),
                                r.gameplayReference(), r.worldBounds(), r.priority()
                        )).toList()
        );
    }

    @Test
    void sameInputsAlwaysEmitSameOrderAndIds() {
        CSGameMinimapRegionProvider provider = fullProvider();
        assertEquals(
                provider.collect(new MapKey("cs", "dust2"), "ground"),
                provider.collect(new MapKey("cs", "dust2"), "ground")
        );
    }

    private static CSGameMinimapRegionProvider fullProvider() {
        return new CSGameMinimapRegionProvider(
                () -> Optional.of(new WorldAxisAlignedBounds(-10, 0, -10, 10, 5, 10)),
                () -> BombSiteIdAssigner.assignAnonymous(List.of(
                        new WorldAxisAlignedBounds(0, 0, 0, 1, 1, 1),
                        new WorldAxisAlignedBounds(2, 0, 2, 3, 1, 3)
                )),
                () -> List.of(new CSGameMinimapRegionProvider.TeamRegionSource(
                        "ct", new WorldAxisAlignedBounds(-8, 0, -8, -6, 2, -6))),
                () -> List.of(new CSGameMinimapRegionProvider.TeamRegionSource(
                        "t", new WorldAxisAlignedBounds(6, 0, 6, 8, 2, 8)))
        );
    }
}