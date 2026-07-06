package com.greyhat.dark_grey.api.capability;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Capability interface for components that perform periodic actions while armor is worn.
 *
 * <p>
 * Triggered by {@code ItemArmor.onArmorTick()}, but <b>throttled by the container</b>
 * to fire only once per second (every 20 game ticks) to prevent tick storms.
 * </p>
 *
 * <p>
 * In Godot terms, this is equivalent to using a {@code Timer} node with a 1-second
 * interval attached to your armor node, rather than running logic every {@code _process()} frame.
 * </p>
 *
 * <p>
 * All calls happen server-side only ({@code !world.isRemote}).
 * </p>
 */
public interface IOnWornTick {

    /**
     * Called once per second while this armor piece is worn by a player.
     *
     * @param world      the world instance (always server-side)
     * @param player     the player wearing the armor
     * @param armorPiece the ItemStack of the armor piece
     */
    void onWornTick(World world, EntityPlayer player, ItemStack armorPiece);
}
