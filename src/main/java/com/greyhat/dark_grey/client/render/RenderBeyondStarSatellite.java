package com.greyhat.dark_grey.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.util.glu.Sphere;

public class RenderBeyondStarSatellite extends Render {

    private static final ResourceLocation SPHERE_TEXTURE = new ResourceLocation(
        "dark_grey",
        "textures/entity/beyond_star_satellite.png");

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_CULL_FACE);

        float ticks = entity.ticksExisted + partialTicks;
        GL11.glRotatef(20.0f, 0.0f, 0.0f, 1.0f);
        GL11.glRotatef(ticks * 2.0f, 0.0f, 1.0f, 0.0f);

        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(SPHERE_TEXTURE);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        Sphere sphere = new Sphere();
        sphere.setDrawStyle(org.lwjgl.util.glu.GLU.GLU_FILL);
        sphere.setTextureFlag(true);
        sphere.setNormals(org.lwjgl.util.glu.GLU.GLU_SMOOTH);
        sphere.draw(0.25F, 16, 16);

        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return SPHERE_TEXTURE;
    }
}
