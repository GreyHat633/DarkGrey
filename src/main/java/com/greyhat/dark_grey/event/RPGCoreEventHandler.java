package com.greyhat.dark_grey.event;

import java.util.List;
import java.util.WeakHashMap;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import com.greyhat.dark_grey.api.CombatTargeting;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.IRPGItemContainer;
import com.greyhat.dark_grey.api.MadokaVolleyDamageManager;
import com.greyhat.dark_grey.api.SetBonusManager;
import com.greyhat.dark_grey.api.capability.IModifyMeleeDamage;
import com.greyhat.dark_grey.api.capability.IOnEquip;
import com.greyhat.dark_grey.api.capability.IOnHeldTick;
import com.greyhat.dark_grey.api.capability.IOnHit;
import com.greyhat.dark_grey.api.capability.IOnHurt;
import com.greyhat.dark_grey.api.capability.IOnPlayerDeath;
import com.greyhat.dark_grey.api.capability.IOnUnequip;
import com.greyhat.dark_grey.item.ItemRPGArmor;
import com.greyhat.dark_grey.item.ItemRPGBow;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Centralized Forge EventBus handler for all cross-cutting RPG component triggers.
 *
 * <p>
 * Handles three categories of events:
 * </p>
 * <ul>
 * <li>{@code LivingHurtEvent} → armor {@link IOnHurt} pipeline, armor IOnHit dispatch
 * (when wearer attacks), bow IOnHit dispatch (when arrow hits)</li>
 * <li>{@code PlayerTickEvent} → {@link IOnEquip} / {@link IOnUnequip} detection</li>
 * <li>{@code LivingDeathEvent} → {@link IOnPlayerDeath} on all equipped RPG items</li>
 * </ul>
 *
 * <p>
 * Performance: All checks use fast {@code instanceof} guards. Non-RPG entities
 * and items are skipped in O(1). The handler never iterates the full entity list.
 * </p>
 */
public class RPGCoreEventHandler {

    /**
     * Per-player snapshot of armor slots from the previous tick.
     * WeakHashMap auto-cleans when a player disconnects.
     */
    private static final WeakHashMap<EntityPlayer, Item[]> PREVIOUS_ARMOR_ITEMS = new WeakHashMap<>();

