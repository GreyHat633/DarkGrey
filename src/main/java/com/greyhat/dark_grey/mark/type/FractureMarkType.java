package com.greyhat.dark_grey.mark.type;

import java.util.UUID;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.util.ResourceLocation;

import com.greyhat.dark_grey.DarkGrey;
import com.greyhat.dark_grey.common.Config;
import com.greyhat.dark_grey.mark.MarkInstance;
import com.greyhat.dark_grey.mark.MarkManager;
import com.greyhat.dark_grey.mark.api.AbstractMarkType;
import com.greyhat.dark_grey.mark.api.MarkApplyContext;
import com.greyhat.dark_grey.mark.api.MarkDecayMode;
import com.greyhat.dark_grey.mark.api.MarkRemovalReason;
import com.greyhat.dark_grey.mark.api.MarkUpdateContext;
import com.greyhat.dark_grey.mark.api.MarkVisualData;

public class FractureMarkType extends AbstractMarkType {

    public static final String ID = "fracture";
    private static final UUID FRACTURE_SPEED_MODIFIER_UUID = UUID.fromString("b896b014-9457-41a4-9646-608b8b0e8b28");

    private MarkVisualData visualData;

    public FractureMarkType() {
        this.visualData = new MarkVisualData(
            new ResourceLocation("dark_grey", "textures/gui/marks/fracture.png"),
            "mark.dark_grey.fracture.name",
            "mark.dark_grey.fracture.description",
            0xFFDDDDDD, // Bone white/gray color
            0xFFDDDDDD, // Decay color (same)
            0xFFDDDDDD, // Max color (same)
            100, // Display priority
            true, // showStacks
            true, // showStableTimer
            false, // showPeriodicTimer
            false, // showDecayTimer
            true, // showOnEntity
            true, // showOnTargetPanel
            true, // showOnSelfHud
            true, // flashWhenNearDecay
            true // glowWhenMaxed
        );
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public int getMaxStacks() {
        return Config.fractureMaxStacks;
    }

    @Override
    public MarkDecayMode getDecayMode() {
        return MarkDecayMode.INSTANT;
    }

    @Override
    public int getDefaultStableDurationTicks() {
        return Config.fractureDefaultStableDurationTicks;
    }

    @Override
    public int getDecayIntervalTicks() {
        return 0;
    }

    @Override
    public int getDecayAmount() {
        return 1;
    }

    @Override
    public MarkVisualData getVisualData() {
        return this.visualData;
    }

    @Override
    public void onFirstApplied(EntityLivingBase target, MarkInstance instance, MarkApplyContext context) {
        if (target.worldObj.isRemote) return;

        instance.setDecaying(false);
        int duration = getStableDurationTicks(instance);
        instance.setStableUntilWorldTime(context.getWorldTime() + duration);

        refreshSpeedModifier(target, instance.getStacks());

        if (instance.getStacks() >= getMaxStacks()) {
            onReachedMaxStacks(target, instance, new MarkUpdateContext(context.getWorldTime()));
        }
    }

    @Override
    public void onApplied(EntityLivingBase target, MarkInstance instance, MarkApplyContext context, int requestedStacks,
        int actualAddedStacks) {
        if (target.worldObj.isRemote) return;

        instance.setDecaying(false);
        boolean reachedMax = instance.getStacks() >= getMaxStacks();
        boolean wasMax = (instance.getStacks() - actualAddedStacks) >= getMaxStacks();

        if (requestedStacks > 0) {
            instance.setStableUntilWorldTime(context.getWorldTime() + getStableDurationTicks(instance));
        }

        if (actualAddedStacks > 0) {
            refreshSpeedModifier(target, instance.getStacks());
            if (reachedMax && !wasMax) {
                onReachedMaxStacks(target, instance, new MarkUpdateContext(context.getWorldTime()));
            }
        }
    }

    @Override
    public void onEnterDecay(EntityLivingBase target, MarkInstance instance, MarkUpdateContext context) {
        if (target.worldObj.isRemote) return;

        int oldStacks = instance.getStacks();
        int newStacks = Math.max(0, oldStacks - getDecayAmount());

        if (oldStacks >= getMaxStacks() && newStacks < getMaxStacks()) {
            onLeftMaxStacks(target, instance, context);
        }

        if (newStacks > 0) {
            instance.setStacks(newStacks);
            instance.setDecaying(false);
            instance.setStableUntilWorldTime(context.getWorldTime() + getStableDurationTicks(instance));
            refreshSpeedModifier(target, newStacks);
            MarkManager.syncMark(target, instance, this, (byte) 5, newStacks - oldStacks, false);
        } else {
            instance.setStacks(0);
        }
    }

    @Override
    public void onRemoved(EntityLivingBase target, MarkInstance instance, MarkUpdateContext context,
        MarkRemovalReason reason) {
        if (target.worldObj.isRemote) return;

        try {
            if (instance.getStacks() >= getMaxStacks()) {
                onLeftMaxStacks(target, instance, context);
            }
        } finally {
            refreshSpeedModifier(target, 0); // Remove modifier
        }
    }

    @Override
    public void onStacksChanged(EntityLivingBase target, MarkInstance instance, int oldStacks, int newStacks,
        MarkUpdateContext context) {
        if (target.worldObj.isRemote) return;

        instance.setDecaying(false);
        if (oldStacks < getMaxStacks() && newStacks >= getMaxStacks()) {
            onReachedMaxStacks(target, instance, context);
        } else if (oldStacks >= getMaxStacks() && newStacks < getMaxStacks()) {
            onLeftMaxStacks(target, instance, context);
        }

        if (newStacks <= 0) {
            refreshSpeedModifier(target, 0);
        } else {
            refreshSpeedModifier(target, newStacks);
            if (instance.getStableUntilWorldTime() <= context.getWorldTime()) {
                instance.setStableUntilWorldTime(context.getWorldTime() + getStableDurationTicks(instance));
            }
        }
    }

    private int getStableDurationTicks(MarkInstance instance) {
        int duration = instance.getStableDurationTicks();
        return duration > 0 ? duration : getDefaultStableDurationTicks();
    }

    private void refreshSpeedModifier(EntityLivingBase target, int stacks) {
        IAttributeInstance speedAttribute = target.getEntityAttribute(SharedMonsterAttributes.movementSpeed);
        if (speedAttribute == null) {
            DarkGrey.LOG.warn(
                "Target " + target.getClass()
                    .getSimpleName() + " has no movement speed attribute.");
            return;
        }

        AttributeModifier oldMod = speedAttribute.getModifier(FRACTURE_SPEED_MODIFIER_UUID);
        if (oldMod != null) {
            speedAttribute.removeModifier(oldMod);
        }

        if (stacks <= 0) {
            return;
        }

        double rawReduction = Config.fractureSpeedReductionPerStack * stacks;
        double reduction = Math.min(rawReduction, 0.95);
        if (reduction < 0) reduction = 0;

        AttributeModifier newMod = new AttributeModifier(
            FRACTURE_SPEED_MODIFIER_UUID,
            "dark_grey.fracture_speed_reduction",
            -reduction,
            2);

        newMod.setSaved(false);
        speedAttribute.applyModifier(newMod);
    }

    protected void onReachedMaxStacks(EntityLivingBase target, MarkInstance instance, MarkUpdateContext context) {
        com.greyhat.dark_grey.mark.util.ShatteredBoneMaintenanceBridge
            .setMaintainedByFracture(target, true, instance.getSourceUuid());
    }

    protected void onLeftMaxStacks(EntityLivingBase target, MarkInstance instance, MarkUpdateContext context) {
        com.greyhat.dark_grey.mark.util.ShatteredBoneMaintenanceBridge
            .setMaintainedByFracture(target, false, instance.getSourceUuid());
    }
}
