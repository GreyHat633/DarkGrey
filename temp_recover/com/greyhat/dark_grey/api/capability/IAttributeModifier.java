package com.greyhat.dark_grey.api.capability;

import com.google.common.collect.Multimap;
import net.minecraft.entity.ai.attributes.AttributeModifier;

/**
 * Capability interface for components that modify an item's attribute modifiers.
 *
 * <p>Attribute modifiers affect player stats like attack damage, movement speed, etc.
 * Called from the container's {@code getItemAttributeModifiers()} override.</p>
 *
 * <p>Note: In 1.7.10, {@code getItemAttributeModifiers()} does not receive an
 * {@code ItemStack} parameter. Components should use their own configured values
 * (set via {@code configure()}) rather than reading from the stack's NBT.</p>
 *
 * <p>In Godot terms, this is like a component modifying the parent node's exported
 * properties — the component can add or change attribute entries on the item.</p>
 */
public interface IAttributeModifier {

    /**
     * Mutates the item's attribute modifier map to add or alter attribute modifiers.
     *
     * <p>Common attribute names in 1.7.10:</p>
     * <ul>
     *   <li>{@code "generic.attackDamage"} — bonus attack damage</li>
     *   <li>{@code "generic.movementSpeed"} — movement speed modifier</li>
     *   <li>{@code "generic.knockbackResistance"} — knockback resistance</li>
     *   <li>{@code "generic.maxHealth"} — max health modifier</li>
     * </ul>
     *
     * @param attributeMap the mutable multimap of attribute name → modifier
     */
    void modifyAttributes(Multimap<String, AttributeModifier> attributeMap);
}