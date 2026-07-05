package com.greyhat.dark_grey.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class RPGItemRenderer implements IItemRenderer {

    private final String equippedTextureName;

    public RPGItemRenderer(String equippedTextureName) {
        this.equippedTextureName = equippedTextureName;
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        // Handle third-person and first-person equipped rendering
        return type == ItemRenderType.EQUIPPED || type == ItemRenderType.EQUIPPED_FIRST_PERSON;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return false;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        GL11.glPushMatrix();

        // Use the explicitly provided equipped texture name
        ResourceLocation equippedTex = new ResourceLocation("dark_grey", "textures/items/" + this.equippedTextureName + ".png");
        Minecraft.getMinecraft().getTextureManager().bindTexture(equippedTex);

        Tessellator tessellator = Tessellator.instance;
        
        // Setup state for ItemRenderer.renderItemIn2D
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        
        // UV coordinates for a full, non-atlased texture
        float maxU = 1.0F;
        float minU = 0.0F;
        float maxV = 1.0F;
        float minV = 0.0F;
        
        // The texture width/height (assuming 64x64 for the equipped texture)
        int texW = 64;
        int texH = 64;
        
        // Thickness of the item
        float thickness = 0.0625F;
        
        ItemRenderer.renderItemIn2D(tessellator, maxU, minV, minU, maxV, texW, texH, thickness);
        
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        GL11.glPopMatrix();
    }
}