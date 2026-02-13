package com.phasetranscrystal.blockoffensive.client.spec;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.phasetranscrystal.blockoffensive.BlockOffensive;
import com.phasetranscrystal.blockoffensive.net.spec.RequestAttachTeammateC2SPacket;
import com.phasetranscrystal.fpsmatch.common.client.FPSMClient;
import com.phasetranscrystal.fpsmatch.common.client.data.FPSMClientGlobalData;
import com.phasetranscrystal.fpsmatch.common.client.spec.SpectateMode;
import com.phasetranscrystal.fpsmatch.core.team.ClientTeam;
import com.phasetranscrystal.fpsmatch.util.FPSMFormatUtil;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(value = {Dist.CLIENT}, bus = Bus.FORGE)
public final class KillCamManager {
    private static final int PULL_T = 40;
    private static final int FADE_T = 20;
    private static final double EXTRA_DIST = 3.75D;
    private static final float GRAY_SWITCH_FRAC = 0.15F;
    private static final int HUD_IN_T = 10;
    private static final float POP_MIN_SCALE = 0.92F;
    private static final float POP_MAX_SCALE = 1.0F;
    private static final int HUD_MIN_TICKS = 60;
    private static final int ICON_MIN_SHOW_TICKS = 20;
    private static final int CAM_HOLD_EXTRA_TICKS = 40;
    private static final int HEAD = 32;
    private static final int ICON = 16;
    private static final int PAD = 8;
    private static final int BG_H = 42;
    private static final int CENTER_OFFSET_Y = 0;
    private static final int PANEL_ALPHA = 224;
    private static final int CT_BASE_RGB = 7319295;
    private static final int T_BASE_RGB = 15913338;
    private static final float VICTIM_BLEND = 0.35F;
    private static final float GRADIENT_GAMMA = 0.55F;
    private static final float KILLER_LIGHTEN = 0.28F;
    private static final float VICTIM_DARKEN = 0.18F;
    private static final ResourceLocation DESAT = ResourceLocation.tryBuild("minecraft", "shaders/post/desaturate.json");

    private static final int GRAY_BACKEND_SHADER = 0;
    private static final int GRAY_BACKEND_LIGHT = 1;
    private static final int GRAY_AUTO_MIN_FPS = 45;
    private static final int GRAY_AUTO_SAMPLE_FRAMES = 16;

    private static Phase phase;
    private static int tickIn;

    private static double vx;
    private static double vy;
    private static double vz;
    private static double kx;
    private static double ky;
    private static double kz;
    private static double dx;
    private static double dy;
    private static double dz;

    private static double lastCamX;
    private static double lastCamY;
    private static double lastCamZ;

    private static boolean hudOn;
    private static int hudTick;
    private static int hudAlive;

    private static boolean iconWanted;
    private static int iconReadyTick;
    private static int camHoldTick;

    private static UUID killerId;
    private static String killerName;
    private static String gunName;
    private static ItemStack gunStack;
    private static ResourceLocation killerSkin;
    private static Side killerSide;
    private static Side victimSide;

    private static int sideResolveCooldown;

    private static long lastNs;
    private static float frameDtSec;
    private static float timeSec;

    private static int clientAttachTries;
    private static int clientAttachCooldown;

    private static Entity ghostCam;

    private static boolean grayEnabled;
    private static boolean holdBlack;

    private static int grayBackend;
    private static boolean grayRequested;
    private static boolean shaderPrewarmAttempted;
    private static boolean shaderPrewarmed;
    private static boolean shaderTooHeavy;
    private static boolean shaderBroken;

    private static float lightStrength;

    private static boolean probeActive;
    private static int probeFrames;
    private static float probeDtSum;
    private static float probeWorstDt;

    private static String killedByText;
    private static String hudText;
    private static int hudTextW;

    private static volatile IHudStyleProvider STYLE_PROVIDER;
    private static volatile ICustomHudRenderer CUSTOM_RENDERER;

    public static void setStyleProvider(BiFunction<UUID, String, Style> f) {
        Objects.requireNonNull(f);
        STYLE_PROVIDER = f::apply;
    }

    public static void setStyleProvider(IHudStyleProvider p) {
        STYLE_PROVIDER = p;
    }

