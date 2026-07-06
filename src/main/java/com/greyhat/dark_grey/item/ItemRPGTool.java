package com.greyhat.dark_grey.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.world.World;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.greyhat.dark_grey.api.ComponentRegistry;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.IRPGItemContainer;
import com.greyhat.dark_grey.api.RPGItemDataManager;
import com.greyhat.dark_grey.api.capability.IAttributeModifier;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnBlockBreak;
import com.greyhat.dark_grey.api.capability.IOnDigSpeed;
import com.greyhat.dark_grey.api.capability.IOnHit;
import com.greyhat.dark_grey.api.capability.IOnPlayerDeath;
import com.greyhat.dark_grey.api.capability.IOnRightClick;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Unified RPG tool container — extends {@link ItemTool}.
 *
 * <p>
 * Consolidates the original framework's separate {@code ItemRPGToolPickaxe},
 * {@code ItemRPGToolAxe}, and {@code ItemRPGToolSpade} into a single class.
 * The tool type (and corresponding effective block set) is selected dynamically
 * at construction time based on the JSON {@code "type"} field.
 * </p>
 *
 * <p>
 * Implements {@link IRPGItemContainer} for hot-reload support via
 * {@link RPGItemDataManager}.
 * </p>
 */
public class ItemRPGTool extends ItemTool implements IRPGItemContainer {

    // ─────────────────────────────────────────────────────────────────────────
    // Effective Block Sets (matching vanilla tool definitions)
    // ─────────────────────────────────────────────────────────────────────────

    /** Blocks that pickaxes are effective against (matches vanilla ItemPickaxe). */
    @SuppressWarnings("unchecked")
    private static final Set<Block> PICKAXE_EFFECTIVE_BLOCKS = Sets.newHashSet(
        Blocks.cobblestone,
        Blocks.double_stone_slab,
        Blocks.stone_slab,
        Blocks.stone,
        Blocks.sandstone,
        Blocks.mossy_cobblestone,
        Blocks.iron_ore,
        Blocks.iron_block,
        Blocks.coal_ore,
        Blocks.gold_block,
        Blocks.gold_ore,
        Blocks.diamond_ore,
        Blocks.diamond_block,
        Blocks.ice,
        Blocks.netherrack,
        Blocks.lapis_ore,
        Blocks.lapis_block,
        Blocks.redstone_ore,
        Blocks.lit_redstone_ore,
        Blocks.rail,
        Blocks.detector_rail,
        Blocks.golden_rail,
        Blocks.activator_rail);

    /** Blocks that axes are effective against (matches vanilla ItemAxe). */
    @SuppressWarnings("unchecked")
    private static final Set<Block> AXE_EFFECTIVE_BLOCKS = Sets.newHashSet(
        Blocks.planks,
        Blocks.bookshelf,
        Blocks.log,
        Blocks.log2,
        Blocks.chest,
        Blocks.pumpkin,
        Blocks.lit_pumpkin);

    /** Blocks that shovels are effective against (matches vanilla ItemSpade). */
    @SuppressWarnings("unchecked")
    private static final Set<Block> SHOVEL_EFFECTIVE_BLOCKS = Sets.newHashSet(
        Blocks.grass,
        Blocks.dirt,
        Blocks.sand,
        Blocks.gravel,
        Blocks.snow_layer,
        Blocks.snow,
        Blocks.clay,
        Blocks.farmland,
        Blocks.soul_sand,
        Blocks.mycelium);

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

    private List<IRPGComponent> allComponents;
    private List<IOnBlockBreak> blockBreakHandlers;
    private List<IOnDigSpeed> digSpeedHandlers;
    private List<IOnHit> hitHandlers;
    private List<IOnRightClick> rightClickHandlers;
    private List<IHasTooltip> tooltipHandlers;
    private List<IAttributeModifier> attributeHandlers;
    private List<IOnPlayerDeath> playerDeathHandlers;

    /** The unique RPG item ID (matches the JSON "id" field). */
    private final String rpgItemId;

    /** The Forge tool class string (e.g. "pickaxe", "axe", "shovel"). */
    private final String toolClass;

