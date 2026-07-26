package com.greyhat.dark_grey.mark;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;

import com.greyhat.dark_grey.DarkGrey;
import com.greyhat.dark_grey.mark.api.IMarkType;
import com.greyhat.dark_grey.mark.api.MarkRemovalReason;
import com.greyhat.dark_grey.mark.api.MarkUpdateContext;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

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

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (!event.entityLiving.worldObj.isRemote) {
            MarkManager.clearAll(event.entityLiving, MarkRemovalReason.ENTITY_DEATH);
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

        for (MarkInstance instance : container.getAllMarks()) {
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
