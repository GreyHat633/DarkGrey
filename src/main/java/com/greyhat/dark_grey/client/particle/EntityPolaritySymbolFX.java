package com.greyhat.dark_grey.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class EntityPolaritySymbolFX extends EntityFX {

    private String text;

    public EntityPolaritySymbolFX(World world, double x, double y, double z, String text, float r, float g, float b) {
        super(world, x, y, z);
        this.text = text;
        this.particleRed = r;
        this.particleGreen = g;
        this.particleBlue = b;
        this.particleMaxAge = 20 + rand.nextInt(20);
        this.motionX = (rand.nextDouble() - 0.5) * 0.05;
        this.motionY = rand.nextDouble() * 0.05 + 0.02;
        this.motionZ = (rand.nextDouble() - 0.5) * 0.05;
        this.particleScale = 3.0f + rand.nextFloat() * 2.0f;
    }

    @Override
    public void renderParticle(Tessellator tessellator, float partialTicks, float rX, float rZ, float rYZ, float rXY,
        float rXZ) {
        float f = ((float) this.particleAge + partialTicks) / (float) this.particleMaxAge;
        this.particleAlpha = 1.0F - f;

        GL11.glPushMatrix();
        float x = (float) (this.prevPosX + (this.posX - this.prevPosX) * (double) partialTicks - interpPosX);
        float y = (float) (this.prevPosY + (this.posY - this.prevPosY) * (double) partialTicks - interpPosY);
        float z = (float) (this.prevPosZ + (this.posZ - this.prevPosZ) * (double) partialTicks - interpPosZ);
        GL11.glTranslatef(x, y, z);

        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-RenderManager.instance.playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(RenderManager.instance.playerViewX, 1.0F, 0.0F, 0.0F);

        float scale = 0.015f * this.particleScale;
        GL11.glScalef(-scale, -scale, scale);

        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);

        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
        int width = fontRenderer.getStringWidth(this.text);

        int r = (int) (this.particleRed * 255.0f);
        int g = (int) (this.particleGreen * 255.0f);
        int b = (int) (this.particleBlue * 255.0f);
        int a = (int) (this.particleAlpha * 255.0f);
        int color = (a << 24) | (r << 16) | (g << 8) | b;

        fontRenderer.drawString(this.text, -width / 2, 0, color);

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDepthMask(true);
        GL11.glPopMatrix();
    }

    @Override
    public int getFXLayer() {
        return 3; // Custom rendering layer
    }
}
