package com.greyhat.dark_grey.api;

import com.google.gson.JsonObject;
import com.greyhat.dark_grey.DarkGrey;
import com.greyhat.dark_grey.component.ComponentSupernovaSet;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Central registry that maps component ID strings to factory functions.
 *
 * <p>In Godot terms, this is like a global {@code ClassDB} or {@code ResourceLoader}
 * that knows how to instantiate any registered component by its string name.
 * The JSON parser asks: "give me a 'lifesteal' component" and the registry
 * creates a fresh instance, ready to be configured.</p>
 *
 * <p>Usage (during mod initialization):</p>
 * <pre>{@code
 * ComponentRegistry.register("lifesteal", LifestealComponent::new);
 * ComponentRegistry.register("auto_smelt", AutoSmeltComponent::new);
 * }</pre>
 *
 * <p>Usage (during JSON parsing):</p>
 * <pre>{@code
 * IRPGComponent component = ComponentRegistry.create("lifesteal", paramsJsonObject);
 * }</pre>
 */
public final class ComponentRegistry {

    /** Maps component ID strings (e.g. "lifesteal") to their factory suppliers. */
    private static final Map<String, Supplier<IRPGComponent>> COMPONENT_FACTORIES = new HashMap<>();

    private ComponentRegistry() {
        // Prevent instantiation — this is a static utility class.
    }

    /**
     * Registers a component factory under the given ID.
     *
     * @param componentId the unique string identifier (must match JSON {@code "name"} field)
     * @param factory     a supplier that creates a new, unconfigured component instance
     * @throws IllegalArgumentException if a component with this ID is already registered
     */
    public static void register(String componentId, Supplier<IRPGComponent> factory) {
        if (COMPONENT_FACTORIES.containsKey(componentId)) {
            throw new IllegalArgumentException(
                "Duplicate RPG component registration: '" + componentId
                + "' is already registered. Each component ID must be unique."
            );
        }
        COMPONENT_FACTORIES.put(componentId, factory);
        DarkGrey.LOG.info("[RPG Components] Registered component type: " + componentId);
    }

    /**
     * Creates a new component instance by ID, then configures it with the given JSON params.
     *
     * @param componentId the component ID string from the JSON definition
     * @param params      the {@code "params"} JSON object to pass to {@link IRPGComponent#configure}
     * @return a fully configured, ready-to-use component instance
     * @throws IllegalArgumentException if no component is registered under this ID
     */
    public static IRPGComponent create(String componentId, JsonObject params) {
        Supplier<IRPGComponent> factory = COMPONENT_FACTORIES.get(componentId);
        if (factory == null) {
            throw new IllegalArgumentException(
                "Unknown RPG component: '" + componentId
                + "'. Make sure it is registered in ComponentRegistry before JSON loading."
            );
        }
        IRPGComponent componentInstance = factory.get();
        if (params != null) {
            componentInstance.configure(params);
        }
        return componentInstance;
    }

    /**
     * Checks whether a component with the given ID has been registered.
     *
     * @param componentId the component ID to check
     * @return true if the ID is registered, false otherwise
     */
    public static boolean isRegistered(String componentId) {
        return COMPONENT_FACTORIES.containsKey(componentId);
    }

    /**
     * Returns the number of registered component types.
     * Useful for logging during initialization.
     *
     * @return the count of registered component factories
     */
    public static int getRegisteredCount() {
        return COMPONENT_FACTORIES.size();
    }
}