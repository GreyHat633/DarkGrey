package com.greyhat.dark_grey.event;

import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.capability.IOnEquip;
import com.greyhat.dark_grey.api.capability.IOnHit;
import com.greyhat.dark_grey.api.capability.IOnHurt;
import com.greyhat.dark_grey.api.capability.IOnPlayerDeath;
import com.greyhat.dark_grey.api.capability.IOnUnequip;
import com.greyhat.dark_grey.api.IRPGItemContainer;
import com.greyhat.dark_grey.api.SetBonusManager;
import com.greyhat.dark_grey.item.ItemRPGArmor;
import com.greyhat.dark_grey.item.ItemRPGBow;
import com.greyhat.dark_grey.item.ItemRPGWeapon;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.List;
import java.util.WeakHashMap;

/**
 * Centralized Forge EventBus handler for all cross-cutting RPG component triggers.
 *
 * <p>Handles three categories of events:</p>
 * <ul>
 *   <li>{@code LivingHurtEvent} → armor {@link IOnHurt} pipeline, armor IOnHit dispatch
 *       (when wearer attacks), bow IOnHit dispatch (when arrow hits)</li>
 *   <li>{@code PlayerTickEvent} → {@link IOnEquip} / {@link IOnUnequip} detection</li>
 *   <li>{@code LivingDeathEvent} → {@link IOnPlayerDeath} on all equipped RPG items</li>
 * </ul>
 *
 * <p>Performance: All checks use fast {@code instanceof} guards. Non-RPG entities
 * and items are skipped in O(1). The handler never iterates the full entity list.</p>
 */
public class RPGCoreEventHandler {

    /**
     * Per-player snapshot of armor slots from the previous tick.
     * WeakHashMap auto-cleans when a player disconnects.
     */
    private static final WeakHashMap<EntityPlayer, Item[]> PREVIOUS_ARMOR_ITEMS = new WeakHashMap<>();

