package com.greyhat.dark_grey.client.render;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.greyhat.dark_grey.entity.EntityMadokaArrow;

public class RenderMadokaArrow extends Render {

    @Override
    public void doRender(Entity entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (!(entity instanceof EntityMadokaArrow)) {
            return;
        }

        EntityMadokaArrow arrow = (EntityMadokaArrow) entity;
        double horizontalSpeed = Math.sqrt(arrow.motionX * arrow.motionX + arrow.motionZ * arrow.motionZ);
        float yaw = (float) Math.toDegrees(Math.atan2(arrow.motionX, arrow.motionZ));
        float pitch = (float) Math.toDegrees(Math.atan2(arrow.motionY, horizontalSpeed));
        float length = arrow.isVolleyArrow() ? 2.15f : 1.45f;

        GL11.glPushMatrix();
        GL11.glPushAttrib(
            GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_CURRENT_BIT);
        try {
            GL11.glTranslated(x, y, z);
            GL11.glRotatef(yaw, 0.0f, 1.0f, 0.0f);
            GL11.glRotatef(-pitch, 1.0f, 0.0f, 0.0f);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glDepthMask(false);

            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GL11.glColor4f(1.0f, 0.35f, 0.78f, 0.28f);
            drawCrossedBolt(length, 0.22f);

            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(1.0f, 0.92f, 1.0f, 0.95f);
            drawCrossedBolt(length, arrow.isVolleyArrow() ? 0.095f : 0.075f);
        } finally {
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }

    private static void drawCrossedBolt(float length, float halfWidth) {
        Tessellator tessellator = Tessellator.instance;
        float tail = -length * 0.72f;
        float tip = length * 0.28f;

        tessellator.startDrawingQuads();
        tessellator.addVertex(-halfWidth, 0.0, tail);
        tessellator.addVertex(halfWidth, 0.0, tail);
        tessellator.addVertex(halfWidth * 0.35f, 0.0, tip);
        tessellator.addVertex(-halfWidth * 0.35f, 0.0, tip);

        tessellator.addVertex(0.0, -halfWidth, tail);
        tessellator.addVertex(0.0, halfWidth, tail);
        tessellator.addVertex(0.0, halfWidth * 0.35f, tip);
        tessellator.addVertex(0.0, -halfWidth * 0.35f, tip);
        tessellator.draw();
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return null;
    }
}
