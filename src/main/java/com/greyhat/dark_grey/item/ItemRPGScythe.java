package com.greyhat.dark_grey.item;

import java.util.List;

import net.minecraft.item.Item;

import com.greyhat.dark_grey.api.IRPGComponent;

public class ItemRPGScythe extends ItemRPGWeapon {

    public ItemRPGScythe(String itemId, Item.ToolMaterial material, List<IRPGComponent> components) {
        super(itemId, material, components);
    }
}
