package com.greyhat.dark_grey.client.render;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.greyhat.dark_grey.entity.EntityBoneMeteor;

public class RenderBoneMeteor extends Render {

    private static final ResourceLocation METEOR_TEXTURE = new ResourceLocation(
        "dark_grey:textures/items/hardened_bone_marrow.png");

    public RenderBoneMeteor() {
        this.shadowSize = 0.5F;
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, float entityYaw, float partialTicks) {
        EntityBoneMeteor meteor = (EntityBoneMeteor) entity;

        GL11.glPushMatrix();
        GL11.glTranslatef((float) x, (float) y, (float) z);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);

        // Billboard effect
        GL11.glRotatef(180.0F - this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-this.renderManager.playerViewX, 1.0F, 0.0F, 0.0F);

        // Spin effect
        float spin = (meteor.ticksExisted + partialTicks) * 20.0F;
        GL11.glRotatef(spin, 0.0F, 0.0F, 1.0F);

        float scale = 4.0F; // Make it much bigger
        GL11.glScalef(scale, scale, scale);

        this.bindEntityTexture(meteor);

        Tessellator tessellator = Tessellator.instance;
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        float f = 0.0F;
        float f1 = 1.0F;
        float f2 = 0.0F;
        float f3 = 1.0F;

        float f4 = 1.0F;
        float f5 = 0.5F;
        float f6 = 0.25F;

        tessellator.startDrawingQuads();
        tessellator.setNormal(0.0F, 1.0F, 0.0F);
        tessellator.addVertexWithUV((double) (0.0F - f5), (double) (0.0F - f6), 0.0D, (double) f, (double) f3);
        tessellator.addVertexWithUV((double) (f4 - f5), (double) (0.0F - f6), 0.0D, (double) f1, (double) f3);
        tessellator.addVertexWithUV((double) (f4 - f5), (double) (1.0F - f6), 0.0D, (double) f1, (double) f2);
        tessellator.addVertexWithUV((double) (0.0F - f5), (double) (1.0F - f6), 0.0D, (double) f, (double) f2);
        tessellator.draw();

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        GL11.glPopMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity p_110775_1_) {
        return METEOR_TEXTURE;
    }
}
