package com.greyhat.dark_grey.item;

import java.util.Collections;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.IRPGItemContainer;
import com.greyhat.dark_grey.api.RPGItemDataManager;

public class ItemRPGAmmo extends Item implements IRPGItemContainer {

    private final String rpgItemId;
    private List<IRPGComponent> components;

    public ItemRPGAmmo(String rpgItemId, List<IRPGComponent> components) {
        this.rpgItemId = rpgItemId;
        this.components = components;
    }

    @Override
    public String getRpgItemId() {
        return this.rpgItemId;
    }

    @Override
    public void rebuildComponents() {
        RPGItemDataManager.ItemConfig config = RPGItemDataManager.getInstance()
            .getConfig(this.rpgItemId);
        if (config == null || config.componentsJson == null) return;

        List<IRPGComponent> newComponents = new java.util.ArrayList<>();
        for (com.google.gson.JsonElement compElem : config.componentsJson) {
            com.google.gson.JsonObject compObj = compElem.getAsJsonObject();
            String compName = compObj.get("name")
                .getAsString();
            com.google.gson.JsonObject params = compObj.has("params") ? compObj.getAsJsonObject("params")
                : new com.google.gson.JsonObject();
            try {
                newComponents.add(com.greyhat.dark_grey.api.ComponentRegistry.create(compName, params));
            } catch (Exception e) {
                com.greyhat.dark_grey.DarkGrey.LOG
                    .error("Failed to rebuild component " + compName + " for item " + rpgItemId, e);
            }
        }
        this.components = Collections.unmodifiableList(newComponents);
    }

    @Override
    public List<IRPGComponent> getAllComponents() {
        return this.components;
    }

    @Override
    public List<com.greyhat.dark_grey.api.capability.IOnPlayerDeath> getPlayerDeathHandlers() {
        return Collections.emptyList();
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        RPGItemDataManager.ItemConfig config = RPGItemDataManager.getInstance()
            .getConfig(this.rpgItemId);
        if (config != null && config.displayName != null && !config.displayName.isEmpty()) {
            return "\u00a7r" + config.displayName;
        }
        return super.getItemStackDisplayName(stack);
    }
}
