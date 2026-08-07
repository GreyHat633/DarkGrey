package com.greyhat.dark_grey.client.render;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderSunflameBullet extends Render {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
        "dark_grey",
        "textures/items/sunflame_round.png");

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x, (float) y, (float) z);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);

        // Point forward using yaw and pitch
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

        float scale = 0.5F;
        GL11.glScalef(scale, scale, scale);

        this.bindEntityTexture(entity);

        Tessellator tessellator = Tessellator.instance;

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_LIGHTING);

        float minU = 0.0F;
        float maxU = 1.0F;
        float minV = 0.0F;
        float maxV = 1.0F;

        for (int i = 0; i < 4; ++i) {
            GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
            GL11.glNormal3f(0.0F, 0.0F, 1.0F);
            tessellator.startDrawingQuads();
            tessellator.addVertexWithUV(-0.5D, -0.5D, 0.0D, minU, maxV);
            tessellator.addVertexWithUV(0.5D, -0.5D, 0.0D, maxU, maxV);
            tessellator.addVertexWithUV(0.5D, 0.5D, 0.0D, maxU, minV);
            tessellator.addVertexWithUV(-0.5D, 0.5D, 0.0D, minU, minV);
            tessellator.draw();
        }

        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        GL11.glPopMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return TEXTURE;
    }
}
