package com.greyhat.dark_grey.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.greyhat.dark_grey.api.ComponentRegistry;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.IRPGItemContainer;
import com.greyhat.dark_grey.api.RPGItemDataManager;
import com.greyhat.dark_grey.api.capability.IAttributeModifier;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnHit;
import com.greyhat.dark_grey.api.capability.IOnPlayerDeath;
import com.greyhat.dark_grey.api.capability.IOnPlayerStoppedUsing;
import com.greyhat.dark_grey.api.capability.IOnWeaponUsingTick;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ItemRPGBow extends ItemBow implements IRPGItemContainer {

    private final String rpgItemId;
    public static final String NBT_TRACKER_TAG = "DarkGreyRPG_ExcelEnchants";
    public static final String NBT_VERSION_TAG = "DarkGreyRPG_DataVersion";

    private List<IRPGComponent> allComponents;
    private List<IOnHit> hitHandlers;
    private List<IHasTooltip> tooltipHandlers;
    private List<IAttributeModifier> attributeHandlers;
    private List<IOnPlayerDeath> playerDeathHandlers;
    private List<com.greyhat.dark_grey.api.capability.IOnHeldTick> heldTickHandlers;
    private List<com.greyhat.dark_grey.api.capability.IOnBowUsingTick> bowUsingTickHandlers;
    private List<com.greyhat.dark_grey.api.capability.IOnBowShoot> bowShootHandlers;

    public ItemRPGBow(String id, List<IRPGComponent> components) {
        this.rpgItemId = id;
        this.allComponents = Collections.unmodifiableList(components);
        
        this.hitHandlers = IRPGComponent.filterByCapability(components, IOnHit.class);
        this.tooltipHandlers = IRPGComponent.filterByCapability(components, IHasTooltip.class);
        this.attributeHandlers = IRPGComponent.filterByCapability(components, IAttributeModifier.class);
        this.playerDeathHandlers = IRPGComponent.filterByCapability(components, IOnPlayerDeath.class);
        this.heldTickHandlers = IRPGComponent.filterByCapability(components, com.greyhat.dark_grey.api.capability.IOnHeldTick.class);
        this.bowUsingTickHandlers = IRPGComponent.filterByCapability(components, com.greyhat.dark_grey.api.capability.IOnBowUsingTick.class);
        this.bowShootHandlers = IRPGComponent.filterByCapability(components, com.greyhat.dark_grey.api.capability.IOnBowShoot.class);
    }

    @Override
    public String getRpgItemId() {
        return rpgItemId;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        RPGItemDataManager.ItemConfig config = RPGItemDataManager.getInstance().getConfig(rpgItemId);
        if (config != null && config.displayName != null && !config.displayName.isEmpty()) {
            return config.displayName;
        }
        return super.getItemStackDisplayName(stack);
    }

    @Override
    public void rebuildComponents() {
        RPGItemDataManager.ItemConfig config = RPGItemDataManager.getInstance().getConfig(rpgItemId);
        if (config == null || config.componentsJson == null) return;
        
        List<IRPGComponent> newComponents = new java.util.ArrayList<>();
        for (com.google.gson.JsonElement compElem : config.componentsJson) {
            com.google.gson.JsonObject compObj = compElem.getAsJsonObject();
            String compName = compObj.get("name").getAsString();
            com.google.gson.JsonObject params = compObj.has("params") ? compObj.getAsJsonObject("params") : new com.google.gson.JsonObject();
            try {
                newComponents.add(ComponentRegistry.create(compName, params));
            } catch (Exception e) {}
        }
        
        this.allComponents = Collections.unmodifiableList(newComponents);
        this.hitHandlers = IRPGComponent.filterByCapability(newComponents, IOnHit.class);
        this.tooltipHandlers = IRPGComponent.filterByCapability(newComponents, IHasTooltip.class);
        this.attributeHandlers = IRPGComponent.filterByCapability(newComponents, IAttributeModifier.class);
        this.playerDeathHandlers = IRPGComponent.filterByCapability(newComponents, IOnPlayerDeath.class);
        this.heldTickHandlers = IRPGComponent.filterByCapability(newComponents, com.greyhat.dark_grey.api.capability.IOnHeldTick.class);
        this.bowUsingTickHandlers = IRPGComponent.filterByCapability(newComponents, com.greyhat.dark_grey.api.capability.IOnBowUsingTick.class);
        this.bowShootHandlers = IRPGComponent.filterByCapability(newComponents, com.greyhat.dark_grey.api.capability.IOnBowShoot.class);
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityPlayer player, int timeLeft) {
        int charge = this.getMaxItemUseDuration(stack) - timeLeft;
        boolean handled = false;
        for (com.greyhat.dark_grey.api.capability.IOnBowShoot handler : bowShootHandlers) {
            if (handler.onBowShoot(stack, world, player, charge)) {
                handled = true;
            }
        }
        if (!handled) {
            super.onPlayerStoppedUsing(stack, world, player, timeLeft);
        }
    }

    @Override
    public void onUsingTick(ItemStack stack, EntityPlayer player, int count) {
        for (com.greyhat.dark_grey.api.capability.IOnBowUsingTick handler : bowUsingTickHandlers) {
            handler.onBowUsingTick(stack, player.worldObj, player, count);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings("unchecked")
    public void addInformation(ItemStack weaponStack, EntityPlayer player, List tooltipLines, boolean showAdvanced) {
        super.addInformation(weaponStack, player, tooltipLines, showAdvanced);
        for (IHasTooltip handler : tooltipHandlers) {
            handler.addTooltipLines(weaponStack, player, tooltipLines, showAdvanced);
        }
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        RPGItemDataManager.ItemConfig config = RPGItemDataManager.getInstance().getConfig(rpgItemId);
        if (config != null && config.durability > 0) {
            return config.durability;
        }
        return super.getMaxDamage(stack);
    }
    
    @Override
    public void onUpdate(ItemStack stack, World world, net.minecraft.entity.Entity entity, int itemSlot, boolean isSelected) {
        if (isSelected && entity instanceof EntityPlayer) {
            if (heldTickHandlers != null) {
                for (com.greyhat.dark_grey.api.capability.IOnHeldTick handler : heldTickHandlers) {
                    handler.onHeldTick(stack, world, (EntityPlayer) entity);
                }
            }
        }
        
        if (world == null || world.isRemote) return;
        RPGItemDataManager dataManager = RPGItemDataManager.getInstance();
        RPGItemDataManager.ItemConfig config = dataManager.getConfig(rpgItemId);
        if (config == null) return;

        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            stack.setTagCompound(nbt);
        }

        int currentDataVersion = dataManager.getDataVersion();
        int itemDataVersion = nbt.getInteger(NBT_VERSION_TAG);
        if (itemDataVersion == currentDataVersion) return;

        syncEnchantments(stack, nbt, config);
        nbt.setInteger(NBT_VERSION_TAG, currentDataVersion);
    }

    private void syncEnchantments(ItemStack stack, NBTTagCompound nbt, RPGItemDataManager.ItemConfig config) {
        Map<Integer, Integer> excelEnchants = parseEnchantments(config.enchantments);
        NBTTagCompound tracker = nbt.getCompoundTag(NBT_TRACKER_TAG);
        NBTTagList enchList = nbt.getTagList("ench", 10);
        NBTTagList newEnchList = new NBTTagList();

        for (int i = 0; i < enchList.tagCount(); i++) {
            NBTTagCompound enchTag = enchList.getCompoundTagAt(i);
            int id = enchTag.getShort("id");
            int lvl = enchTag.getShort("lvl");
            boolean wasAppliedBySystem = tracker.hasKey(String.valueOf(id)) && tracker.getInteger(String.valueOf(id)) == lvl;
            if (!wasAppliedBySystem) {
                newEnchList.appendTag(enchTag.copy());
            }
        }

        NBTTagCompound newTracker = new NBTTagCompound();
        for (Map.Entry<Integer, Integer> entry : excelEnchants.entrySet()) {
            int newId = entry.getKey();
            int newLvl = entry.getValue();
            boolean playerHasOverride = false;
            for (int i = 0; i < newEnchList.tagCount(); i++) {
                if (newEnchList.getCompoundTagAt(i).getShort("id") == newId) {
                    playerHasOverride = true;
                    break;
                }
            }
            if (!playerHasOverride) {
                NBTTagCompound newEnchTag = new NBTTagCompound();
                newEnchTag.setShort("id", (short) newId);
                newEnchTag.setShort("lvl", (short) newLvl);
                newEnchList.appendTag(newEnchTag);
                newTracker.setInteger(String.valueOf(newId), newLvl);
            }
        }

        if (newEnchList.tagCount() > 0) {
            nbt.setTag("ench", newEnchList);
        } else {
            nbt.removeTag("ench");
        }
        nbt.setTag(NBT_TRACKER_TAG, newTracker);
    }

    private Map<Integer, Integer> parseEnchantments(String enchantmentsStr) {
        Map<Integer, Integer> map = new HashMap<>();
        if (enchantmentsStr == null || enchantmentsStr.trim().isEmpty()) return map;
        String[] parts = enchantmentsStr.split(",");
        for (String part : parts) {
            String[] ench = part.trim().split(":");
            if (ench.length >= 1) {
                try {
                    int id = Integer.parseInt(ench[0].trim());
                    int lvl = 1;
                    if (ench.length >= 2) {
                        try { lvl = Integer.parseInt(ench[1].trim()); }
                        catch (NumberFormatException e) {
                            if (ench.length >= 3) {
                                try { lvl = Integer.parseInt(ench[2].trim()); }
                                catch (NumberFormatException ignored) {}
                            }
                        }
                    }
                    if (id >= 0 && id < Enchantment.enchantmentsList.length && Enchantment.enchantmentsList[id] != null) {
                        map.put(id, lvl);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return map;
    }
    
    @SideOnly(Side.CLIENT)
    private net.minecraft.util.IIcon[] bowPullIcons;

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(net.minecraft.client.renderer.texture.IIconRegister iconRegister) {
        this.itemIcon = iconRegister.registerIcon(this.getIconString());
        this.bowPullIcons = new net.minecraft.util.IIcon[3];
        for (int i = 0; i < this.bowPullIcons.length; ++i) {
            this.bowPullIcons[i] = iconRegister.registerIcon(this.getIconString() + "_pulling_" + i);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public net.minecraft.util.IIcon getIcon(ItemStack stack, int renderPass, EntityPlayer player, ItemStack usingItem, int useRemaining) {
        if (usingItem != null) {
            int j = usingItem.getMaxItemUseDuration() - useRemaining;
            if ("rainbow_bow".equals(this.rpgItemId)) {
                if (j >= 40) {
                    return this.bowPullIcons[2];
                }
                if (j >= 20) {
                    return this.bowPullIcons[1];
                }
                if (j > 0) {
                    return this.bowPullIcons[0];
                }
            } else if ("law_of_cycles".equals(this.rpgItemId)) {
                // Total charge time is 140 ticks (7.0s)
                if (j >= 93) {
                    return this.bowPullIcons[2];
                }
                if (j >= 46) {
                    return this.bowPullIcons[1];
                }
                if (j > 0) {
                    return this.bowPullIcons[0];
                }
            } else {
                // Default fallback
                if (j >= 18) {
                    return this.bowPullIcons[2];
                }
                if (j > 13) {
                    return this.bowPullIcons[1];
                }
                if (j > 0) {
                    return this.bowPullIcons[0];
                }
            }
        }
        return this.itemIcon;
    }

    public List<IOnPlayerDeath> getPlayerDeathHandlers() { return playerDeathHandlers; }
    public List<IOnHit> getHitHandlers() { return hitHandlers; }
}