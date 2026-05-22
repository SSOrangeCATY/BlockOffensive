package com.xuebi1145.xuplus_client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.phasetranscrystal.blockoffensive.client.data.CSClientData;
import com.phasetranscrystal.blockoffensive.client.data.WeaponData;
import com.phasetranscrystal.blockoffensive.client.screen.hud.CSGameOverlay;
import com.phasetranscrystal.blockoffensive.util.BOUtil;
import com.phasetranscrystal.fpsmatch.common.client.FPSMClient;
import com.phasetranscrystal.fpsmatch.core.data.PlayerData;
import com.phasetranscrystal.fpsmatch.util.RenderUtil;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.xuebi1145.xuplus_client.PlayerHeadTextureManager;
import com.xuebi1145.xuplus_client.hud.ClientBombItemCache;
import com.xuebi1145.xuplus_client.hud.ClientGrenadeCache;
import com.xuebi1145.xuplus_client.hud.ClientRoundKillCache;
import com.xuebi1145.xuplus_client.hud.ClientWeaponItemCache;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import java.util.*;

/**
 * 增强CSGameOverlay的玩家头像行渲染：
 * 1. 始终显示玩家名称（不仅等待阶段）
 * 2. 始终显示血条
 * 3. 队友：血条下方显示从黑到透明渐变条，内含金钱
 * 4. 队友：显示武器信息（手枪上半/步枪下半）
 * 5. 敌方：仅显示名称
 */
@Mixin(value = CSGameOverlay.class, remap = false)
public class MixinCSGameOverlayEnhanced {

    @Shadow private Map<UUID, String> cachedName;

    // ===== 低血量阈值 =====
    @Unique
    private static final float xuplus$LOW_HEALTH_THRESHOLD = 0.25f;

    /**
     * 重写renderAvatarRow，始终显示名称+血条，队友额外显示金钱和武器
     */
    @Overwrite(remap = false)
    private void renderAvatarRow(GuiGraphics guiGraphics,
                                  List<PlayerInfo> players,
                                  int boxStartX,
                                  int rowY,
                                  int boxWidth,
                                  int avatarSize,
                                  int gap,
                                  boolean leftSide,
                                  boolean showNameInfo,
                                  String rowTeam,
                                  float scaleFactor)
    {
        boolean isSameTeam = FPSMClient.getGlobalData().isCurrentTeam(rowTeam);
        boolean isCT = rowTeam.equals("ct");
        // 始终显示名称，所以始终偏移rowY
        rowY += 6;
        Font font = Minecraft.getInstance().font;

        for (int i = 0; i < players.size(); i++) {
            PlayerInfo player = players.get(i);
            UUID uuid = player.getProfile().getId();
            int drawX = leftSide
                    ? (boxStartX + boxWidth - avatarSize - 2 - i * (avatarSize + gap))
                    : (boxStartX + 2 + i * (avatarSize + gap));

            Optional<PlayerData> data = RenderUtil.getPlayerData(player);
            boolean checked = data.isPresent();
            boolean isLiving = checked && data.get().isLiving();

            // 血量：优先使用客户端本地实体血量（实时），备用服务端同步数据
            float barRatio;
            Minecraft mc = Minecraft.getInstance();
            Player localPlayer = mc.level != null ? mc.level.getPlayerByUUID(uuid) : null;
            if (localPlayer != null) {
                float maxHp = localPlayer.getMaxHealth();
                barRatio = maxHp > 0 ? localPlayer.getHealth() / maxHp : 0f;
            } else {
                barRatio = checked ? data.get().getHealthPercent() : 0f;
            }

            // 背景色
            int bgColor = BOUtil.getColor(uuid);
            guiGraphics.fill(drawX, rowY, drawX + avatarSize, rowY + avatarSize, bgColor);

            // 灰度头像(dead)
            float r = 1f, g = 1f, b = 1f, a = 1f;
            if (checked && !isLiving) {
                r = g = b = 0.3f;
            }
            RenderSystem.setShaderColor(r, g, b, a);

            int margin = 1;
            int avX = drawX + margin;
            int avY = rowY + margin;
            int smallAvSize = avatarSize - margin * 2;

            // 自定义头像渲染
            xuplus$drawPlayerFace(guiGraphics, player, avX, avY, smallAvSize);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

            // ===== C4图标（头像右下角，仅拿C4的队友可见）=====
            if (isSameTeam) {
                xuplus$drawC4Badge(guiGraphics, uuid, drawX, rowY, avatarSize, scaleFactor);
            }

            // ===== 始终显示名称（头像上方）=====
            xuplus$drawPlayerName(guiGraphics, font, uuid, avX, avY, smallAvSize, avatarSize, isCT, scaleFactor);

            // ===== 血条（仅己方队伍显示）=====
            int startY = rowY + avatarSize + margin;
            int barHeight = 3;
            if (isSameTeam) {
                float currentRatio = isLiving ? barRatio : 0f;
                xuplus$drawSmoothHealthBar(guiGraphics, currentRatio, uuid, drawX, startY, drawX + avatarSize, startY + barHeight);
            }
            startY += barHeight + margin;

            // ===== 击杀数（仅己方队伍显示）=====
            if (isSameTeam) {
                int killCount = ClientRoundKillCache.getRoundKills(uuid);
                if (killCount > 0) {
                    int killIconSize = (int) (5 * scaleFactor);
                    xuplus$drawPlayerKills(guiGraphics, font, killCount, avX + (smallAvSize / 2), startY, scaleFactor);
                    startY += killIconSize + margin;
                }
            }

            // ===== 队友额外信息：渐变box（金钱+武器+护甲） =====
            if (isSameTeam) {
                startY = xuplus$drawInfoBox(guiGraphics, font, uuid, drawX, startY, avatarSize, scaleFactor, isCT);
            }
        }
    }

