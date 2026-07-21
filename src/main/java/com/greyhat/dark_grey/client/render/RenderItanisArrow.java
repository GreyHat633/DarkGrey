package com.greyhat.dark_grey.client.render;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class RenderItanisArrow extends Render {

    private static final ResourceLocation arrowTextures = new ResourceLocation(
        "dark_grey:textures/entity/itanis_arrow.png");

    @Override
    public void doRender(Entity entity, double x, double y, double z, float entityYaw, float partialTicks) {
        this.bindEntityTexture(entity);
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef((float) x, (float) y, (float) z);
            GL11.glRotatef(
                entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks - 90.0F,
                0.0F,
                1.0F,
                0.0F);
            GL11.glRotatef(
                entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks,
                0.0F,
                0.0F,
                1.0F);
            GL11.glRotatef(45.0F, 0.0F, 0.0F, 1.0F);

            Tessellator tessellator = Tessellator.instance;
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            GL11.glScalef(1.0F, 1.0F, 1.0F);

            for (int i = 0; i < 2; ++i) {
                GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
                GL11.glNormal3f(0.0F, 0.0F, 1.0F);
                tessellator.startDrawingQuads();
                tessellator.addVertexWithUV(-0.5D, -0.5D, 0.0D, 0.0D, 1.0D);
                tessellator.addVertexWithUV(0.5D, -0.5D, 0.0D, 1.0D, 1.0D);
                tessellator.addVertexWithUV(0.5D, 0.5D, 0.0D, 1.0D, 0.0D);
                tessellator.addVertexWithUV(-0.5D, 0.5D, 0.0D, 0.0D, 0.0D);
                tessellator.draw();
            }
        } finally {
            GL11.glDisable(GL12.GL_RESCALE_NORMAL);
            GL11.glPopMatrix();
        }
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return arrowTextures;
    }
}
