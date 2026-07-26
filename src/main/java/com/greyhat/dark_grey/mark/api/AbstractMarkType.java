package com.greyhat.dark_grey.mark.api;

import net.minecraft.entity.EntityLivingBase;

import com.greyhat.dark_grey.mark.MarkInstance;

public abstract class AbstractMarkType implements IMarkType {

    @Override
    public boolean canApply(EntityLivingBase target, MarkInstance instance, MarkApplyContext context) {
        return true;
    }

    @Override
    public void onFirstApplied(EntityLivingBase target, MarkInstance instance, MarkApplyContext context) {}

    @Override
    public void onApplied(EntityLivingBase target, MarkInstance instance, MarkApplyContext context, int requestedStacks,
        int actualAddedStacks) {}

    @Override
    public void onPeriodicTrigger(EntityLivingBase target, MarkInstance instance, MarkUpdateContext context) {}

    @Override
    public void onEnterDecay(EntityLivingBase target, MarkInstance instance, MarkUpdateContext context) {}

    @Override
    public void onDecayTrigger(EntityLivingBase target, MarkInstance instance, MarkUpdateContext context) {}

    @Override
    public void onRemoved(EntityLivingBase target, MarkInstance instance, MarkUpdateContext context,
        MarkRemovalReason reason) {}

    @Override
    public void onStacksChanged(EntityLivingBase target, MarkInstance instance, int oldStacks, int newStacks,
        MarkUpdateContext context) {}
}