    // ========== 名称渲染 ==========
    @Unique
    private void xuplus$drawPlayerName(GuiGraphics guiGraphics, Font font, UUID uuid,
                                        int avX, int avY, int smallAvSize, int width,
                                        boolean isCT, float scale) {
        String nameStr = xuplus$getNameFromUUID(uuid);
        float textScale = 0.8f * scale;
        int xCenter = avX + (smallAvSize / 2);
        int nameY = avY - 8;
        guiGraphics.fill(avX - 1, nameY, avX + width - 1, nameY + 6, -1072689136);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(xCenter, nameY - 1, 0);
        guiGraphics.pose().scale(textScale, textScale, 1f);

        int nameWidth = font.width(nameStr);
        String displayName = nameStr;
        if (nameWidth >= width) {
            StringBuilder modified = new StringBuilder();
            for (char c : nameStr.toCharArray()) {
                int currentWidth = font.width(modified.toString());
                if (currentWidth + font.width(String.valueOf(c)) + 2 < width) {
                    modified.append(c);
                } else {
                    modified.append("..");
                    break;
                }
            }
            displayName = modified.toString();
            nameWidth = font.width(displayName);
        }
        guiGraphics.drawString(font, displayName, -nameWidth / 2, 0,
                isCT ? BOUtil.CT_COLOR : BOUtil.T_COLOR, false);
        guiGraphics.pose().popPose();
    }