    // ─────────────────────────────────────────────────────────────────────────
    //  LivingHurtEvent → IOnHurt (Armor Damage Pipeline)
    //                   + IOnHit (Armor attack components + Bow arrow components)
    // ─────────────────────────────────────────────────────────────────────────

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.entity.worldObj.isRemote) {
            return;
        }

        EntityLivingBase hurtEntity = event.entityLiving;
        float modifiedDamage = event.ammount;

        // ── Part 1: Armor damage pipeline (IOnHurt on the TARGET's armor) ──
        for (int equipmentSlot = 1; equipmentSlot <= 4; equipmentSlot++) {
            ItemStack armorStack = hurtEntity.getEquipmentInSlot(equipmentSlot);
            if (armorStack == null || !(armorStack.getItem() instanceof ItemRPGArmor)) {
                continue;
            }

            ItemRPGArmor rpgArmor = (ItemRPGArmor) armorStack.getItem();
            for (IOnHurt hurtHandler : rpgArmor.getHurtHandlers()) {
                modifiedDamage = hurtHandler.onHurt(armorStack, hurtEntity, event.source, modifiedDamage);
            }
        }
        event.ammount = modifiedDamage;

        // ── Part 2: Armor attack components (IOnHit on the ATTACKER's armor) ──
        // When a player attacks something while wearing RPG armor with IOnHit components,
        // those components trigger with the same semantics as weapon components.
        Entity sourceEntity = event.source.getEntity();
        if (sourceEntity instanceof EntityPlayer) {
            EntityPlayer attacker = (EntityPlayer) sourceEntity;

            // Check attacker's 4 armor slots
            for (int slot = 0; slot < 4; slot++) {
                ItemStack armorStack = attacker.inventory.armorInventory[slot];
                if (armorStack == null || !(armorStack.getItem() instanceof ItemRPGArmor)) {
                    continue;
                }

                ItemRPGArmor rpgArmor = (ItemRPGArmor) armorStack.getItem();
                List<IOnHit> hitHandlers = rpgArmor.getHitHandlers();
                if (hitHandlers.isEmpty()) continue;

                for (IOnHit handler : hitHandlers) {
                    handler.onHit(armorStack, attacker, hurtEntity, event.ammount);
                }
            }

            // ── Part 3: Bow arrow impact (IOnHit on the shooter's bow) ──
            // Check if the damage came from an arrow and the shooter is holding an RPG bow
            Entity directSource = event.source.getSourceOfDamage();
            if (directSource instanceof EntityArrow && directSource != sourceEntity) {
                // This is projectile damage — check if the shooter is holding an RPG bow
                ItemStack heldItem = attacker.getCurrentEquippedItem();
                if (heldItem != null && heldItem.getItem() instanceof ItemRPGBow) {
                    ItemRPGBow rpgBow = (ItemRPGBow) heldItem.getItem();
                    List<IOnHit> bowHitHandlers = rpgBow.getHitHandlers();
                    for (IOnHit handler : bowHitHandlers) {
                        handler.onHit(heldItem, attacker, hurtEntity, event.ammount);
                    }
                }
            }

            // ── Part 4: Armor Set Bonuses ──
            event.ammount = SetBonusManager.fireOnHit(attacker, hurtEntity, event.ammount);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PlayerTickEvent → IOnEquip / IOnUnequip (Armor Change Detection)
    // ─────────────────────────────────────────────────────────────────────────

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (event.player.worldObj.isRemote) {
            return;
        }

        EntityPlayer player = event.player;
        Item[] previousItems = PREVIOUS_ARMOR_ITEMS.get(player);

        if (previousItems == null) {
            previousItems = new Item[4];
            for (int slotIndex = 0; slotIndex < 4; slotIndex++) {
                ItemStack currentStack = player.inventory.armorInventory[slotIndex];
                previousItems[slotIndex] = (currentStack != null) ? currentStack.getItem() : null;

                if (currentStack != null) {
                    fireEquipEvent(player, currentStack);
                }
            }
            PREVIOUS_ARMOR_ITEMS.put(player, previousItems);
            SetBonusManager.recalculateSets(player);
            return;
        }

        boolean changed = false;
        for (int slotIndex = 0; slotIndex < 4; slotIndex++) {
            ItemStack currentStack = player.inventory.armorInventory[slotIndex];
            Item currentItem = (currentStack != null) ? currentStack.getItem() : null;
            Item previousItem = previousItems[slotIndex];

            if (currentItem != previousItem) {
                if (previousItem instanceof ItemRPGArmor) {
                    fireUnequipEventForItem(player, (ItemRPGArmor) previousItem);
                }
                if (currentStack != null) {
                    fireEquipEvent(player, currentStack);
                }
                previousItems[slotIndex] = currentItem;
                changed = true;
            }
        }

        if (changed) {
            SetBonusManager.recalculateSets(player);
        }
    }

    private void fireEquipEvent(EntityPlayer player, ItemStack armorStack) {
        if (!(armorStack.getItem() instanceof ItemRPGArmor)) {
            return;
        }
        ItemRPGArmor rpgArmor = (ItemRPGArmor) armorStack.getItem();
        for (IOnEquip equipHandler : rpgArmor.getEquipHandlers()) {
            equipHandler.onEquip(player.worldObj, player, armorStack);
        }
    }

    private void fireUnequipEventForItem(EntityPlayer player, ItemRPGArmor rpgArmor) {
        ItemStack phantomStack = new ItemStack(rpgArmor);
        for (IOnUnequip unequipHandler : rpgArmor.getUnequipHandlers()) {
            unequipHandler.onUnequip(player.worldObj, player, phantomStack);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  LivingDeathEvent → IOnPlayerDeath (All RPG item types)
    // ─────────────────────────────────────────────────────────────────────────

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (event.entity.worldObj.isRemote) {
            return;
        }

        // Handle Set Bonus Kill triggers
        Entity sourceEntity = event.source.getEntity();
        if (sourceEntity instanceof EntityPlayer) {
            SetBonusManager.fireOnKill((EntityPlayer) sourceEntity, event.entityLiving);
        }

        if (!(event.entity instanceof EntityPlayer)) {
            return;
        }

        EntityPlayer player = (EntityPlayer) event.entity;

        // Check held item (any IRPGItemContainer type)
        ItemStack heldStack = player.getCurrentEquippedItem();
        if (heldStack != null) {
            firePlayerDeathOnStack(player, heldStack, event.source);
        }

        // Check all 4 armor slots
        for (int slotIndex = 0; slotIndex < 4; slotIndex++) {
            ItemStack armorStack = player.inventory.armorInventory[slotIndex];
            if (armorStack != null) {
                firePlayerDeathOnStack(player, armorStack, event.source);
            }
        }

        PREVIOUS_ARMOR_ITEMS.remove(player);
    }

    /**
     * Fires {@link IOnPlayerDeath} on all death-capable components of a given ItemStack.
     * Uses the unified {@link IRPGItemContainer} interface to support all item types.
     */
    private void firePlayerDeathOnStack(EntityPlayer player, ItemStack itemStack,
                                        DamageSource deathCause) {
        Item item = itemStack.getItem();

        // Use specific type checks to access playerDeathHandlers
        // (IRPGItemContainer doesn't expose component accessors to keep it minimal)
        List<IOnPlayerDeath> deathHandlers = null;

        if (item instanceof ItemRPGWeapon) {
            deathHandlers = ((ItemRPGWeapon) item).getPlayerDeathHandlers();
        } else if (item instanceof ItemRPGArmor) {
            deathHandlers = ((ItemRPGArmor) item).getPlayerDeathHandlers();
        } else if (item instanceof ItemRPGBow) {
            deathHandlers = ((ItemRPGBow) item).getPlayerDeathHandlers();
        }
        // ItemRPGTool, ItemRPGHoe, ItemRPGAccessory will be checked via their
        // getPlayerDeathHandlers() once their refactored versions are compiled.
        // For now, use a generic approach:
        if (deathHandlers == null) {
            try {
                java.lang.reflect.Method method = item.getClass().getMethod("getPlayerDeathHandlers");
                Object result = method.invoke(item);
                if (result instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<IOnPlayerDeath> handlers = (List<IOnPlayerDeath>) result;
                    deathHandlers = handlers;
                }
            } catch (Exception ignored) {
                // Not an RPG item or doesn't have death handlers
            }
        }

        if (deathHandlers != null) {
            for (IOnPlayerDeath deathHandler : deathHandlers) {
                deathHandler.onPlayerDeath(itemStack, player, deathCause);
            }
        }
    }
}