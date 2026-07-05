package com.greyhat.dark_grey.client.render;

import net.minecraftforge.client.IItemRenderer;
import net.minecraft.item.ItemStack;

public class RenderRPGBow implements IItemRenderer {
    @Override public boolean handleRenderType(ItemStack item, ItemRenderType type) { return false; }
    @Override public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) { return false; }
    @Override public void renderItem(ItemRenderType type, ItemStack item, Object... data) {}
}