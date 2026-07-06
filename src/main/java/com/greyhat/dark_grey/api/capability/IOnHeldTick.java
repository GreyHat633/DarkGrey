package com.greyhat.dark_grey.api.capability;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Capability interface for RPG components that trigger continuously
 * while the player is actively holding the weapon.
 */
public interface IOnHeldTick {

    /**
     * Called every tick while the item is held by the player.
     *
     * @param weaponStack the item stack being held
     * @param world       the world the player is in
     * @param player      the player holding the item
     */
    void onHeldTick(ItemStack weaponStack, World world, EntityPlayer player);
}