    public static void setCustomRenderer(ICustomHudRenderer r) {
        CUSTOM_RENDERER = r;
    }

    public static void startFromPacket() {
        if (phase != Phase.NONE) return;

        Vec3 kPos = KillCamClientCache.consumeKiller();
        Vec3 vPos = KillCamClientCache.consumeVictim();
        if (kPos == null || vPos == null) return;

        kx = kPos.x;
        ky = kPos.y;
        kz = kPos.z;

        vx = vPos.x;
        vy = vPos.y;
        vz = vPos.z;

        double rx = vx - kx;
        double ry = vy - ky;
        double rz = vz - kz;
        double len2 = rx * rx + ry * ry + rz * rz;

        if (len2 < 1.0E-6) {
            LocalPlayer p = Minecraft.getInstance().player;
            if (p == null) return;
            Vec3 d = Vec3.directionFromRotation(p.getXRot(), p.getYRot());
            dx = d.x;
            dy = d.y;
            dz = d.z;
        } else {
            double inv = 1.0D / Math.sqrt(len2);
            dx = rx * inv;
            dy = ry * inv;
            dz = rz * inv;
        }

        killerId = KillCamClientCache.getKillerUUID();
        killerName = KillCamClientCache.getKillerName();
        gunStack = KillCamClientCache.getWeapon();
        gunName = FPSMFormatUtil.i18n(gunStack);
        killerSkin = fetchSkin(killerId, killerName);

        killerSide = detectSide(killerId);
        UUID myId = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getUUID() : null;
        victimSide = detectSide(myId);
        sideResolveCooldown = 0;

        killedByText = I18n.exists("blockoffensive.killed_by") ? I18n.get("blockoffensive.killed_by") : "击杀了你";
        hudText = "§c" + killerName + " §7使用 §e" + gunName + " §7" + killedByText;
        Font font = Minecraft.getInstance().font;
        hudTextW = font.width(hudText);

        hudOn = true;
        hudTick = 0;
        hudAlive = 0;

        camHoldTick = 0;
        iconWanted = gunStack != null && !gunStack.isEmpty();
        iconReadyTick = iconWanted ? 0 : -1;

        clientAttachTries = 8;
        clientAttachCooldown = 0;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.setCameraEntity(mc.player);
        }

        phase = Phase.PULL;
        tickIn = 0;

        grayEnabled = false;
        grayRequested = false;
        holdBlack = false;
        lightStrength = 0.0F;

        lastNs = 0L;
        frameDtSec = 0.0F;
        timeSec = 0.0F;