    // ─────────────────────────────────────────────────────────────────────────
    // LivingHurtEvent → IOnHurt (Armor Damage Pipeline)
    // + IOnHit (Armor attack components + Bow arrow components)
    // ─────────────────────────────────────────────────────────────────────────

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.entity.worldObj.isRemote) {
            return;
        }

        EntityLivingBase hurtEntity = event.entityLiving;

        // Scorched Mark Explosion Logic
        if (ScorchedMarkTracker.getTimer(hurtEntity) > 0) {
            if (event.source.getEntity() instanceof EntityPlayer && !event.source.isMagicDamage()
                && !event.source.isExplosion()) {
                EntityPlayer player = (EntityPlayer) event.source.getEntity();
                ScorchedMarkTracker.clear(hurtEntity);

                float baseDmg = 1.0f;
                if (player.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.attackDamage) != null) {
                    baseDmg = (float) player
                        .getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.attackDamage)
                        .getAttributeValue();
                }
                float explosionDmg = baseDmg * 1.25f;

                hurtEntity.worldObj.playSoundEffect(
                    hurtEntity.posX,
                    hurtEntity.posY,
                    hurtEntity.posZ,
                    "random.explode",
                    1.0F,
                    (1.0F + (hurtEntity.worldObj.rand.nextFloat() - hurtEntity.worldObj.rand.nextFloat()) * 0.2F)
                        * 0.7F);

                if (hurtEntity.worldObj instanceof net.minecraft.world.WorldServer) {
                    ((net.minecraft.world.WorldServer) hurtEntity.worldObj).func_147487_a(
                        "largeexplode",
                        hurtEntity.posX,
                        hurtEntity.posY + hurtEntity.height / 2.0f,
                        hurtEntity.posZ,
                        5,
                        0.0,
                        0.0,
                        0.0,
                        0.0);
                }

                net.minecraft.util.AxisAlignedBB aabb = hurtEntity.boundingBox.expand(3.0, 3.0, 3.0);
                @SuppressWarnings("unchecked")
                List<Entity> list = hurtEntity.worldObj.getEntitiesWithinAABBExcludingEntity(player, aabb);
                for (Entity e : list) {
                    if (e instanceof EntityLivingBase
                        && CombatTargeting.canDamage(player, (EntityLivingBase) e, false)) {
                        DamageSource explosionSource = DamageSource.causePlayerDamage(player)
                            .setExplosion();
                        e.attackEntityFrom(explosionSource, explosionDmg);
                    }
                }
            }
        }

        float modifiedDamage = event.ammount;

        // Custom Weapon: Suspended Clockhand Damage Scaling
        if (event.source.getEntity() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.source.getEntity();
            ItemStack heldItem = player.getCurrentEquippedItem();
            if (heldItem != null && heldItem.hasTagCompound()
                && heldItem.getTagCompound()
                    .hasKey("SoulValue")) {
                // Ensure we only apply this for the specific clockhand item
                if (heldItem.getUnlocalizedName()
                    .endsWith("suspended_clockhand")) {
                    int soulValue = heldItem.getTagCompound()
                        .getInteger("SoulValue");
                    // 叠加在原有伤害之上 (event.ammount 已经包含了力量buff和暴击等加成)
                    modifiedDamage = event.ammount + (soulValue / 4.0F);
                }
            }
        }

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
                EntityArrow arrow = (EntityArrow) directSource;
                ItemStack heldItem = ArrowTracker.getBow(arrow);
                if (heldItem == null) {
                    heldItem = attacker.getCurrentEquippedItem();
                }
                if (heldItem != null && heldItem.getItem() instanceof ItemRPGBow) {
                    ItemRPGBow rpgBow = (ItemRPGBow) heldItem.getItem();
                    List<IOnHit> bowHitHandlers = rpgBow.getHitHandlers();
                    for (IOnHit handler : bowHitHandlers) {
                        handler.onHit(heldItem, attacker, hurtEntity, event.ammount);
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onMeleeDamageModify(LivingHurtEvent event) {
        if (event.entity.worldObj.isRemote || event.isCanceled()) {
            return;
        }
        EntityPlayer attacker = resolveDirectMeleeAttacker(event.source, event.entityLiving);
        if (attacker == null) {
            return;
        }
        float modifiedDamage = applyHeldMeleeDamageModifiers(attacker, event.source, event.entityLiving, event.ammount);
        event.ammount = SetBonusManager.fireOnHit(attacker, event.entityLiving, modifiedDamage);
    }

    /**
     * Folds scaled-burst and other melee modifiers into the original damage
     * event. Re-entering {@code attackEntityFrom} here would cause Forge,
     * KCauldron and every protection/combat plugin to process a second nested hit.
     */
    private EntityPlayer resolveDirectMeleeAttacker(DamageSource source, EntityLivingBase target) {
        Entity sourceEntity = source.getEntity();
        Entity directSource = source.getSourceOfDamage();
        EntityPlayer attacker = sourceEntity instanceof EntityPlayer ? (EntityPlayer) sourceEntity
            : directSource instanceof EntityPlayer ? (EntityPlayer) directSource : null;
        if (attacker == null || target == attacker
            || source.isProjectile()
            || source.isMagicDamage()
            || source.isExplosion()) {
            return null;
        }

        // A non-player direct entity means this is an indirect/modded projectile even
        // when its DamageSource forgot to set the projectile flag.
        if (directSource != null && directSource != attacker && !(directSource instanceof EntityPlayer)) {
            return null;
        }
        return attacker;
    }

    private float applyHeldMeleeDamageModifiers(EntityPlayer attacker, DamageSource source, EntityLivingBase target,
        float currentDamage) {
        ItemStack heldStack = attacker.getCurrentEquippedItem();
        if (heldStack == null || !(heldStack.getItem() instanceof IRPGItemContainer)) {
            return currentDamage;
        }

        List<IRPGComponent> components = ((IRPGItemContainer) heldStack.getItem()).getAllComponents();
        if (components == null || components.isEmpty()) {
            return currentDamage;
        }

        float modifiedDamage = currentDamage;
        for (IRPGComponent component : components) {
            if (component instanceof IModifyMeleeDamage) {
                IModifyMeleeDamage modifier = (IModifyMeleeDamage) component;
                modifiedDamage = modifier.modifyMeleeDamage(heldStack, attacker, target, source, modifiedDamage);
                if (modifier.bypassesArmor()) {
                    source.setDamageBypassesArmor();
                }
                if (modifier.isAbsoluteDamage()) {
                    source.setDamageIsAbsolute();
                }
            }
        }
        return modifiedDamage;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PlayerTickEvent → IOnEquip / IOnUnequip (Armor Change Detection)
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
        fireServerHeldTick(player);
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
            SetBonusManager.fireTick(player);
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
        SetBonusManager.fireTick(player);
    }

    private void fireServerHeldTick(EntityPlayer player) {
        ItemStack heldStack = player.getCurrentEquippedItem();
        if (heldStack == null || !(heldStack.getItem() instanceof IRPGItemContainer)) {
            return;
        }

        List<IRPGComponent> components = ((IRPGItemContainer) heldStack.getItem()).getAllComponents();
        if (components == null || components.isEmpty()) {
            return;
        }
        for (IRPGComponent component : components) {
            if (component instanceof IOnHeldTick) {
                ((IOnHeldTick) component).onHeldTick(heldStack, player.worldObj, player);
            }
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
    // LivingDeathEvent → IOnPlayerDeath (All RPG item types)
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
    private void firePlayerDeathOnStack(EntityPlayer player, ItemStack itemStack, DamageSource deathCause) {
        Item item = itemStack.getItem();
        if (item instanceof IRPGItemContainer) {
            List<IOnPlayerDeath> deathHandlers = ((IRPGItemContainer) item).getPlayerDeathHandlers();
            if (deathHandlers != null) {
                for (IOnPlayerDeath deathHandler : deathHandlers) {
                    deathHandler.onPlayerDeath(itemStack, player, deathCause);
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ServerTickEvent → Scorched Mark & Watcher Reload Checks
    // ─────────────────────────────────────────────────────────────────────────
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            if (com.greyhat.dark_grey.api.RPGItemDataManager.getInstance()
                .isReloadPending()) {
                com.greyhat.dark_grey.api.RPGItemDataManager.getInstance()
                    .reload(true);
            }
            ScorchedMarkTracker.tick();
        } else {
            MadokaVolleyDamageManager.flushExpired();
        }
    }

    @SubscribeEvent
    public void onEntityJoinWorld(net.minecraftforge.event.entity.EntityJoinWorldEvent event) {
        if (event.entity instanceof EntityArrow) {
            EntityArrow arrow = (EntityArrow) event.entity;
            if (arrow.shootingEntity instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) arrow.shootingEntity;
                ItemStack held = player.getCurrentEquippedItem();
                if (held != null && held.getItem() instanceof ItemRPGBow) {
                    ArrowTracker.registerArrow(arrow, held);
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof net.minecraft.entity.player.EntityPlayerMP) {
            com.greyhat.dark_grey.api.RPGItemDataManager.getInstance()
                .syncToPlayer((net.minecraft.entity.player.EntityPlayerMP) event.player);
        }
    }
}
