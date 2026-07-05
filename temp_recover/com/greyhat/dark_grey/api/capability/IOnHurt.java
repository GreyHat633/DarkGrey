package com.greyhat.dark_grey.api.capability;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;

/**
 * Capability interface for components that react to the wearer receiving damage.
 *
 * <p>Triggered by {@code LivingHurtEvent} on the Forge EventBus, NOT by an Item
 * method override. This is critical because 1.7.10's {@code ItemArmor} has no
 * {@code onHurt()} method — only the EventBus can intercept damage before it is applied.</p>
 *
 * <p><b>Pipeline pattern:</b> Multiple {@code IOnHurt} components are chained.
 * The return value of one component becomes the {@code incomingDamage} input of
 * the next. This is analogous to how Godot's {@code _input(event)} can call
 * {@code get_viewport().set_input_as_handled()} to consume or modify input.</p>
 */
public interface IOnHurt {

    /**
     * Called when the armor wearer takes damage. Return the modified damage amount.
     *
     * @param armorPiece     the ItemStack of the armor piece providing this component
     * @param wearer         the entity wearing the armor
     * @param damageSource   the source of the incoming damage
     * @param incomingDamage the current damage amount (may have been modified by earlier components)
     * @return the modified damage amount to pass to the next component or apply to the entity
     */
    float onHurt(ItemStack armorPiece, EntityLivingBase wearer,
                 DamageSource damageSource, float incomingDamage);
}