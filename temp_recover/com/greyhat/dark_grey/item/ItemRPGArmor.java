package com.greyhat.dark_grey.item;

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

import com.google.common.collect.Multimap;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Universal RPG armor container — extends {@link ItemArmor}.
 *
 * <p>In Godot terms, this is like a {@code Node3D} with multiple child scripts that
 * each handle a different aspect: one script handles damage reduction (IOnHurt),
 * another provides night vision every second (IOnWornTick), another fires once
 * on equip (IOnEquip), etc. The parent node just routes lifecycle events to its children.</p>
 *
 * <p>Key events handled by this container:</p>
 * <ul>
 *   <li>{@code onArmorTick} → delegates to {@link IOnWornTick} (throttled to 1/sec)</li>
 *   <li>{@code addInformation} → delegates to {@link IHasTooltip}</li>
 *   <li>{@code getItemAttributeModifiers} → delegates to {@link IAttributeModifier}</li>
 * </ul>
 *
 * <p>Events NOT handled here (require Forge EventBus, managed by {@code RPGCoreEventHandler}):</p>
 * <ul>
 *   <li>Damage received → {@link IOnHurt}</li>
 *   <li>Equip/Unequip → {@link IOnEquip} / {@link IOnUnequip}</li>
 *   <li>Player death → {@link IOnPlayerDeath}</li>
 *   <li>Wearer attacks → {@link IOnHit} (dispatched externally when the WEARER hits a target)</li>
 * </ul>
 */
public class ItemRPGArmor extends ItemArmor implements IRPGItemContainer {

    /** The global ID of this RPG item (matches JSON "id" field). */
    private final String rpgItemId;

    /** Tracking key for Excel enchants applied by the data-driven system. */
    public static final String NBT_TRACKER_TAG = "DarkGreyRPG_ExcelEnchants";

    /** Tracking version — compared against {@link RPGItemDataManager#getDataVersion()}. */
    public static final String NBT_VERSION_TAG = "DarkGreyRPG_DataVersion";

    // ─────────────────────────────────────────────────────────────────────────
    //  Component Lists (non-final for hot-reload)
    // ─────────────────────────────────────────────────────────────────────────

    /** All components attached to this armor piece. */
    private List<IRPGComponent> allComponents;

    /** Components that run periodically while worn (throttled to once per second). */
    private List<IOnWornTick> wornTickHandlers;

    /** Components that modify incoming damage (pipeline pattern, handled by EventBus). */
    private List<IOnHurt> hurtHandlers;

    /** Components that fire once when this armor is equipped (handled by EventBus). */
    private List<IOnEquip> equipHandlers;

    /** Components that fire once when this armor is unequipped (handled by EventBus). */
    private List<IOnUnequip> unequipHandlers;

    /** Components that contribute tooltip lines. */
    private List<IHasTooltip> tooltipHandlers;

    /** Components that modify item attributes. */
    private List<IAttributeModifier> attributeHandlers;

    /** Components that react to the player's death. */
    private List<IOnPlayerDeath> playerDeathHandlers;

    /**
     * Components that trigger when the WEARER attacks a target.
     * Attack-type components on armor — dispatched externally by {@code RPGCoreEventHandler}.
     */
    private List<IOnHit> hitHandlers;