        ensureGhost();
        if (ghostCam != null) {
            float yaw0 = (float)Math.toDegrees(Math.atan2(-(kx - vx), kz - vz));
            float pitch0 = (float)Math.toDegrees(Math.atan2(-(ky - vy), Math.sqrt((kx - vx) * (kx - vx) + (kz - vz) * (kz - vz))));
            ghostCam.moveTo(vx, vy, vz, yaw0, pitch0);
            ghostCam.setOldPosAndRot();
            lastCamX = vx;
            lastCamY = vy;
            lastCamZ = vz;
            mc.setCameraEntity(ghostCam);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer pl = mc.player;
        if (pl == null) {
            reset();
            return;
        }

        boolean isSpec = pl.isSpectator();
        if (hudOn) {
            ++hudAlive;
            if (sideResolveCooldown > 0) {
                --sideResolveCooldown;
            }
        }

        if (phase == Phase.NONE) {
            attemptAttachRetryIfNeeded();
            prewarmShaderIfNeeded();
            if (!isSpec) {
                forceRestoreCameraToPlayer();
                reset();
            }
            return;
        }

        switch (phase) {
            case PULL -> {
                ensureGhost();
                double t = Mth.clamp((double)tickIn / (double)PULL_T, 0.0D, 1.0D);
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

                lastCamX = nx;
                lastCamY = ny;
                lastCamZ = nz;

                if (!grayEnabled && t >= (double)GRAY_SWITCH_FRAC) {
                    enableGray();
                    grayEnabled = true;
                }

                if (tickIn < PULL_T) {
                    ++tickIn;
                } else {
                    ++camHoldTick;
                }

                boolean uiLongEnough = hudAlive >= HUD_MIN_TICKS;
                boolean iconShownEnough = !iconWanted || (iconReadyTick >= 0 && hudAlive - iconReadyTick >= ICON_MIN_SHOW_TICKS) || hudAlive >= (HUD_MIN_TICKS + ICON_MIN_SHOW_TICKS);
                boolean canEnterFade = uiLongEnough && iconShownEnough && camHoldTick >= CAM_HOLD_EXTRA_TICKS;

                if (canEnterFade) {
                    phase = Phase.FADE;
                    tickIn = 0;
                    holdBlack = false;
                }
            }
            case FADE -> {
                if (!holdBlack && ++tickIn >= FADE_T) {
                    holdBlack = true;
                    BlockOffensive.INSTANCE.sendToServer(new RequestAttachTeammateC2SPacket());
                    tryLocalAttachToNearestTeammate();
                }
            }
        }

        if (isSpec) {
            Entity camEnt = mc.getCameraEntity();
            if (camEnt != null && camEnt != pl && camEnt != ghostCam) {
                holdBlack = false;
                disableGray();
                hudOn = false;
                phase = Phase.NONE;
                tickIn = 0;
            }
        }

        if (!isSpec) {
            forceRestoreCameraToPlayer();
            reset();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCamAngles(ViewportEvent.ComputeCameraAngles e) {
        if (phase != Phase.PULL) return;

        double nx = lastCamX;
        double ny = lastCamY;
        double nz = lastCamZ;

        if (Double.isNaN(nx)) {
            nx = vx;
            ny = vy;
            nz = vz;
        }

        double vx_ = kx - nx;
        double vy_ = ky - ny;
        double vz_ = kz - nz;

        float yaw = (float)Math.toDegrees(Math.atan2(-vx_, vz_));
        float pitch = (float)Math.toDegrees(Math.atan2(-vy_, Math.sqrt(vx_ * vx_ + vz_ * vz_)));

        e.setYaw(yaw);
        e.setPitch(pitch);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderGuiPre(RenderGuiEvent.Pre e) {
        if (!grayRequested && lightStrength <= 0.01F && !(probeActive && grayBackend == GRAY_BACKEND_SHADER)) return;

        long now = System.nanoTime();
        float dt = lastNs == 0L ? 0.0F : (float)((now - lastNs) / 1.0E9D);
        lastNs = now;
        frameDtSec = Mth.clamp(dt, 0.0F, 0.1F);
        timeSec += frameDtSec;

        if (grayBackend == GRAY_BACKEND_SHADER && grayRequested) {
            probeFrame(frameDtSec);
        }

        if (grayBackend != GRAY_BACKEND_LIGHT && lightStrength <= 0.01F) return;
        renderLightGrayLayer(e.getGuiGraphics(), e.getWindow(), frameDtSec);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderGuiPost(RenderGuiEvent.Post e) {
        float blackAlpha = 0.0F;
        if (phase == Phase.FADE) {
            blackAlpha = holdBlack ? 1.0F : (float)tickIn / (float)FADE_T;
            blackAlpha = Mth.clamp(blackAlpha, 0.0F, 1.0F);
        }

        if (!hudOn && blackAlpha <= 0.0F) return;

        GuiGraphics gg = e.getGuiGraphics();
        Window win = e.getWindow();
        int sw = win.getGuiScaledWidth();
        int sh = win.getGuiScaledHeight();

        if (hudOn) {
            renderKillHud(gg, sw, sh, blackAlpha);
        }

        if (blackAlpha > 0.0F) {
            int a = Mth.clamp(Math.round(255.0F * blackAlpha), 0, 255);
            int argb = a << 24;
            RenderSystem.enableBlend();
            gg.fill(0, 0, sw, sh, argb);
            RenderSystem.disableBlend();
        }
    }

    private static void renderLightGrayLayer(GuiGraphics gg, Window win, float dt) {
        float target = grayRequested ? 1.0F : 0.0F;
        float speed = grayRequested ? 14.0F : 18.0F;

        lightStrength += (target - lightStrength) * Math.min(1.0F, dt * speed);
        lightStrength = Mth.clamp(lightStrength, 0.0F, 1.0F);

        if (lightStrength <= 0.01F) return;

        int sw = win.getGuiScaledWidth();
        int sh = win.getGuiScaledHeight();
        if (sw <= 0 || sh <= 0) return;

        float s = lightStrength;

        int grayA = Mth.clamp((int)(112.0F * s), 0, 170);
        int blackA = Mth.clamp((int)(34.0F * s), 0, 96);

        int grayArgb = (grayA << 24) | 0x808080;
        int blackArgb = blackA << 24;

        RenderSystem.enableBlend();
        gg.fill(0, 0, sw, sh, grayArgb);
        gg.fill(0, 0, sw, sh, blackArgb);

        if (dt > 0.0F) {
            float fps = 1.0F / dt;
            if (fps >= 30.0F) {
                int minDim = Math.min(sw, sh);
                int edgeH = Mth.clamp(Math.round(minDim * 0.10F), 18, 72);
                int sideW = Mth.clamp(Math.round(minDim * 0.06F), 10, 48);

                int edgeA = Mth.clamp((int)(70.0F * s), 0, 110);
                int sideA = Mth.clamp((int)(45.0F * s), 0, 90);

                int top0 = edgeA << 24;
                int top1 = 0;
                gg.fillGradient(0, 0, sw, edgeH, top0, top1);
                gg.fillGradient(0, sh - edgeH, sw, sh, top1, top0);

                gg.fill(0, 0, sideW, sh, sideA << 24);
                gg.fill(sw - sideW, 0, sw, sh, sideA << 24);
            }
        }

        RenderSystem.disableBlend();
    }

    private static void renderKillHud(GuiGraphics gg, int sw, int sh, float blackAlpha) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        if (hudTick < HUD_IN_T) {
            ++hudTick;
        }

        float in = Mth.clamp((float)hudTick / (float)HUD_IN_T, 0.0F, 1.0F);
        float alphaMul = in * (1.0F - blackAlpha);
        if (alphaMul <= 0.01F) return;

        if (killerSide == Side.UNKNOWN || victimSide == Side.UNKNOWN) {
            if (sideResolveCooldown <= 0) {
                if (killerSide == Side.UNKNOWN) {
                    killerSide = detectSide(killerId);
                }
                if (victimSide == Side.UNKNOWN) {
                    UUID myId = mc.player != null ? mc.player.getUUID() : null;
                    victimSide = detectSide(myId);
                }
                sideResolveCooldown = 10;
            }
        }

        int killerRGB = lighten(sideBaseRGB(killerSide));
        int victimRGB = darken(sideBaseRGB(victimSide));
        int startARGB = (PANEL_ALPHA << 24) | (killerRGB & 0xFFFFFF);
        int endARGB = (PANEL_ALPHA << 24) | (mixRGB(killerRGB, victimRGB, VICTIM_BLEND) & 0xFFFFFF);

        String text = hudText != null ? hudText : "";
        int txtW = hudTextW > 0 ? hudTextW : font.width(text);

        boolean reserveIcon = iconWanted;
        int iconPart = reserveIcon ? 24 : 0;
        int totalW = 40 + txtW + 8 + iconPart;

        int cx = sw / 2;
        int cy = sh / 2 + CENTER_OFFSET_Y;

        float pop = easeOutBack(in);
        float scale = Mth.lerp(pop, POP_MIN_SCALE, POP_MAX_SCALE);

        float maxW = (float)sw * 0.92F;
        if ((float)totalW * scale > maxW) {
            scale *= maxW / ((float)totalW * scale);
        }

        float tx = snapToPixel((float)cx, scale);
        float ty = snapToPixel((float)cy, scale);

        int x0 = Math.round((float)(-totalW) / 2.0F);
        int y0 = -21;
        int x1 = x0 + totalW;
        int y1 = y0 + BG_H;

        RenderSystem.enableBlend();

        PoseStack pose = gg.pose();
        pose.pushPose();
        pose.translate(tx, ty, 0.0F);
        pose.scale(scale, scale, 1.0F);

        drawGradientPanelRect(gg, x0, y0, x1, y1, mulAlpha(startARGB, alphaMul), mulAlpha(endARGB, alphaMul));

        int headX = x0 + PAD;
        int headY = y0 + 5;
        if (killerSkin != null) {
            gg.blit(killerSkin, headX, headY, 32, 32, 8.0F, 8.0F, 8, 8, 64, 64);
            gg.blit(killerSkin, headX, headY, 32, 32, 40.0F, 8.0F, 8, 8, 64, 64);
        }

        int textLeft = headX + HEAD + PAD;
        int textRight = x1 - (reserveIcon ? 32 : PAD);
        int allowedW = Math.max(0, textRight - textLeft);

        float textScale = Math.min(1.0F, (float)allowedW / Math.max(1.0F, (float)txtW));
        int tDrawW = Math.round((float)txtW * textScale);
        int textX = textLeft + (allowedW - tDrawW) / 2;
        int textY = y0 + (BG_H - 9) / 2;

        pose.pushPose();
        pose.translate((float)textX, (float)textY, 0.0F);
        pose.scale(textScale, textScale, 1.0F);
        gg.drawString(font, text, 0, 0, mulAlpha(0xFFFFFFFF, alphaMul), false);
        pose.popPose();

        pose.popPose();

        if (gunStack != null && !gunStack.isEmpty()) {
            int localIconX = x1 - PAD - ICON;
            int localIconY = y0 + 13;

            int absIconX = Math.round(tx + (float)localIconX * scale);
            int absIconY = Math.round(ty + (float)localIconY * scale);
            int size = Math.max(1, Math.round((float)ICON * scale));

            renderItemAt(gg, gunStack, absIconX, absIconY, size, size, font);
            if (iconWanted && iconReadyTick < 0) {
                iconReadyTick = hudAlive;
            }
        }

        RenderSystem.disableBlend();
    }

    private static double kickThenEaseOut(double t) {
        t = Mth.clamp(t, 0.0D, 1.0D);
        double pre = 0.2D;
        double kick = 0.33D;
        if (t <= pre) {
            double u = t / pre;
            return kick * u * u;
        }
        double u = (t - pre) / (1.0D - pre);
        double easeOutCubic = 1.0D - Math.pow(1.0D - u, 3.0D);
        return kick + (1.0D - kick) * easeOutCubic;
    }

    public static void onSpectateModeUpdate(SpectateMode mode) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer pl = mc.player;
        if (pl == null) return;

        if (!pl.isSpectator()) {
            forceRestoreCameraToPlayer();
            reset();
        }
    }

    private static Side detectSide(UUID id) {
        if (id == null) return Side.UNKNOWN;

        try {
            FPSMClientGlobalData gd = FPSMClient.getGlobalData();
            Optional<ClientTeam> opt = gd.getTeamByUUID(id);
            if (opt.isPresent()) {
                String s = opt.get().getName();
                s = s.trim().toLowerCase(Locale.ROOT);
                if (s.equals("ct") || s.contains("counter")) return Side.CT;
                if (s.equals("t") || s.contains("terror")) return Side.T;
            }
        } catch (Throwable ignored) {
        }

        Minecraft mc = Minecraft.getInstance();
        ClientLevel lvl = mc.level;
        if (lvl != null) {
            Player p = lvl.getPlayerByUUID(id);
            if (p != null) {
                Team team = p.getTeam();
                if (team != null) {
                    String n = team.getName().toLowerCase(Locale.ROOT);
                    if (n.contains("ct") || n.contains("counter") || n.contains("blue")) return Side.CT;
                    if (n.equals("t") || n.contains("terror") || n.contains("red") || n.matches(".*\\bt\\b")) return Side.T;
                }
            }
        }

        return Side.UNKNOWN;
    }

    private static int sideBaseRGB(Side s) {
        return switch (s) {
            case CT -> CT_BASE_RGB;
            case T -> T_BASE_RGB;
            default -> 8421504;
        };
    }

    private static ResourceLocation fetchSkin(UUID id, String name) {
        return Minecraft.getInstance().getSkinManager().getInsecureSkinLocation(new GameProfile(id, name));
    }

    private static void drawGradientPanelRect(GuiGraphics gg, int x0, int y0, int x1, int y1, int startARGB, int endARGB) {
        int w = Math.max(1, x1 - x0);
        if (w <= 2) {
            gg.fill(x0, y0, x1, y1, startARGB);
            return;
        }

        int seg = w / 6;
        seg = Mth.clamp(seg, 12, 48);
        seg = Math.min(seg, w);

        for (int i = 0; i < seg; ++i) {
            int sx0 = x0 + (int)((long)i * (long)w / (long)seg);
            int sx1 = x0 + (int)((long)(i + 1) * (long)w / (long)seg);
            if (sx1 <= sx0) continue;

            float t = ((float)i + 0.5F) / (float)seg;
            float tg = (float)Math.pow(t, KillCamManager.GRADIENT_GAMMA);
            int col = lerpARGB(startARGB, endARGB, tg);
            gg.fill(sx0, y0, sx1, y1, col);
        }
    }

    private static void renderItemAt(GuiGraphics g, ItemStack s, int x, int y, int w, int h, Font font) {
        if (s == null || s.isEmpty()) return;

        PoseStack pose = g.pose();
        pose.pushPose();

        float sc = Math.max(0.001F, (float)Math.min(w, h) / 16.0F);
        int dx = x + Math.round(((float)w - 16.0F * sc) / 2.0F);
        int dy = y + Math.round(((float)h - 16.0F * sc) / 2.0F);

        pose.translate((float)dx, (float)dy, 0.0F);
        pose.scale(sc, sc, 1.0F);

        g.renderItem(s, 0, 0);
        g.renderItemDecorations(font, s, 0, 0);

        pose.popPose();
    }

    private static int lighten(int rgb) {
        return mixRGB(rgb, 0xFFFFFF, KILLER_LIGHTEN);
    }

    private static int darken(int rgb) {
        return mixRGB(rgb, 0x000000, VICTIM_DARKEN);
    }

    private static float snapToPixel(float v, float scale) {
        return (float)Math.round(v * scale) / scale;
    }

    private static int mulAlpha(int argb, float mul) {
        mul = Mth.clamp(mul, 0.0F, 1.0F);
        int a = (int)((float)((argb >>> 24) & 255) * mul);
        return (a << 24) | (argb & 0xFFFFFF);
    }

    private static int lerpARGB(int a, int b, float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);

        int aA = (a >>> 24) & 255;
        int aR = (a >>> 16) & 255;
        int aG = (a >>> 8) & 255;
        int aB = a & 255;

        int bA = (b >>> 24) & 255;
        int bR = (b >>> 16) & 255;
        int bG = (b >>> 8) & 255;
        int bB = b & 255;

        int rA = aA + Math.round((float)(bA - aA) * t);
        int rR = aR + Math.round((float)(bR - aR) * t);
        int rG = aG + Math.round((float)(bG - aG) * t);
        int rB = aB + Math.round((float)(bB - aB) * t);

        return (rA << 24) | (rR << 16) | (rG << 8) | rB;
    }

    private static int mixRGB(int c1, int c2, float p) {
        p = Mth.clamp(p, 0.0F, 1.0F);

        int r1 = (c1 >>> 16) & 255;
        int g1 = (c1 >>> 8) & 255;
        int b1 = c1 & 255;

        int r2 = (c2 >>> 16) & 255;
        int g2 = (c2 >>> 8) & 255;
        int b2 = c2 & 255;

        int r = (int)((float)r1 + (float)(r2 - r1) * p);
        int g = (int)((float)g1 + (float)(g2 - g1) * p);
        int b = (int)((float)b1 + (float)(b2 - b1) * p);

        return (r << 16) | (g << 8) | b;
    }

    private static float easeOutBack(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        float c1 = 1.70158F;
        float c3 = c1 + 1.0F;
        float u = t - 1.0F;
        return 1.0F + c3 * u * u * u + c1 * u * u;
    }

    private static void enableGray() {
        grayRequested = true;

        if (grayBackend == GRAY_BACKEND_LIGHT || shaderTooHeavy || shaderBroken || !canUseShaderBackend()) {
            grayBackend = GRAY_BACKEND_LIGHT;
            lightStrength = Math.max(lightStrength, 0.82F);
            return;
        }

        grayBackend = GRAY_BACKEND_SHADER;
        beginProbe();
        try {
            Minecraft mc = Minecraft.getInstance();
            mc.gameRenderer.loadEffect(DESAT);
        } catch (Throwable t) {
            shaderBroken = true;
            grayBackend = GRAY_BACKEND_LIGHT;
            lightStrength = Math.max(lightStrength, 0.82F);
            endProbe();
        }
    }

    private static void disableGray() {
        grayRequested = false;
        endProbe();

        if (grayBackend != GRAY_BACKEND_SHADER) return;

        try {
            Minecraft mc = Minecraft.getInstance();
            mc.gameRenderer.shutdownEffect();
        } catch (Throwable ignored) {
        }
    }

    private static boolean canUseShaderBackend() {
        try {
            Minecraft mc = Minecraft.getInstance();
            GraphicsStatus gs = mc.options.graphicsMode().get();
            return gs.getId() != 0;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void prewarmShaderIfNeeded() {
        if (shaderPrewarmAttempted || shaderPrewarmed || shaderTooHeavy || shaderBroken) return;
        if (!canUseShaderBackend()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (grayRequested || phase != Phase.NONE) return;

        shaderPrewarmAttempted = true;
        try {
            mc.gameRenderer.loadEffect(DESAT);
            mc.gameRenderer.shutdownEffect();
            shaderPrewarmed = true;
        } catch (Throwable t) {
            shaderBroken = true;
            shaderTooHeavy = true;
            grayBackend = GRAY_BACKEND_LIGHT;
        }
    }

    private static void beginProbe() {
        if (shaderTooHeavy || shaderBroken) return;
        probeActive = true;
        probeFrames = 0;
        probeDtSum = 0.0F;
        probeWorstDt = 0.0F;
    }

    private static void endProbe() {
        probeActive = false;
        probeFrames = 0;
        probeDtSum = 0.0F;
        probeWorstDt = 0.0F;
    }

    private static void probeFrame(float dt) {
        if (!probeActive || dt <= 0.0F) return;
        if (probeFrames >= GRAY_AUTO_SAMPLE_FRAMES) return;

        ++probeFrames;
        probeDtSum += dt;
        if (dt > probeWorstDt) probeWorstDt = dt;

        if (probeFrames < GRAY_AUTO_SAMPLE_FRAMES) return;

        float avgDt = probeDtSum / (float)Math.max(1, probeFrames);
        float dtBudget = 1.0F / Math.max(20.0F, (float)GRAY_AUTO_MIN_FPS);

        boolean tooHeavy = avgDt > dtBudget * 1.20F || probeWorstDt > dtBudget * 1.70F;
        probeActive = false;

        if (!tooHeavy) return;

        shaderTooHeavy = true;
        switchToLightBackendNow();
    }

    private static void switchToLightBackendNow() {
        Minecraft mc = Minecraft.getInstance();
        try {
            mc.gameRenderer.shutdownEffect();
        } catch (Throwable ignored) {
        }

        grayBackend = GRAY_BACKEND_LIGHT;
        lightStrength = Math.max(lightStrength, 0.82F);
    }

    private static void ensureGhost() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        if (ghostCam == null || ghostCam.level() != mc.level || ghostCam.isRemoved()) {
            Entity g = EntityType.MARKER.create(mc.level);
            if (g == null) {
                g = EntityType.ARMOR_STAND.create(mc.level);
            }
            ghostCam = g;
        }
    }

    private static void attemptAttachRetryIfNeeded() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer me = mc.player;
        if (me == null || mc.level == null) return;

        boolean needAttach = me.isSpectator() && mc.getCameraEntity() == me;
        if (!needAttach) return;

        if (clientAttachTries <= 0) return;

        if (clientAttachCooldown > 0) {
            --clientAttachCooldown;
            return;
        }

        BlockOffensive.INSTANCE.sendToServer(new RequestAttachTeammateC2SPacket());
        tryLocalAttachToNearestTeammate();
        --clientAttachTries;
        clientAttachCooldown = 5;
    }

    private static void tryLocalAttachToNearestTeammate() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer me = mc.player;
        ClientLevel lvl = mc.level;
        if (me == null || lvl == null || !me.isSpectator()) return;

        Optional<String> myTeam = getTeam(me.getUUID());
        Player best = null;
        double bestD = Double.MAX_VALUE;

        for (Player p : lvl.players()) {
            if (p == null || p == me) continue;
            if (!p.isAlive() || p.isSpectator()) continue;
            if (!isSameTeamClientSide(myTeam, me, p)) continue;

            double d = p.distanceToSqr(me);
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }

        if (best != null) {
            mc.setCameraEntity(best);
        }
    }

    private static Optional<String> getTeam(UUID id) {
        try {
            return FPSMClient.getGlobalData().getTeamByUUID(id).map(t -> t.getName().trim().toLowerCase(Locale.ROOT));
        } catch (Throwable ignored) {
            return Optional.empty();
        }
    }

    private static boolean isSameTeamClientSide(Optional<String> myTeam, Player me, Player other) {
        Optional<String> ot = getTeam(other.getUUID());
        if (myTeam.isPresent() && ot.isPresent()) {
            return ot.get().equals(myTeam.get());
        }

        Team mt = me.getTeam();
        Team otm = other.getTeam();
        if (mt != null && otm != null) {
            return mt.getName().equalsIgnoreCase(otm.getName());
        }

        return false;
    }

    private static void forceRestoreCameraToPlayer() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;
        if (mc.getCameraEntity() != p) {
            mc.setCameraEntity(p);
        }
    }

