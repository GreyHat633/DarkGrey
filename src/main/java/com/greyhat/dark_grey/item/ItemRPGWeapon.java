package com.greyhat.dark_grey.item;



import com.greyhat.dark_grey.api.IRPGComponent;

import com.greyhat.dark_grey.api.IRPGItemContainer;

import com.greyhat.dark_grey.api.capability.IAttributeModifier;

import com.greyhat.dark_grey.api.capability.IHasTooltip;

import com.greyhat.dark_grey.api.capability.IOnHit;

import com.greyhat.dark_grey.api.capability.IOnPlayerDeath;

import com.greyhat.dark_grey.api.capability.IOnRightClick;



import com.google.common.collect.Multimap;

import cpw.mods.fml.relauncher.Side;

import cpw.mods.fml.relauncher.SideOnly;

import net.minecraft.entity.EntityLivingBase;

import net.minecraft.entity.ai.attributes.AttributeModifier;

import net.minecraft.entity.player.EntityPlayer;

import net.minecraft.item.ItemStack;

import net.minecraft.item.ItemSword;

import net.minecraft.world.World;



import net.minecraft.enchantment.Enchantment;

import net.minecraft.creativetab.CreativeTabs;

import net.minecraft.entity.SharedMonsterAttributes;

import net.minecraft.nbt.NBTTagCompound;

import net.minecraft.nbt.NBTTagList;

import com.greyhat.dark_grey.api.RPGItemDataManager;

import com.google.common.collect.HashMultimap;

import java.util.Collections;

import java.util.List;

import java.util.Map;

import java.util.HashMap;

import java.util.UUID;

import cpw.mods.fml.common.FMLLog;



/**

 * Universal RPG weapon container 閳?extends {@link ItemSword}.

 *

 * <p>In Godot terms, this is a {@code CharacterBody3D} node that holds a list of

 * attached component scripts. The node itself provides the physics/collision behavior

 * (sword mechanics), while each component script handles its own game logic

 * (lifesteal, damage bonus, etc.). The node simply iterates its children and

 * calls their methods at the right time.</p>

 *

 * <p>Components are partitioned into capability-typed sublists at construction time

 * (not at runtime), making dispatch a zero-cost list iteration.</p>

 */

public class ItemRPGWeapon extends ItemSword implements IRPGItemContainer {

    private List<IRPGComponent> allComponents;
    private List<IOnHit> hitHandlers;
    private List<IOnRightClick> rightClickHandlers;
    private List<IHasTooltip> tooltipHandlers;
    private List<IAttributeModifier> attributeHandlers;
    private List<IOnPlayerDeath> playerDeathHandlers;
    private List<com.greyhat.dark_grey.api.capability.IOnHeldTick> heldTickHandlers;
    private List<com.greyhat.dark_grey.api.capability.IOnWeaponUsingTick> weaponUsingTickHandlers;
    private List<com.greyhat.dark_grey.api.capability.IOnPlayerStoppedUsing> playerStoppedUsingHandlers;

    private final String rpgItemId;
    public static final String NBT_TRACKER_TAG = "DarkGreyRPG_ExcelEnchants";
    public static final String NBT_VERSION_TAG = "DarkGreyRPG_DataVersion";

    public ItemRPGWeapon(String id, ToolMaterial material, List<IRPGComponent> components) {
        super(material);
        this.rpgItemId = id;
        this.allComponents = Collections.unmodifiableList(components);
        this.hitHandlers = IRPGComponent.filterByCapability(components, IOnHit.class);
        this.rightClickHandlers = IRPGComponent.filterByCapability(components, IOnRightClick.class);
        this.tooltipHandlers = IRPGComponent.filterByCapability(components, IHasTooltip.class);
        this.attributeHandlers = IRPGComponent.filterByCapability(components, IAttributeModifier.class);
        this.playerDeathHandlers = IRPGComponent.filterByCapability(components, IOnPlayerDeath.class);
        this.heldTickHandlers = IRPGComponent.filterByCapability(components, com.greyhat.dark_grey.api.capability.IOnHeldTick.class);
        this.weaponUsingTickHandlers = IRPGComponent.filterByCapability(components, com.greyhat.dark_grey.api.capability.IOnWeaponUsingTick.class);
        this.playerStoppedUsingHandlers = IRPGComponent.filterByCapability(components, com.greyhat.dark_grey.api.capability.IOnPlayerStoppedUsing.class);
    }

