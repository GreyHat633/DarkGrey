package com.greyhat.dark_grey.item;

import java.util.List;

import com.greyhat.dark_grey.api.IRPGComponent;

/**
 * Wand class. Currently inherits identical mechanics from ItemRPGWeapon,
 * but allows for specialized "wand-only" logic or components in the future.
 */
public class ItemRPGWand extends ItemRPGWeapon {

    public ItemRPGWand(String id, ToolMaterial material, List<IRPGComponent> components) {
        super(id, material, components);
    }

}
