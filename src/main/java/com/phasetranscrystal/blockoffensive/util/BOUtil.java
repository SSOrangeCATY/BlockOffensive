package com.phasetranscrystal.blockoffensive.util;

import com.phasetranscrystal.blockoffensive.data.DeathMessage;
import com.phasetranscrystal.blockoffensive.entity.CompositionC4Entity;
import com.phasetranscrystal.blockoffensive.item.BOItemRegister;
import com.phasetranscrystal.blockoffensive.item.CompositionC4;
import com.phasetranscrystal.blockoffensive.map.team.capability.ColoredPlayerCapability;
import com.phasetranscrystal.blockoffensive.net.DeathMessageS2CPacket;
import com.phasetranscrystal.fpsmatch.common.client.FPSMClient;
import com.phasetranscrystal.fpsmatch.compat.CounterStrikeGrenadesCompat;
import com.phasetranscrystal.fpsmatch.compat.impl.FPSMImpl;
import com.phasetranscrystal.fpsmatch.core.data.PlayerData;
import com.phasetranscrystal.fpsmatch.core.map.BaseMap;
import com.phasetranscrystal.fpsmatch.core.team.MapTeams;
import com.phasetranscrystal.fpsmatch.util.RenderUtil;
import me.xjqsh.lrtactical.entity.ThrowableItemEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.phasetranscrystal.fpsmatch.util.RenderUtil.color;

public class BOUtil {
    public static int CT_COLOR = color(182, 210, 240);
    public static int T_COLOR = color(253,217,141);

    private static final Map<Item, ThrowableType> throwables = new ConcurrentHashMap<>();

    public static void registerThrowable(ThrowableType type, Item item) {
        throwables.put(item, type);
    }

    public static ThrowableType getThrowableType(Item item) {
        if (item != null && throwables.containsKey(item)) {
            return throwables.get(item);
        } else {
            return ThrowableType.UNKNOWN;
        }
    }

    public static MutableComponent buildTeamChatMessage(MutableComponent message) {
        return buildTeamChatMessage(message, Component.empty());
    }

    public static int getTeamColor(UUID uuid){
        return FPSMClient.getGlobalData().getTeamByUUID(uuid)
                .map(team -> team.name.equals("ct") ? CT_COLOR :  T_COLOR).orElse(RenderUtil.WHITE);
    }

    public static int getColor(UUID uuid){
       return FPSMClient.getGlobalData().getTeamByUUID(uuid)
               .map(team-> team.getCapabilityMap().get(ColoredPlayerCapability.class)
                       .map(cap-> {
                           TeamPlayerColor color = cap.getColor(uuid);
                           return color == null ? RenderUtil.WHITE :  color.getRGBA();
                       }).orElse(RenderUtil.WHITE))
               .orElse(RenderUtil.WHITE);
    }

    public static MutableComponent buildTeamChatMessage(MutableComponent message, MutableComponent location) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return message;

        return FPSMClient.getGlobalData().getCurrentClientTeam().map(team -> {
            TextColor textColor = TextColor.parseColor(team.name.equals("ct") ? "#96C8FA" : "#EAC055");

            MutableComponent head = Component.literal("[" + team.name.toUpperCase(Locale.US) + "]")
                    .withStyle(Style.EMPTY.withColor(textColor));

            MutableComponent teamColor = Component.literal(" ● ");

            team.getCapabilityMap().get(ColoredPlayerCapability.class).ifPresent(cap -> {
                TeamPlayerColor c = cap.getColor(player.getUUID());
                if (c != null) {
                    teamColor.withStyle(Style.EMPTY.withColor(TextColor.parseColor(c.getHex())));
                }
            });

            MutableComponent playerName = ((MutableComponent) player.getName()).withStyle(Style.EMPTY.withColor(textColor));

            return head.append(teamColor)
                    .append(playerName)
                    .append(location.withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(":"))
                    .append(message)
                    .withStyle(ChatFormatting.BOLD);

        }).orElse(message);
    }

    /**
     * 从击杀者和伤害源中解析导致死亡的物品栈
     *
     * @param attacker 击杀者
     * @param source   伤害源
     * @return 致死物品栈（非空，默认返回主手物品）
     */
    public static ItemStack getDeathItemStack(ServerPlayer attacker, DamageSource source) {
        if (source.getDirectEntity() instanceof ThrowableItemEntity projectile) {
            return projectile.getItem();
        }

        if (FPSMImpl.findCounterStrikeGrenadesMod()) {
            ItemStack grenade = CounterStrikeGrenadesCompat.getItemFromDamageSource(source);
            if (!grenade.isEmpty()) {
                return grenade;
            }
        }

        if(source.getEntity() instanceof CompositionC4Entity) {
            return BOItemRegister.C4.get().getDefaultInstance();
        }

        return attacker.getMainHandItem().isEmpty() ? ItemStack.EMPTY : attacker.getMainHandItem();
    }

    /**
     * 计算助攻玩家（符合伤害阈值的首个助攻者）
     *
     * @param deadPlayer 死亡玩家
     * @return 助攻玩家数据（可能为空）
     */
    public static Optional<PlayerData> calculateAssistPlayer(BaseMap map, ServerPlayer deadPlayer, float minAssistDamageRatio) {
        MapTeams mapTeams = map.getMapTeams();

        if (mapTeams.getTeamByPlayer(deadPlayer).isEmpty()) return Optional.empty();

        Map<UUID, Float> hurtDataMap = mapTeams.getDamageMap().getOrDefault(deadPlayer.getUUID(), null);
        if (hurtDataMap == null || hurtDataMap.isEmpty()) {
            return Optional.empty();
        }

        float minAssistDamage = deadPlayer.getMaxHealth() * minAssistDamageRatio;
        return hurtDataMap.entrySet().stream()
                .filter(entry -> entry.getValue() > minAssistDamage)
                .sorted(Map.Entry.<UUID, Float>comparingByValue().reversed())
                .limit(1)
                .findAny()
                .flatMap(entry -> mapTeams.getTeamByPlayer(entry.getKey())
                        .flatMap(team -> team.getPlayerData(entry.getKey())));
    }


    /**
     * 构建死亡消息（包含击杀、助攻、爆头信息）
     *
     * @param map        地图
     * @param attacker   击杀者
     * @param deadPlayer 死亡玩家
     * @param deathItem  致死物品
     * @param isHeadShot 是否爆头
     * @return 死亡消息数据包
     */
    public static DeathMessageS2CPacket buildDeathMessagePacket(BaseMap map, ServerPlayer attacker, ServerPlayer deadPlayer,
                                                                ItemStack deathItem, boolean isHeadShot,boolean isPassWall, boolean isPassSmoke, float minAssistDamageRatio) {

        DeathMessage.Builder builder = new DeathMessage.Builder(attacker, deadPlayer, deathItem);

        // 设置爆头标记
        builder.setHeadShot(isHeadShot);
        builder.setFlying(!attacker.equals(deadPlayer) && !attacker.onGround());
        builder.setThroughWall(isPassWall);
        builder.setThroughSmoke(isPassSmoke);

        // 设置助攻信息
        calculateAssistPlayer(map, deadPlayer, minAssistDamageRatio).ifPresent(assistData -> {
            if (!attacker.getUUID().equals(assistData.getOwner())) {
                builder.setAssist(assistData.name(), assistData.getOwner());
            }
        });

        return new DeathMessageS2CPacket(builder.build());
    }
}
