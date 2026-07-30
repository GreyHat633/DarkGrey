package com.greyhat.dark_grey.mark.type;

import net.minecraft.entity.EntityLivingBase;
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

public class ScorchMarkType extends AbstractMarkType {

    public static final String ID = "scorch";
    private final MarkVisualData visualData;

    public ScorchMarkType() {
        this.visualData = new MarkVisualData(
            new ResourceLocation(DarkGrey.MODID, "textures/gui/marks/scorch.png"),
            "mark.dark_grey.scorch.name",
            "mark.dark_grey.scorch.description",
            0xFFFF4400, // primary (orange/red)
            0xFFFF4400, // decay
            0xFFFF4400, // max
            90, // priority
            false, // showStacks (max 1 stack, don't need to show)
            true, // showStableTimer
            false, // showPeriodicTimer
            false, // showDecayTimer
            true, // showOnEntity
            true, // showOnTargetPanel
            true, // showOnSelfHud
            true, // flashWhenNearDecay
            false // glowWhenMaxed
        );
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public int getMaxStacks() {
        return 1;
    }

    @Override
    public MarkDecayMode getDecayMode() {
        return MarkDecayMode.INSTANT;
    }

    @Override
    public int getDefaultStableDurationTicks() {
        return Config.scorchDefaultStableDurationTicks;
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
        return visualData;
    }

    @Override
    public void onFirstApplied(EntityLivingBase target, MarkInstance instance, MarkApplyContext context) {
        if (target.worldObj.isRemote) return;

        instance.setDecaying(false);
        int duration = getStableDurationTicks(instance);
        instance.setStableUntilWorldTime(context.getWorldTime() + duration);
    }

    @Override
    public void onApplied(EntityLivingBase target, MarkInstance instance, MarkApplyContext context, int requestedStacks,
        int actualAddedStacks) {
        if (target.worldObj.isRemote) return;

        instance.setDecaying(false);
        if (requestedStacks > 0) {
            instance.setStableUntilWorldTime(context.getWorldTime() + getStableDurationTicks(instance));
        }
    }

    @Override
    public void onEnterDecay(EntityLivingBase target, MarkInstance instance, MarkUpdateContext context) {
        if (target.worldObj.isRemote) return;

        int oldStacks = instance.getStacks();
        int newStacks = Math.max(0, oldStacks - getDecayAmount());

        if (newStacks > 0) {
            instance.setStacks(newStacks);
            instance.setDecaying(false);
            instance.setStableUntilWorldTime(context.getWorldTime() + getStableDurationTicks(instance));
            MarkManager.syncMark(target, instance, this, (byte) 5, newStacks - oldStacks, false);
        } else {
            instance.setStacks(0);
        }
    }

    @Override
    public void onRemoved(EntityLivingBase target, MarkInstance instance, MarkUpdateContext context,
        MarkRemovalReason reason) {
        // Nothing special to clean up
    }

    @Override
    public void onStacksChanged(EntityLivingBase target, MarkInstance instance, int oldStacks, int newStacks,
        MarkUpdateContext context) {
        if (target.worldObj.isRemote) return;

        instance.setDecaying(false);
        if (newStacks > 0 && instance.getStableUntilWorldTime() <= context.getWorldTime()) {
            instance.setStableUntilWorldTime(context.getWorldTime() + getStableDurationTicks(instance));
        }
    }

    private int getStableDurationTicks(MarkInstance instance) {
        int duration = instance.getStableDurationTicks();
        return duration > 0 ? duration : getDefaultStableDurationTicks();
    }
}
