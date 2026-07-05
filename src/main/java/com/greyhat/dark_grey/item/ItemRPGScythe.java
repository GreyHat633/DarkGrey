package com.greyhat.dark_grey.item;

import com.greyhat.dark_grey.api.IRPGComponent;
import net.minecraft.item.Item;
import java.util.List;

public class ItemRPGScythe extends ItemRPGWeapon {
    public ItemRPGScythe(String itemId, Item.ToolMaterial material, List<IRPGComponent> components) {
        super(itemId, material, components);
    }
}