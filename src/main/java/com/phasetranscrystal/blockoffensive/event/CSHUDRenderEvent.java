package com.phasetranscrystal.blockoffensive.event;

import com.phasetranscrystal.blockoffensive.client.screen.hud.CSMvpHud;
import com.phasetranscrystal.blockoffensive.data.MvpReason;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.neoforged.bus.api.Event;

public class CSHUDRenderEvent extends Event {
    public final GuiGraphicsExtractor guiGraphics;
    public final int screenWidth;
    public final int screenHeight;

    public CSHUDRenderEvent(GuiGraphicsExtractor guiGraphics, int screenWidth, int screenHeight) {
        this.guiGraphics = guiGraphics;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    public GuiGraphicsExtractor getGuiGraphics() {
        return guiGraphics;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public static class RenderMvpHud extends CSHUDRenderEvent {
        private final CSMvpHud hud;

        public RenderMvpHud(GuiGraphicsExtractor guiGraphics, int screenWidth, int screenHeight, CSMvpHud hud) {
            super(guiGraphics, screenWidth, screenHeight);
            this.hud = hud;
        }

        public CSMvpHud getHud() {
            return hud;
        }

        public static class Pre extends RenderMvpHud {
            public Pre(GuiGraphicsExtractor guiGraphics, int screenWidth, int screenHeight, CSMvpHud hud) {
                super(guiGraphics, screenWidth, screenHeight, hud);
            }
        }

        public static class Post extends RenderMvpHud {
            public Post(GuiGraphicsExtractor guiGraphics, int screenWidth, int screenHeight, CSMvpHud hud) {
                super(guiGraphics, screenWidth, screenHeight, hud);
            }
        }

        public static class TriggeredAnimation extends Event {
            public final MvpReason reason;

            public TriggeredAnimation(MvpReason reason) {
                this.reason = reason;
            }

            public MvpReason getReason() {
                return reason;
            }
        }
   }
}
