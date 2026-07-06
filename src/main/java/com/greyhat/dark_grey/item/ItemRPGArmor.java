//
// Decompiled by Procyon v0.6.0
//

package com.greyhat.dark_grey.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.greyhat.dark_grey.api.ComponentRegistry;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.IRPGItemContainer;
import com.greyhat.dark_grey.api.RPGItemDataManager;
import com.greyhat.dark_grey.api.capability.IAttributeModifier;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnEquip;
import com.greyhat.dark_grey.api.capability.IOnHit;
import com.greyhat.dark_grey.api.capability.IOnHurt;
import com.greyhat.dark_grey.api.capability.IOnPlayerDeath;
import com.greyhat.dark_grey.api.capability.IOnUnequip;
import com.greyhat.dark_grey.api.capability.IOnWornTick;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemRPGArmor extends ItemArmor implements IRPGItemContainer {

    private final String rpgItemId;
    private List<IRPGComponent> allComponents;
    private List<IOnWornTick> wornTickHandlers;
    private List<IOnHurt> hurtHandlers;
    private List<IOnEquip> equipHandlers;
    private List<IOnUnequip> unequipHandlers;
    private List<IHasTooltip> tooltipHandlers;
    private List<IAttributeModifier> attributeHandlers;
    private List<IOnPlayerDeath> playerDeathHandlers;
    private List<IOnHit> hitHandlers;
    public static final String NBT_TRACKER_TAG = "DarkGreyRPG_ExcelEnchants";
    public static final String NBT_VERSION_TAG = "DarkGreyRPG_DataVersion";

    public ItemRPGArmor(final String id, final ItemArmor.ArmorMaterial material, final int renderIndex,
        final int armorType, final List<IRPGComponent> components) {
        super(material, renderIndex, armorType);
        this.rpgItemId = id;
        this.allComponents = Collections.unmodifiableList((List<? extends IRPGComponent>) components);
        this.wornTickHandlers = IRPGComponent.filterByCapability(components, IOnWornTick.class);
        this.hurtHandlers = IRPGComponent.filterByCapability(components, IOnHurt.class);
        this.equipHandlers = IRPGComponent.filterByCapability(components, IOnEquip.class);
        this.unequipHandlers = IRPGComponent.filterByCapability(components, IOnUnequip.class);
        this.tooltipHandlers = IRPGComponent.filterByCapability(components, IHasTooltip.class);
        this.attributeHandlers = IRPGComponent.filterByCapability(components, IAttributeModifier.class);
        this.playerDeathHandlers = IRPGComponent.filterByCapability(components, IOnPlayerDeath.class);
        this.hitHandlers = IRPGComponent.filterByCapability(components, IOnHit.class);
    }

    public String getItemStackDisplayName(final ItemStack stack) {
        final RPGItemDataManager.ItemConfig config = RPGItemDataManager.getInstance()
            .getConfig(this.rpgItemId);
        if (config != null && config.displayName != null && !config.displayName.isEmpty()) {
            return config.displayName;
        }
        return super.getItemStackDisplayName(stack);
    }

    public String getRpgItemId() {
        return this.rpgItemId;
    }

    public void rebuildComponents() {
        final RPGItemDataManager.ItemConfig config = RPGItemDataManager.getInstance()
            .getConfig(this.rpgItemId);
        if (config == null || config.componentsJson == null) {
            return;
        }
        final List<IRPGComponent> newComponents = new ArrayList<IRPGComponent>();
        for (final JsonElement compElem : config.componentsJson) {
            final JsonObject compObj = compElem.getAsJsonObject();
            final String compName = compObj.get("name")
                .getAsString();
            final JsonObject params = compObj.has("params") ? compObj.getAsJsonObject("params") : new JsonObject();
            try {
                newComponents.add(ComponentRegistry.create(compName, params));
            } catch (final Exception e) {
                FMLLog.warning(
                    "[DarkGrey] Failed to create component '%s' for armor '%s': %s",
                    new Object[] { compName, this.rpgItemId, e.getMessage() });
            }
        }
        this.allComponents = Collections.unmodifiableList((List<? extends IRPGComponent>) newComponents);
        this.wornTickHandlers = IRPGComponent.filterByCapability(newComponents, IOnWornTick.class);
        this.hurtHandlers = IRPGComponent.filterByCapability(newComponents, IOnHurt.class);
        this.equipHandlers = IRPGComponent.filterByCapability(newComponents, IOnEquip.class);
        this.unequipHandlers = IRPGComponent.filterByCapability(newComponents, IOnUnequip.class);
        this.tooltipHandlers = IRPGComponent.filterByCapability(newComponents, IHasTooltip.class);
        this.attributeHandlers = IRPGComponent.filterByCapability(newComponents, IAttributeModifier.class);
        this.playerDeathHandlers = IRPGComponent.filterByCapability(newComponents, IOnPlayerDeath.class);
        this.hitHandlers = IRPGComponent.filterByCapability(newComponents, IOnHit.class);
    }

    public void onArmorTick(final World world, final EntityPlayer player, final ItemStack armorStack) {
        if (world.isRemote) {
            return;
        }
        if (world.getTotalWorldTime() % 20L != 0L) {
            return;
        }
        for (final IOnWornTick handler : this.wornTickHandlers) {
            handler.onWornTick(world, player, armorStack);
        }
    }

    @SideOnly(Side.CLIENT)
    public void addInformation(final ItemStack armorStack, final EntityPlayer player, final List tooltipLines,
        final boolean showAdvanced) {
        super.addInformation(armorStack, player, tooltipLines, showAdvanced);
        for (final IHasTooltip handler : this.tooltipHandlers) {
            handler.addTooltipLines(armorStack, player, tooltipLines, showAdvanced);
        }
    }

    public Multimap getItemAttributeModifiers() {
        final Multimap<String, AttributeModifier> attributeMap = (Multimap<String, AttributeModifier>) super.getItemAttributeModifiers();
        for (final IAttributeModifier handler : this.attributeHandlers) {
            handler.modifyAttributes(attributeMap);
        }
        return attributeMap;
    }

    public int getMaxDamage(final ItemStack stack) {
        final RPGItemDataManager.ItemConfig config = RPGItemDataManager.getInstance()
            .getConfig(this.rpgItemId);
        if (config != null && config.durability > 0) {
            return config.durability;
        }
        return super.getMaxDamage(stack);
    }

    public void func_77663_a(final ItemStack stack, final World world, final Entity entity, final int itemSlot,
        final boolean isSelected) {
        com.greyhat.dark_grey.api.RPGItemStackSync.syncIfVersionChanged(stack, this.rpgItemId, world);
    }

    @SideOnly(Side.CLIENT)
    public void func_150895_a(final Item item, final CreativeTabs tab, final List list) {
        final ItemStack stack = new ItemStack(item, 1, 0);
        com.greyhat.dark_grey.api.RPGItemStackSync.forceSync(stack, this.rpgItemId);
        list.add(stack);
    }

    public List<IRPGComponent> getAllComponents() {
        return this.allComponents;
    }

    public List<IOnHurt> getHurtHandlers() {
        return this.hurtHandlers;
    }

    public List<IOnEquip> getEquipHandlers() {
        return this.equipHandlers;
    }

    public List<IOnUnequip> getUnequipHandlers() {
        return this.unequipHandlers;
    }

    @Override
    public List<IOnPlayerDeath> getPlayerDeathHandlers() {
        return this.playerDeathHandlers;
    }

    public List<IOnHit> getHitHandlers() {
        return this.hitHandlers;
    }

    public String getArmorTexture(final ItemStack stack, final Entity entity, final int slot, final String type) {
        final RPGItemDataManager.ItemConfig config = RPGItemDataManager.getInstance()
            .getConfig(this.rpgItemId);
        if (config != null && config.texture != null && !config.texture.isEmpty()) {
            final String[] parts = config.texture.split(":");
            if (parts.length == 2) {
                final String modid = parts[0];
                String baseName;
                final String itemName = baseName = parts[1];
                if (itemName.endsWith("_helmet")) {
                    baseName = itemName.substring(0, itemName.length() - 7);
                } else if (itemName.endsWith("_chestplate")) {
                    baseName = itemName.substring(0, itemName.length() - 11);
                } else if (itemName.endsWith("_leggings")) {
                    baseName = itemName.substring(0, itemName.length() - 9);
                } else if (itemName.endsWith("_boots")) {
                    baseName = itemName.substring(0, itemName.length() - 6);
                }
                final int layer = (slot == 2) ? 2 : 1;
                return modid + ":textures/models/armor/" + baseName + "_layer_" + layer + ".png";
            }
        }
        return super.getArmorTexture(stack, entity, slot, type);
    }
}
