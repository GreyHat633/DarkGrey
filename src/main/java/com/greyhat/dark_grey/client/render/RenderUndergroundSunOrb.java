package com.greyhat.dark_grey.client.render;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.greyhat.dark_grey.DarkGrey;

public class RenderUndergroundSunOrb extends Render {

    private static final ResourceLocation ORB_TEXTURE = new ResourceLocation(
        DarkGrey.MODID,
        "textures/items/underground_sun.png");

    @Override
    public void doRender(Entity entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (!(entity instanceof com.greyhat.dark_grey.entity.EntityUndergroundSunOrb)) return;
        com.greyhat.dark_grey.entity.EntityUndergroundSunOrb orb = (com.greyhat.dark_grey.entity.EntityUndergroundSunOrb) entity;

        double renderX = x;
        double renderY = y + 0.3D;
        double renderZ = z;

        if (orb.getOrbState() == com.greyhat.dark_grey.entity.EntityUndergroundSunOrb.STATE_FOLLOWING) {
            Entity owner = orb.getOwnerEntity();
            if (owner != null) {
                long time = orb.worldObj.getTotalWorldTime();
                float angle = ((time + partialTicks) * orb.getOrbitSpeed()) + (orb.getFormationSlot() * 120.0F);
                double rad = Math.toRadians(angle);

                double offsetX = Math.cos(rad) * orb.getOrbitRadius();
                double offsetZ = Math.sin(rad) * orb.getOrbitRadius();
                double offsetY = orb.getOrbitHeight() + Math.sin((time + partialTicks) * 0.1) * 0.2;

                double interpOwnerX = owner.lastTickPosX + (owner.posX - owner.lastTickPosX) * partialTicks;
                double interpOwnerY = owner.lastTickPosY + (owner.posY - owner.lastTickPosY) * partialTicks;
                double interpOwnerZ = owner.lastTickPosZ + (owner.posZ - owner.lastTickPosZ) * partialTicks;

                renderX = interpOwnerX - this.renderManager.renderPosX + offsetX;
                renderY = interpOwnerY - this.renderManager.renderPosY + offsetY;
                renderZ = interpOwnerZ - this.renderManager.renderPosZ + offsetZ;
            }
        }

        GL11.glPushMatrix();
        GL11.glTranslated(renderX, renderY, renderZ);

        GL11.glEnable(GL12.GL_RESCALE_NORMAL);

        float scale = 0.5F;
        GL11.glScalef(scale, scale, scale);

        this.bindEntityTexture(entity);

        Tessellator tessellator = Tessellator.instance;

        // Billboard rotation
        GL11.glRotatef(180.0F - this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-this.renderManager.playerViewX, 1.0F, 0.0F, 0.0F);

        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);

        long time = entity.worldObj.getTotalWorldTime();
        float rotation = (time + partialTicks) * 5.0F;
        GL11.glRotatef(rotation, 0.0F, 0.0F, 1.0F);

        tessellator.startDrawingQuads();
        tessellator.setNormal(0.0F, 1.0F, 0.0F);
        tessellator.addVertexWithUV(-0.5D, -0.5D, 0.0D, 0.0D, 1.0D);
        tessellator.addVertexWithUV(0.5D, -0.5D, 0.0D, 1.0D, 1.0D);
        tessellator.addVertexWithUV(0.5D, 0.5D, 0.0D, 1.0D, 0.0D);
        tessellator.addVertexWithUV(-0.5D, 0.5D, 0.0D, 0.0D, 0.0D);
        tessellator.draw();

        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        GL11.glPopMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return ORB_TEXTURE;
    }
}
