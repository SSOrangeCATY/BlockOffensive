package net.minecraft.client.gui.components;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class PlayerFaceRenderer {
    private PlayerFaceRenderer() {
    }

    public static void draw(GuiGraphicsExtractor gui, Identifier skin, int x, int y, int size) {
        gui.blit(RenderPipelines.GUI_TEXTURED, skin, x, y, 8.0F, 8.0F, size, size, 64, 64);
        gui.blit(RenderPipelines.GUI_TEXTURED, skin, x, y, 40.0F, 8.0F, size, size, 64, 64);
    }
}
