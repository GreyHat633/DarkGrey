package com.greyhat.dark_grey.client.render;

import net.minecraft.client.renderer.entity.RenderSnowball;
import net.minecraft.item.Item;

import com.greyhat.dark_grey.DarkGrey;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderCorruptionBomb extends RenderSnowball {

    private static Item corruptionBombItem;

    public RenderCorruptionBomb() {
        super(getBombItem(), 0);
    }

    private static Item getBombItem() {
        if (corruptionBombItem == null) {
            corruptionBombItem = GameRegistry.findItem(DarkGrey.MODID, "corruption_bomb");
        }
        return corruptionBombItem;
    }
}
