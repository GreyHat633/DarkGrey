package com.greyhat.dark_grey.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;

import com.google.gson.JsonObject;

/**
 * Top-level interface for all RPG components.
 *
 * <p>
 * In Godot terms, think of this as the base {@code Resource} script that every
 * custom component must extend. Just as every Godot Resource has {@code _init()} and
 * serialization hooks, every RPG component has {@link #configure} and NBT methods.
 * </p>
 *
 * <p>
 * Concrete components implement this interface plus one or more capability sub-interfaces
 * (e.g. {@link com.greyhat.dark_grey.api.capability.IOnHit}) to declare what triggers they respond to.
 * </p>
 *
 * <p>
 * <b>CRITICAL CONCURRENCY AND STATE RULE:</b> Concrete components are registered as global singletons
 * per RPG item definition. They are shared across all players and all ItemStacks of that item type.
 * Component classes <i>must not</i> store any mutable instance states inside their own fields (unless
 * thread-safe and intentionally global). Any item-specific or player-specific mutable state must
 * either be stored inside the {@link net.minecraft.item.ItemStack} NBT tag compound, or managed by a
 * WeakHashMap-based tracker mapped by the {@link net.minecraft.entity.Entity} instance.
 * </p>
 */
public interface IRPGComponent {

    /**
     * Returns the unique string identifier for this component type.
     * Must match the {@code "name"} field used in the JSON item definitions.
     *
     * @return a lowercase, underscore-separated identifier (e.g. {@code "lifesteal"})
     */
    String getComponentId();

    /**
     * Receives configuration parameters from the JSON definition.
     * Called exactly once by the parser layer immediately after instantiation.
     *
     * <p>
     * Override this to read component-specific settings. The default implementation
     * does nothing, which is appropriate for parameterless components.
     * </p>
     *
     * @param params the {@code "params"} JSON object from the item definition
     */
    default void configure(JsonObject params) {
        // No-op by default — stateless components need not override.
    }

    /**
     * Writes any persistent component state into the given NBT compound.
     * Called by the container item when the ItemStack is being saved.
     *
     * <p>
     * Stateless components (e.g. lifesteal) need not override this.
     * Stateful components (e.g. cooldown timers, stored XP) must override
     * both this and {@link #readFromNBT}.
     * </p>
     *
     * @param compound the NBT compound to write into
     */
    default void writeToNBT(NBTTagCompound compound) {
        // No-op by default — stateless components need not override.
    }

    /**
     * Reads persistent component state from the given NBT compound.
     * Called by the container item when the ItemStack is being loaded.
     *
     * @param compound the NBT compound to read from
     */
    default void readFromNBT(NBTTagCompound compound) {
        // No-op by default — stateless components need not override.
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utility Methods
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Filters a list of components, returning only those that implement the
     * specified capability interface.
     *
     * <p>
     * This is analogous to Godot's {@code get_children().filter(func(node): node is SomeType)}
     * pattern — it partitions a flat component list by capability at construction time,
     * so runtime dispatch is a simple list iteration with zero reflection.
     * </p>
     *
     * @param components     the full list of components attached to an item
     * @param capabilityType the capability interface class to filter by
     * @param <T>            the capability interface type
     * @return an unmodifiable list containing only the matching components
     */
    static <T> List<T> filterByCapability(List<IRPGComponent> components, Class<T> capabilityType) {
        List<T> matchingComponents = new ArrayList<>();
        for (IRPGComponent component : components) {
            if (capabilityType.isInstance(component)) {
                matchingComponents.add(capabilityType.cast(component));
            }
        }
        return Collections.unmodifiableList(matchingComponents);
    }
}
