package com.phasetranscrystal.blockoffensive.client.renderer;

import com.phasetranscrystal.blockoffensive.entity.CompositionC4Entity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.jetbrains.annotations.NotNull;

public class C4Renderer implements EntityRendererProvider<CompositionC4Entity> {

    @Override
    public @NotNull EntityRenderer<CompositionC4Entity, EntityRenderState> create(@NotNull Context pContext) {
        return new EntityRenderer<>(pContext) {
            @Override
            public @NotNull EntityRenderState createRenderState() {
                return new EntityRenderState();
            }
        };
    }
}
