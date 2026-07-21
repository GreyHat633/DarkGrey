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
    private final int frames;
    private final int frameTimeMs;

    public AnimatedRPGItemRenderer(String equippedTextureName, int frames, int frameTimeMs) {
        this.equippedTex = new ResourceLocation("dark_grey", "textures/items/" + equippedTextureName + ".png");
        this.frames = frames;
        this.frameTimeMs = frameTimeMs;
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

        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(this.equippedTex);

        Tessellator tessellator = Tessellator.instance;

        GL11.glEnable(GL12.GL_RESCALE_NORMAL);

        long time = Minecraft.getSystemTime();
        int currentFrame = (int) ((time / frameTimeMs) % frames);

        float minU = 0.0F;
        float maxU = 1.0F;

        float frameHeight = 1.0F / frames;
        float minV = currentFrame * frameHeight;
        float maxV = minV + frameHeight;

        int texW = 256;
        int texH = 256;

        float thickness = 0.0625F;

        ItemRenderer.renderItemIn2D(tessellator, maxU, minV, minU, maxV, texW, texH, thickness);

        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        GL11.glPopMatrix();
    }
}
