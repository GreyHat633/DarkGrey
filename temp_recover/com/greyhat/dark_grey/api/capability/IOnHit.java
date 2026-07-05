package com.greyhat.dark_grey.api.capability;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

/**
 * Capability interface for components that react to a weapon hitting an entity.
 *
 * <p>Triggered inside {@code ItemSword.hitEntity()} for weapons, and
 * {@code ItemTool.hitEntity()} for tools used as weapons (e.g. axes).</p>
 *
 * <p>The {@code actualDamage} parameter is obtained from {@code target.lastDamage},
 * giving the real post-armor, post-enchantment damage that the target received.
 * This fixes the critical design flaw identified in the framework review.</p>
 */
public interface IOnHit {

    /**
     * Called when the item holder successfully hits a living entity.
     *
     * @param weaponStack  the ItemStack of the weapon used
     * @param attacker     the entity wielding the weapon
     * @param target       the entity that was hit
     * @param actualDamage the actual damage dealt after armor and resistance calculations
     */
    void onHit(ItemStack weaponStack, EntityLivingBase attacker,
               EntityLivingBase target, float actualDamage);
}