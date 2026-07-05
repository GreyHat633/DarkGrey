package com.greyhat.dark_grey.api.capability;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * Capability interface for RPG components that trigger continuously
 * while the player is holding right-click to "use" or "charge" a weapon.
 */
public interface IOnWeaponUsingTick {
    /**
     * Called every tick while the item is in use.
     *
     * @param weaponStack the item stack being used
     * @param player      the player using the item
     * @param count       the remaining use duration ticks (starts at max and counts down)
     */
    void onUsingTick(ItemStack weaponStack, EntityPlayer player, int count);
}