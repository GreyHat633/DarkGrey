package com.greyhat.dark_grey.api.capability;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;

/**
 * Capability interface for components that react when the player dies
 * while holding or wearing the RPG item.
 *
 * <p>Triggered by {@code LivingDeathEvent} on the Forge EventBus.
 * Useful for soul-bound items, death prevention mechanics, or loot protection.</p>
 *
 * <p>In Godot terms, this is like connecting to a global {@code player_died} signal
 * that allows each equipped item's script to run cleanup or revival logic.</p>
 */
public interface IOnPlayerDeath {

    /**
     * Called when the player dies while this item is equipped or held.
     *
     * @param itemStack   the ItemStack of the RPG item
     * @param player      the player who died
     * @param deathCause  the source of damage that killed the player
     */
    void onPlayerDeath(ItemStack itemStack, EntityPlayer player, DamageSource deathCause);
}