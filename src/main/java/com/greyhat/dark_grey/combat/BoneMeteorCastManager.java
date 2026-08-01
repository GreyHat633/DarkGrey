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

import com.greyhat.dark_grey.entity.EntityBoneMeteor;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class BoneMeteorCastManager {

    public static final BoneMeteorCastManager INSTANCE = new BoneMeteorCastManager();
    public static final UUID BONE_METEOR_CAST_SPEED_UUID = UUID.fromString("6a35d970-1768-4a6c-94cc-5c74230bdf31");

    private final Map<Integer, BoneMeteorCastState> activeCasts = new ConcurrentHashMap<>();

    private BoneMeteorCastManager() {
        cpw.mods.fml.common.FMLCommonHandler.instance()
            .bus()
            .register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void startCast(EntityLivingBase caster, BoneMeteorCastState state) {
        if (caster.worldObj.isRemote) return;

        activeCasts.put(caster.getEntityId(), state);

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
        if (activeCasts.remove(caster.getEntityId()) != null) {
            IAttributeInstance speedAttr = caster.getEntityAttribute(SharedMonsterAttributes.movementSpeed);
            if (speedAttr != null) {
                speedAttr.removeModifier(
                    new AttributeModifier(BONE_METEOR_CAST_SPEED_UUID, "Bone Meteor Cast Slow", 0, 2).setSaved(false));
            }
        }
    }

    public boolean isCasting(EntityLivingBase caster) {
        return activeCasts.containsKey(caster.getEntityId());
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Iterator<Map.Entry<Integer, BoneMeteorCastState>> it = activeCasts.entrySet()
            .iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, BoneMeteorCastState> entry = it.next();
            BoneMeteorCastState state = entry.getValue();

            EntityLivingBase caster = state.caster;

            // Check if dead or invalid
            if (caster.isDead || !caster.isEntityAlive()) {
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
                        hasMaterial = consumeMaterial(player);
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
        IAttributeInstance speedAttr = caster.getEntityAttribute(SharedMonsterAttributes.movementSpeed);
        if (speedAttr != null) {
            speedAttr.removeModifier(
                new AttributeModifier(BONE_METEOR_CAST_SPEED_UUID, "Bone Meteor Cast Slow", 0, 2).setSaved(false));
        }
    }

    private boolean consumeMaterial(EntityPlayer player) {
        IInventory inv = player.inventory;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack != null && stack.getItem() != null) {
                if (Item.itemRegistry.getNameForObject(stack.getItem())
                    .equals("dark_grey:hardened_bone_marrow")) {
                    stack.stackSize--;
                    if (stack.stackSize <= 0) {
                        inv.setInventorySlotContents(i, null);
                    }
                    return true;
                }
            }
        }
        return false;
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
}
