package com.greyhat.dark_grey.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.greyhat.dark_grey.api.ComponentRegistry;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.IRPGItemContainer;
import com.greyhat.dark_grey.api.RPGItemDataManager;
import com.greyhat.dark_grey.api.capability.IAttributeModifier;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnHit;
import com.greyhat.dark_grey.api.capability.IOnPlayerDeath;
import com.greyhat.dark_grey.api.capability.IOnRightClick;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * RPG hoe container — extends {@link ItemHoe}.
 *
 * <p>
 * This container exists separately because {@link ItemHoe} extends {@code Item}
 * directly (not {@code ItemTool}), so it cannot be merged into {@link ItemRPGTool}.
 * In Java's single-inheritance model, one class cannot extend both
 * {@code ItemTool} and {@code ItemHoe}.
 * </p>
 *
 * <p>
 * The hoe's tilling behavior (right-click on dirt/grass to create farmland)
 * is inherited from {@link ItemHoe#onItemUse}. Components here handle additional
 * behaviors like right-click effects in air, tooltips, attribute modifiers,
 * hit effects, and player death reactions.
 * </p>
 *
 * <p>
 * Implements {@link IRPGItemContainer} for hot-reload support via
 * {@link RPGItemDataManager}.
 * </p>
 */
public class ItemRPGHoe extends ItemHoe implements IRPGItemContainer {

    // ─────────────────────────────────────────────────────────────────────────
    // NBT Constants
    // ─────────────────────────────────────────────────────────────────────────

    /** NBT key used to track system-applied enchantments for non-destructive sync. */
    public static final String NBT_TRACKER_TAG = "DarkGreyRPG_ExcelEnchants";

    /** NBT key for the data version stamp, used to skip redundant sync passes. */
    public static final String NBT_VERSION_TAG = "DarkGreyRPG_DataVersion";

    // ─────────────────────────────────────────────────────────────────────────
    // Component Lists (non-final for hot-reload support)
    // ─────────────────────────────────────────────────────────────────────────

    /** All components attached to this hoe. */
    private List<IRPGComponent> allComponents;

    /** Components that react to hitting an entity. */
    private List<IOnHit> hitHandlers;

    /** Components that react to right-click in air. */
    private List<IOnRightClick> rightClickHandlers;

    /** Components that contribute tooltip lines. */
    private List<IHasTooltip> tooltipHandlers;

    /** Components that modify item attributes. */
    private List<IAttributeModifier> attributeHandlers;

    /** Components that react to the player's death. */
    private List<IOnPlayerDeath> playerDeathHandlers;

    /** The unique RPG item ID (matches the JSON "id" field). */
    private final String rpgItemId;

    /**
     * Constructs a new RPG hoe.
     *
     * @param id         the unique string ID of this hoe (matches JSON "id")
     * @param material   the tool material (determines durability)
     * @param components the full list of RPG components to attach
     */
    public ItemRPGHoe(String id, ToolMaterial material, List<IRPGComponent> components) {
        super(material);
        this.rpgItemId = id;

        this.allComponents = Collections.unmodifiableList(components);
        this.hitHandlers = IRPGComponent.filterByCapability(components, IOnHit.class);
        this.rightClickHandlers = IRPGComponent.filterByCapability(components, IOnRightClick.class);
        this.tooltipHandlers = IRPGComponent.filterByCapability(components, IHasTooltip.class);
        this.attributeHandlers = IRPGComponent.filterByCapability(components, IAttributeModifier.class);
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
     * Re-reads {@code config.componentsJson} and re-filters all capability sublists.
     */
    @Override
    public void rebuildComponents() {
        RPGItemDataManager.ItemConfig config = RPGItemDataManager.getInstance()
            .getConfig(rpgItemId);
        if (config == null || config.componentsJson == null) return;

        List<IRPGComponent> newComponents = new ArrayList<>();
        for (JsonElement compElem : config.componentsJson) {
            JsonObject compObj = compElem.getAsJsonObject();
            String compName = compObj.get("name")
                .getAsString();
            JsonObject params = compObj.has("params") ? compObj.getAsJsonObject("params") : new JsonObject();
            try {
                newComponents.add(ComponentRegistry.create(compName, params));
            } catch (Exception e) {
                FMLLog.warning(
                    "[DarkGrey-Hoe] Failed to create component '%s' for hoe '%s': %s",
                    compName,
                    rpgItemId,
                    e.getMessage());
            }
        }

        this.allComponents = Collections.unmodifiableList(newComponents);
        this.hitHandlers = IRPGComponent.filterByCapability(newComponents, IOnHit.class);
        this.rightClickHandlers = IRPGComponent.filterByCapability(newComponents, IOnRightClick.class);
        this.tooltipHandlers = IRPGComponent.filterByCapability(newComponents, IHasTooltip.class);
        this.attributeHandlers = IRPGComponent.filterByCapability(newComponents, IAttributeModifier.class);
        this.playerDeathHandlers = IRPGComponent.filterByCapability(newComponents, IOnPlayerDeath.class);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Dynamic Attributes & Durability from RPGItemDataManager
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns attribute modifiers with attack damage dynamically read from
     * {@link RPGItemDataManager}, then applies any component-based modifiers.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Multimap getItemAttributeModifiers() {
        Multimap attributeMap = HashMultimap.create();

        // Dynamically fetch damage from the global manager
        RPGItemDataManager.ItemConfig config = RPGItemDataManager.getInstance()
            .getConfig(rpgItemId);
        float damage = 0.0f;
        if (config != null) {
            damage = config.damage;
        }

        // Must use exactly this vanilla UUID so the engine recognizes it as base weapon damage
        UUID damageOverrideUUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
        attributeMap.put(
            SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName(),
            new AttributeModifier(damageOverrideUUID, "Hoe modifier", (double) damage, 0));

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
        RPGItemDataManager.ItemConfig config = RPGItemDataManager.getInstance()
            .getConfig(rpgItemId);
        if (config != null && config.durability > 0) {
            return config.durability;
        }
        return super.getMaxDamage(stack);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Enchantment Sync via onUpdate()
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Ticks the item in the player's inventory to verify and dynamically merge
     * enchantment changes non-destructively. Uses version stamping to skip
     * redundant sync passes on every tick.
     */
    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected) {
        com.greyhat.dark_grey.api.RPGItemStackSync.syncIfVersionChanged(stack, rpgItemId, world);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Creative Tab — Pre-synced Enchantments
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Overrides creative tab listing to produce an ItemStack with enchantments
     * already applied, so the item appears correctly in the creative inventory.
     */
    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings("unchecked")
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        ItemStack stack = new ItemStack(item, 1, 0);
        com.greyhat.dark_grey.api.RPGItemStackSync.forceSync(stack, rpgItemId);
        list.add(stack);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Item Method Overrides — Delegate to Components
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Called when this hoe hits an entity.
     * Uses the attacker's attack damage attribute for a predictable RPG damage value
     * instead of unreliable health snapshotting.
     */
    @Override
    public boolean hitEntity(ItemStack hoeStack, EntityLivingBase target, EntityLivingBase attacker) {
        boolean result = super.hitEntity(hoeStack, target, attacker);

        if (!attacker.worldObj.isRemote) {
            float rawDamage = 1.0F;
            if (attacker.getEntityAttribute(SharedMonsterAttributes.attackDamage) != null) {
                rawDamage = (float) attacker.getEntityAttribute(SharedMonsterAttributes.attackDamage)
                    .getAttributeValue();
            }

            for (IOnHit handler : hitHandlers) {
                handler.onHit(hoeStack, attacker, target, rawDamage);
            }
        }
        return result;
    }

    /**
     * Called when the player right-clicks in air with this hoe.
     * Note: Right-click on blocks (tilling) is handled by the inherited
     * {@link ItemHoe#onItemUse} method and is NOT intercepted here.
     */
    @Override
    public ItemStack onItemRightClick(ItemStack hoeStack, World world, EntityPlayer player) {
        ItemStack resultStack = hoeStack;
        for (IOnRightClick handler : rightClickHandlers) {
            resultStack = handler.onRightClick(resultStack, world, player);
        }
        return resultStack;
    }

    /**
     * Appends component tooltip information.
     */
    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings("unchecked")
    public void addInformation(ItemStack hoeStack, EntityPlayer player, List tooltipLines, boolean showAdvanced) {
        super.addInformation(hoeStack, player, tooltipLines, showAdvanced);
        for (IHasTooltip handler : tooltipHandlers) {
            handler.addTooltipLines(hoeStack, player, tooltipLines, showAdvanced);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public Accessors
    // ─────────────────────────────────────────────────────────────────────────

    public List<IRPGComponent> getAllComponents() {
        return allComponents;
    }

    @Override
    public List<IOnPlayerDeath> getPlayerDeathHandlers() {
        return playerDeathHandlers;
    }
}
