package com.phasetranscrystal.blockoffensive.client.screen.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.phasetranscrystal.blockoffensive.BOConfig;
import com.phasetranscrystal.blockoffensive.client.data.CSClientData;
import com.phasetranscrystal.blockoffensive.client.screen.hud.animation.EnderKillAnimator;
import com.phasetranscrystal.blockoffensive.client.screen.hud.animation.KillAnimator;
import com.phasetranscrystal.blockoffensive.compat.BOImpl;
import com.phasetranscrystal.blockoffensive.compat.HitIndicationCompat;
import com.phasetranscrystal.blockoffensive.data.DeathMessage;
import com.phasetranscrystal.fpsmatch.common.attributes.ammo.BulletproofArmorAttribute;
import com.phasetranscrystal.fpsmatch.common.client.FPSMClient;
import com.phasetranscrystal.fpsmatch.common.client.screen.hud.IHudRenderer;
import com.phasetranscrystal.fpsmatch.compat.gun.GunCompatManager;
import com.phasetranscrystal.fpsmatch.util.RenderUtil;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.client.resource.pojo.display.gun.AmmoCountStyle;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.AttachmentDataUtils;
import com.tacz.guns.api.TimelessAPI;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.fml.ModList;

import static com.phasetranscrystal.blockoffensive.client.screen.hud.CSGameTabRenderer.GUI_ICONS_LOCATION;

public class CSGameHud implements IHudRenderer {
    private static final CSGameHud INSTANCE = new CSGameHud();
    private final CSMvpHud mvpHud = new CSMvpHud();
    private final CSDeathMessageHud deathMessageHud = new CSDeathMessageHud();
    private final CSGameOverlay gameOverlay = new CSGameOverlay();
    private final CSDMOverlay dmOverlay = new CSDMOverlay();
    private final CSSpectatorHudOverlay spectatorHudOverlay = new CSSpectatorHudOverlay();
    private static final Identifier SEMI = Identifier.tryBuild("tacz", "textures/hud/fire_mode_semi.png");
    private static final Identifier AUTO = Identifier.tryBuild("tacz", "textures/hud/fire_mode_auto.png");
    private static final Identifier BURST = Identifier.tryBuild("tacz", "textures/hud/fire_mode_burst.png");
    private static final int MOVE_DURATION = 500; // 移动动画时长（毫秒）
    private static final int FADE_DURATION = 500; // 淡出动画时长（毫秒）
    private static final int SELECTED_BG_COLOR = RenderUtil.color(255,255,255,65); // 选中时的背景颜色（半透明白）
    private final Animation[] slotAnimations = new Animation[9]; // 扩展到7个槽位
    private KillAnimator killAnimator = new EnderKillAnimator();
    private boolean isStarted = false;

    public static CSGameHud getInstance(){
        return INSTANCE;
    }

    public CSGameHud(){
        for (int i = 0; i < 9; i++) {
            slotAnimations[i] = new Animation();
        }
    }

    public CSDeathMessageHud getDeathMessageHud() {
        return deathMessageHud;
    }

    public CSGameOverlay getGameOverlay() {
        return gameOverlay;
    }

    public CSMvpHud getMvpHud() {
        return mvpHud;
    }

    public void setKillAnimator(KillAnimator killAnimator) {
        if(killAnimator == null) return;
        this.killAnimator = killAnimator;
    }

    public void addKill(DeathMessage deathMessage) {
        if(BOImpl.isGD656KillIconLoaded()) return;

        if(killAnimator.isActive() || isStarted){
            killAnimator.addKill(deathMessage);
        }else{
            killAnimator.start(deathMessage);
            isStarted = true;
        }
    }

    public void stopKillAnim(){
        killAnimator.reset();
        isStarted = false;
    }

    public void reset(){
        mvpHud.resetAnimation();
        stopKillAnim();
        deathMessageHud.reset();
    }

    @Override
    public void onSpectatorRender(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, int screenWidth, int screenHeight) {
        if(!FPSMClient.getGlobalData().isCurrentGameType("csdm")){
            gameOverlay.render(guiGraphics, screenWidth, screenHeight);
        }else{
            dmOverlay.render(guiGraphics, screenWidth, screenHeight);
        }
        deathMessageHud.render(guiGraphics);
        spectatorHudOverlay.render(guiGraphics);
        mvpHud.render(guiGraphics, screenWidth, screenHeight);
    }

