//
// Decompiled by Procyon v0.6.0
//

package com.greyhat.dark_grey.client.render;

import java.nio.FloatBuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.IItemRenderer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public class RenderRPGBow implements IItemRenderer {

    public boolean handleRenderType(final ItemStack item, final IItemRenderer.ItemRenderType type) {
        return type == IItemRenderer.ItemRenderType.EQUIPPED
            || type == IItemRenderer.ItemRenderType.EQUIPPED_FIRST_PERSON;
    }

    public boolean shouldUseRenderHelper(final IItemRenderer.ItemRenderType type, final ItemStack item,
        final IItemRenderer.ItemRendererHelper helper) {
        return false;
    }

    public void renderItem(final IItemRenderer.ItemRenderType type, final ItemStack item, final Object... data) {
        if (data.length < 2 || !(data[1] instanceof EntityLivingBase)) {
            return;
        }
        final EntityLivingBase entity = (EntityLivingBase) data[1];
        final boolean detachedFromForgeTransform = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH) > 1;
        FloatBuffer originalMatrix = null;
        if (detachedFromForgeTransform) {
            originalMatrix = BufferUtils.createFloatBuffer(16);
            GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, originalMatrix);
            GL11.glPopMatrix();
        }
        GL11.glPushMatrix();
        try {
            if (type == IItemRenderer.ItemRenderType.EQUIPPED) {
                GL11.glRotatef(-20.0f, 0.0f, 0.0f, 1.0f);
                GL11.glRotatef(90.0f, 1.0f, 0.0f, 0.0f);
                GL11.glRotatef(-60.0f, 0.0f, 0.0f, 1.0f);
                GL11.glScalef(2.6666667f, 2.6666667f, 2.6666667f);
                GL11.glTranslatef(-0.25f, -0.1875f, 0.1875f);
                final float f2 = 0.625f;
                GL11.glTranslatef(0.0f, 0.125f, 0.3125f);
                GL11.glRotatef(-20.0f, 0.0f, 1.0f, 0.0f);
                GL11.glScalef(f2, -f2, f2);
                GL11.glRotatef(-100.0f, 1.0f, 0.0f, 0.0f);
                GL11.glRotatef(45.0f, 0.0f, 1.0f, 0.0f);
            }
            GL11.glTranslatef(0.0f, -0.3f, 0.0f);
            GL11.glScalef(1.5f, 1.5f, 1.5f);
            GL11.glRotatef(50.0f, 0.0f, 1.0f, 0.0f);
            GL11.glRotatef(335.0f, 0.0f, 0.0f, 1.0f);
            GL11.glTranslatef(-0.9375f, -0.0625f, 0.0f);
            final IIcon icon = entity.getItemIcon(item, 0);
            if (icon != null) {
                Minecraft.getMinecraft().renderEngine
                    .bindTexture(Minecraft.getMinecraft().renderEngine.getResourceLocation(item.getItemSpriteNumber()));
                final Tessellator tessellator = Tessellator.instance;
                ItemRenderer.renderItemIn2D(
                    tessellator,
                    icon.getMaxU(),
                    icon.getMinV(),
                    icon.getMinU(),
                    icon.getMaxV(),
                    icon.getIconWidth(),
                    icon.getIconHeight(),
                    0.0625f);
            }
        } finally {
            GL11.glPopMatrix();
            if (detachedFromForgeTransform) {
                GL11.glPushMatrix();
                originalMatrix.rewind();
                GL11.glLoadMatrix(originalMatrix);
            }
        }
    }
}
