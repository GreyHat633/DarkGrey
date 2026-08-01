package com.greyhat.dark_grey.api.death;

import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class AbsoluteDeathEventHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onLivingDeathLowest(LivingDeathEvent event) {
        if (!(event.source instanceof AbsoluteDeathSource)) {
            return;
        }

        if (event.entityLiving != null
            && AbsoluteDeathService.ACTIVE_CONTEXT.containsKey(event.entityLiving.getUniqueID())) {
            // Absolute death is unstoppable by normal means
            if (event.isCanceled()) {
                event.setCanceled(false);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onLivingAttackLowest(LivingAttackEvent event) {
        if (!(event.source instanceof AbsoluteDeathSource)) return;
        if (event.entityLiving != null
            && AbsoluteDeathService.ACTIVE_CONTEXT.containsKey(event.entityLiving.getUniqueID())) {
            if (event.isCanceled()) {
                event.setCanceled(false);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onLivingHurtLowest(LivingHurtEvent event) {
        if (!(event.source instanceof AbsoluteDeathSource)) return;
        if (event.entityLiving != null
            && AbsoluteDeathService.ACTIVE_CONTEXT.containsKey(event.entityLiving.getUniqueID())) {
            if (event.isCanceled()) {
                event.setCanceled(false);
            }
            if (event.ammount <= 0.0F) {
                event.ammount = 1.0F;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onLivingHealHighest(LivingHealEvent event) {
        if (event.entityLiving != null
            && AbsoluteDeathService.ACTIVE_CONTEXT.containsKey(event.entityLiving.getUniqueID())) {
            event.setCanceled(true);
            event.amount = 0.0F;
        }
    }
}
