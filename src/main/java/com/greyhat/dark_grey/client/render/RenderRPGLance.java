package com.greyhat.dark_grey.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class RenderRPGLance implements IItemRenderer {

    private final String equippedTextureName;

    public RenderRPGLance(String equippedTextureName) {
        this.equippedTextureName = equippedTextureName;
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

        ResourceLocation equippedTex = new ResourceLocation("dark_grey", "textures/items/" + this.equippedTextureName + ".png");
        Minecraft.getMinecraft().getTextureManager().bindTexture(equippedTex);

        Tessellator tessellator = Tessellator.instance;
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);

        if (type == ItemRenderType.EQUIPPED) {
            // Scale up 1.5x in third person
            GL11.glScalef(1.5F, 1.5F, 1.5F);
            // Translate slightly to make it fit in hand nicely after scaling
            GL11.glTranslatef(-0.1F, -0.15F, 0.0F); 
        }

        float maxU = 1.0F;
        float minU = 0.0F;
        float maxV = 1.0F;
        float minV = 0.0F;
        
        // Use 256x256 to fix jagged/pixelated edges when extruding the 3D model!
        int texW = 256;
        int texH = 256;
        
        float thickness = 0.0625F;
        
        ItemRenderer.renderItemIn2D(tessellator, maxU, minV, minU, maxV, texW, texH, thickness);
        
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        GL11.glPopMatrix();
    }
}
