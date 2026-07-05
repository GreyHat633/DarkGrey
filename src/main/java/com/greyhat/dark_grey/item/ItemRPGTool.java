package com.greyhat.dark_grey.item;

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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Unified RPG tool container — extends {@link ItemTool}.
 *
 * <p>Consolidates the original framework's separate {@code ItemRPGToolPickaxe},
 * {@code ItemRPGToolAxe}, and {@code ItemRPGToolSpade} into a single class.
 * The tool type (and corresponding effective block set) is selected dynamically
 * at construction time based on the JSON {@code "type"} field.</p>
 *
 * <p>Implements {@link IRPGItemContainer} for hot-reload support via
 * {@link RPGItemDataManager}.</p>
 */
public class ItemRPGTool extends ItemTool implements IRPGItemContainer {

    // ─────────────────────────────────────────────────────────────────────────
    //  Effective Block Sets (matching vanilla tool definitions)
    // ─────────────────────────────────────────────────────────────────────────

    /** Blocks that pickaxes are effective against (matches vanilla ItemPickaxe). */
    @SuppressWarnings("unchecked")
    private static final Set<Block> PICKAXE_EFFECTIVE_BLOCKS = Sets.newHashSet(
        Blocks.cobblestone, Blocks.double_stone_slab, Blocks.stone_slab,
        Blocks.stone, Blocks.sandstone, Blocks.mossy_cobblestone,
        Blocks.iron_ore, Blocks.iron_block, Blocks.coal_ore, Blocks.gold_block,
        Blocks.gold_ore, Blocks.diamond_ore, Blocks.diamond_block, Blocks.ice,
        Blocks.netherrack, Blocks.lapis_ore, Blocks.lapis_block,
        Blocks.redstone_ore, Blocks.lit_redstone_ore, Blocks.rail,
        Blocks.detector_rail, Blocks.golden_rail, Blocks.activator_rail
    );

    /** Blocks that axes are effective against (matches vanilla ItemAxe). */
    @SuppressWarnings("unchecked")
    private static final Set<Block> AXE_EFFECTIVE_BLOCKS = Sets.newHashSet(
        Blocks.planks, Blocks.bookshelf, Blocks.log, Blocks.log2,
        Blocks.chest, Blocks.pumpkin, Blocks.lit_pumpkin
    );

    /** Blocks that shovels are effective against (matches vanilla ItemSpade). */
    @SuppressWarnings("unchecked")
    private static final Set<Block> SHOVEL_EFFECTIVE_BLOCKS = Sets.newHashSet(
        Blocks.grass, Blocks.dirt, Blocks.sand, Blocks.gravel,
        Blocks.snow_layer, Blocks.snow, Blocks.clay, Blocks.farmland,
        Blocks.soul_sand, Blocks.mycelium
    );

    // ─────────────────────────────────────────────────────────────────────────
    //  NBT Constants
    // ─────────────────────────────────────────────────────────────────────────

    /** NBT key used to track system-applied enchantments for non-destructive sync. */
    public static final String NBT_TRACKER_TAG = "DarkGreyRPG_ExcelEnchants";

    /** NBT key for the data version stamp, used to skip redundant sync passes. */
    public static final String NBT_VERSION_TAG = "DarkGreyRPG_DataVersion";

    // ─────────────────────────────────────────────────────────────────────────
    //  Component Lists (non-final for hot-reload support)
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
    //  IRPGItemContainer Implementation
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
        RPGItemDataManager.ItemConfig config = RPGItemDataManager.getInstance().getConfig(rpgItemId);
        if (config == null || config.componentsJson == null) return;

