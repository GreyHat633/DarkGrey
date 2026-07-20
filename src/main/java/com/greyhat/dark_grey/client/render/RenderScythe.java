package com.greyhat.dark_grey.client.render;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.greyhat.dark_grey.entity.EntityScythe;

public class RenderScythe extends Render {

    // Same texture as the held item texture generated for Calamity
    private static final ResourceLocation SCYTHE_TEXTURE = new ResourceLocation(
        "dark_grey",
        "textures/items/calamity_scythe_equipped.png");

    @Override
    public void doRender(Entity entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (!(entity instanceof EntityScythe)) return;
        EntityScythe scythe = (EntityScythe) entity;

        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);

        // Blending for Phantom effect
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        // Dark crimson red phantom effect (r, g, b, alpha)
        GL11.glColor4f(0.8F, 0.1F, 0.2F, 0.7F);

        // Interpolated rotation based on ticksExisted and partialTicks for 20 ticks duration
        // 360 degrees over 20 ticks
        float current_tick = (scythe.ticksExisted - 1) + partialTicks;
        float current_rotation_angle = (current_tick / 20.0F) * 360.0F;

        // Rotate around Y axis for the sweep
        GL11.glRotatef(current_rotation_angle, 0.0F, 1.0F, 0.0F);

        // Tilt it slightly so it looks like it's sweeping horizontally but angled
        GL11.glRotatef(15.0F, 1.0F, 0.0F, 0.0F);

        // Bind the texture
        this.bindEntityTexture(scythe);

        // Render a large 2D quad (billboard-like but rotated) for the scythe
        Tessellator tessellator = Tessellator.instance;
        float size = 5.0F; // Match the five-block gameplay radius.

        GL11.glDisable(GL11.GL_CULL_FACE);

        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(-size, 0, -size, 0, 0);
        tessellator.addVertexWithUV(-size, 0, size, 0, 1);
        tessellator.addVertexWithUV(size, 0, size, 1, 1);
        tessellator.addVertexWithUV(size, 0, -size, 1, 0);
        tessellator.draw();

        GL11.glEnable(GL11.GL_CULL_FACE);

        // Reset blending and color state
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(GL11.GL_BLEND);

        GL11.glPopMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return SCYTHE_TEXTURE;
    }
}
