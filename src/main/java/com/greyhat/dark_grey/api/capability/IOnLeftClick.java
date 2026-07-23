package com.greyhat.dark_grey.api.capability;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * Capability interface for RPG components that respond to item left-click input.
 */
public interface IOnLeftClick {

    /**
     * Called when the player left-clicks with the item holding this component.
     *
     * @param stack  The item stack currently held
     * @param player The player performing the left-click
     * @return true if the event was handled and standard attack/dig should be canceled if possible
     */
    boolean onLeftClick(ItemStack stack, EntityPlayer player);
}
