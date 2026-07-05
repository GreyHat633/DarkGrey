package com.greyhat.dark_grey.api.capability;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Capability interface for components that react when armor is equipped.
 *
 * <p>Since 1.7.10's {@code ItemArmor} has no {@code onEquip()} callback,
 * this event is detected by {@code RPGCoreEventHandler} via {@code PlayerTickEvent}.
 * The handler compares each armor slot against the previous tick's snapshot to
 * detect changes.</p>
 *
 * <p>In Godot terms, this is like connecting to the {@code tree_entered} signal —
 * it fires once when the node (armor piece) joins the scene tree (player's armor slot).</p>
 */
public interface IOnEquip {

    /**
     * Called once when this armor piece is equipped into a player's armor slot.
     *
     * @param world      the world instance (always server-side)
     * @param wearer     the entity that equipped the armor
     * @param armorPiece the ItemStack of the equipped armor piece
     */
    void onEquip(World world, EntityLivingBase wearer, ItemStack armorPiece);
}