    /**
     * Constructs a new RPG armor piece.
     *
     * @param id          the unique string ID of this armor item
     * @param material    the armor material (determines defense and durability)
     * @param renderIndex the render index for the armor model (usually 0 or 1)
     * @param armorType   the armor slot: 0=helmet, 1=chestplate, 2=leggings, 3=boots
     * @param components  the full list of RPG components to attach
     */
    public ItemRPGArmor(String id, ArmorMaterial material, int renderIndex, int armorType,
                        List<IRPGComponent> components) {
        super(material, renderIndex, armorType);
        this.rpgItemId = id;
        this.allComponents = Collections.unmodifiableList(components);
        this.wornTickHandlers = IRPGComponent.filterByCapability(components, IOnWornTick.class);
        this.hurtHandlers = IRPGComponent.filterByCapability(components, IOnHurt.class);
        this.equipHandlers = IRPGComponent.filterByCapability(components, IOnEquip.class);
        this.unequipHandlers = IRPGComponent.filterByCapability(components, IOnUnequip.class);
        this.tooltipHandlers = IRPGComponent.filterByCapability(components, IHasTooltip.class);
        this.attributeHandlers = IRPGComponent.filterByCapability(components, IAttributeModifier.class);
        this.playerDeathHandlers = IRPGComponent.filterByCapability(components, IOnPlayerDeath.class);
        this.hitHandlers = IRPGComponent.filterByCapability(components, IOnHit.class);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  IRPGItemContainer Implementation
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public String getRpgItemId() {
        return rpgItemId;
    }

    /**
     * Hot-reloads all components from the global data manager.
     * Called by the hot-reload system when the JSON config changes.
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
        this.wornTickHandlers = IRPGComponent.filterByCapability(newComponents, IOnWornTick.class);
        this.hurtHandlers = IRPGComponent.filterByCapability(newComponents, IOnHurt.class);
        this.equipHandlers = IRPGComponent.filterByCapability(newComponents, IOnEquip.class);
        this.unequipHandlers = IRPGComponent.filterByCapability(newComponents, IOnUnequip.class);
        this.tooltipHandlers = IRPGComponent.filterByCapability(newComponents, IHasTooltip.class);
        this.attributeHandlers = IRPGComponent.filterByCapability(newComponents, IAttributeModifier.class);
        this.playerDeathHandlers = IRPGComponent.filterByCapability(newComponents, IOnPlayerDeath.class);
        this.hitHandlers = IRPGComponent.filterByCapability(newComponents, IOnHit.class);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Item Method Overrides — Delegate to Components
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Called every tick while this armor piece is worn.
     * Throttled to once per second (every 20 game ticks) to prevent tick storms.
     * Server-side only.
     */
    @Override
    public void onArmorTick(World world, EntityPlayer player, ItemStack armorStack) {
        if (world.isRemote) {
            return;
        }

        // Throttle: only fire once per second (every 20 game ticks)
        if (world.getTotalWorldTime() % 20 != 0) {
            return;
        }

        for (IOnWornTick handler : wornTickHandlers) {
            handler.onWornTick(world, player, armorStack);
        }
    }

    /**
     * Appends component tooltip information to the armor's hover text.
     */
    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings("unchecked")
    public void addInformation(ItemStack armorStack, EntityPlayer player,
                               List tooltipLines, boolean showAdvanced) {
        super.addInformation(armorStack, player, tooltipLines, showAdvanced);
        for (IHasTooltip handler : tooltipHandlers) {
            handler.addTooltipLines(armorStack, player, tooltipLines, showAdvanced);
        }
    }

    /**
     * Returns the armor's attribute modifiers with component modifications applied.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Multimap getItemAttributeModifiers() {
        Multimap<String, AttributeModifier> attributeMap = super.getItemAttributeModifiers();
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
     * non-destructively. Syncs enchantments when data version changes.
     */
    @Override
    public void onUpdate(ItemStack stack, World world, net.minecraft.entity.Entity entity, int itemSlot, boolean isSelected) {
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

    // ─────────────────────────────────────────────────────────────────────────
    //  Enchantment Sync Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Non-destructive enchantment synchronization.
     * Preserves player-applied enchantments while syncing data-driven ones.
     */
    private void syncEnchantments(ItemStack stack, NBTTagCompound nbt, RPGItemDataManager.ItemConfig config) {
        Map<Integer, Integer> excelEnchants = parseEnchantments(config.enchantments);

        NBTTagCompound tracker = nbt.getCompoundTag(NBT_TRACKER_TAG);
        NBTTagList enchList = nbt.getTagList("ench", 10);
        NBTTagList newEnchList = new NBTTagList();

        // Step 1: Filter out old Excel enchants, keep player enchants
        for (int i = 0; i < enchList.tagCount(); i++) {
            NBTTagCompound enchTag = enchList.getCompoundTagAt(i);
            int id = enchTag.getShort("id");
            int lvl = enchTag.getShort("lvl");

            boolean wasAppliedBySystem = tracker.hasKey(String.valueOf(id)) && tracker.getInteger(String.valueOf(id)) == lvl;

            if (!wasAppliedBySystem) {
                newEnchList.appendTag(enchTag.copy());
            }
        }

        // Step 2: Apply new Excel enchants (only if they aren't already overridden by player)
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

    // ─────────────────────────────────────────────────────────────────────────
    //  Creative Tab
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Populates the creative tab with a pre-enchanted stack using the data manager config.
     */
    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings("unchecked")
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        ItemStack stack = new ItemStack(item, 1, 0);
        RPGItemDataManager.ItemConfig config = RPGItemDataManager.getInstance().getConfig(rpgItemId);
        if (config != null) {
            NBTTagCompound nbt = new NBTTagCompound();
            nbt.setInteger(NBT_VERSION_TAG, RPGItemDataManager.getInstance().getDataVersion());
            syncEnchantments(stack, nbt, config);
            stack.setTagCompound(nbt);
        }
        list.add(stack);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Public Accessors (for RPGCoreEventHandler)
    // ─────────────────────────────────────────────────────────────────────────

    public List<IRPGComponent> getAllComponents() {
        return allComponents;
    }

    public List<IOnHurt> getHurtHandlers() {
        return hurtHandlers;
    }

    public List<IOnEquip> getEquipHandlers() {
        return equipHandlers;
    }

    public List<IOnUnequip> getUnequipHandlers() {
        return unequipHandlers;
    }

    public List<IOnPlayerDeath> getPlayerDeathHandlers() {
        return playerDeathHandlers;
    }

    /**
     * Returns hit handlers for wearer-attacks-target dispatch.
     * These are attack-type components on armor that trigger when the WEARER attacks,
     * dispatched externally by {@code RPGCoreEventHandler}.
     */
    public List<IOnHit> getHitHandlers() {
        return hitHandlers;
    }
}