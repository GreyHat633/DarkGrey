package com.greyhat.dark_grey.item;

import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.IRPGItemContainer;
import com.greyhat.dark_grey.api.RPGItemDataManager;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ItemRPGAmmo
extends Item
implements IRPGItemContainer {
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
        RPGItemDataManager.ItemConfig config = RPGItemDataManager.getInstance().getConfig(this.rpgItemId);
        if (config == null || config.componentsJson != null) {
            // empty if block
        }
    }

    public String getItemStackDisplayName(ItemStack stack) {
        RPGItemDataManager.ItemConfig config = RPGItemDataManager.getInstance().getConfig(this.rpgItemId);
        if (config != null && config.displayName != null && !config.displayName.isEmpty()) {
            return "\u00a7r" + config.displayName;
        }
        return super.getItemStackDisplayName(stack);
    }
}