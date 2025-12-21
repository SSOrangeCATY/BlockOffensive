package com.phasetranscrystal.blockoffensive.client.spec;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import com.phasetranscrystal.blockoffensive.BlockOffensive;
import com.phasetranscrystal.blockoffensive.net.spec.RequestAttachTeammateC2SPacket;
import com.phasetranscrystal.fpsmatch.common.client.spec.SpectateMode;
import com.phasetranscrystal.fpsmatch.util.FPSMFormatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.phasetranscrystal.fpsmatch.common.client.FPSMClient;


import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KillCamManager {
    private static final int  PULL_T = 40; // 拉镜头时长（tick）
    private static final int  FADE_T = 20; // 黑屏渐变到满黑的时长（tick）
    private static final double EXTRA_DIST = 3.75; // 从 A 向外拉的距离
    private static final float GRAY_SWITCH_FRAC = 0.15f;//彩色 -> 灰色的切换时刻

    /* ===== HUD 入场/时序 ===== */
    private static final int   HUD_IN_T      = 10;
    private static final float POP_MIN_SCALE = 0.92f;
    private static final float POP_MAX_SCALE = 1.00f;

    private static final int HUD_MIN_TICKS        = 60;
    private static final int ICON_MIN_SHOW_TICKS  = 20;
    private static final int ICON_MAX_WAIT_TICKS  = 40;
    private static final int CAM_HOLD_EXTRA_TICKS = 40;

    /* ===== HUD 布局/颜色 ===== */
    private static final int HEAD = 32, ICON = 16, PAD = 8, BG_H = 42;
    private static final int CENTER_OFFSET_Y = 0;

    private static final int PANEL_ALPHA = 0xE0;
    private static final int CT_BASE_RGB = 0x6FAEFF;
    private static final int T_BASE_RGB  = 0xF2D17A;
    private static final float VICTIM_BLEND = 0.35f;
    private static final float GRADIENT_GAMMA = 0.55f;
    private static final float KILLER_LIGHTEN = 0.28f;
    private static final float VICTIM_DARKEN  = 0.18f;

    /** 灰度后处理 */
    private static final ResourceLocation DESAT =
            ResourceLocation.tryBuild("minecraft", "shaders/post/desaturate.json");

    /* ===== 状态机 ===== */
    private enum Phase { NONE, PULL, FADE }
    private static Phase phase = Phase.NONE;
    private static int   tickIn = 0;

    /* ===== 相机路径参数（A、B、dir） ===== */
    private static double vx, vy, vz; // A（受害者死亡点）
    private static double kx, ky, kz; // B（击杀者位置）
    private static double dx, dy, dz; // dir = normalize(A - B)

    /* ===== 记录最后一次相机位置（渲染朝向用） ===== */
    private static double lastCamX = Double.NaN, lastCamY = Double.NaN, lastCamZ = Double.NaN;

    /* ===== HUD / 文案数据 ===== */
    private static boolean   hudOn   = false;
    private static int       hudTick = 0;
    private static int       hudAlive = 0;
    private static boolean   iconWanted = false;
    private static int       iconReadyTick = -1;
    private static int       camHoldTick = 0;

    private static String    killerName = "???";
    private static String    gunName    = "未知武器";
    private static ItemStack gunStack   = ItemStack.EMPTY;
    private static ResourceLocation killerSkin;

    /* 队伍侧 */
    private enum Side { CT, T, UNKNOWN }
    private static Side killerSide = Side.UNKNOWN;
    private static Side victimSide = Side.UNKNOWN;

    /* 时间（HUD 动效） */
    private static long  lastNs  = 0L;
    private static float timeSec = 0f;

    /* 客户端兜底附身 */
    private static int clientAttachTries = 0;
    private static int clientAttachCooldown = 0;

    /* 相机 ghost（仅客户端） */
    private static Entity ghostCam = null;

    /* 灰度/黑屏状态 */
    private static boolean grayEnabled = false;
    private static boolean holdBlack   = false;

    /* ===== HUD 自定义扩展接口 ===== */
    public record Style(int bg0,int bg1,int border){}
    @FunctionalInterface
    public interface IHudStyleProvider extends BiFunction<UUID,String,Style>{}
    public interface ICustomHudRenderer{
        boolean render(GuiGraphics gg,int sw,int sh,UUID killerId,String killerName,
                       String unusedGunId,String gunName,ItemStack gunStack,Style style);
    }
    private static volatile IHudStyleProvider STYLE_PROVIDER = null;
    private static volatile ICustomHudRenderer CUSTOM_RENDERER = null;
    public static void setStyleProvider(BiFunction<UUID,String,Style> f){ STYLE_PROVIDER = f::apply; }
    public static void setStyleProvider(IHudStyleProvider p){ STYLE_PROVIDER = p; }
    public static void setCustomRenderer(ICustomHudRenderer r){ CUSTOM_RENDERER = r; }

    /* ======================= 启动 ======================= */
    public static void startFromPacket(){
        if (phase != Phase.NONE) return;

        Vec3 kPos = KillCamClientCache.consumeKiller();
        Vec3 vPos = KillCamClientCache.consumeVictim();
        if (kPos==null || vPos==null) return;

        kx = kPos.x; ky = kPos.y; kz = kPos.z;  // B
        vx = vPos.x; vy = vPos.y; vz = vPos.z;  // A

        double rx = vx - kx, ry = vy - ky, rz = vz - kz;
        double len2 = rx*rx + ry*ry + rz*rz;
        if (len2 < 1e-6){
            var p = Minecraft.getInstance().player;
            if (p == null) return;
            Vec3 d = Vec3.directionFromRotation(p.getXRot(), p.getYRot());
            dx = d.x; dy = d.y; dz = d.z;
        }else{
            double inv = 1.0 / Math.sqrt(len2);
            dx = rx * inv; dy = ry * inv; dz = rz * inv;
        }

        // HUD 数据
        UUID kid = KillCamClientCache.getKillerUUID();
        killerName = KillCamClientCache.getKillerName();
        gunStack   = KillCamClientCache.getWeapon();
        gunName    = FPSMFormatUtil.i18n(gunStack);
        killerSkin = fetchSkin(kid, killerName);

        // 队伍侧
        killerSide = detectSide(kid);
        UUID myId = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getUUID() : null;
        victimSide = detectSide(myId);

        // HUD / 时间状态
        hudOn = true; hudTick = 0; hudAlive = 0; camHoldTick = 0;
        iconWanted = !gunStack.isEmpty();
        iconReadyTick = iconWanted ? 0 : -1;

        clientAttachTries = 8;
        clientAttachCooldown = 0;

        var mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.setCameraEntity(mc.player); // 先切回自身，确保干净状态
        }

        // 进入 PULL
        phase = Phase.PULL; tickIn = 0;
        grayEnabled = false;
        holdBlack   = false;

        // 初始化 ghost 到 A，并立刻切相机到 ghost
        ensureGhost();
        float yaw0   = (float)Math.toDegrees(Math.atan2(-(kx - vx), (kz - vz)));
        float pitch0 = (float)Math.toDegrees(Math.atan2(-(ky - vy), Math.sqrt((kx - vx)*(kx - vx) + (kz - vz)*(kz - vz))));
        ghostCam.absMoveTo(vx, vy, vz, yaw0, pitch0);
        ghostCam.setOldPosAndRot();
        lastCamX = vx; lastCamY = vy; lastCamZ = vz;

        mc.setCameraEntity(ghostCam);
    }

    /* ======================= 更新 ghost 位置 + 状态时序 ======================= */
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if (e.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        var pl = mc.player;
        if (pl == null) { reset(); return; }

        boolean isSpec = pl.isSpectator();

        if (hudOn) hudAlive++;

        switch (phase){
            case PULL -> {
                ensureGhost();
                double t = Mth.clamp(tickIn / (double) PULL_T, 0.0, 1.0);
                double s = kickThenEaseOut(t);
                double out = EXTRA_DIST * s;
                double nx = vx + dx * out;
                double ny = vy + dy * out;
                double nz = vz + dz * out;

                if (ghostCam != null) {
                    ghostCam.setOldPosAndRot();
                    ghostCam.setPos(nx, ny, nz);
                    if (mc.getCameraEntity() != ghostCam) {
                        mc.setCameraEntity(ghostCam);
                    }
                }
                lastCamX = nx; lastCamY = ny; lastCamZ = nz;

                if (!grayEnabled && t >= GRAY_SWITCH_FRAC) {
                    enableGray();
                    grayEnabled = true;
                }

                if (tickIn < PULL_T) { tickIn++; } else { camHoldTick++; }

                boolean uiLongEnough = hudAlive >= HUD_MIN_TICKS;
                boolean iconShownEnough =
                        !iconWanted
                                || (iconReadyTick >= 0 && (hudAlive - iconReadyTick) >= ICON_MIN_SHOW_TICKS)
                                || (hudAlive >= HUD_MIN_TICKS + ICON_MAX_WAIT_TICKS);
                boolean canEnterFade = uiLongEnough && iconShownEnough && (camHoldTick >= CAM_HOLD_EXTRA_TICKS);

                if (canEnterFade){
                    phase = Phase.FADE;
                    tickIn = 0;
                    holdBlack = false;
                }
            }
            case FADE -> {
                // 渐黑到满黑
                if (!holdBlack) {
                    if (++tickIn >= FADE_T){
                        holdBlack = true;
                        BlockOffensive.INSTANCE.sendToServer(new RequestAttachTeammateC2SPacket());
                        tryLocalAttachToNearestTeammate();
                    }
                }
                // 满黑等待期间不推进到 NONE
            }
        }

        if (isSpec) {
            var camEnt = mc.getCameraEntity();
            if (camEnt != null && camEnt != pl && camEnt != ghostCam) {
                holdBlack = false;
                disableGray();
                hudOn = false;
                phase = Phase.NONE;
                tickIn = 0;
            }
        }

        if (phase == Phase.NONE) {
            attemptAttachRetryIfNeeded();
        }

        // 一旦玩家非观战，归还相机并重置
        if (!isSpec) {
            forceRestoreCameraToPlayer();
            reset();
        }
    }

    /* ======================= 渲染帧：只设置朝向 ======================= */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCamAngles(ViewportEvent.ComputeCameraAngles e){
        if (phase != Phase.PULL) return;

        double nx = lastCamX, ny = lastCamY, nz = lastCamZ;
        if (Double.isNaN(nx)) { nx = vx; ny = vy; nz = vz; }

        double vx_ = kx - nx, vy_ = ky - ny, vz_ = kz - nz;
        float yaw   = (float)Math.toDegrees(Math.atan2(-vx_, vz_));
        float pitch = (float)Math.toDegrees(Math.atan2(-vy_, Math.sqrt(vx_*vx_+vz_*vz_)));
        e.setYaw(yaw);
        e.setPitch(pitch);
    }

    private static double kickThenEaseOut(double t){
        t = Mth.clamp(t, 0.0, 1.0);
        final double PRE    = 0.20;
        final double KICK   = 0.33;
        if (t <= PRE){
            double u = t / PRE;
            return KICK * (u*u);
        }else{
            double u = (t - PRE) / (1.0 - PRE);
            double easeOutCubic = 1.0 - Math.pow(1.0 - u, 3.0);
            return KICK + (1.0 - KICK) * easeOutCubic;
        }
    }

    /* ======================= HUD ======================= */
    @SubscribeEvent public static void overlay(RenderGuiOverlayEvent.Pre e){
        float blackAlpha = 0f;
        if (phase == Phase.FADE) {
            blackAlpha = holdBlack ? 1f : (tickIn / (float)FADE_T);
            blackAlpha = Mth.clamp(blackAlpha, 0f, 1f);
        }

        if (hudOn) {
            var mc = Minecraft.getInstance();
            GuiGraphics gg = e.getGuiGraphics();
            var win = mc.getWindow();
            int sw = win.getGuiScaledWidth(), sh = win.getGuiScaledHeight();

            long now = System.nanoTime();
            float dt = (lastNs==0L)?0f:(now-lastNs)/1_000_000_000f;
            lastNs = now;
            dt = Mth.clamp(dt, 0f, 0.1f);
            timeSec += dt;

            float in = Mth.clamp(hudTick/(float)HUD_IN_T, 0f, 1f);
            if (hudTick < HUD_IN_T) hudTick++;

            // 让 HUD 随黑幕一起黑掉
            float alpha = in * (1f - blackAlpha);

            if (alpha > 0.01f) {
                if (killerSide == Side.UNKNOWN) killerSide = detectSide(KillCamClientCache.getKillerUUID());
                if (victimSide == Side.UNKNOWN) {
                    UUID myId = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getUUID() : null;
                    victimSide = detectSide(myId);
                }

                int killerRGB  = lighten(sideBaseRGB(killerSide));
                int victimRGB0 = sideBaseRGB(victimSide);
                int victimRGB  = darken(victimRGB0);

                int startColor = (PANEL_ALPHA << 24) | (killerRGB & 0x00FFFFFF);
                int endMix     = mixRGB(killerRGB, victimRGB, VICTIM_BLEND);
                int endColor   = (PANEL_ALPHA << 24) | (endMix & 0x00FFFFFF);

                Font font = mc.font;
                String killedByKey = I18n.exists("blockoffensive.killed_by")? I18n.get("blockoffensive.killed_by") : "击杀了你";
                String text = "§c" + killerName + " §7使用 §e" + gunName + " §7" + killedByKey;

                int txtW = font.width(text);
                boolean reserveIcon = iconWanted;
                int iconPart = reserveIcon ? (ICON + PAD) : 0;
                int totalW = HEAD + PAD + txtW + PAD + iconPart;

                int cx = sw / 2;
                int cy = sh / 2 + CENTER_OFFSET_Y;

                float pop = easeOutBack(in);
                float scale = Mth.lerp(pop, POP_MIN_SCALE, POP_MAX_SCALE);

                float maxW = sw * 0.92f;
                if (totalW * scale > maxW) scale *= (maxW / (totalW * scale));

                float tx = snapToPixel(cx, scale);
                float ty = snapToPixel(cy, scale);

                int x0 = (int)Math.round(-totalW/2f);
                int y0 = -BG_H/2;
                int x1 = x0 + totalW;
                int y1 = y0 + BG_H;

                RenderSystem.enableBlend();
                gg.pose().pushPose();
                gg.pose().translate(tx, ty, 0);
                gg.pose().scale(scale, scale, 1f);

                // 矩形
                drawGradientPanelRect(gg, x0, y0, x1, y1,
                        mulAlpha(startColor, alpha), mulAlpha(endColor, alpha), GRADIENT_GAMMA);

                // 头像
                int headX = x0 + PAD, headY = y0 + (BG_H - HEAD)/2;
                gg.blit(killerSkin, headX, headY, HEAD, HEAD, 8,8,8,8,64,64);
                gg.blit(killerSkin, headX, headY, HEAD, HEAD, 40,8,8,8,64,64);

                // 文本
                int textLeft = headX + HEAD + PAD;
                int textRight = x1 - (reserveIcon ? (PAD + ICON + PAD) : PAD);
                int allowedW = Math.max(0, textRight - textLeft);

                float textScale = Math.min(1f, allowedW / Math.max(1f, txtW));
                int tDrawW = Math.round(txtW * textScale);
                int textX = textLeft + (allowedW - tDrawW) / 2;
                int textY = y0 + (BG_H - font.lineHeight) / 2;

                gg.pose().pushPose();
                gg.pose().translate(textX, textY, 0);
                gg.pose().scale(textScale, textScale, 1f);
                gg.drawString(font, text, 0, 0, mulAlpha(0xFFFFFFFF, alpha), false);
                gg.pose().popPose();

                int localIconX = x1 - PAD - ICON;
                int localIconY = y0 + (BG_H - ICON) / 2;
                gg.pose().popPose();

                if (!gunStack.isEmpty()){
                    int absIconX = Math.round(tx + localIconX * scale);
                    int absIconY = Math.round(ty + localIconY * scale);
                    int max = Math.max(1, Math.round(ICON * scale));
                    renderItemAt(gg, gunStack, absIconX, absIconY, max, max, font);

                    if (iconWanted && iconReadyTick < 0) iconReadyTick = hudAlive;
                }

                RenderSystem.disableBlend();
            }
        }

        if (blackAlpha > 0f) {
            GuiGraphics gg = e.getGuiGraphics();
            var win = Minecraft.getInstance().getWindow();
            int sw = win.getGuiScaledWidth(), sh = win.getGuiScaledHeight();
            int a = (int)Mth.clamp(Math.round(255f * blackAlpha), 0, 255);
            int argb = (a << 24); // 纯黑
            RenderSystem.enableBlend();
            gg.fill(0, 0, sw, sh, argb);
            RenderSystem.disableBlend();
        }
    }

    public static void onSpectateModeUpdate(SpectateMode mode){
        var mc = Minecraft.getInstance();
        var pl = mc.player;
        if (pl == null) return;
        if (!pl.isSpectator()) {
            forceRestoreCameraToPlayer();
            reset();
        }
    }

    private static Side detectSide(UUID id){
        if (id == null) return Side.UNKNOWN;
        try {
            var gd = FPSMClient.getGlobalData();
            if (gd != null) {
                var opt = gd.getPlayerTeam(id);
                if (opt != null && opt.isPresent()) {
                    String s = opt.get();
                    s = s.trim().toLowerCase(Locale.ROOT);
                    if (s.equals("ct") || s.contains("counter")) return Side.CT;
                    if (s.equals("t")  || s.contains("terror"))  return Side.T;
                }
            }
        } catch (Throwable ignored) {}
        var mc = Minecraft.getInstance();
        var lvl = mc.level;
        if (lvl != null) {
            Player p = lvl.getPlayerByUUID(id);
            if (p != null) {
                var team = p.getTeam();
                if (team != null) {
                    String n = team.getName();
                    n = n.toLowerCase(Locale.ROOT);
                    if (n.contains("ct") || n.contains("counter") || n.contains("blue")) return Side.CT;
                    if (n.equals("t") || n.contains("terror") || n.contains("red") || n.matches(".*\\bt\\b")) return Side.T;
                }
            }
        }
        return Side.UNKNOWN;
    }
    private static int sideBaseRGB(Side s){
        return switch (s){
            case CT -> CT_BASE_RGB;
            case T  -> T_BASE_RGB;
            default -> 0x808080;
        };
    }
    private static ResourceLocation fetchSkin(UUID id, String name){
        return Minecraft.getInstance().getSkinManager()
                .getInsecureSkinLocation(new GameProfile(id, name));
    }

    private static void drawGradientPanelRect(GuiGraphics gg, int x0, int y0, int x1, int y1,
                                              int startARGB, int endARGB, float gamma){
        int w = Math.max(1, x1 - x0);
        for (int x = x0; x < x1; x++){
            float t = (x - x0) / (float)(w - 1);
            float tg = (gamma <= 0f) ? t : (float)Math.pow(t, gamma);
            int col = lerpARGB(startARGB, endARGB, tg);
            gg.fill(x, y0, x + 1, y1, col);
        }
    }

    private static void renderItemAt(GuiGraphics g, ItemStack s, int x, int y, int w, int h, Font font){
        if (s == null || s.isEmpty()) return;
        var pose = g.pose();
        pose.pushPose();
        float sc = Math.max(0.001f, Math.min(w, h) / 16f);
        int dx = x + Math.round((w - 16 * sc) / 2f);
        int dy = y + Math.round((h - 16 * sc) / 2f);
        pose.translate(dx, dy, 0);
        pose.scale(sc, sc, 1f);
        g.renderItem(s, 0, 0);
        g.renderItemDecorations(font, s, 0, 0);
        pose.popPose();
    }

    private static int lighten(int rgb){ return mixRGB(rgb, 0xFFFFFF, KillCamManager.KILLER_LIGHTEN); }
    private static int darken (int rgb){ return mixRGB(rgb, 0x000000, KillCamManager.VICTIM_DARKEN); }
    private static float snapToPixel(float v, float scale){ return Math.round(v * scale) / scale; }
    private static int mulAlpha(int argb, float mul){
        mul = Mth.clamp(mul, 0f, 1f);
        int a = (int)(((argb>>>24)&0xFF) * mul);
        return (a<<24) | (argb & 0x00FFFFFF);
    }
    private static int lerpARGB(int a, int b, float t){
        t = Mth.clamp(t, 0f, 1f);
        int aA = (a >>> 24) & 0xFF, aR = (a >>> 16) & 0xFF, aG = (a >>> 8) & 0xFF, aB = a & 0xFF;
        int bA = (b >>> 24) & 0xFF, bR = (b >>> 16) & 0xFF, bG = (b >>> 8) & 0xFF, bB = b & 0xFF;
        int rA = aA + Math.round((bA - aA) * t);
        int rR = aR + Math.round((bR - aR) * t);
        int rG = aG + Math.round((bG - aG) * t);
        int rB = aB + Math.round((bB - aB) * t);
        return (rA << 24) | (rR << 16) | (rG << 8) | rB;
    }
    private static int mixRGB(int c1, int c2, float p){
        p = Mth.clamp(p, 0f, 1f);
        int r1=(c1>>>16)&0xFF, g1=(c1>>>8)&0xFF, b1=c1&0xFF;
        int r2=(c2>>>16)&0xFF, g2=(c2>>>8)&0xFF, b2=c2&0xFF;
        int r=(int)(r1+(r2-r1)*p), g=(int)(g1+(g2-g1)*p), b=(int)(b1+(b2-b1)*p);
        return (r<<16)|(g<<8)|b;
    }
    private static float easeOutBack(float t){
        t = Mth.clamp(t, 0f, 1f);
        float c1 = 1.70158f, c3 = c1 + 1f;
        float u = t - 1f;
        return 1f + c3*u*u*u + c1*u*u;
    }

    /* ===== 灰度开关 ===== */
    private static void enableGray(){
        try {
            if (Minecraft.getInstance().options.graphicsMode().get().getId()!=0){
                Minecraft.getInstance().gameRenderer.loadEffect(DESAT);
            }
        } catch (Throwable ignored){}
    }
    private static void disableGray(){
        try {
            Minecraft.getInstance().gameRenderer.shutdownEffect();
        } catch (Throwable ignored){}
    }

    /* ========== ghost 管理 ========== */
    private static void ensureGhost(){
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (ghostCam != null && ghostCam.level() == mc.level && !ghostCam.isRemoved()) return;

        // 优先 MARKER，不行退 AREA_EFFECT_CLOUD
        Entity g = EntityType.MARKER.create(mc.level);
        if (g == null) g = EntityType.AREA_EFFECT_CLOUD.create(mc.level);
        ghostCam = g;
    }

    /* ======================= 附身重试 ======================= */
    private static void attemptAttachRetryIfNeeded(){
        var mc = Minecraft.getInstance();
        var me = mc.player;
        if (me == null || mc.level == null) return;
        boolean needAttach = me.isSpectator() && mc.getCameraEntity() == me;
        if (!needAttach) return;
        if (clientAttachTries <= 0) return;
        if (clientAttachCooldown > 0) { clientAttachCooldown--; return; }

        BlockOffensive.INSTANCE.sendToServer(new RequestAttachTeammateC2SPacket());
        tryLocalAttachToNearestTeammate();

        clientAttachTries--;
        clientAttachCooldown = 5;
    }

    private static void tryLocalAttachToNearestTeammate(){
        var mc = Minecraft.getInstance();
        var me = mc.player;
        var lvl = mc.level;
        if (me == null || lvl == null || !me.isSpectator()) return;

        Optional<String> myTeam = getTeam(me.getUUID());

        Player best = null;
        double bestD = Double.MAX_VALUE;
        for (Player p : lvl.players()){
            if (p == null || p == me || !p.isAlive() || p.isSpectator()) continue;
            if (!isSameTeamClientSide(myTeam.get(), me, p)) continue;
            double d = p.distanceToSqr(me);
            if (d < bestD){ bestD = d; best = p; }
        }
        if (best != null) {
            mc.setCameraEntity(best);
        }
    }

    private static Optional<String> getTeam(UUID id){
        try {
            return FPSMClient.getGlobalData().getPlayerTeam(id)
                    .map(s -> s.trim().toLowerCase(Locale.ROOT));
        } catch (Throwable t){
            return Optional.empty();
        }
    }
    private static boolean isSameTeamClientSide(String myTeam, Player me, Player other){
        Optional<String> ot = getTeam(other.getUUID());
        if (ot.isPresent()) return ot.get().equals(myTeam);
        
        var mt = me.getTeam();
        var otm = other.getTeam();
        if (mt != null && otm != null) {
            String a = mt.getName(), b = otm.getName();
            return a.equalsIgnoreCase(b);
        }
        return false;
    }

    /** 将相机强制归还给玩家 */
    private static void forceRestoreCameraToPlayer(){
        try{
            var mc = Minecraft.getInstance();
            var p = mc.player;
            if (p != null && mc.getCameraEntity() != p){
                mc.setCameraEntity(p);
            }
        }catch (Throwable ignored){}
    }

    private static void reset(){
        forceRestoreCameraToPlayer();

        disableGray();
        phase = Phase.NONE; tickIn = 0;
        hudOn = false;

        hudTick = 0; hudAlive = 0; iconReadyTick = -1; camHoldTick = 0;
        killerSide = Side.UNKNOWN;
        victimSide = Side.UNKNOWN;

        clientAttachTries = 0;
        clientAttachCooldown = 0;

        lastNs = 0L; timeSec = 0f;

        lastCamX = lastCamY = lastCamZ = Double.NaN;

        ghostCam = null;
        grayEnabled = false;
        holdBlack = false;
    }

    private KillCamManager(){}
}