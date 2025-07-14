package com.phasetranscrystal.blockoffensive.attributes;

import net.minecraft.world.entity.player.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class BulletproofArmorAttribute {
    private static final Map<Player, BulletproofArmorAttribute> PLAYER_ATTRIBUTES = new HashMap<>();
    private boolean hasHelmet;
    private int durability = 100;

    public BulletproofArmorAttribute(boolean hasHelmet) {
        this.hasHelmet = hasHelmet;
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

    public static void removePlayer(Player player) {
        PLAYER_ATTRIBUTES.remove(player);
    }

    public static void addPlayer(Player player, BulletproofArmorAttribute attribute) {
        PLAYER_ATTRIBUTES.put(player, attribute);
    }
}