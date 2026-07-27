package com.greyhat.dark_grey.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class AnimatedRPGItemRenderer implements IItemRenderer {

    private final ResourceLocation equippedTex;
    private final ResourceLocation inventoryTex;
    private final int frames;
    private final int frameTimeMs;

    public AnimatedRPGItemRenderer(String equippedTextureName, int frames, int frameTimeMs) {
        this.equippedTex = new ResourceLocation("dark_grey", "textures/items/" + equippedTextureName + ".png");
        String inventoryTextureName = equippedTextureName.endsWith("_equipped")
            ? equippedTextureName.substring(0, equippedTextureName.length() - "_equipped".length())
            : equippedTextureName;
        this.inventoryTex = new ResourceLocation("dark_grey", "textures/items/" + inventoryTextureName + ".png");
        this.frames = frames;
        this.frameTimeMs = frameTimeMs;
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return type == ItemRenderType.ENTITY || type == ItemRenderType.INVENTORY
            || type == ItemRenderType.EQUIPPED
            || type == ItemRenderType.EQUIPPED_FIRST_PERSON;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return false;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        long time = Minecraft.getSystemTime();
        int currentFrame = (int) ((time / this.frameTimeMs) % this.frames);
        float frameHeight = 1.0F / this.frames;
        float minV = currentFrame * frameHeight;
        float maxV = minV + frameHeight;

        if (type == ItemRenderType.INVENTORY) {
            this.renderInventoryIcon(minV, maxV);
            return;
        }
        if (type == ItemRenderType.ENTITY) {
            this.renderDroppedItem(minV, maxV);
            return;
        }

        GL11.glPushMatrix();

        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(this.equippedTex);

        Tessellator tessellator = Tessellator.instance;

        GL11.glEnable(GL12.GL_RESCALE_NORMAL);

        float minU = 0.0F;
        float maxU = 1.0F;

        int texW = 256;
        int texH = 256;

        float thickness = 0.0625F;

        ItemRenderer.renderItemIn2D(tessellator, maxU, minV, minU, maxV, texW, texH, thickness);

        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        GL11.glPopMatrix();
    }

    private void renderInventoryIcon(float minV, float maxV) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            Minecraft.getMinecraft()
                .getTextureManager()
                .bindTexture(this.inventoryTex);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

            Tessellator tessellator = Tessellator.instance;
            tessellator.startDrawingQuads();
            tessellator.addVertexWithUV(0.0D, 16.0D, 0.0D, 0.0D, maxV);
            tessellator.addVertexWithUV(16.0D, 16.0D, 0.0D, 1.0D, maxV);
            tessellator.addVertexWithUV(16.0D, 0.0D, 0.0D, 1.0D, minV);
            tessellator.addVertexWithUV(0.0D, 0.0D, 0.0D, 0.0D, minV);
            tessellator.draw();
        } finally {
            GL11.glPopAttrib();
        }
    }

    private void renderDroppedItem(float minV, float maxV) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        try {
            Minecraft.getMinecraft()
                .getTextureManager()
                .bindTexture(this.inventoryTex);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            ItemRenderer.renderItemIn2D(Tessellator.instance, 1.0F, minV, 0.0F, maxV, 32, 32, 0.0625F);
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }
}
