package com.greyhat.dark_grey.api.capability;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;

/**
 * Capability for components that modify a direct melee hit before Minecraft
 * applies armor, potion resistance and absorption.
 *
 * <p>
 * Implementations must only return a new damage value. They must not call
 * {@code attackEntityFrom} again, otherwise the complete Forge/KCauldron damage
 * pipeline is entered recursively for the same swing.
 * </p>
 */
public interface IModifyMeleeDamage {

    float modifyMeleeDamage(ItemStack itemStack, EntityLivingBase attacker, EntityLivingBase target,
        DamageSource source, float currentDamage);

    boolean bypassesArmor();

    boolean isAbsoluteDamage();
}
