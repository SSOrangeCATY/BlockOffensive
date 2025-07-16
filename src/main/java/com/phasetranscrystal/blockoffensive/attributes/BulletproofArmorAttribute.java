package com.phasetranscrystal.blockoffensive.attributes;

import com.phasetranscrystal.blockoffensive.BlockOffensive;
import com.phasetranscrystal.blockoffensive.net.attribute.BulletproofArmorAttributeS2CPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class BulletproofArmorAttribute {
    public static final BulletproofArmorAttribute EMPTY = new BulletproofArmorAttribute(false,0);
    private static final Map<Player, BulletproofArmorAttribute> PLAYER_ATTRIBUTES = new HashMap<>();
    private boolean hasHelmet;
    private int durability;

    public BulletproofArmorAttribute(boolean hasHelmet) {
        this(hasHelmet, 100);
    }

    public BulletproofArmorAttribute(boolean hasHelmet, int durability) {
        this.hasHelmet = hasHelmet;
        this.durability = durability;
    }

    public static Optional<BulletproofArmorAttribute> getInstance(Player player) {
        return Optional.ofNullable(PLAYER_ATTRIBUTES.getOrDefault(player,null));
    }

    public boolean hasHelmet() {
        return hasHelmet;
    }

    public void setHasHelmet(boolean hasHelmet) {
        this.hasHelmet = hasHelmet;
    }

    public int getDurability() {
        return durability;
    }

    public void setDurability(int durability) {
        this.durability = Math.max(0, durability);
    }

    public void reduceDurability(int amount) {
        setDurability(this.durability - amount);
    }

    public static void removePlayer(ServerPlayer player) {
        PLAYER_ATTRIBUTES.remove(player);
        BlockOffensive.INSTANCE.send(PacketDistributor.PLAYER.with(()->player),new BulletproofArmorAttributeS2CPacket(EMPTY));
    }

    public static void addPlayer(ServerPlayer player, BulletproofArmorAttribute attribute) {
        PLAYER_ATTRIBUTES.put(player, attribute);
        BlockOffensive.INSTANCE.send(PacketDistributor.PLAYER.with(()->player),new BulletproofArmorAttributeS2CPacket(attribute));
    }
}