    // ========== 信息box（金钱+武器+护甲，从上黑到下透明渐变） ==========
    @Unique
    private int xuplus$drawInfoBox(GuiGraphics guiGraphics, Font font, UUID uuid,
                                    int drawX, int startY, int avatarSize,
                                    float scaleFactor, boolean isCT) {
        // 游戏开始后（非等待阶段）隐藏金钱和武器
        boolean isRoundActive = CSClientData.isStart && !CSClientData.isWaiting && !CSClientData.isWarmTime;

        WeaponData weaponData = CSClientData.getWeaponData(uuid);
        Map<String, List<String>> wd = weaponData.weaponData();
        Map<String, List<ResourceLocation>> ids = ClientWeaponItemCache.get(uuid);
        List<String> pistols = wd.getOrDefault("SECONDARY_WEAPON", Collections.emptyList());
        List<String> mainWeapons = wd.getOrDefault("MAIN_WEAPON", Collections.emptyList());
        List<ResourceLocation> pistolIds = ids.getOrDefault("SECONDARY_WEAPON", Collections.emptyList());
        List<ResourceLocation> mainWeaponIds = ids.getOrDefault("MAIN_WEAPON", Collections.emptyList());

        // 护甲检测（其他玩家通过装备栏检测）
        Minecraft mc = Minecraft.getInstance();
        Player otherPlayer = mc.level != null ? mc.level.getPlayerByUUID(uuid) : null;
        boolean hasChestplate = false;
        boolean hasHelmet = false;
        if (otherPlayer != null) {
            hasChestplate = otherPlayer.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ArmorItem;
            hasHelmet = otherPlayer.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof ArmorItem;
        }
        boolean hasArmor = hasChestplate || hasHelmet;

        // 检测背包中是否有C4和剪线钳（通过服务端同步缓存，客户端无法访问其他玩家背包）
        boolean hasC4 = ClientBombItemCache.hasC4(uuid);
        boolean hasDefuser = ClientBombItemCache.hasDefuser(uuid);
        boolean hasBombItems = hasC4 || hasDefuser;

        // 投掷物数量（通过服务端同步缓存）
        int[] grenadeCounts = ClientGrenadeCache.get(uuid);
        int totalGrenades = 0;
        for (int c : grenadeCounts) totalGrenades += c;
        boolean hasGrenades = totalGrenades > 0;

        int moneyRowH = (int) (8 * scaleFactor);
        int iconSize = (int) (14 * scaleFactor);
        int gap = (int) (1 * scaleFactor);
        int armorRowH = (int) (12 * scaleFactor);
        int grenadeRowH = (int) (8 * scaleFactor);

        // 计算总高度
        boolean hasPistol = !pistols.isEmpty();
        boolean hasMain = !mainWeapons.isEmpty();
        int totalHeight = 0;

        if (!isRoundActive) {
            // 等待阶段：显示金钱+武器
            totalHeight += moneyRowH;
            if (hasPistol) totalHeight += iconSize + gap;
            if (hasMain) totalHeight += iconSize + gap;
        }

        // 投掷物行：仅等待阶段显示
        if (!isRoundActive && hasGrenades) {
            if (totalHeight > 0) totalHeight += gap;
            totalHeight += grenadeRowH;
        }

        // 护甲标识+C4/剪线钳：仅等待阶段显示
        if (!isRoundActive && (hasArmor || hasBombItems)) {
            if (totalHeight > 0) totalHeight += gap;
            totalHeight += armorRowH;
        }

        if (totalHeight <= 0) return startY;

        // 绘制从上（黑）到下（完全透明）纵向渐变背景
        for (int row = 0; row < totalHeight; row++) {
            float progress = (float) row / totalHeight;
            int alpha = (int) (200 * (1.0f - progress));
            guiGraphics.fill(drawX, startY + row, drawX + avatarSize, startY + row + 1,
                    (alpha << 24) | 0x000000);
        }

        int currentY = startY;

        if (!isRoundActive) {
            // 金钱文字（居中渲染）
            int moneyValue = FPSMClient.getGlobalData().getPlayerMoney(uuid);
            Component moneyStr = Component.literal("$" + moneyValue).withStyle(ChatFormatting.BOLD);
            float textScale = 0.7f * scaleFactor;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(drawX + (float) avatarSize / 2, currentY + (float) moneyRowH / 2 - 2 * scaleFactor, 0);
            guiGraphics.pose().scale(textScale, textScale, 1f);
            int w = font.width(moneyStr);
            guiGraphics.drawString(font, moneyStr, -w / 2, 0, isCT ? BOUtil.CT_COLOR : BOUtil.T_COLOR, false);
            guiGraphics.pose().popPose();
            currentY += moneyRowH + gap;

            // 手枪图标（居中，放大1.25倍）
            if (hasPistol) {
                xuplus$drawWeaponIconNoBg(guiGraphics, pistolIds.isEmpty() ? null : pistolIds.get(0), drawX, currentY, avatarSize, (int)(iconSize * 1.25f));
                currentY += iconSize + gap;
            }
            // 步枪图标（居中步枪）
            if (hasMain) {
                xuplus$drawWeaponIconNoBg(guiGraphics, mainWeaponIds.isEmpty() ? null : mainWeaponIds.get(0), drawX, currentY, avatarSize, iconSize);
                currentY += iconSize + gap;
            }
        }

        // 投掷物行（武器下方，盾牌上方，仅等待阶段）
        if (!isRoundActive && hasGrenades) {
            xuplus$drawGrenadeRow(guiGraphics, grenadeCounts, drawX, currentY, avatarSize, grenadeRowH, scaleFactor);
            currentY += grenadeRowH + gap;
        }

        // 护甲标识+C4/剪线钳（金钱下方，仅等待阶段）
        if (!isRoundActive && (hasArmor || hasBombItems)) {
            int durabilityPercent = 100;
            if (hasChestplate && otherPlayer != null) {
                ItemStack chestplate = otherPlayer.getItemBySlot(EquipmentSlot.CHEST);
                int maxDmg = chestplate.getMaxDamage();
                int dmg = chestplate.getDamageValue();
                durabilityPercent = maxDmg > 0 ? Math.round((1f - (float) dmg / maxDmg) * 100) : 100;
            }
            // 计算整体居中起始X
            int iconH = (int) (10 * scaleFactor);
            int iconGap = 1;
            float shieldScale = (float) iconH / SHIELD_TEX_H;
            int shieldRenderW = Math.round(SHIELD_TEX_W * shieldScale);
            float c4Scale = (float) iconH / C4_TEX_H;
            int c4RenderW = Math.round(C4_TEX_W * c4Scale);
            float defuserScale = (float) iconH / DEFUSER_TEX_H;
            int defuserRenderW = Math.round(DEFUSER_TEX_W * defuserScale);
            int totalW = 0;
            if (hasArmor) totalW += shieldRenderW;
            if (hasArmor && (hasC4 || hasDefuser)) totalW += iconGap;
            if (hasC4) totalW += c4RenderW;
            if (hasC4 && hasDefuser) totalW += iconGap;
            if (hasDefuser && !hasC4) {
                if (hasArmor) totalW += iconGap;
                totalW += defuserRenderW;
            } else if (hasDefuser) {
                totalW += iconGap;
                totalW += defuserRenderW;
            }
            int layoutStartX = drawX + (avatarSize - totalW) / 2;

            xuplus$drawArmorIndicator(guiGraphics, font, hasChestplate, hasHelmet, durabilityPercent, layoutStartX, currentY, avatarSize, scaleFactor);
            xuplus$drawBombItemIcons(guiGraphics, hasC4, hasDefuser, hasArmor, layoutStartX, currentY, avatarSize, scaleFactor);
            currentY += armorRowH;
        }

        return startY + totalHeight + 1;
    }