        List<IRPGComponent> newComponents = new ArrayList<>();
        for (JsonElement compElem : config.componentsJson) {
            JsonObject compObj = compElem.getAsJsonObject();
            String compName = compObj.get("name").getAsString();
            JsonObject params = compObj.has("params") ? compObj.getAsJsonObject("params") : new JsonObject();
            try {
                newComponents.add(ComponentRegistry.create(compName, params));
            } catch (Exception e) {
                FMLLog.warning("[DarkGrey-Tool] Failed to create component '%s' for tool '%s': %s",
                    compName, rpgItemId, e.getMessage());
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
    //  Static Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the default attack damage bonus for each tool type (matching vanilla values).
     * This bonus is added to the material's base damage.
     */
    private static float getDefaultAttackDamageBonus(String toolClass) {
        switch (toolClass) {
            case "pickaxe": return 2.0F;
            case "axe":     return 3.0F;
            case "shovel":  return 1.0F;
            default:        return 1.0F;
        }
    }

    /**
     * Returns the set of blocks this tool type is effective against.
     * Falls back to an empty set for unknown tool types.
     */
    private static Set<Block> getEffectiveBlocksForType(String toolClass) {
        switch (toolClass) {
            case "pickaxe": return PICKAXE_EFFECTIVE_BLOCKS;
            case "axe":     return AXE_EFFECTIVE_BLOCKS;
            case "shovel":  return SHOVEL_EFFECTIVE_BLOCKS;
            default:        return Collections.emptySet();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Dynamic Attributes & Durability from RPGItemDataManager
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
        RPGItemDataManager.ItemConfig config = RPGItemDataManager.getInstance().getConfig(rpgItemId);
        float damage = 0.0f;
        if (config != null) {
            damage = config.damage;
        }

        // Must use exactly this vanilla UUID so the engine recognizes it as base weapon damage
        UUID damageOverrideUUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
        attributeMap.put(SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName(),
            new AttributeModifier(damageOverrideUUID, "Tool modifier", (double) damage, 0));

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

    // ─────────────────────────────────────────────────────────────────────────
    //  Enchantment Sync via onUpdate()
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Ticks the item in the player's inventory to verify and dynamically merge
     * enchantment changes non-destructively. Uses version stamping to skip
     * redundant sync passes on every tick.
     */
    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected) {
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

        // Version mismatch detected — perform non-destructive sync
        syncEnchantments(stack, nbt, config);

        // Update version to prevent running again until next reload
        nbt.setInteger(NBT_VERSION_TAG, currentDataVersion);
    }

    /**
     * Non-destructive enchantment synchronization.
     * Strips previously system-applied enchantments, then re-applies current
     * config enchantments without overwriting player-applied enchantments.
     */
    private void syncEnchantments(ItemStack stack, NBTTagCompound nbt, RPGItemDataManager.ItemConfig config) {
        Map<Integer, Integer> excelEnchants = parseEnchantments(config.enchantments);

        NBTTagCompound tracker = nbt.getCompoundTag(NBT_TRACKER_TAG);

        NBTTagList enchList = nbt.getTagList("ench", 10);
        NBTTagList newEnchList = new NBTTagList();

        // Step 1: Filter out old system-applied enchants, keep player enchants
        for (int i = 0; i < enchList.tagCount(); i++) {
            NBTTagCompound enchTag = enchList.getCompoundTagAt(i);
            int id = enchTag.getShort("id");
            int lvl = enchTag.getShort("lvl");

            boolean wasAppliedBySystem = tracker.hasKey(String.valueOf(id))
                && tracker.getInteger(String.valueOf(id)) == lvl;

            if (!wasAppliedBySystem) {
                newEnchList.appendTag(enchTag.copy());
            }
        }

        // Step 2: Apply new config enchants (only if player hasn't overridden)
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

    /**
     * Parses the comma-separated enchantment string from JSON config.
     * Format: "id:level,id:level,..." — tolerates non-numeric name segments.
     */
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
    //  Creative Tab — Pre-synced Enchantments
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
    //  Item Method Overrides — Delegate to Components
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Called when a block is successfully destroyed with this tool.
     * Delegates to all {@link IOnBlockBreak} components.
     */
    @Override
    public boolean onBlockDestroyed(ItemStack toolStack, World world, Block block,
                                    int x, int y, int z, EntityLivingBase entityLiving) {
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
    public boolean hitEntity(ItemStack toolStack, EntityLivingBase target,
                             EntityLivingBase attacker) {
        boolean result = super.hitEntity(toolStack, target, attacker);

        if (!attacker.worldObj.isRemote) {
            float rawDamage = 1.0F;
            if (attacker.getEntityAttribute(SharedMonsterAttributes.attackDamage) != null) {
                rawDamage = (float) attacker.getEntityAttribute(SharedMonsterAttributes.attackDamage).getAttributeValue();
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
    public void addInformation(ItemStack toolStack, EntityPlayer player,
                               List tooltipLines, boolean showAdvanced) {
        super.addInformation(toolStack, player, tooltipLines, showAdvanced);
        for (IHasTooltip handler : tooltipHandlers) {
            handler.addTooltipLines(toolStack, player, tooltipLines, showAdvanced);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Public Accessors
    // ─────────────────────────────────────────────────────────────────────────

    public String getToolClass() {
        return toolClass;
    }

    public List<IRPGComponent> getAllComponents() {
        return allComponents;
    }

    public List<IOnPlayerDeath> getPlayerDeathHandlers() {
        return playerDeathHandlers;
    }
}