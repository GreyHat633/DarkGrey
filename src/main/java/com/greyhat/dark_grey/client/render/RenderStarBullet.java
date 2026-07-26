package com.greyhat.dark_grey.client.render;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class RenderStarBullet extends Render {

    private static final ResourceLocation STAR_TEXTURE = new ResourceLocation(
        "dark_grey",
        "textures/entity/supernova_star.png");

    private static final net.minecraft.util.ResourceLocation SPHERE_TEXTURE = new net.minecraft.util.ResourceLocation(
        "dark_grey",
        "textures/entity/sphere_texture.png");
    private static final net.minecraft.util.ResourceLocation RING_TEXTURE = new net.minecraft.util.ResourceLocation(
        "dark_grey",
        "textures/entity/ring_texture.png");

    private static org.lwjgl.util.glu.Sphere sphereRenderer;

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        if (sphereRenderer == null) {
            sphereRenderer = new org.lwjgl.util.glu.Sphere();
            sphereRenderer.setDrawStyle(org.lwjgl.util.glu.GLU.GLU_FILL);
            sphereRenderer.setNormals(org.lwjgl.util.glu.GLU.GLU_SMOOTH);
            sphereRenderer.setTextureFlag(true);
        }

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        GL11.glTranslated(x, y + 0.5, z);

        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        GL11.glDisable(GL11.GL_LIGHTING); // Manually shaded
        GL11.glEnable(GL11.GL_BLEND);

        // Sphere is mostly opaque, so we ENABLE depth writing and cull face for proper 3D sorting
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_CULL_FACE);

        // Give the planet a fixed tilt (e.g., 20 degrees on Z axis like Earth/Saturn)
        GL11.glRotatef(20.0f, 0.0f, 0.0f, 1.0f);

        // Slow rotation around its local Y axis
        float rotationSpeed = 2.0f;
        float ticks = entity.ticksExisted + partialTicks;
        GL11.glRotatef(ticks * rotationSpeed, 0.0f, 1.0f, 0.0f);

        // --- Render Planet Sphere ---
        bindTexture(SPHERE_TEXTURE);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA); // Normal blending for solid sphere

        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f); // Full color from texture
        float sphereRadius = 0.25f * 1.5f;

        // Setup texture perturbation matrix
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPushMatrix();
        GL11.glTranslatef((ticks * 0.0005f) % 1.0f, (ticks * 0.0002f) % 1.0f, 0.0f); // Extremely slow scroll
        GL11.glRotatef((float) Math.sin(ticks * 0.01) * 1.5f, 0.0f, 0.0f, 1.0f); // Tiny swirling
        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        // Sphere needs to be rotated 90 degrees on X to align texture poles properly (GLU Sphere poles are on Z axis)
        GL11.glPushMatrix();
        GL11.glRotatef(90.0f, 1.0f, 0.0f, 0.0f);
        sphereRenderer.draw(sphereRadius, 32, 32);
        GL11.glPopMatrix();

        // Restore texture matrix
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        // --- Render Planetary Ring ---
        // Ring is transparent, so disable depth write and cull face
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_CULL_FACE);

        bindTexture(RING_TEXTURE);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE); // Additive blending for glowing ring

        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        float ringRadius = sphereRadius * 2.5f; // Ring is much larger than the planet
        net.minecraft.client.renderer.Tessellator tessellator = net.minecraft.client.renderer.Tessellator.instance;
        tessellator.startDrawingQuads();

        // Draw flat quad on XZ plane
        tessellator.addVertexWithUV(-ringRadius, 0, -ringRadius, 0.0, 0.0);
        tessellator.addVertexWithUV(-ringRadius, 0, ringRadius, 0.0, 1.0);
        tessellator.addVertexWithUV(ringRadius, 0, ringRadius, 1.0, 1.0);
        tessellator.addVertexWithUV(ringRadius, 0, -ringRadius, 1.0, 0.0);

        tessellator.draw();

        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDepthMask(true);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return STAR_TEXTURE;
    }
}
