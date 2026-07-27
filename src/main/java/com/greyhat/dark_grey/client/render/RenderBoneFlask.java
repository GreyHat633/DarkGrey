package com.greyhat.dark_grey.client.render;

import net.minecraft.client.renderer.entity.RenderSnowball;
import net.minecraft.item.Item;

import com.greyhat.dark_grey.DarkGrey;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderBoneFlask extends RenderSnowball {

    private static Item boneFlaskItem;

    public RenderBoneFlask() {
        super(getFlaskItem(), 0);
    }

    private static Item getFlaskItem() {
        if (boneFlaskItem == null) {
            boneFlaskItem = GameRegistry.findItem(DarkGrey.MODID, "bone_flask");
        }
        return boneFlaskItem;
    }
}
