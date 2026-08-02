package com.greyhat.dark_grey.combat;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.WorldEvent;

import com.greyhat.dark_grey.entity.EntityBoneMeteor;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class BoneMeteorCastManager {

    public static final BoneMeteorCastManager INSTANCE = new BoneMeteorCastManager();
    public static final UUID BONE_METEOR_CAST_SPEED_UUID = UUID.fromString("6a35d970-1768-4a6c-94cc-5c74230bdf31");

    private final Map<UUID, BoneMeteorCastState> activeCasts = new ConcurrentHashMap<>();

    private BoneMeteorCastManager() {
        cpw.mods.fml.common.FMLCommonHandler.instance()
            .bus()
            .register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void startCast(EntityLivingBase caster, BoneMeteorCastState state) {
        if (caster.worldObj.isRemote) return;

        activeCasts.put(caster.getUniqueID(), state);

        IAttributeInstance speedAttr = caster.getEntityAttribute(SharedMonsterAttributes.movementSpeed);
        if (speedAttr != null) {
            speedAttr.removeModifier(
                new AttributeModifier(
                    BONE_METEOR_CAST_SPEED_UUID,
                    "Bone Meteor Cast Slow",
                    state.castingMoveSpeedMultiplier - 1.0D,
                    2).setSaved(false));
            AttributeModifier mod = new AttributeModifier(
                BONE_METEOR_CAST_SPEED_UUID,
                "Bone Meteor Cast Slow",
                state.castingMoveSpeedMultiplier - 1.0D,
                2).setSaved(false);
            speedAttr.applyModifier(mod);
        }
    }

    public void endCast(EntityLivingBase caster) {
        if (caster.worldObj.isRemote) return;
        cancelByUuid(caster.getUniqueID());
    }

    public boolean isCasting(EntityLivingBase caster) {
        return activeCasts.containsKey(caster.getUniqueID());
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Iterator<Map.Entry<UUID, BoneMeteorCastState>> it = activeCasts.entrySet()
            .iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, BoneMeteorCastState> entry = it.next();
            BoneMeteorCastState state = entry.getValue();

            EntityLivingBase caster = state.caster;

            // Check if dead or invalid
            if (caster.worldObj == null || caster.worldObj != state.castStartWorld
                || caster.dimension != state.castStartDimension
                || caster.isDead
                || !caster.isEntityAlive()) {
                cleanUpCasterSpeed(caster);
                it.remove();
                continue;
            }

            // Check if player changed dimension or unequipped
            if (caster instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) caster;
                ItemStack inUse = player.getItemInUse();
                if (inUse == null) {
                    cleanUpCasterSpeed(caster);
                    it.remove();
                    continue;
                }
                boolean hasComponent = false;
                if (inUse.getItem() instanceof com.greyhat.dark_grey.api.IRPGItemContainer) {
                    java.util.List<com.greyhat.dark_grey.api.IRPGComponent> comps = ((com.greyhat.dark_grey.api.IRPGItemContainer) inUse
                        .getItem()).getAllComponents();
                    for (com.greyhat.dark_grey.api.IRPGComponent comp : comps) {
                        if ("陨骨星".equals(comp.getComponentId())) {
                            hasComponent = true;
                            break;
                        }
                    }
                }
                if (!hasComponent) {
                    cleanUpCasterSpeed(caster);
                    it.remove();
                    continue;
                }
            } else {
                cleanUpCasterSpeed(caster);
                it.remove();
                continue;
            }

            long now = caster.worldObj.getTotalWorldTime();
            if (now - state.lastConsumeWorldTime >= state.consumeIntervalTicks) {
                boolean hasMaterial = true;

                if (caster instanceof EntityPlayer) {
                    EntityPlayer player = (EntityPlayer) caster;
                    boolean creative = player.capabilities.isCreativeMode;
                    if (!creative || state.requireMaterialInCreative) {
                        hasMaterial = consumeMaterial(player, state.materialItemId, state.materialCost);
                    } else if (creative && !state.consumeInCreative) {
                        hasMaterial = true; // Auto success
                    }
                }

                if (hasMaterial) {
                    state.lastConsumeWorldTime = now;
                    spawnMeteorBatch(caster, state);
                    if (caster instanceof EntityPlayer) {
                        EntityPlayer p = (EntityPlayer) caster;
                        p.worldObj.playSoundAtEntity(p, "random.orb", 1.0F, 0.8F);
                    }
                } else {
                    if (caster instanceof EntityPlayer) {
                        ((EntityPlayer) caster).clearItemInUse();
                        ((EntityPlayer) caster)
                            .addChatComponentMessage(new net.minecraft.util.ChatComponentText("§c硬化骨髓已耗尽，施法中断！"));
                    }
                    cleanUpCasterSpeed(caster);
                    it.remove();
                }
            }
        }
    }

    private void cleanUpCasterSpeed(EntityLivingBase caster) {
        if (caster instanceof EntityPlayer && caster.worldObj != null) {
            ((EntityPlayer) caster).clearItemInUse();
        }
        IAttributeInstance speedAttr = caster.getEntityAttribute(SharedMonsterAttributes.movementSpeed);
        if (speedAttr != null) {
            speedAttr.removeModifier(
                new AttributeModifier(BONE_METEOR_CAST_SPEED_UUID, "Bone Meteor Cast Slow", 0, 2).setSaved(false));
        }
    }

    private boolean consumeMaterial(EntityPlayer player, String materialItemId, int materialCost) {
        IInventory inv = player.inventory;
        int available = 0;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack != null && stack.getItem() != null) {
                String registryName = Item.itemRegistry.getNameForObject(stack.getItem());
                if (registryName != null && registryName.equals(materialItemId)) {
                    available = Math.min(materialCost, available + Math.max(0, stack.stackSize));
                    if (available >= materialCost) break;
                }
            }
        }
        if (available < materialCost) return false;

        int remaining = materialCost;
        for (int i = 0; i < inv.getSizeInventory() && remaining > 0; i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack == null || stack.getItem() == null) continue;
            String registryName = Item.itemRegistry.getNameForObject(stack.getItem());
            if (registryName == null || !registryName.equals(materialItemId)) continue;

            int consumed = Math.min(remaining, Math.max(0, stack.stackSize));
            stack.stackSize -= consumed;
            remaining -= consumed;
            if (stack.stackSize <= 0) {
                inv.setInventorySlotContents(i, null);
            }
        }
        return remaining == 0;
    }

    private void spawnMeteorBatch(EntityLivingBase caster, BoneMeteorCastState state) {
        for (int i = 0; i < state.meteorsPerConsume; i++) {
            double theta = caster.worldObj.rand.nextDouble() * Math.PI * 2.0;
            double r = Math.sqrt(caster.worldObj.rand.nextDouble()) * state.summonRadius;

            double spawnX = caster.posX + Math.cos(theta) * r;
            double spawnZ = caster.posZ + Math.sin(theta) * r;
            double spawnY = caster.posY + state.summonHeight;

            EntityBoneMeteor meteor = new EntityBoneMeteor(
                caster.worldObj,
                caster,
                state.meteorFallSpeed,
                state.meteorGravity,
                state.meteorLifetimeTicks,
                state.impactBoxSize,
                state.impactDamage,
                state.fractureMarkId,
                state.fractureStacks);
            meteor.setPosition(spawnX, spawnY, spawnZ);

            caster.worldObj.spawnEntityInWorld(meteor);
        }
    }

    public static void init() {}

    private void cancelByUuid(UUID casterUuid) {
        Iterator<Map.Entry<UUID, BoneMeteorCastState>> it = activeCasts.entrySet()
            .iterator();
        while (it.hasNext()) {
            BoneMeteorCastState state = it.next()
                .getValue();
            if (casterUuid.equals(state.casterUuid)) {
                cleanUpCasterSpeed(state.caster);
                it.remove();
            }
        }
    }

    private void cancelWorld(WorldEvent.Unload event) {
        Iterator<Map.Entry<UUID, BoneMeteorCastState>> it = activeCasts.entrySet()
            .iterator();
        while (it.hasNext()) {
            BoneMeteorCastState state = it.next()
                .getValue();
            if (state.castStartWorld == event.world || state.caster.worldObj == event.world) {
                cleanUpCasterSpeed(state.caster);
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (!event.entityLiving.worldObj.isRemote) {
            cancelByUuid(event.entityLiving.getUniqueID());
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        cancelByUuid(event.original.getUniqueID());
        cancelByUuid(event.entityPlayer.getUniqueID());
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
        cancelByUuid(event.player.getUniqueID());
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
        cancelByUuid(event.player.getUniqueID());
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        cancelWorld(event);
    }
}
