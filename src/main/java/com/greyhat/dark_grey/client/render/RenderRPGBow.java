// 
// Decompiled by Procyon v0.6.0
// 

package com.greyhat.dark_grey.client.render;

import net.minecraft.util.IIcon;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;

public class RenderRPGBow implements IItemRenderer
{
    public boolean handleRenderType(final ItemStack item, final IItemRenderer.ItemRenderType type) {
        return type == IItemRenderer.ItemRenderType.EQUIPPED || type == IItemRenderer.ItemRenderType.EQUIPPED_FIRST_PERSON;
    }
    
    public boolean shouldUseRenderHelper(final IItemRenderer.ItemRenderType type, final ItemStack item, final IItemRenderer.ItemRendererHelper helper) {
        return false;
    }
    
    public void renderItem(final IItemRenderer.ItemRenderType type, final ItemStack item, final Object... data) {
        final EntityLivingBase entity = (EntityLivingBase)data[1];
        GL11.glPopMatrix();
        GL11.glPushMatrix();
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
            Minecraft.getMinecraft().renderEngine.bindTexture(Minecraft.getMinecraft().renderEngine.getResourceLocation(item.getItemSpriteNumber()));
            final Tessellator tessellator = Tessellator.instance;
            ItemRenderer.renderItemIn2D(tessellator, icon.getMaxU(), icon.getMinV(), icon.getMinU(), icon.getMaxV(), icon.getIconWidth(), icon.getIconHeight(), 0.0625f);
        }
        GL11.glPopMatrix();
        GL11.glPushMatrix();
    }
}
