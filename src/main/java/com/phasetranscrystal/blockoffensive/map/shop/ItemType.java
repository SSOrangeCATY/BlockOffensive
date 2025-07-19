package com.phasetranscrystal.blockoffensive.map.shop;

import com.phasetranscrystal.fpsmatch.core.shop.INamedType;
import com.phasetranscrystal.fpsmatch.core.shop.slot.ShopSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public enum ItemType implements INamedType {
    EQUIPMENT(0),PISTOL(1),MID_RANK(2),RIFLE(3),THROWABLE(4,false);
    public final int typeIndex;
    public final boolean dropUnlock;

    ItemType(int typeIndex) {
        this(typeIndex, true);
    }
    ItemType(int typeIndex,boolean dropUnlock) {
        this.typeIndex = typeIndex;
        this.dropUnlock = dropUnlock;
    }

    @Override
    public int slotCount() {
        return 5;
    }

    @Override
    public boolean dorpUnlock() {
        return dropUnlock;
    }

    /**
     * 获取默认的商店数据。
     * <p>
     * 默认数据包含每个物品类型的 5 个空槽位。
     *
     * @return 默认的商店数据
     */
    public static Map<ItemType, ArrayList<ShopSlot>> getRawData() {
        Map<ItemType, ArrayList<ShopSlot>> data = new HashMap<>();
        for (ItemType type : ItemType.values()) {
            ArrayList<ShopSlot> list = new ArrayList<>();
            for (int i = 0; i < type.slotCount(); i++) {
                list.add(new ShopSlot(ItemStack.EMPTY, 0));
            }
            data.put(type, list);
        }
        return data;
    }

}