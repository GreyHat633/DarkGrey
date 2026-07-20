package com.greyhat.dark_grey.item;

import java.util.Collections;
import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.google.common.collect.Multimap;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.IRPGItemContainer;
import com.greyhat.dark_grey.api.RPGItemDataManager;
import com.greyhat.dark_grey.api.RPGItemStackSync;
import com.greyhat.dark_grey.api.capability.IAttributeModifier;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnPlayerDeath;
import com.greyhat.dark_grey.api.capability.IOnRightClick;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Universal RPG Accessory container — extends {@link Item}.
 * Designed for items like rings, amulets, or off-hand items.
 *
 * <p>
 * Implements {@link IRPGItemContainer} for hot-reload support via
 * the global {@link RPGItemDataManager}.
 * </p>
 */
public class ItemRPGAccessory extends Item implements IRPGItemContainer {

    /** The global ID of this RPG item (matches JSON "id" field). */
    private final String rpgItemId;

    /** Tracking key for Excel enchants applied by the data-driven system. */
    public static final String NBT_TRACKER_TAG = "DarkGreyRPG_ExcelEnchants";

    /** Tracking version — compared against {@link RPGItemDataManager#getDataVersion()}. */
    public static final String NBT_VERSION_TAG = "DarkGreyRPG_DataVersion";

    // ─────────────────────────────────────────────────────────────────────────
    // Component Lists (non-final for hot-reload)
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
    // IRPGItemContainer Implementation
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
        RPGItemDataManager.ItemConfig config = RPGItemDataManager.getInstance()
            .getConfig(rpgItemId);
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

        this.allComponents = Collections.unmodifiableList(newComponents);
        this.tooltipHandlers = IRPGComponent.filterByCapability(newComponents, IHasTooltip.class);
        this.attributeHandlers = IRPGComponent.filterByCapability(newComponents, IAttributeModifier.class);
        this.rightClickHandlers = IRPGComponent.filterByCapability(newComponents, IOnRightClick.class);
        this.playerDeathHandlers = IRPGComponent.filterByCapability(newComponents, IOnPlayerDeath.class);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Item Method Overrides — Delegate to Components
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
    public void onUpdate(ItemStack stack, World world, net.minecraft.entity.Entity entity, int itemSlot,
        boolean isSelected) {
        RPGItemStackSync.syncIfVersionChanged(stack, rpgItemId, world);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Creative Tab
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Populates the creative tab with a pre-enchanted stack using the data manager config.
     */
    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings("unchecked")
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        ItemStack stack = new ItemStack(item, 1, 0);
        RPGItemStackSync.forceSync(stack, rpgItemId);
        list.add(stack);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public Accessors (for RPGCoreEventHandler)
    // ─────────────────────────────────────────────────────────────────────────

    public List<IRPGComponent> getAllComponents() {
        return allComponents;
    }

    @Override
    public List<IOnPlayerDeath> getPlayerDeathHandlers() {
        return playerDeathHandlers;
    }
}