    @Override
    public void onPlayerRender(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if(BOImpl.isHitIndicationLoaded()){
            HitIndicationCompat.Renderer.render(mc.getWindow(), guiGraphics);
        }
        if(!FPSMClient.getGlobalData().isCurrentGameType("csdm")){
            gameOverlay.render(guiGraphics, screenWidth, screenHeight);
        }else{
            dmOverlay.render(guiGraphics, screenWidth, screenHeight);
        }
        deathMessageHud.render(guiGraphics);
        renderInfoLine(mc, guiGraphics, screenWidth, screenHeight);
        renderItemBar(mc, guiGraphics, screenWidth, screenHeight);
        mvpHud.render(guiGraphics, screenWidth, screenHeight);
    }

    public void renderInfoLine(Minecraft mc, GuiGraphicsExtractor guiGraphics, int screenWidth, int screenHeight) {
        int lineWidth = (int) (screenWidth * 0.26);
        int lineHeight = 1;
        int bottomMargin = (int) (screenHeight * 0.046);
        int fadeWidth = (int) (screenWidth * 0.026);

        int centerX = screenWidth / 2;
        int y = screenHeight - bottomMargin;

        for (int x = -lineWidth / 2; x <= lineWidth / 2; x++) {
            int alpha = 255;
            if (x < -lineWidth / 2 + fadeWidth) {
                alpha = (int) (255 * (x + (float) lineWidth / 2) / (float) fadeWidth);
            } else if (x > lineWidth / 2 - fadeWidth) {
                alpha = (int) (255 * ((float) lineWidth / 2 - x) / (float) fadeWidth);
            }
            int color = (alpha << 24) | 0xFFFFFF;
            guiGraphics.fill(centerX + x, y, centerX + x + 1, y + lineHeight, color);
        }

        renderHealthBar(mc, guiGraphics, centerX,lineWidth,y);
        if (mc.player != null) {
            Inventory inv = mc.player.getInventory();
            ItemStack selectItem = mc.player.getInventory().getSelectedItem();
            if(GunCompatManager.isGun(selectItem)){
                renderGunInfo(mc, guiGraphics, screenWidth, screenHeight, selectItem, centerX,lineWidth,y);
            }
        }

        renderCombatKillTips(mc, guiGraphics,centerX,y);
    }

    public void renderHealthBar(Minecraft mc, GuiGraphicsExtractor guiGraphics, int centerX, int lineWidth, int y) {
        LocalPlayer player = mc.player;
        if (player != null) {
            int health = (int) player.getHealth();
            int maxHealth = (int) player.getMaxHealth();
            float healthPercent = (float) health / maxHealth;
            Font font = mc.font;

            // Render health number
            int tempWidth = font.width("000") * 2;
            String healthText = String.valueOf((int) (healthPercent * 100));
            int healthTextX = centerX - lineWidth / 2 - 10 - tempWidth ;

            int healthTextY = y - font.lineHeight + 1;
            int healthBarY = y + font.lineHeight;
            int healthBarHeight = 3;
            int healthBarFillWidth = (int) (healthPercent * tempWidth);

            renderArmorBar(mc, guiGraphics, healthTextX, healthTextY);

            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(healthTextX + (float) tempWidth / 2 - (float) font.width(healthText), healthTextY);
            guiGraphics.pose().scale(2,2);
            guiGraphics.text(font, healthText, 0, 0, 0xFFFFFFFF, false);
            guiGraphics.pose().popMatrix();

            // Render health bar
            guiGraphics.fill(healthTextX, healthBarY, healthTextX + tempWidth, healthBarY + healthBarHeight, 0x80000000); // Background
            guiGraphics.fill(healthTextX, healthBarY, healthTextX + healthBarFillWidth, healthBarY + healthBarHeight, 0x8000FF00); // Fill
        }
    }

