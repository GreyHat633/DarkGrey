package com.greyhat.dark_grey.api.capability;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Capability interface for components that react to a right-click (use in air).
 *
 * <p>
 * Triggered by {@code Item.onItemRightClick()}. Multiple components are chained:
 * the returned {@code ItemStack} from one component becomes the input for the next.
 * </p>
 *
 * <p>
 * In Godot terms, this is like a chain of {@code _unhandled_input()} handlers
 * where each handler can transform the event before passing it along.
 * </p>
 */
public interface IOnRightClick {

    /**
     * Called when the player right-clicks with this item in hand (not on a block).
     *
     * @param itemStack the current ItemStack (may have been modified by earlier components)
     * @param world     the world instance
     * @param player    the player using the item
     * @return the resulting ItemStack after this component's processing
     */
    ItemStack onRightClick(ItemStack itemStack, World world, EntityPlayer player);
}
