package com.greyhat.dark_grey.mark.type;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;

import com.greyhat.dark_grey.DarkGrey;
import com.greyhat.dark_grey.api.RPGDamageSources;
import com.greyhat.dark_grey.common.Config;
import com.greyhat.dark_grey.mark.MarkInstance;
import com.greyhat.dark_grey.mark.MarkManager;
import com.greyhat.dark_grey.mark.api.AbstractMarkType;
import com.greyhat.dark_grey.mark.api.MarkApplyContext;
import com.greyhat.dark_grey.mark.api.MarkUpdateContext;
import com.greyhat.dark_grey.mark.api.MarkVisualData;

public class PoisonMarkType extends AbstractMarkType {

    private final MarkVisualData visualData;

    public PoisonMarkType() {
        this.visualData = new MarkVisualData(
            new ResourceLocation(DarkGrey.MODID, "textures/gui/marks/poison.png"),
            "mark.dark_grey.poison.name",
            "mark.dark_grey.poison.description",
            0x00AA00, // primary
            0x55AA55, // decay
            0x55FF55, // max
            10,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true);
    }

    @Override
    public String getId() {
        return "poison";
    }

    @Override
    public int getMaxStacks() {
        return Config.poisonMaxStacks;
    }

    @Override
    public MarkVisualData getVisualData() {
        return visualData;
    }

    @Override
    public void onFirstApplied(EntityLivingBase target, MarkInstance instance, MarkApplyContext context) {
        long now = context.getWorldTime();
        instance.setNextPeriodicTriggerWorldTime(now + Config.poisonPeriodicIntervalTicks);
        int duration = context.getDurationTicks() > 0 ? context.getDurationTicks() : Config.poisonStableDurationTicks;
        instance.setStableUntilWorldTime(now + duration);
        instance.setDecaying(false);
        instance.setNextDecayTriggerWorldTime(0);
    }

    @Override
    public void onApplied(EntityLivingBase target, MarkInstance instance, MarkApplyContext context, int requestedStacks,
        int actualAddedStacks) {
        long now = context.getWorldTime();
        int duration = context.getDurationTicks() > 0 ? context.getDurationTicks() : Config.poisonStableDurationTicks;
        instance.setStableUntilWorldTime(now + duration);
        instance.setDecaying(false);
        instance.setNextDecayTriggerWorldTime(0);

        if (Config.poisonImmediateDamageOnApply && actualAddedStacks > 0) {
            triggerPoisonDamage(target, instance);
        } else if (Config.poisonImmediateDamageAtMaxRefresh && actualAddedStacks == 0
            && instance.getStacks() >= getMaxStacks()) {
                triggerPoisonDamage(target, instance);
            }
    }

    @Override
    public void onPeriodicTrigger(EntityLivingBase target, MarkInstance instance, MarkUpdateContext context) {
        if (instance.getStacks() <= 0) return;

        triggerPoisonDamage(target, instance);

        instance.setLastPeriodicTriggerWorldTime(context.getWorldTime());
        instance.setNextPeriodicTriggerWorldTime(context.getWorldTime() + Config.poisonPeriodicIntervalTicks);

        if (Config.poisonPeriodicConsumesStacks) {
            // Not used currently, logic could be here if needed
        }
    }

    @Override
    public void onEnterDecay(EntityLivingBase target, MarkInstance instance, MarkUpdateContext context) {
        instance.setDecaying(true);
        instance.setNextDecayTriggerWorldTime(context.getWorldTime() + Config.poisonDecayIntervalTicks);
        MarkManager.syncMark(target, instance, this, (byte) 4, 0, false);
    }

    @Override
    public void onDecayTrigger(EntityLivingBase target, MarkInstance instance, MarkUpdateContext context) {
        long now = context.getWorldTime();

        int newStacks = Math.max(0, instance.getStacks() - Config.poisonDecayAmount);
        int delta = newStacks - instance.getStacks();
        instance.setStacks(newStacks);

        if (newStacks > 0) {
            instance.setNextDecayTriggerWorldTime(now + Config.poisonDecayIntervalTicks);
            MarkManager.syncMark(target, instance, this, (byte) 5, delta, false);
        }
    }

    private void triggerPoisonDamage(EntityLivingBase target, MarkInstance instance) {
        float damage = instance.getStacks();
        EntityLivingBase source = null;
        if (instance.getSourceUuid() != null) {
            // Optimally lookup entity by UUID, or assume we don't have direct ref.
            // In a real mod we'd probably have a utility to find entity by UUID or ID.
            net.minecraft.entity.Entity e = target.worldObj.getEntityByID(instance.getSourceEntityId());
            if (e instanceof EntityLivingBase && e.getUniqueID()
                .equals(instance.getSourceUuid())) {
                source = (EntityLivingBase) e;
            }
        }

        if (Config.poisonIgnoreHurtResistance) {
            target.hurtResistantTime = 0;
        }

        double prevMotionX = target.motionX;
        double prevMotionY = target.motionY;
        double prevMotionZ = target.motionZ;
        boolean prevIsAirBorne = target.isAirBorne;

        RPGDamageSources
            .dealDamageWithoutInvulnerability(target, RPGDamageSources.causeMarkDamage(getId(), source), damage);

        target.motionX = prevMotionX;
        target.motionY = prevMotionY;
        target.motionZ = prevMotionZ;
        target.isAirBorne = prevIsAirBorne;
    }
}
