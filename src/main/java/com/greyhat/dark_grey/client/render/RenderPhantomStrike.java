package com.greyhat.dark_grey.client.render;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

public class RenderPhantomStrike extends Render {

    private static final ResourceLocation LANCE_TEXTURE = new ResourceLocation(
        "dark_grey",
        "textures/items/solar_flare_lance_equipped.png");

    public RenderPhantomStrike() {}

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        // Render completely invisible (particles are handled in EntityPhantomStrike.onUpdate)
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return LANCE_TEXTURE;
    }
}