    private static void reset() {
        forceRestoreCameraToPlayer();
        disableGray();

        phase = Phase.NONE;
        tickIn = 0;

        hudOn = false;
        hudTick = 0;
        hudAlive = 0;

        iconWanted = false;
        iconReadyTick = -1;
        camHoldTick = 0;

        killerId = null;
        killerSide = Side.UNKNOWN;
        victimSide = Side.UNKNOWN;
        sideResolveCooldown = 0;

        killerName = "???";
        gunName = "未知武器";
        gunStack = ItemStack.EMPTY;
        killerSkin = null;

        killedByText = null;
        hudText = null;
        hudTextW = 0;

        clientAttachTries = 0;
        clientAttachCooldown = 0;

        lastNs = 0L;
        frameDtSec = 0.0F;
        timeSec = 0.0F;

        lastCamX = Double.NaN;
        lastCamY = Double.NaN;
        lastCamZ = Double.NaN;

        ghostCam = null;

        grayEnabled = false;
        grayRequested = false;
        holdBlack = false;
        lightStrength = 0.0F;
        endProbe();
    }

    private KillCamManager() {
    }

    static {
        phase = Phase.NONE;
        tickIn = 0;

        lastCamX = Double.NaN;
        lastCamY = Double.NaN;
        lastCamZ = Double.NaN;

        hudOn = false;
        hudTick = 0;
        hudAlive = 0;

        iconWanted = false;
        iconReadyTick = -1;
        camHoldTick = 0;

        killerId = null;
        killerName = "???";
        gunName = "未知武器";
        gunStack = ItemStack.EMPTY;
        killerSkin = null;

        killerSide = Side.UNKNOWN;
        victimSide = Side.UNKNOWN;
        sideResolveCooldown = 0;

        killedByText = null;
        hudText = null;
        hudTextW = 0;

        lastNs = 0L;
        frameDtSec = 0.0F;
        timeSec = 0.0F;

        clientAttachTries = 0;
        clientAttachCooldown = 0;

        ghostCam = null;

        grayEnabled = false;
        holdBlack = false;

        grayBackend = GRAY_BACKEND_SHADER;
        grayRequested = false;
        shaderPrewarmAttempted = false;
        shaderPrewarmed = false;
        shaderTooHeavy = false;
        shaderBroken = false;

        lightStrength = 0.0F;

        probeActive = false;
        probeFrames = 0;
        probeDtSum = 0.0F;
        probeWorstDt = 0.0F;

        STYLE_PROVIDER = null;
        CUSTOM_RENDERER = null;
    }

    private enum Phase {
        NONE,
        PULL,
        FADE
    }

    private enum Side {
        CT,
        T,
        UNKNOWN
    }

    public record Style(int bg0, int bg1, int border) {
        public Style(int bg0, int bg1, int border) {
            this.bg0 = bg0;
            this.bg1 = bg1;
            this.border = border;
        }

        public int bg0() {
            return this.bg0;
        }

        public int bg1() {
            return this.bg1;
        }

        public int border() {
            return this.border;
        }
    }

    public interface ICustomHudRenderer {
        boolean render(GuiGraphics gg, int sw, int sh, UUID killer, String killerName, String gunName, String killedByText, ItemStack gun, Style style);
    }

    @FunctionalInterface
    public interface IHudStyleProvider extends BiFunction<UUID, String, Style> {
    }
}