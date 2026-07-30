package com.greyhat.dark_grey.client.render;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class RenderBoneMarrowProjectile extends Render {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
        "dark_grey",
        "textures/items/hardened_bone_marrow.png");

    public RenderBoneMarrowProjectile() {}

    @Override
    public void doRender(Entity entity, double x, double y, double z, float entityYaw, float partialTicks) {
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x, (float) y, (float) z);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);

        // Billboard setup - face the player
        GL11.glRotatef(180.0F - this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-this.renderManager.playerViewX, 1.0F, 0.0F, 0.0F);

        // Adjust scale (Reduced size)
        float scale = 1.5F;
        GL11.glScalef(scale, scale, scale);

        this.bindEntityTexture(entity);
        Tessellator tessellator = Tessellator.instance;

        tessellator.startDrawingQuads();
        tessellator.setNormal(0.0F, 1.0F, 0.0F);
        tessellator.addVertexWithUV(-0.5D, -0.25D, 0.0D, 0.0D, 1.0D);
        tessellator.addVertexWithUV(0.5D, -0.25D, 0.0D, 1.0D, 1.0D);
        tessellator.addVertexWithUV(0.5D, 0.75D, 0.0D, 1.0D, 0.0D);
        tessellator.addVertexWithUV(-0.5D, 0.75D, 0.0D, 0.0D, 0.0D);
        tessellator.draw();

        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        GL11.glPopMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return TEXTURE;
    }
}