    /**
     * <p>To obtain the actual damage dealt (post-armor, post-enchantment),

     * we snapshot the target's health BEFORE the super call and compare

     * it AFTER. The difference is the real HP the target lost.</p>

     *

     * <p>This avoids accessing the protected {@code lastDamage} field on

     * {@code EntityLivingBase}, which is not accessible from non-subclasses.</p>

     */

    @Override

    public boolean hitEntity(ItemStack weaponStack, EntityLivingBase target,

                             EntityLivingBase attacker) {

        boolean result = super.hitEntity(weaponStack, target, attacker);



        if (!attacker.worldObj.isRemote) {

            // hitEntity is called AFTER damage is applied, so health snapshotting won't work.

            // We use the attacker's raw attack damage attribute instead, which is more predictable for RPGs.

            float rawDamage = 1.0F;

            if (attacker.getEntityAttribute(SharedMonsterAttributes.attackDamage) != null) {

                rawDamage = (float) attacker.getEntityAttribute(SharedMonsterAttributes.attackDamage).getAttributeValue();

            }



            for (IOnHit handler : hitHandlers) {

                handler.onHit(weaponStack, attacker, target, rawDamage);

            }

        }

        return result;

    }



    @Override

    public String getItemStackDisplayName(ItemStack stack) {

        RPGItemDataManager.ItemConfig config = RPGItemDataManager.getInstance().getConfig(rpgItemId);

        if (config != null && config.displayName != null && !config.displayName.isEmpty()) {

            return config.displayName;

        }

        return super.getItemStackDisplayName(stack);

    }



    public String getRpgItemId() {

        return rpgItemId;

    }



    /**

     * Hot-reloads all components from the global data manager.

     */

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

