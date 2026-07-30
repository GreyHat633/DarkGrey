package com.greyhat.dark_grey.item;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.world.World;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.greyhat.dark_grey.api.ComponentRegistry;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.IRPGItemContainer;
import com.greyhat.dark_grey.api.RPGItemDataManager;
import com.greyhat.dark_grey.api.RPGItemStackSync;
import com.greyhat.dark_grey.api.capability.IAttributeModifier;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnHit;
import com.greyhat.dark_grey.api.capability.IOnLeftClick;
import com.greyhat.dark_grey.api.capability.IOnPlayerDeath;
import com.greyhat.dark_grey.api.capability.IOnPlayerStoppedUsing;
import com.greyhat.dark_grey.api.capability.IOnRightClick;
import com.greyhat.dark_grey.api.capability.IOnWeaponUsingTick;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Item container for the "闁? (GUN) weapon category.
 *
 * <p>
 * Similar to {@link ItemRPGWeapon} (inherits {@code ItemSword} for melee base behavior),
 * but designed specifically for ranged weapons that:
 * <ul>
 * <li>Use right-click to begin a reload/charge sequence (via {@code setItemInUse})</li>
 * <li>Do NOT use the bow's pulling animation or its three _pulling_ textures</li>
 * <li>Support a reload-then-fire two-phase workflow managed by components</li>
 * </ul>
 * </p>
 *
 * <p>
 * The first-person equipped rendering uses a custom {@code RenderRPGGun} that adds
 * a subtle recoil/shake effect while the player is in the "using" state (reloading).
 * </p>
 */
public class ItemRPGGun extends ItemSword implements IRPGItemContainer {

    private final String rpgItemId;

    private List<IRPGComponent> allComponents;
    private List<IOnHit> hitHandlers;
    private List<IOnRightClick> rightClickHandlers;
    private List<IOnLeftClick> leftClickHandlers;
    private List<IHasTooltip> tooltipHandlers;
    private List<IAttributeModifier> attributeHandlers;
    private List<IOnPlayerDeath> playerDeathHandlers;
    private List<com.greyhat.dark_grey.api.capability.IOnHeldTick> heldTickHandlers;
    private List<IOnWeaponUsingTick> weaponUsingTickHandlers;
    private List<IOnPlayerStoppedUsing> playerStoppedUsingHandlers;

    private static final UUID ITEM_DAMAGE_UUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    private Multimap cachedModifiers = null;
    private int cachedModifiersVersion = -1;

    public ItemRPGGun(String id, ToolMaterial material, List<IRPGComponent> components) {
        super(material);
        this.setMaxDamage(0);
        this.rpgItemId = id;
        this.allComponents = Collections.unmodifiableList(components);
        rebuildCapabilityLists(components);
    }

    private void rebuildCapabilityLists(List<IRPGComponent> components) {
        this.hitHandlers = IRPGComponent.filterByCapability(components, IOnHit.class);
        this.rightClickHandlers = IRPGComponent.filterByCapability(components, IOnRightClick.class);
        this.leftClickHandlers = IRPGComponent.filterByCapability(components, IOnLeftClick.class);
        this.tooltipHandlers = IRPGComponent.filterByCapability(components, IHasTooltip.class);
        this.attributeHandlers = IRPGComponent.filterByCapability(components, IAttributeModifier.class);
        this.playerDeathHandlers = IRPGComponent.filterByCapability(components, IOnPlayerDeath.class);
        this.heldTickHandlers = IRPGComponent
            .filterByCapability(components, com.greyhat.dark_grey.api.capability.IOnHeldTick.class);
        this.weaponUsingTickHandlers = IRPGComponent.filterByCapability(components, IOnWeaponUsingTick.class);
        this.playerStoppedUsingHandlers = IRPGComponent.filterByCapability(components, IOnPlayerStoppedUsing.class);
    }