    // ========== 护甲标识渲染（其他玩家，套用血条旁的盾牌+头盔图标逻辑） ==========
    private static final ResourceLocation SHIELD_ICON = ResourceLocation.tryBuild("xuplus_client", "textures/cs2/icon/shield.png");
    private static final ResourceLocation HELMET_ICON = ResourceLocation.tryBuild("xuplus_client", "textures/cs2/icon/helmet.png");
    private static final ResourceLocation DEFUSER_ICON = ResourceLocation.tryBuild("xuplus_client", "textures/cs2/icon/Bomb_defuser.png");
    private static final ResourceLocation C4_ICON = ResourceLocation.tryBuild("xuplus_client", "textures/cs2/icon/CS2_Weapon_c4.png");
    private static final int SHIELD_TEX_W = 115;
    private static final int SHIELD_TEX_H = 125;
    private static final int HELMET_TEX_W = 150;
    private static final int HELMET_TEX_H = 100;
    private static final int DEFUSER_TEX_W = 32;
    private static final int DEFUSER_TEX_H = 32;
    private static final int C4_TEX_W = 256;
    private static final int C4_TEX_H = 256;

    // 投掷物图标（白色背景，CS2风格）
    private static final ResourceLocation GRENADE_HE_ICON = ResourceLocation.tryBuild("xuplus_client", "textures/cs2/grenades/hegrenade.png");
    private static final ResourceLocation GRENADE_FLASH_ICON = ResourceLocation.tryBuild("xuplus_client", "textures/cs2/grenades/flashbang.png");
    private static final ResourceLocation GRENADE_SMOKE_ICON = ResourceLocation.tryBuild("xuplus_client", "textures/cs2/grenades/smokegrenade.png");
    private static final ResourceLocation GRENADE_MOLOTOV_ICON = ResourceLocation.tryBuild("xuplus_client", "textures/cs2/grenades/molotov.png");
    private static final ResourceLocation GRENADE_INCENDIARY_ICON = ResourceLocation.tryBuild("xuplus_client", "textures/cs2/grenades/incendiary.png");
    private static final ResourceLocation GRENADE_DECOY_ICON = ResourceLocation.tryBuild("xuplus_client", "textures/cs2/grenades/decoy.png");
    // 投掷物纹理统一尺寸（正方形）
    private static final int GRENADE_TEX_SIZE = 32;

