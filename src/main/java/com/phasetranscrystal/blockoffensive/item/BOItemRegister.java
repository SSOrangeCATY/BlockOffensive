package com.phasetranscrystal.blockoffensive.item;

import com.phasetranscrystal.blockoffensive.BlockOffensive;
import com.phasetranscrystal.blockoffensive.item.test.TestItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

public class BOItemRegister {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BlockOffensive.MODID);
    public static DeferredHolder<CreativeModeTab, CreativeModeTab> BO_TAB;

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BlockOffensive.MODID);
    public static final DeferredItem<CompositionC4> C4 = ITEMS.registerItem("c4", CompositionC4::new);
    public static final DeferredItem<TestItem> OPEN_TEST_SHOP = ITEMS.registerItem("open_test_shop", TestItem::new);
    public static final DeferredItem<BombDisposalKit> BOMB_DISPOSAL_KIT = ITEMS.registerItem("bomb_disposal_kit", BombDisposalKit::new);

    static {
        BO_TAB = TABS.register("other", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.tab.blockoffensive"))
                .icon(() -> C4.get().getDefaultInstance()).displayItems((parameters, output) -> {
            ITEMS.getEntries().forEach((entry) -> {
                output.accept(entry.get());
            });
        }).build());
    }
}