    @Override
    public String getRpgItemId() {
        return rpgItemId;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        RPGItemDataManager.ItemConfig config = RPGItemDataManager.getInstance()
            .getConfig(rpgItemId);
        if (config != null && config.displayName != null && !config.displayName.isEmpty()) {
            return config.displayName;
        }
        return super.getItemStackDisplayName(stack);
    }

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
                newComponents.add(ComponentRegistry.create(compName, params));
            } catch (Exception e) {
                com.greyhat.dark_grey.DarkGrey.LOG
                    .error("Failed to rebuild component " + compName + " for item " + rpgItemId, e);
            }
        }

        this.allComponents = Collections.unmodifiableList(newComponents);
        rebuildCapabilityLists(newComponents);
    }

    // 閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓
    // Combat: melee hit dispatch (same as ItemRPGWeapon)
    // 閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓

    @Override
    public boolean hitEntity(ItemStack weaponStack, EntityLivingBase target, EntityLivingBase attacker) {
        boolean result = super.hitEntity(weaponStack, target, attacker);
        if (!attacker.worldObj.isRemote) {
            float rawDamage = 1.0F;
            if (attacker.getEntityAttribute(SharedMonsterAttributes.attackDamage) != null) {
                rawDamage = (float) attacker.getEntityAttribute(SharedMonsterAttributes.attackDamage)
                    .getAttributeValue();
            }
            for (IOnHit handler : hitHandlers) {
                handler.onHit(weaponStack, attacker, target, rawDamage);
            }
        }
        return result;
    }

    // 閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓
    // Left-click: swing item (can be intercepted for firing)
    // 閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓

    @Override
    public boolean onEntitySwing(EntityLivingBase entityLiving, ItemStack stack) {
        if (!(entityLiving instanceof EntityPlayer)) return false;
        EntityPlayer player = (EntityPlayer) entityLiving;
        boolean cancelled = false;
        for (IOnLeftClick handler : leftClickHandlers) {
            if (handler.onLeftClick(stack, player)) {
                cancelled = true;
            }
        }
        return cancelled;
    }

    // 閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓
    // Right-click: start using (reload) or fire (via component IOnRightClick)
    // 閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        // First let IOnRightClick handlers run 鈥斺€?they may intercept for firing.
        ItemStack resultStack = stack;
        boolean intercepted = false;
        for (IOnRightClick handler : rightClickHandlers) {
            resultStack = handler.onRightClick(resultStack, world, player);
            // If a handler consumed the right-click (e.g. fired a projectile), we do
            // NOT enter the reload use-state. The handler signals this by calling
            // player.clearItemInUse() internally.
        }

        // If the player is not already "using" the item after the handler chain,
        // begin the reload sequence by entering the use state.
        if (player.getItemInUse() == null) {
            player.setItemInUse(resultStack, this.getMaxItemUseDuration(resultStack));
        }
        return resultStack;
    }

    @Override
    public void onUsingTick(ItemStack stack, EntityPlayer player, int count) {
        if (weaponUsingTickHandlers != null) {
            for (IOnWeaponUsingTick handler : weaponUsingTickHandlers) {
                handler.onUsingTick(stack, player, count);
            }
        }
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityPlayer player, int timeLeft) {
        if (playerStoppedUsingHandlers != null) {
            for (IOnPlayerStoppedUsing handler : playerStoppedUsingHandlers) {
                handler.onPlayerStoppedUsing(stack, world, player, timeLeft);
            }
        }
    }

    // 閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓
    // Tooltip
    // 閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings("unchecked")
    public void addInformation(ItemStack weaponStack, EntityPlayer player, List tooltipLines, boolean showAdvanced) {
        super.addInformation(weaponStack, player, tooltipLines, showAdvanced);
        for (IHasTooltip handler : tooltipHandlers) {
            handler.addTooltipLines(weaponStack, player, tooltipLines, showAdvanced);
        }
    }

    // 閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓
    // Attribute modifiers (attack damage from JSON config)
    // 閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓

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
                new AttributeModifier(ITEM_DAMAGE_UUID, "Weapon modifier", (double) damage, 0));
            for (IAttributeModifier handler : attributeHandlers) {
                handler.modifyAttributes(attributeMap);
            }
            this.cachedModifiers = attributeMap;
            this.cachedModifiersVersion = currentVersion;
        }
        return this.cachedModifiers;
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return 0;
    }

    // 閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓
    // Tick: held-tick dispatch + NBT sync
    // 閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓

    @Override
    public void onUpdate(ItemStack stack, World world, net.minecraft.entity.Entity entity, int itemSlot,
        boolean isSelected) {
        // Server-side held capabilities are dispatched from PlayerTickEvent so they
        // do not depend on ItemStack's selected flag. Keep this path for client visuals.
        if (world.isRemote && isSelected && entity instanceof EntityPlayer) {
            for (com.greyhat.dark_grey.api.capability.IOnHeldTick handler : heldTickHandlers) {
                handler.onHeldTick(stack, world, (EntityPlayer) entity);
            }
        }
        RPGItemStackSync.syncIfVersionChanged(stack, rpgItemId, world);
    }

    // 閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓
    // Container accessors
    // 閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓

    @Override
    public List<IRPGComponent> getAllComponents() {
        return allComponents;
    }

    public List<IOnHit> getHitHandlers() {
        return hitHandlers;
    }

    @Override
    public List<IOnPlayerDeath> getPlayerDeathHandlers() {
        return playerDeathHandlers;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings("unchecked")
    public void getSubItems(net.minecraft.item.Item item, CreativeTabs tab, List list) {
        ItemStack stack = new ItemStack(item, 1, 0);
        RPGItemStackSync.forceSync(stack, rpgItemId);
        list.add(stack);
    }
}
