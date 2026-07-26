package com.greyhat.dark_grey.event;

import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import com.greyhat.dark_grey.api.CombatTargeting;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.IRPGItemContainer;
import com.greyhat.dark_grey.combat.BoneCrusherHitCounterManager;
import com.greyhat.dark_grey.component.ComponentBoneCrusher;
import com.greyhat.dark_grey.mark.MarkManager;
import com.greyhat.dark_grey.mark.api.MarkApplyContext;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class BoneCrusherCombatHandler {

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.entity.worldObj.isRemote || event.isCanceled() || event.ammount <= 0) {
            return;
        }

        if (!(event.source.getEntity() instanceof EntityLivingBase)) {
            return;
        }

        EntityLivingBase attacker = (EntityLivingBase) event.source.getEntity();
        EntityLivingBase target = event.entityLiving;

        if (attacker == target || !CombatTargeting.canDamage(attacker, target, false)) {
            return;
        }

        // Must be direct melee attack
        if (!event.source.getDamageType()
            .equals("player")
            && !event.source.getDamageType()
                .equals("mob")) {
            return;
        }
        if (event.source.isProjectile() || event.source.isExplosion() || event.source.isMagicDamage()) {
            return;
        }

        ItemStack heldItem = attacker.getHeldItem();
        if (heldItem == null || !(heldItem.getItem() instanceof IRPGItemContainer)) {
            return;
        }

        IRPGItemContainer container = (IRPGItemContainer) heldItem.getItem();
        List<IRPGComponent> components = container.getAllComponents();
        ComponentBoneCrusher boneCrusher = null;

        for (IRPGComponent comp : components) {
            if (comp instanceof ComponentBoneCrusher) {
                boneCrusher = (ComponentBoneCrusher) comp;
                break;
            }
        }

        if (boneCrusher == null) {
            return;
        }

        // Add hit
        int newCount = BoneCrusherHitCounterManager.addHit(attacker.getUniqueID(), target);

        if (newCount >= boneCrusher.getRequiredHits()) {
            // Apply Fracture
            BoneCrusherHitCounterManager.clearCount(attacker.getUniqueID(), target);

            MarkApplyContext context = new MarkApplyContext.Builder().source(attacker)
                .requestedStacks(boneCrusher.getFractureStacksPerTrigger())
                .durationTicks(boneCrusher.getFractureDurationTicks())
                .worldTime(target.worldObj.getTotalWorldTime())
                .applicationId("bone_crusher")
                .refreshDuration(true)
                .build();

            MarkManager.apply(target, boneCrusher.getFractureMarkId(), context);

            if (boneCrusher.isShowThirdHitFeedback()) {
                target.worldObj
                    .playSoundEffect(target.posX, target.posY, target.posZ, "mob.zombie.woodbreak", 1.0F, 0.5F);
            }
        }
    }

    @SubscribeEvent
    public void onEntityDeath(LivingDeathEvent event) {
        if (!event.entity.worldObj.isRemote) {
            BoneCrusherHitCounterManager.clearTarget(event.entityLiving);
            BoneCrusherHitCounterManager.clearAttacker(event.entityLiving.getUniqueID());
        }
    }
}
