package com.greyhat.dark_grey.mark.api;

import net.minecraft.entity.EntityLivingBase;

import com.greyhat.dark_grey.mark.MarkInstance;

public interface IMarkType {

    String getId();

    int getMaxStacks();

    boolean canApply(EntityLivingBase target, MarkInstance instance, MarkApplyContext context);

    void onFirstApplied(EntityLivingBase target, MarkInstance instance, MarkApplyContext context);

    void onApplied(EntityLivingBase target, MarkInstance instance, MarkApplyContext context, int requestedStacks,
        int actualAddedStacks);

    void onPeriodicTrigger(EntityLivingBase target, MarkInstance instance, MarkUpdateContext context);

    void onEnterDecay(EntityLivingBase target, MarkInstance instance, MarkUpdateContext context);

    void onDecayTrigger(EntityLivingBase target, MarkInstance instance, MarkUpdateContext context);

    void onRemoved(EntityLivingBase target, MarkInstance instance, MarkUpdateContext context, MarkRemovalReason reason);

    MarkVisualData getVisualData();
}
