package com.greyhat.dark_grey.item;

import com.google.common.collect.Multimap;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.IRPGItemContainer;
import com.greyhat.dark_grey.api.RPGItemDataManager;
import com.greyhat.dark_grey.api.capability.IAttributeModifier;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnPlayerDeath;
import com.greyhat.dark_grey.api.capability.IOnRightClick;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Universal RPG Accessory container — extends {@link Item}.
 * Designed for items like rings, amulets, or off-hand items.
 *
 * <p>Implements {@link IRPGItemContainer} for hot-reload support via
 * the global {@link RPGItemDataManager}.</p>
 */
public class ItemRPGAccessory extends Item implements IRPGItemContainer {

    /** The global ID of this RPG item (matches JSON "id" field). */
    private final String rpgItemId;

    /** Tracking key for Excel enchants applied by the data-driven system. */
    public static final String NBT_TRACKER_TAG = "DarkGreyRPG_ExcelEnchants";

    /** Tracking version — compared against {@link RPGItemDataManager#getDataVersion()}. */
    public static final String NBT_VERSION_TAG = "DarkGreyRPG_DataVersion";

    // ─────────────────────────────────────────────────────────────────────────
    //  Component Lists (non-final for hot-reload)
    // ─────────────────────────────────────────────────────────────────────────

    /** All components attached to this accessory. */
    private List<IRPGComponent> allComponents;

    /** Components that contribute tooltip lines. */
    private List<IHasTooltip> tooltipHandlers;

    /** Components that modify item attributes. */
    private List<IAttributeModifier> attributeHandlers;

    /** Components that react to right-click in air. */
    private List<IOnRightClick> rightClickHandlers;

    /** Components that react to the player's death. */
    private List<IOnPlayerDeath> playerDeathHandlers;

    /**
     * Constructs a new RPG accessory with the given ID and components.
     *
     * @param id         the unique string ID of this accessory item
     * @param components the full list of RPG components to attach
     */
    public ItemRPGAccessory(String id, List<IRPGComponent> components) {
        super();
        this.setMaxStackSize(1);
        this.rpgItemId = id;
        this.allComponents = Collections.unmodifiableList(components);
        this.tooltipHandlers = IRPGComponent.filterByCapability(components, IHasTooltip.class);
        this.attributeHandlers = IRPGComponent.filterByCapability(components, IAttributeModifier.class);
        this.rightClickHandlers = IRPGComponent.filterByCapability(components, IOnRightClick.class);
        this.playerDeathHandlers = IRPGComponent.filterByCapability(components, IOnPlayerDeath.class);
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
        this.tooltipHandlers = IRPGComponent.filterByCapability(newComponents, IHasTooltip.class);
        this.attributeHandlers = IRPGComponent.filterByCapability(newComponents, IAttributeModifier.class);
        this.rightClickHandlers = IRPGComponent.filterByCapability(newComponents, IOnRightClick.class);
        this.playerDeathHandlers = IRPGComponent.filterByCapability(newComponents, IOnPlayerDeath.class);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Item Method Overrides — Delegate to Components
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Called when the player right-clicks in air with this accessory.
     * Chains through all {@link IOnRightClick} components.
     */
    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        ItemStack resultStack = stack;
        for (IOnRightClick handler : rightClickHandlers) {
            resultStack = handler.onRightClick(resultStack, world, player);
        }
        return resultStack;
    }

    /**
     * Appends component tooltip information to the accessory's hover text.
     */
    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings("unchecked")
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltipLines, boolean showAdvanced) {
        super.addInformation(stack, player, tooltipLines, showAdvanced);
        for (IHasTooltip handler : tooltipHandlers) {
            handler.addTooltipLines(stack, player, tooltipLines, showAdvanced);
        }
    }

    /**
     * Returns the accessory's attribute modifiers with component modifications applied.
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

    public List<IOnPlayerDeath> getPlayerDeathHandlers() {
        return playerDeathHandlers;
    }
}