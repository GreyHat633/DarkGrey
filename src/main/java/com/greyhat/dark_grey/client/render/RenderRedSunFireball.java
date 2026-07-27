package com.greyhat.dark_grey.client.render;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.greyhat.dark_grey.entity.EntityRedSunFireball;

public class RenderRedSunFireball extends Render {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
        "dark_grey:textures/entity/red_sun_fireball.png");

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        EntityRedSunFireball fireball = (EntityRedSunFireball) entity;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        try {
            GL11.glTranslated(x, y + entity.height / 2.0F, z);

            float size = fireball.getCurrentSize() / 2.0F;

            GL11.glDisable(GL11.GL_LIGHTING);
            net.minecraft.client.renderer.OpenGlHelper
                .setLightmapTextureCoords(net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);

            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GL11.glDisable(GL11.GL_CULL_FACE);

            this.bindEntityTexture(entity);

            float rotation = (entity.ticksExisted + partialTicks) * 5.0F;
            GL11.glRotatef(rotation, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(rotation * 0.5F, 1.0F, 0.0F, 0.0F);

            GL11.glDisable(GL11.GL_CULL_FACE);

            // Core
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(1.0F, 0.9F, 0.6F, 1.0F);
            SphereDisplayList.draw(size * 0.9F);

            // Inner textured swirling aura
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            this.bindEntityTexture(entity);
            GL11.glColor4f(1.0F, 0.4F, 0.0F, 0.8F);

            GL11.glMatrixMode(GL11.GL_TEXTURE);
            GL11.glPushMatrix();
            GL11.glRotatef(entity.ticksExisted * 3.0F, 0, 1, 0);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);

            SphereDisplayList.draw(size * 1.05F);

            GL11.glMatrixMode(GL11.GL_TEXTURE);
            GL11.glPopMatrix();

            // Outer fainter swirling aura in opposite direction
            GL11.glPushMatrix();
            GL11.glRotatef(entity.ticksExisted * -2.0F, 0, 1, 0);
            GL11.glRotatef(entity.ticksExisted * 1.5F, 1, 0, 0);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);

            GL11.glColor4f(1.0F, 0.1F, 0.0F, 0.5F);
            SphereDisplayList.draw(size * 1.15F);

            GL11.glMatrixMode(GL11.GL_TEXTURE);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);

            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        } finally {
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity p_110775_1_) {
        return TEXTURE;
    }
}