    @Unique
    private void xuplus$drawArmorIndicator(GuiGraphics guiGraphics, Font font,
                                            boolean hasChestplate, boolean hasHelmet, int durabilityPercent,
                                            int layoutStartX, int y, int avatarSize,
                                            float scaleFactor) {
        // 盾牌渲染高度：适配小头像格子
        int shieldRenderH = (int) (10 * scaleFactor);
        float shieldScale = (float) shieldRenderH / SHIELD_TEX_H;
        int shieldRenderW = Math.round(SHIELD_TEX_W * shieldScale);
        // 盾牌从layoutStartX开始（整体居中布局的左侧）
        int shieldX = layoutStartX;
        int shieldY = y + 1;

        // 画盾牌
        if (hasChestplate) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(shieldX, shieldY, 0);
            guiGraphics.pose().scale(shieldScale, shieldScale, 1);
            guiGraphics.blit(SHIELD_ICON, 0, 0, 0, 0, SHIELD_TEX_W, SHIELD_TEX_H, SHIELD_TEX_W, SHIELD_TEX_H);
            guiGraphics.pose().popPose();
            RenderSystem.disableBlend();

            // 耐久数字（盾牌内部居中）
            String text = String.valueOf(durabilityPercent);
            int width = font.width(text);
            float textScale = 0.5f * scaleFactor;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(
                    shieldX + shieldRenderW / 2f - (width * textScale) / 2f,
                    shieldY + shieldRenderH / 2f - (font.lineHeight * textScale) / 2f,
                    0);
            guiGraphics.pose().scale(textScale, textScale, 1);
            guiGraphics.drawString(font, text, 0, 0, 0xFFFFFFFF, false);
            guiGraphics.pose().popPose();
        }

