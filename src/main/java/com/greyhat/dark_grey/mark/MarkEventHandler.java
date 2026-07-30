package com.greyhat.dark_grey.mark;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import com.greyhat.dark_grey.DarkGrey;
import com.greyhat.dark_grey.api.WorldTimeRebaseHelper;
import com.greyhat.dark_grey.mark.api.IMarkType;
import com.greyhat.dark_grey.mark.api.MarkRemovalReason;
import com.greyhat.dark_grey.mark.api.MarkUpdateContext;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;

public class MarkEventHandler {

    @SubscribeEvent
    public void onEntityConstructing(EntityConstructing event) {
        if (event.entity instanceof EntityLivingBase) {
            EntityLivingBase living = (EntityLivingBase) event.entity;
            if (living.getExtendedProperties(MarkContainer.PROP_NAME) == null) {
                living.registerExtendedProperties(MarkContainer.PROP_NAME, new MarkContainer());
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onDeath(LivingDeathEvent event) {
        if (!event.entityLiving.worldObj.isRemote) {
            MarkManager.clearAll(event.entityLiving, MarkRemovalReason.ENTITY_DEATH);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (event.entityPlayer.worldObj.isRemote) {
            return;
        }

        MarkContainer original = MarkContainer.get(event.original);
        MarkContainer current = MarkContainer.get(event.entityPlayer);

        if (event.wasDeath) {
            if (original != null) {
                original.clear();
            }
            MarkManager.clearAll(event.entityPlayer, MarkRemovalReason.ENTITY_DEATH);
        } else {
            if (original != null && current != null) {
                net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
                original.saveNBTData(tag);
                current.loadNBTData(tag);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
        if (event.player.worldObj.isRemote) return;
        MarkContainer container = MarkContainer.get(event.player);
        if (container == null || container.isEmpty()) return;

        long delta = WorldTimeRebaseHelper.getDimensionTimeDelta(event.player, event.fromDim);
        if (delta == 0L) return;
        for (MarkInstance instance : container.getAllMarks()) {
            instance.rebaseWorldTimes(delta);
        }
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingUpdateEvent event) {
        EntityLivingBase target = event.entityLiving;
        if (target.worldObj.isRemote) return; // Server only logic

        MarkContainer container = MarkContainer.get(target);
        if (container == null || container.isEmpty()) return;

        long now = target.worldObj.getTotalWorldTime();
        MarkUpdateContext context = new MarkUpdateContext(now);
        List<String> toRemove = null;

        for (MarkInstance instance : new ArrayList<>(container.getAllMarks())) {
            if (!container.hasMark(instance.getMarkId())) {
                continue;
            }
            IMarkType type = MarkRegistry.get(instance.getMarkId());
            if (type == null) {
                if (toRemove == null) toRemove = new ArrayList<>();
                toRemove.add(instance.getMarkId());
                continue;
            }

            try {
                // Decay trigger check
                if (instance.isDecaying() && instance.getNextDecayTriggerWorldTime() > 0
                    && now >= instance.getNextDecayTriggerWorldTime()) {
                    type.onDecayTrigger(target, instance, context);
                }

                // Periodic check
                if (instance.getNextPeriodicTriggerWorldTime() > 0
                    && now >= instance.getNextPeriodicTriggerWorldTime()) {
                    type.onPeriodicTrigger(target, instance, context);
                }

                // Enter decay check
                if (!instance.isDecaying() && instance.getStableUntilWorldTime() > 0
                    && now >= instance.getStableUntilWorldTime()) {
                    type.onEnterDecay(target, instance, context);
                }

                if (instance.getStacks() <= 0) {
                    if (toRemove == null) toRemove = new ArrayList<>();
                    toRemove.add(instance.getMarkId());
                }
            } catch (Exception e) {
                DarkGrey.LOG.error("Error in mark update loop for " + instance.getMarkId(), e);
            }
        }

        if (toRemove != null) {
            for (String markId : toRemove) {
                MarkManager.remove(target, markId, MarkRemovalReason.DECAYED_TO_ZERO);
            }
        }
    }
}