                newComponents.add(com.greyhat.dark_grey.api.ComponentRegistry.create(compName, params));

            } catch (Exception e) {}

        }

        

        this.allComponents = Collections.unmodifiableList(newComponents);

        this.hitHandlers = IRPGComponent.filterByCapability(newComponents, IOnHit.class);

        this.rightClickHandlers = IRPGComponent.filterByCapability(newComponents, IOnRightClick.class);

        this.tooltipHandlers = IRPGComponent.filterByCapability(newComponents, IHasTooltip.class);

        this.attributeHandlers = IRPGComponent.filterByCapability(newComponents, IAttributeModifier.class);

        this.playerDeathHandlers = IRPGComponent.filterByCapability(newComponents, IOnPlayerDeath.class);
        this.heldTickHandlers = IRPGComponent.filterByCapability(newComponents, com.greyhat.dark_grey.api.capability.IOnHeldTick.class);
        this.weaponUsingTickHandlers = IRPGComponent.filterByCapability(newComponents, com.greyhat.dark_grey.api.capability.IOnWeaponUsingTick.class);
        this.playerStoppedUsingHandlers = IRPGComponent.filterByCapability(newComponents, com.greyhat.dark_grey.api.capability.IOnPlayerStoppedUsing.class);

    }



    /**

     * Called when the player right-clicks in air with this weapon.

     * Chains through all {@link IOnRightClick} components.

     */

        @Override
    public void onUsingTick(ItemStack stack, EntityPlayer player, int count) {
        if (weaponUsingTickHandlers != null) {
            for (com.greyhat.dark_grey.api.capability.IOnWeaponUsingTick handler : weaponUsingTickHandlers) {
                handler.onUsingTick(stack, player, count);
            }
        }
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityPlayer player, int timeLeft) {
        if (playerStoppedUsingHandlers != null) {
            for (com.greyhat.dark_grey.api.capability.IOnPlayerStoppedUsing handler : playerStoppedUsingHandlers) {
                handler.onPlayerStoppedUsing(stack, world, player, timeLeft);
            }
        }
    }

    @Override

    public ItemStack onItemRightClick(ItemStack weaponStack, World world,

                                      EntityPlayer player) {

        // Start block animation

        player.setItemInUse(weaponStack, this.getMaxItemUseDuration(weaponStack));

        

        ItemStack resultStack = weaponStack;

        for (IOnRightClick handler : rightClickHandlers) {

            resultStack = handler.onRightClick(resultStack, world, player);

        }

        return resultStack;

    }



    /**

     * Appends component tooltip information to the item's hover text.

     * Runs client-side only.

     */

    @Override

    @SideOnly(Side.CLIENT)

    @SuppressWarnings("unchecked")

    public void addInformation(ItemStack weaponStack, EntityPlayer player,

                               List tooltipLines, boolean showAdvanced) {

        super.addInformation(weaponStack, player, tooltipLines, showAdvanced);

        for (IHasTooltip handler : tooltipHandlers) {

            handler.addTooltipLines(weaponStack, player, tooltipLines, showAdvanced);

        }

    }



    /**

     * Returns the weapon's attribute modifiers, dynamically overriding attack damage

     * from the global data manager.

     */

    @Override

    @SuppressWarnings("unchecked")

    public Multimap getItemAttributeModifiers() {

        Multimap attributeMap = HashMultimap.create();

        

        // Dynamically fetch damage from the global manager

        RPGItemDataManager.ItemConfig config = RPGItemDataManager.getInstance().getConfig(rpgItemId);

        float damage = 0.0f;

        if (config != null) {

            damage = config.damage;

        }



        // Must use exactly this vanilla UUID so the engine recognizes it as base weapon damage!

        UUID damageOverrideUUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");

        attributeMap.put(SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName(), 

            new AttributeModifier(damageOverrideUUID, "Weapon modifier", (double) damage, 0));

        

        // Apply any component modifiers

        for (IAttributeModifier handler : attributeHandlers) {

            handler.modifyAttributes(attributeMap);

        }

        return attributeMap;

    }

    

    /**

     * Overrides max durability to dynamically load from the data manager.

     */

    @Override

    public int getMaxDamage(ItemStack stack) {

        RPGItemDataManager.ItemConfig config = RPGItemDataManager.getInstance().getConfig(rpgItemId);

        if (config != null && config.durability > 0) {

            return config.durability;

        }

        return super.getMaxDamage(stack);

    }

    

    /**

     * Ticks the item in the player's inventory to verify and dynamically merge NBT changes

     * non-destructively.

     */

    @Override

    public void onUpdate(ItemStack stack, World world, net.minecraft.entity.Entity entity, int itemSlot, boolean isSelected) {
        if (isSelected && entity instanceof EntityPlayer) {
            for (com.greyhat.dark_grey.api.capability.IOnHeldTick handler : heldTickHandlers) {
                handler.onHeldTick(stack, world, (EntityPlayer) entity);
            }
        }
        
        if (world == null || world.isRemote) return; // Only process on server side

        

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

        

        // Skip NBT heavy operations if versions match

        if (itemDataVersion == currentDataVersion) {

            return;

        }

        

        // Version mismatch detected. We need to perform non-destructive sync.

        syncEnchantments(stack, nbt, config);

        

        // Update version to prevent running again until next reload

        nbt.setInteger(NBT_VERSION_TAG, currentDataVersion);

    }

    

    /**

     * Non-destructive enchantment synchronization.

     */

    private void syncEnchantments(ItemStack stack, NBTTagCompound nbt, RPGItemDataManager.ItemConfig config) {

        Map<Integer, Integer> excelEnchants = parseEnchantments(config.enchantments);

        

        NBTTagCompound tracker = nbt.getCompoundTag(NBT_TRACKER_TAG);

        // Tracker stores what the system previously applied: { "16": 2, "21": 1 }

        

        NBTTagList enchList = nbt.getTagList("ench", 10);

        NBTTagList newEnchList = new NBTTagList();

        

        // Step 1: Filter out old Excel enchants, keep player enchants

        for (int i = 0; i < enchList.tagCount(); i++) {

            NBTTagCompound enchTag = enchList.getCompoundTagAt(i);

            int id = enchTag.getShort("id");

            int lvl = enchTag.getShort("lvl");

            

            boolean wasAppliedBySystem = tracker.hasKey(String.valueOf(id)) && tracker.getInteger(String.valueOf(id)) == lvl;

            

            // If the system didn't apply this exact level, or it's a completely different enchantment, we KEEP it.

            if (!wasAppliedBySystem) {

                // However, if the new Excel config ALSO provides this enchantment, 

                // we should respect the player's level over the Excel level if they conflict.

                // Actually, to be safe, if a player modified an enchantment that Excel also modifies,

                // we just keep the player's version and don't re-apply Excel's.

                newEnchList.appendTag(enchTag.copy());

            }

        }

        

        // Step 2: Apply new Excel enchants (only if they aren't already overridden by player)

        NBTTagCompound newTracker = new NBTTagCompound();

        for (Map.Entry<Integer, Integer> entry : excelEnchants.entrySet()) {

            int newId = entry.getKey();

            int newLvl = entry.getValue();

            

            // Check if player has this enchantment already with a different level

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

                

                String enchName = Enchantment.enchantmentsList[newId].getTranslatedName(newLvl);

                FMLLog.info("[DarkGrey-Sync] Applying " + enchName + " (ID: " + newId + ") to " + config.displayName + " (" + rpgItemId + ")");

            }

        }

        

        // Step 3: Save lists

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

                        try {

                            lvl = Integer.parseInt(ench[1].trim());

                        } catch (NumberFormatException e) {

                            // If they selected e.g. "35:閺冩儼绻? from the dropdown and didn't specify a level

                            if (ench.length >= 3) {

                                try { lvl = Integer.parseInt(ench[2].trim()); } 

                                catch (NumberFormatException ignored) {}

                            } else {

                                FMLLog.info("[DarkGrey-Data] Non-numeric level '" + ench[1].trim() + "' detected for Enchantment ID " + id + ". Defaulting to Level 1.");

                            }

                        }

                    }

                    

                    if (id >= 0 && id < Enchantment.enchantmentsList.length && Enchantment.enchantmentsList[id] != null) {

                        map.put(id, lvl);

                    } else {

                        FMLLog.warning("[DarkGrey-Data] Enchantment ID " + id + " does not exist in the game!");

                    }

                } catch (NumberFormatException ignored) {}

            }

        }

        return map;

    }



    // 閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓

    //  Public Accessors (for RPGCoreEventHandler)

    // 閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓



    public List<IRPGComponent> getAllComponents() {

        return allComponents;

    }



    public List<IOnHit> getHitHandlers() {

        return hitHandlers;

    }



    public List<IOnPlayerDeath> getPlayerDeathHandlers() {

        return playerDeathHandlers;

    }



    @Override

    @SideOnly(Side.CLIENT)

    @SuppressWarnings("unchecked")

    public void getSubItems(net.minecraft.item.Item item, CreativeTabs tab, List list) {

        ItemStack stack = new ItemStack(item, 1, 0);

        // Force an initialization update on creative tab creation so the sword appears with base enchantments immediately

        RPGItemDataManager.ItemConfig config = RPGItemDataManager.getInstance().getConfig(rpgItemId);

        if (config != null) {

            NBTTagCompound nbt = new NBTTagCompound();

            nbt.setInteger(NBT_VERSION_TAG, RPGItemDataManager.getInstance().getDataVersion());

            syncEnchantments(stack, nbt, config);

            stack.setTagCompound(nbt);

        }

        list.add(stack);

    }

}