package com.greyhat.dark_grey.item;

import java.util.List;

import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;

import com.greyhat.dark_grey.api.IRPGComponent;

public class ItemRPGLance extends ItemRPGWeapon {

    public ItemRPGLance(String name, ToolMaterial material, List<IRPGComponent> components) {
        super(name, material, components);
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.none; // Lances do not block when right-clicked
    }
}
