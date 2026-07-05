package com.greyhat.dark_grey.api.capability;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Capability interface for components that trigger when a player stops
 * using (charging) an item.
 */
public interface IOnPlayerStoppedUsing {
    /**
     * Called when the player releases right-click after charging.
     *
     * @param stack          the item stack
     * @param world          the world
     * @param player         the player
     * @param itemInUseCount the remaining use duration ticks when released
     */
    void onPlayerStoppedUsing(ItemStack stack, World world, EntityPlayer player, int itemInUseCount);
}