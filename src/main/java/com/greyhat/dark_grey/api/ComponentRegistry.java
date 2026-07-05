// 
// Decompiled by Procyon v0.6.0
// 

package com.greyhat.dark_grey.api;

import java.util.HashMap;
import com.google.gson.JsonObject;
import com.greyhat.dark_grey.DarkGrey;
import java.util.function.Supplier;
import java.util.Map;

public final class ComponentRegistry
{
    private static final Map<String, Supplier<IRPGComponent>> COMPONENT_FACTORIES;
    
    private ComponentRegistry() {
    }
    
    public static void register(final String componentId, final Supplier<IRPGComponent> factory) {
        if (ComponentRegistry.COMPONENT_FACTORIES.containsKey(componentId)) {
            throw new IllegalArgumentException("Duplicate RPG component registration: '" + componentId + "' is already registered. Each component ID must be unique.");
        }
        ComponentRegistry.COMPONENT_FACTORIES.put(componentId, factory);
        DarkGrey.LOG.info("[RPG Components] Registered component type: " + componentId);
    }
    
    public static IRPGComponent create(final String componentId, final JsonObject params) {
        final Supplier<IRPGComponent> factory = ComponentRegistry.COMPONENT_FACTORIES.get(componentId);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown RPG component: '" + componentId + "'. Make sure it is registered in ComponentRegistry before JSON loading.");
        }
        final IRPGComponent componentInstance = factory.get();
        if (params != null) {
            componentInstance.configure(params);
        }
        return componentInstance;
    }
    
    public static boolean isRegistered(final String componentId) {
        return ComponentRegistry.COMPONENT_FACTORIES.containsKey(componentId);
    }
    
    public static int getRegisteredCount() {
        return ComponentRegistry.COMPONENT_FACTORIES.size();
    }
    
    static {
        COMPONENT_FACTORIES = new HashMap<String, Supplier<IRPGComponent>>();
    }
}