    /**
     * Constructs a new RPG tool.
     *
     * @param id         the unique string ID of this tool (matches JSON "id")
     * @param material   the tool material (determines harvest level, durability, speed)
     * @param toolClass  the Forge tool class: "pickaxe", "axe", or "shovel"
     * @param components the full list of RPG components to attach
     */
    public ItemRPGTool(String id, ToolMaterial material, String toolClass, List<IRPGComponent> components) {
        super(getDefaultAttackDamageBonus(toolClass), material, getEffectiveBlocksForType(toolClass));
        this.rpgItemId = id;
        this.toolClass = toolClass;
        this.setHarvestLevel(toolClass, material.getHarvestLevel());

        this.allComponents = Collections.unmodifiableList(components);
        this.blockBreakHandlers = IRPGComponent.filterByCapability(components, IOnBlockBreak.class);
        this.digSpeedHandlers = IRPGComponent.filterByCapability(components, IOnDigSpeed.class);
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
                    "[DarkGrey-Tool] Failed to create component '%s' for tool '%s': %s",
                    compName,
                    rpgItemId,
                    e.getMessage());
            }
        }

        this.allComponents = Collections.unmodifiableList(newComponents);
        this.blockBreakHandlers = IRPGComponent.filterByCapability(newComponents, IOnBlockBreak.class);
        this.digSpeedHandlers = IRPGComponent.filterByCapability(newComponents, IOnDigSpeed.class);
        this.hitHandlers = IRPGComponent.filterByCapability(newComponents, IOnHit.class);
        this.rightClickHandlers = IRPGComponent.filterByCapability(newComponents, IOnRightClick.class);
        this.tooltipHandlers = IRPGComponent.filterByCapability(newComponents, IHasTooltip.class);
        this.attributeHandlers = IRPGComponent.filterByCapability(newComponents, IAttributeModifier.class);
        this.playerDeathHandlers = IRPGComponent.filterByCapability(newComponents, IOnPlayerDeath.class);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Static Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the default attack damage bonus for each tool type (matching vanilla values).
     * This bonus is added to the material's base damage.
     */
    private static float getDefaultAttackDamageBonus(String toolClass) {
        switch (toolClass) {
            case "pickaxe":
                return 2.0F;
            case "axe":
                return 3.0F;
            case "shovel":
                return 1.0F;
            default:
                return 1.0F;
        }
    }

    /**
     * Returns the set of blocks this tool type is effective against.
     * Falls back to an empty set for unknown tool types.
     */
    private static Set<Block> getEffectiveBlocksForType(String toolClass) {
        switch (toolClass) {
            case "pickaxe":
                return PICKAXE_EFFECTIVE_BLOCKS;
            case "axe":
                return AXE_EFFECTIVE_BLOCKS;
            case "shovel":
                return SHOVEL_EFFECTIVE_BLOCKS;
            default:
                return Collections.emptySet();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Dynamic Attributes & Durability from RPGItemDataManager
    // ─────────────────────────────────────────────────────────────────────────

    private static final UUID ITEM_DAMAGE_UUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    private Multimap cachedModifiers = null;
    private int cachedModifiersVersion = -1;

    /**
     * Returns attribute modifiers with attack damage dynamically read from
     * {@link RPGItemDataManager}, then applies any component-based modifiers.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Multimap getItemAttributeModifiers() {
        int currentVersion = RPGItemDataManager.getInstance()
            .getDataVersion();
        if (this.cachedModifiers == null || this.cachedModifiersVersion != currentVersion) {
            Multimap attributeMap = HashMultimap.create();
            RPGItemDataManager.ItemConfig config = RPGItemDataManager.getInstance()
                .getConfig(rpgItemId);
            float damage = 0.0f;
            if (config != null) {
                damage = config.damage;
            }
            attributeMap.put(
                SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName(),
                new AttributeModifier(ITEM_DAMAGE_UUID, "Tool modifier", (double) damage, 0));
            for (IAttributeModifier handler : attributeHandlers) {
                handler.modifyAttributes(attributeMap);
            }
            this.cachedModifiers = attributeMap;
            this.cachedModifiersVersion = currentVersion;
        }
        return this.cachedModifiers;
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
     * Called when a block is successfully destroyed with this tool.
     * Delegates to all {@link IOnBlockBreak} components.
     */
    @Override
    public boolean onBlockDestroyed(ItemStack toolStack, World world, Block block, int x, int y, int z,
        EntityLivingBase entityLiving) {
        if (!world.isRemote && entityLiving instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entityLiving;
            int blockMetadata = world.getBlockMetadata(x, y, z);
            for (IOnBlockBreak handler : blockBreakHandlers) {
                handler.onBlockBreak(toolStack, player, world, x, y, z, block, blockMetadata);
            }
        }
        return super.onBlockDestroyed(toolStack, world, block, x, y, z, entityLiving);
    }

    /**
     * Returns the dig speed for this tool against the given block.
     * Passes through all {@link IOnDigSpeed} components in a pipeline.
     */
    @Override
    public float getDigSpeed(ItemStack toolStack, Block block, int metadata) {
        float currentSpeed = super.getDigSpeed(toolStack, block, metadata);
        for (IOnDigSpeed handler : digSpeedHandlers) {
            currentSpeed = handler.modifyDigSpeed(toolStack, block, metadata, currentSpeed);
        }
        return currentSpeed;
    }

    /**
     * Called when this tool hits an entity (e.g. using an axe as a weapon).
     * Uses the attacker's attack damage attribute for a predictable RPG damage value
     * instead of unreliable health snapshotting.
     */
    @Override
    public boolean hitEntity(ItemStack toolStack, EntityLivingBase target, EntityLivingBase attacker) {
        boolean result = super.hitEntity(toolStack, target, attacker);

        if (!attacker.worldObj.isRemote) {
            float rawDamage = 1.0F;
            if (attacker.getEntityAttribute(SharedMonsterAttributes.attackDamage) != null) {
                rawDamage = (float) attacker.getEntityAttribute(SharedMonsterAttributes.attackDamage)
                    .getAttributeValue();
            }

            for (IOnHit handler : hitHandlers) {
                handler.onHit(toolStack, attacker, target, rawDamage);
            }
        }
        return result;
    }

    /**
     * Called when the player right-clicks in air with this tool.
     */
    @Override
    public ItemStack onItemRightClick(ItemStack toolStack, World world, EntityPlayer player) {
        ItemStack resultStack = toolStack;
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
    public void addInformation(ItemStack toolStack, EntityPlayer player, List tooltipLines, boolean showAdvanced) {
        super.addInformation(toolStack, player, tooltipLines, showAdvanced);
        for (IHasTooltip handler : tooltipHandlers) {
            handler.addTooltipLines(toolStack, player, tooltipLines, showAdvanced);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public Accessors
    // ─────────────────────────────────────────────────────────────────────────

    public String getToolClass() {
        return toolClass;
    }

    public List<IRPGComponent> getAllComponents() {
        return allComponents;
    }

    @Override
    public List<IOnPlayerDeath> getPlayerDeathHandlers() {
        return playerDeathHandlers;
    }
}