        // 画头盔（居中于盾牌上方）
        if (hasHelmet) {
            int helmetRenderH = (int) (shieldRenderH / 4 * 1.5f);
            float helmetScale = (float) helmetRenderH / HELMET_TEX_H;
            int helmetRenderW = Math.round(HELMET_TEX_W * helmetScale);
            int helmetX = shieldX + (shieldRenderW - helmetRenderW) / 2;
            int helmetY = shieldY - helmetRenderH / 3;
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(helmetX, helmetY, 0);
            guiGraphics.pose().scale(helmetScale, helmetScale, 1);
            guiGraphics.blit(HELMET_ICON, 0, 0, 0, 0, HELMET_TEX_W, HELMET_TEX_H, HELMET_TEX_W, HELMET_TEX_H);
            guiGraphics.pose().popPose();
            RenderSystem.disableBlend();
        }
    }

    // ========== C4/剪线钳图标（盾牌右侧，使用整体居中布局的起始X） ==========
    @Unique
    private void xuplus$drawBombItemIcons(GuiGraphics guiGraphics,
                                            boolean hasC4, boolean hasDefuser, boolean hasArmor,
                                            int layoutStartX, int y, int avatarSize,
                                            float scaleFactor) {
        if (!hasC4 && !hasDefuser) return;

        int iconH = (int) (10 * scaleFactor);
        int gap = 1;

        // 计算各图标渲染宽度
        float shieldScale = (float) iconH / SHIELD_TEX_H;
        int shieldRenderW = Math.round(SHIELD_TEX_W * shieldScale);
        float c4Scale = (float) iconH / C4_TEX_H;
        int c4RenderW = Math.round(C4_TEX_W * c4Scale);
        float defuserScale = (float) iconH / DEFUSER_TEX_H;
        int defuserRenderW = Math.round(DEFUSER_TEX_W * defuserScale);

        // 从盾牌右侧开始
        int curX = layoutStartX;
        int iconY = y + 1;
        if (hasArmor) {
            curX += shieldRenderW + gap;
        }

        // C4
        if (hasC4) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(curX, iconY, 0);
            guiGraphics.pose().scale(c4Scale, c4Scale, 1);
            guiGraphics.blit(C4_ICON, 0, 0, 0, 0, C4_TEX_W, C4_TEX_H, C4_TEX_W, C4_TEX_H);
            guiGraphics.pose().popPose();
            RenderSystem.disableBlend();
            curX += c4RenderW + gap;
        }

        // 剪线钳
        if (hasDefuser) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(curX, iconY, 0);
            guiGraphics.pose().scale(defuserScale, defuserScale, 1);
            guiGraphics.blit(DEFUSER_ICON, 0, 0, 0, 0, DEFUSER_TEX_W, DEFUSER_TEX_H, DEFUSER_TEX_W, DEFUSER_TEX_H);
            guiGraphics.pose().popPose();
            RenderSystem.disableBlend();
        }
    }

    // ========== 投掷物行渲染（武器下方，盾牌上方，最多5个展示位） ==========
    @Unique
    private void xuplus$drawGrenadeRow(GuiGraphics guiGraphics, int[] grenadeCounts,
                                        int drawX, int y, int avatarSize, int rowH,
                                        float scaleFactor) {
        // 构建展示列表：每种投掷物一个图标，闪光弹可能有2个所以重复展示
        // 顺序：HE → Flash×count → Smoke → Molotov/Incendiary(互斥) → Decoy
        java.util.List<ResourceLocation> icons = new java.util.ArrayList<>();
        java.util.List<Integer> counts = new java.util.ArrayList<>();

        if (grenadeCounts[ClientGrenadeCache.HE] > 0) {
            icons.add(GRENADE_HE_ICON); counts.add(grenadeCounts[ClientGrenadeCache.HE]);
        }
        for (int i = 0; i < grenadeCounts[ClientGrenadeCache.FLASH]; i++) {
            icons.add(GRENADE_FLASH_ICON); counts.add(grenadeCounts[ClientGrenadeCache.FLASH]);
        }
        if (grenadeCounts[ClientGrenadeCache.SMOKE] > 0) {
            icons.add(GRENADE_SMOKE_ICON); counts.add(grenadeCounts[ClientGrenadeCache.SMOKE]);
        }
        // T方molotov，CT方incendiary（互斥，但以防万一都检测）
        if (grenadeCounts[ClientGrenadeCache.MOLOTOV] > 0) {
            icons.add(GRENADE_MOLOTOV_ICON); counts.add(grenadeCounts[ClientGrenadeCache.MOLOTOV]);
        }
        if (grenadeCounts[ClientGrenadeCache.INCENDIARY] > 0) {
            icons.add(GRENADE_INCENDIARY_ICON); counts.add(grenadeCounts[ClientGrenadeCache.INCENDIARY]);
        }
        if (grenadeCounts[ClientGrenadeCache.DECOY] > 0) {
            icons.add(GRENADE_DECOY_ICON); counts.add(grenadeCounts[ClientGrenadeCache.DECOY]);
        }

        if (icons.isEmpty()) return;

        // 图标尺寸：根据盒子宽度自动缩小，防止溢出
        int gap = 1;
        int totalSlots = icons.size();
        // 最大可用宽度 = avatarSize，需要容纳 totalSlots 个图标 + (totalSlots-1) 个间距
        int maxIconSize = (avatarSize - (totalSlots - 1) * gap) / totalSlots;
        int iconSize = Math.min(rowH, maxIconSize);
        if (iconSize <= 0) return;
        int totalW = totalSlots * iconSize + (totalSlots - 1) * gap;
        int startX = drawX + (avatarSize - totalW) / 2;
        int iconY = y;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (int i = 0; i < icons.size(); i++) {
            int iconX = startX + i * (iconSize + gap);
            float scale = (float) iconSize / GRENADE_TEX_SIZE;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(iconX, iconY, 0);
            guiGraphics.pose().scale(scale, scale, 1);
            guiGraphics.blit(icons.get(i), 0, 0, 0, 0, GRENADE_TEX_SIZE, GRENADE_TEX_SIZE, GRENADE_TEX_SIZE, GRENADE_TEX_SIZE);
            guiGraphics.pose().popPose();
        }
        RenderSystem.disableBlend();
    }

    // ========== C4徽章（头像右下角，仅拿C4的队友） ==========
    @Unique
    private void xuplus$drawC4Badge(GuiGraphics guiGraphics, UUID uuid,
                                     int drawX, int rowY, int avatarSize,
                                     float scaleFactor) {
        // 使用服务端同步缓存检测C4
        boolean hasC4 = ClientBombItemCache.hasC4(uuid);
        if (!hasC4) return;

        // C4徽章大小：头像的约1/3
        int badgeSize = (int) (avatarSize * 0.35f);
        float badgeScale = (float) badgeSize / C4_TEX_H;
        int badgeX = drawX + avatarSize - badgeSize - 1;
        int badgeY = rowY + avatarSize - badgeSize - 1;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(badgeX, badgeY, 0);
        guiGraphics.pose().scale(badgeScale, badgeScale, 1);
        guiGraphics.blit(C4_ICON, 0, 0, 0, 0, C4_TEX_W, C4_TEX_H, C4_TEX_W, C4_TEX_H);
        guiGraphics.pose().popPose();
        RenderSystem.disableBlend();
    }

    /**
     * 渲染武器图标（无单独背景，在统一渐变box内）
     * itemId可能是TACZ的gunId（如tacz:ak47）或普通物品的registry name
     */
    @Unique
    private void xuplus$drawWeaponIconNoBg(GuiGraphics guiGraphics, ResourceLocation itemId,
                                            int drawX, int y, int avatarSize, int iconSize) {
        if (itemId == null) return;

        // 先尝试作为TACZ gunId查找
        ClientGunIndex gunIndex = TimelessAPI.getClientGunIndex(itemId).orElse(null);
        if (gunIndex != null) {
            ResourceLocation missingTex = net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation();
            ResourceLocation hudTexture = gunIndex.getDefaultDisplay().getHUDTexture();
            ResourceLocation slotTexture = gunIndex.getDefaultDisplay().getSlotTexture();

            ResourceLocation texToUse = null;
            int texW = 39, texH = 13;
            if (hudTexture != null && !hudTexture.equals(missingTex)) {
                texToUse = hudTexture;
            } else if (slotTexture != null && !slotTexture.equals(missingTex)) {
                texToUse = slotTexture;
            }

            if (texToUse != null) {
                // 按比例缩放，iconSize控制最大尺寸
                float scale = Math.min((float) iconSize / (float) texW, (float) iconSize / (float) texH);
                int drawW = Math.max(1, Math.round(texW * scale));
                int drawH = Math.max(1, Math.round(texH * scale));

                // 始终基于avatarSize居中，避免iconSize>avatarSize时偏移
                int texDrawX = drawX + (avatarSize - drawW) / 2;
                int texDrawY = y + (iconSize - drawH) / 2;

                // 裁剪到头像格子区域，确保不会跑出渐变盒子
                guiGraphics.enableScissor(drawX, y, drawX + avatarSize, y + avatarSize);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                // 白底渲染：过曝shader color使彩色纹理变白
                RenderSystem.setShaderColor(10f, 10f, 10f, 1f);
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(texDrawX, texDrawY, 0);
                guiGraphics.pose().scale(scale, scale, 1f);
                guiGraphics.blit(texToUse, 0, 0, 0.0F, 0.0F, texW, texH, texW, texH);
                guiGraphics.pose().popPose();
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                RenderSystem.disableBlend();
                guiGraphics.disableScissor();
                return;
            }
        }

        // 非枪械或获取纹理失败，尝试作为物品registry name渲染
        Item item = ForgeRegistries.ITEMS.getValue(itemId);
        if (item != null) {
            int itemX = drawX + (avatarSize - 16) / 2;
            guiGraphics.renderItem(new ItemStack(item), itemX, y + (iconSize - 16) / 2);
        }
    }

    // ========== 血条渲染：白色=当前血量，红色=已掉血，低血量外发光 ==========
    @Unique
    private void xuplus$drawSmoothHealthBar(GuiGraphics gg, float ratio, UUID uuid,
                                             int startX, int startY, int endX, int endY) {
        int total = endX - startX;

        // 红色背景（已掉血部分）
        gg.fill(startX, startY, endX, endY, RenderUtil.color(200, 40, 40));
        // 白色填充（当前血量）
        int fillW = (int) (total * Math.max(0, Math.min(1, ratio)));
        gg.fill(startX, startY, startX + fillW, endY, RenderUtil.color(240, 240, 240));

        // ===== 低血量红色外发光 =====
        if (ratio > 0f && ratio < xuplus$LOW_HEALTH_THRESHOLD) {
            // 脉动效果：alpha随时间波动
            float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() * 0.006);
            int glowAlpha = (int) (0x50 * pulse);
            if (glowAlpha > 0) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                // 血条四周红色发光
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dy = -2; dy <= 2; dy++) {
                        if (dx == 0 && dy == 0) continue;
                        float dist = (float) Math.sqrt(dx * dx + dy * dy);
                        if (dist > 2.5f) continue;
                        float t = dist / 2.5f;
                        int a = (int) (glowAlpha * (1.0f - t) * (1.0f - t));
                        if (a <= 0) continue;
                        int glowColor = (a << 24) | 0x00FF3333;
                        gg.fill(startX + dx, startY + dy, endX + dx, endY + dy, glowColor);
                    }
                }
                RenderSystem.disableBlend();
            }
        }
    }

    // ========== 击杀数渲染（图标居中排列，上限5个） ==========
    private static final ResourceLocation KILL_ICON = ResourceLocation.tryBuild("xuplus_client", "textures/cs2/icon/kills.png");
    private static final int KILL_ICON_TEX_SIZE = 32;

    @Unique
    private void xuplus$drawPlayerKills(GuiGraphics guiGraphics, Font font, int count,
                                          int centerX, int startY, float scaleFactor) {
        int displayCount = Math.min(count, 5);
        if (displayCount <= 0) return;

        int iconSize = (int) (5 * scaleFactor);
        int gap = 1;
        int totalW = displayCount * iconSize + (displayCount - 1) * gap;
        int startX = centerX - totalW / 2;

        float scale = (float) iconSize / KILL_ICON_TEX_SIZE;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (int i = 0; i < displayCount; i++) {
            int iconX = startX + i * (iconSize + gap);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(iconX, startY, 0);
            guiGraphics.pose().scale(scale, scale, 1);
            guiGraphics.blit(KILL_ICON, 0, 0, 0, 0, KILL_ICON_TEX_SIZE, KILL_ICON_TEX_SIZE, KILL_ICON_TEX_SIZE, KILL_ICON_TEX_SIZE);
            guiGraphics.pose().popPose();
        }
        RenderSystem.disableBlend();
    }

    @Unique
    private String xuplus$getNameFromUUID(UUID uuid) {
        Player p = Minecraft.getInstance().level.getPlayerByUUID(uuid);
        if (p != null) {
            String name = p.getName().getString();
            cachedName.put(uuid, name);
            return name;
        }
        return cachedName.getOrDefault(uuid, uuid.toString().substring(0, 8));
    }

    // ========== 自定义头像渲染 ==========
    @Unique
    private void xuplus$drawPlayerFace(GuiGraphics guiGraphics, PlayerInfo player, int x, int y, int size) {
        ResourceLocation skinTexture = player.getSkinLocation();
        String playerName = xuplus$getNameFromSkinTexture(skinTexture);

        if (playerName != null) {
            if (!PlayerHeadTextureManager.isLoaded(playerName)) {
                PlayerHeadTextureManager.preloadPlayerHead(playerName);
            }
            ResourceLocation customHead = PlayerHeadTextureManager.getPlayerHeadTexture(playerName);
            if (customHead != null && !customHead.equals(skinTexture)) {
                guiGraphics.blit(customHead, x, y, size, size, 0, 0, size, size, size, size);
                return;
            }
        }
        PlayerFaceRenderer.draw(guiGraphics, skinTexture, x, y, size);
    }

    @Unique
    private String xuplus$getNameFromSkinTexture(ResourceLocation skinTexture) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            for (PlayerInfo playerInfo : mc.getConnection().getOnlinePlayers()) {
                if (playerInfo.getSkinLocation().equals(skinTexture)) {
                    return playerInfo.getProfile().getName();
                }
            }
        }
        return null;
    }

}
