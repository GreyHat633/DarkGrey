package com.greyhat.dark_grey.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class RenderScytheWeapon implements IItemRenderer {

    private final String equippedTextureName;
    private final ResourceLocation equippedTex;

    public RenderScytheWeapon(String equippedTextureName) {
        this.equippedTextureName = equippedTextureName;
        this.equippedTex = new ResourceLocation("dark_grey", "textures/items/" + this.equippedTextureName + ".png");
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return type == ItemRenderType.EQUIPPED || type == ItemRenderType.EQUIPPED_FIRST_PERSON;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return false;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        GL11.glPushMatrix();

        if (type == ItemRenderType.EQUIPPED) {
            // Third-person adjustments
            // Translate the pivot point so the handle is in the player's hand
            GL11.glTranslatef(0.2F, -0.55F, 0.1F);

            // Enlarge the scythe
            GL11.glScalef(2.2F, 2.2F, 2.2F);

            // Rotate 90 degrees around the Z axis to make the handle perpendicular to the arm
            GL11.glRotatef(90.0F, 0.0F, 0.0F, 1.0F);

            // local X made larger (from -0.5F to 0.1F), local Y adjusted downwards (from -0.8F to -1.1F)
            GL11.glTranslatef(0.1F, -0.55F, 0.0F);

        } else if (type == ItemRenderType.EQUIPPED_FIRST_PERSON) {
            // First-person adjustments
            // Translate to push it forward (up and left on screen): Y is larger (0.45F), X is smaller (0.3F), Z is 0.0F
            GL11.glTranslatef(0.3F, 0.45F, -0.3F);

            // Enlarge the scythe
            GL11.glScalef(1.5F, 1.5F, 1.5F);

            // Keep the same correct angle
            GL11.glRotatef(45.0F, 0.0F, 0.0F, 1.0F);

            // Shift the rotated item to align the handle
            GL11.glTranslatef(-0.5F, -0.3F, 0.0F);
        }

        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(this.equippedTex);

        Tessellator tessellator = Tessellator.instance;
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);

        ItemRenderer.renderItemIn2D(tessellator, 1.0F, 0.0F, 0.0F, 1.0F, 64, 64, 0.0625F);

        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        GL11.glPopMatrix();
    }
}
