package com.greyhat.dark_grey.common;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

import com.greyhat.dark_grey.DarkGrey;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class DarkGreyTab extends CreativeTabs {

    public DarkGreyTab(String label) {
        super(label);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Item getTabIconItem() {
        // Return the custom item we created specifically for the icon
        return DarkGrey.tabIcon;
    }
}
