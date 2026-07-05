package com.greyhat.dark_grey.api.capability;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Capability interface for components that react when armor is unequipped.
 *
 * <p>Counterpart to {@link IOnEquip}. Detected by the same {@code PlayerTickEvent}
 * comparison mechanism in {@code RPGCoreEventHandler}.</p>
 *
 * <p>In Godot terms, this is like connecting to the {@code tree_exited} signal —
 * it fires once when the node (armor piece) leaves the scene tree (player's armor slot).
 * This reliably handles all removal scenarios: manual unequip, death drops, inventory swaps.</p>
 */
public interface IOnUnequip {

    /**
     * Called once when this armor piece is removed from a player's armor slot.
     *
     * @param world      the world instance (always server-side)
     * @param wearer     the entity that unequipped the armor
     * @param armorPiece the ItemStack of the unequipped armor piece
     */
    void onUnequip(World world, EntityLivingBase wearer, ItemStack armorPiece);
}