    public void renderArmorBar(Minecraft mc, GuiGraphicsExtractor guiGraphics, int healthTextX, int healthTextY) {
        if(BulletproofArmorAttribute.Client.bpAttributeDurability == 0) return;
        Font font = mc.font;
        String text = String.valueOf(BulletproofArmorAttribute.Client.bpAttributeDurability);
        int width = font.width(text);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI_ICONS_LOCATION, healthTextX - 9, healthTextY,
                BulletproofArmorAttribute.Client.bpAttributeHasHelmet ? 34.0F : 25.0F, 9.0F, 9, 9, 256, 256);
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(healthTextX - width + 1, healthTextY + 6);
        guiGraphics.text(font, text, 0, 0, 0xFFFFFFFF, false);
        guiGraphics.pose().popMatrix();
    }

    private void renderGunInfo(Minecraft mc, GuiGraphicsExtractor guiGraphics, int screenWidth, int screenHeight, ItemStack stack, int centerX, int lineWidth, int y) {
        if (!ModList.get().isLoaded("tacz")) return;
        com.tacz.guns.api.item.IGun iGun = (com.tacz.guns.api.item.IGun) stack.getItem();
        Identifier var27 = GunCompatManager.findProvider(stack).getGunId(stack);
        GunData gunData = TimelessAPI.getClientGunIndex(var27).map(ClientGunIndex::getGunData).orElse(null);
        GunDisplayInstance display = TimelessAPI.getGunDisplay(stack).orElse(null);
        if (gunData == null || display == null || mc.player == null) return;

        int cacheMaxAmmoCount = AttachmentDataUtils.getAmmoCountWithAttachment(stack, gunData);
        int ammoCount = iGun.getCurrentAmmoCount(stack) + (iGun.hasBulletInBarrel(stack) && gunData.getBolt() != Bolt.OPEN_BOLT ? 1 : 0);
        int cacheInventoryAmmoCount = 0;
        String currentAmmoCountText;
        if (display.getAmmoCountStyle() == AmmoCountStyle.PERCENT) {
            currentAmmoCountText = String.valueOf((float)ammoCount / (cacheMaxAmmoCount == 0 ? 1.0F : (float)cacheMaxAmmoCount));
        } else {
            currentAmmoCountText = String.valueOf(ammoCount);
        }

        Inventory inventory = mc.player.getInventory();
        FireMode fireMode = com.tacz.guns.api.item.IGun.getMainhandFireMode(mc.player);
        Identifier fireModeTexture = switch (fireMode) {
            case AUTO -> AUTO;
            case BURST -> BURST;
            default -> SEMI;
        };

        if (IGunOperator.fromLivingEntity(mc.player).needCheckAmmo()) {
            if (iGun.useDummyAmmo(stack)) {
                cacheInventoryAmmoCount = iGun.getDummyAmmoAmount(stack);
            } else {
                for(int i = 0; i < inventory.getContainerSize(); ++i) {
                    ItemStack inventoryItem = inventory.getItem(i);
                    Item var5 = inventoryItem.getItem();
                    if (var5 instanceof IAmmo iAmmo) {
                        if (iAmmo.isAmmoOfGun(stack, inventoryItem)) {
                            cacheInventoryAmmoCount += inventoryItem.getCount();
                        }
                    }

                    var5 = inventoryItem.getItem();
                    if (var5 instanceof IAmmoBox iAmmoBox) {
                        if (iAmmoBox.isAmmoBoxOfGun(stack, inventoryItem)) {
                            if (iAmmoBox.isAllTypeCreative(inventoryItem) || iAmmoBox.isCreative(inventoryItem)) {
                                cacheInventoryAmmoCount = 9999;
                                break;
                            }

                            cacheInventoryAmmoCount += iAmmoBox.getAmmoCount(inventoryItem);
                        }
                    }
                }
            }
        } else {
            cacheInventoryAmmoCount = 9999;
        }
        String inventoryAmmoCountText = String.valueOf(cacheInventoryAmmoCount);
        Font font = mc.font;
        int tempWidth = font.width(currentAmmoCountText) * 2;
        int invAmmoTextX = centerX + lineWidth / 2 + 10;
        int textY = y - font.lineHeight + 1;

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(invAmmoTextX, textY);
        guiGraphics.pose().scale(2,2);
        guiGraphics.text(font, currentAmmoCountText, 0, 0, ammoCount == 0 ? 0xFFFF0000 : 0xFFFFFFFF, false);
        guiGraphics.pose().popMatrix();

        int sY = y - (font.lineHeight / 2);
        int ttt = invAmmoTextX + tempWidth + 5;

        guiGraphics.fill(ttt, sY - 1, ttt + 1, sY + font.lineHeight + 1, 0xFFFFFFFF);

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate((float) (ttt + 3.5), y - (font.lineHeight * 1.5F / 2) + 0.5F);
        guiGraphics.pose().scale(1.5F,1.5F);
        guiGraphics.text(font, inventoryAmmoCountText, 0, 0, cacheInventoryAmmoCount == 0 ? 0xFFFF0000 : 0xFFFFFFFF, false);
        guiGraphics.pose().popMatrix();


        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate((float) (ttt + font.width(inventoryAmmoCountText) * 1.5 + 5.5), y - 4.5F);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, fireModeTexture, 0, 0, 0.0F, 0.0F, 10, 10, 10, 10);
        guiGraphics.pose().popMatrix();
    }



    public void renderItemBar(Minecraft mc, GuiGraphicsExtractor guiGraphics, int screenWidth, int screenHeight) {
        if (mc.player == null) return;

        // 布局参数
        final int MARGIN_RIGHT = 10;
        final int RECT_WIDTH = 40;
        final int RECT_HEIGHT = 20;
        final int SQUARE_SIZE = 20;
        final int SPACING = 5; // 竖排槽位之间的间距
        final int TEXT_COLOR = 0xFFFFFFFF;
        final int MAX_OFFSET = 15;
        final int MOVE_DURATION = 250;

        Player player = mc.player;
        Font font = mc.font;
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        Inventory inv = player.getInventory();
        int selectedSlot = inv.getSelectedSlot();

        // ========== 渲染前3个竖排物品栏 ==========
        int totalRectHeight = 3 * RECT_HEIGHT + 2 * SPACING;
        int anchorY = screenH - MARGIN_RIGHT - (totalRectHeight + SPACING + SQUARE_SIZE);

        for (int i = 0; i < 3; i++) {
            Animation anim = slotAnimations[i];
            boolean isSelected = (selectedSlot == i);
            int baseX = screenW - MARGIN_RIGHT - RECT_WIDTH;
            int baseY = anchorY + i * (RECT_HEIGHT + SPACING);

            // 状态切换处理
            handleAnimationState(anim, isSelected);

            // 动画计算
            int offsetX = 0;
            int bgColor = calculateBackgroundColor(anim, isSelected, true);

            // 前3个槽位的移动动画
            if (isSelected && bgColor == SELECTED_BG_COLOR) {
                long moveElapsed = System.currentTimeMillis() - anim.moveStartTime;
                float progress = Math.min(moveElapsed / (float)MOVE_DURATION, 1.0f);
                offsetX = (int)(-MAX_OFFSET * (1 - progress));
            }

            // 实际渲染
            renderSlot(guiGraphics, font, inv, i,
                    baseX + offsetX - 3, baseY - 3,
                    RECT_WIDTH + 3, RECT_HEIGHT + 3,
                    bgColor, TEXT_COLOR);
        }

        // ========== 渲染4-9号物品栏 ==========
        int squareAreaY = anchorY + totalRectHeight + SPACING;
        int totalSquareWidth = 6 * SQUARE_SIZE + 3 * 3;
        int squareAnchorX = screenW - MARGIN_RIGHT - totalSquareWidth;

        for (int i = 3; i < 9; i++) {
            Animation anim = slotAnimations[i];
            boolean isSelected = (selectedSlot == i);
            int indexInRow = i - 3;
            int baseX = squareAnchorX + indexInRow * (SQUARE_SIZE + 3);

            // 状态切换处理
            handleAnimationState(anim, isSelected);

            // 动画计算（无偏移）
            int bgColor = calculateBackgroundColor(anim, isSelected, false);
            renderSlot(guiGraphics, font, inv, i,
                    baseX, squareAreaY,
                    SQUARE_SIZE, SQUARE_SIZE,
                    bgColor, TEXT_COLOR);
        }
    }

    // 通用状态处理方法
    private void handleAnimationState(Animation anim, boolean isSelected) {
        if (isSelected != anim.wasSelected) {
            if (isSelected) {
                anim.moveStartTime = System.currentTimeMillis();
                anim.fadeStartTime = 0;
            } else {
                anim.fadeStartTime = System.currentTimeMillis();
            }
            anim.wasSelected = isSelected;
        }
    }

    // 通用背景颜色计算
    private int calculateBackgroundColor(Animation anim, boolean isSelected, boolean isVerticalSlot) {
        if (isSelected) {
            // 入场动画阶段（仅竖排需要检查动画时间）
            if (isVerticalSlot) {
                long moveElapsed = System.currentTimeMillis() - anim.moveStartTime;
                if (moveElapsed < MOVE_DURATION) {
                    return SELECTED_BG_COLOR;
                }
            }
            return SELECTED_BG_COLOR; // 保持选中状态
        } else if (anim.fadeStartTime > 0) {
            // 淡出动画阶段
            long fadeElapsed = System.currentTimeMillis() - anim.fadeStartTime;
            if (fadeElapsed < FADE_DURATION) {
                float progress = fadeElapsed / (float)FADE_DURATION;
                int alpha = (int)(128 * (1 - progress));
                return (alpha << 24) | 0x00FFFFFF;
            }
            anim.fadeStartTime = 0; // 结束淡出
        }
        return 0x00000000; // 默认透明
    }

    // 通用槽位渲染方法
    private void renderSlot(GuiGraphicsExtractor guiGraphics, Font font, Inventory inv, int slotIndex,
                            int x, int y, int width, int height,
                            int bgColor, int textColor) {
        // 绘制背景
        guiGraphics.fill(x, y, x + width, y + height, bgColor);

        // 物品渲染
        ItemStack stack = inv.getItem(slotIndex);
        int itemX = x + (width - 16) / 2;
        int itemY = y + (height - 16) / 2;
        guiGraphics.item(inv.player, stack, itemX, itemY, slotIndex);
        guiGraphics.itemDecorations(font, stack, itemX, itemY);

        // 槽位编号
        KeyMapping keyMapping = Minecraft.getInstance().options.keyHotbarSlots[slotIndex];
        Component key = keyMapping.getKey().getDisplayName();
        guiGraphics.text(font, key, x + width - font.width(key) - 1, y + 1, textColor, true);
        if(slotIndex + 1 <= 3){
            // 渲染名称
            if(!stack.isEmpty() && inv.getSelectedSlot() == slotIndex) {
                String itemName = stack.getHoverName().getString();
                float nameWidth = font.width(itemName) * 0.5F;
                float nameX = x + (width - nameWidth - 2);
                float nameY = y + height - 10 + font.lineHeight * 0.5f;
                guiGraphics.pose().pushMatrix();
                guiGraphics.pose().translate(nameX, nameY);
                guiGraphics.pose().scale(0.5F, 0.5F);
                guiGraphics.text(font, itemName, 0, 0, textColor, true);
                guiGraphics.pose().popMatrix();
            }
        }
    }

    public void renderCombatKillTips(Minecraft mc, GuiGraphicsExtractor guiGraphics,int centerX, int y) {
        if(!BOConfig.client.killIconHudEnabled.get()) return;
        killAnimator.render(mc, guiGraphics, centerX, y);
    }

    @Override
    public void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event) {
        if (event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH)
                || event.getName().equals(VanillaGuiLayers.ARMOR_LEVEL)
                || event.getName().equals(VanillaGuiLayers.FOOD_LEVEL)
                || event.getName().equals(VanillaGuiLayers.HOTBAR)
                || event.getName().getPath().equals("experience_bar")
                || event.getName().getPath().equals("mount_health")
                || event.getName().getPath().equals("tac_gun_hud_overlay")
        ){
            event.setCanceled(true);
        }
    }

    private static class Animation {
        long moveStartTime = 0;    // 入场动画开始时间
        long fadeStartTime = 0;    // 淡出动画开始时间
        boolean wasSelected = false; // 上次选中状态
    }
}
