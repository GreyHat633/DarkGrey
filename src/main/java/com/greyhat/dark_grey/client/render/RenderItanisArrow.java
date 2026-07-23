package com.greyhat.dark_grey.client.render;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.greyhat.dark_grey.entity.EntityItanisArrow;
import com.greyhat.dark_grey.entity.EntityItanisArrow.ArrowState;

public class RenderItanisArrow extends Render {

    private static final ResourceLocation ARROW_TEXTURE = new ResourceLocation(
        "dark_grey",
        "textures/entity/itanis_arrow.png");

    @Override
    public void doRender(Entity entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (!(entity instanceof EntityItanisArrow)) {
            return;
        }

        EntityItanisArrow arrow = (EntityItanisArrow) entity;
        ArrowState state = arrow.getArrowState();

        double horizontalSpeed = Math.sqrt(arrow.motionX * arrow.motionX + arrow.motionZ * arrow.motionZ);
        float yaw;
        float pitch;
        if (state == ArrowState.HOVERING) {
            EntityLivingBase thrower = arrow.getThrower();
            if (thrower != null) {
                yaw = -thrower.rotationYaw;
                pitch = -thrower.rotationPitch;
            } else {
                yaw = -arrow.rotationYaw;
                pitch = -arrow.rotationPitch;
            }
        } else {
            yaw = (float) Math.toDegrees(Math.atan2(arrow.motionX, arrow.motionZ));
            pitch = (float) Math.toDegrees(Math.atan2(arrow.motionY, horizontalSpeed));
        }

        float scale = state == ArrowState.PIERCING ? 2.5F : (state == ArrowState.HOVERING ? 1.2F : 1.5F);

        GL11.glPushMatrix();
        GL11.glPushAttrib(
            GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_CURRENT_BIT);
        try {
            GL11.glTranslated(x, y, z);
            GL11.glRotatef(yaw, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(-pitch, 1.0F, 0.0F, 0.0F);

            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glDepthMask(false);

            if (this.renderManager.renderEngine != null && ARROW_TEXTURE != null) {
                this.bindTexture(ARROW_TEXTURE);
            }

            if (state == ArrowState.PIERCING) {
                // Golden Radiant Beam Rendering for Full-Charge Piercing Arrow
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                GL11.glColor4f(1.0F, 0.85F, 0.2F, 0.6F);
                drawTexturedArrow(scale * 1.3F);

                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.95F);
                drawTexturedArrow(scale);
            } else if (state == ArrowState.HOVERING) {
                // Gold Floating Arrow Rendering
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                GL11.glColor4f(1.0F, 0.85F, 0.2F, 0.4F);
                drawTexturedArrow(scale * 1.2F);

                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                drawTexturedArrow(scale);
            } else {
                // Homing Launched Arrow Rendering
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                GL11.glColor4f(1.0F, 0.9F, 0.3F, 0.35F);
                drawTexturedArrow(scale * 1.15F);

                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                drawTexturedArrow(scale);
            }
        } finally {
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }

    private static void drawTexturedArrow(float scale) {
        Tessellator tessellator = Tessellator.instance;
        float size = scale * 0.8F; // Slightly larger to compensate for diagonal mapping

        tessellator.startDrawingQuads();
        
        // Horizontal Quad (XZ plane) - Diamond mapping to align diagonal texture with Z-axis
        tessellator.addVertexWithUV(-size, 0.0,  0.0,  0.0, 0.0); // Top-Left
        tessellator.addVertexWithUV( 0.0,  0.0, -size, 0.0, 1.0); // Bottom-Left (Tail)
        tessellator.addVertexWithUV( size, 0.0,  0.0,  1.0, 1.0); // Bottom-Right
        tessellator.addVertexWithUV( 0.0,  0.0,  size, 1.0, 0.0); // Top-Right (Tip)

        // Vertical Quad (YZ plane) - Diamond mapping
        tessellator.addVertexWithUV(0.0,  size,  0.0,  0.0, 0.0); // Top-Left
        tessellator.addVertexWithUV(0.0,  0.0,  -size, 0.0, 1.0); // Bottom-Left (Tail)
        tessellator.addVertexWithUV(0.0, -size,  0.0,  1.0, 1.0); // Bottom-Right
        tessellator.addVertexWithUV(0.0,  0.0,   size, 1.0, 0.0); // Top-Right (Tip)
        
        tessellator.draw();
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return ARROW_TEXTURE;
    }
}
