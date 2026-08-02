package com.greyhat.dark_grey.mark.type;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;

import com.greyhat.dark_grey.DarkGrey;
import com.greyhat.dark_grey.combat.PolarityPhysicsManager;
import com.greyhat.dark_grey.mark.MarkInstance;
import com.greyhat.dark_grey.mark.MarkManager;
import com.greyhat.dark_grey.mark.api.AbstractMarkType;
import com.greyhat.dark_grey.mark.api.MarkApplyContext;
import com.greyhat.dark_grey.mark.api.MarkDecayMode;
import com.greyhat.dark_grey.mark.api.MarkRemovalReason;
import com.greyhat.dark_grey.mark.api.MarkUpdateContext;
import com.greyhat.dark_grey.mark.api.MarkVisualData;

public class NegativePolarityMarkType extends AbstractMarkType {

    public static final String ID = "negative_polarity";
    private final MarkVisualData visualData;

    public NegativePolarityMarkType() {
        this.visualData = new MarkVisualData(
            new ResourceLocation(DarkGrey.MODID, "textures/marks/negative_polarity.png"),
            "mark.dark_grey.negative_polarity.name",
            "mark.dark_grey.negative_polarity.description",
            0xFF3C64FF, // primary (blue)
            0xFF3C64FF, // decay
            0xFF3C64FF, // max
            100, // priority
            false, // showStacks
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
    public int getDecayIntervalTicks() {
        return 0;
    }

    @Override
    public int getDecayAmount() {
        return 1;
    }

    @Override
    public int getDefaultStableDurationTicks() {
        return 1200; // 60s
    }

    @Override
    public boolean canApply(EntityLivingBase target, MarkInstance instance, MarkApplyContext context) {
        if (!target.worldObj.isRemote) {
            if (MarkManager.has(target, PositivePolarityMarkType.ID)) {
                MarkManager.remove(target, PositivePolarityMarkType.ID, MarkRemovalReason.CLEANSED);
                return false;
            }
        }
        return super.canApply(target, instance, context);
    }

    @Override
    public void onFirstApplied(EntityLivingBase target, MarkInstance instance, MarkApplyContext context) {
        if (target.worldObj.isRemote) return;
        instance.setDecaying(false);
        int duration = instance.getStableDurationTicks();
        instance.setStableUntilWorldTime(context.getWorldTime() + duration);
        PolarityPhysicsManager.track(target);
    }

    @Override
    public void onApplied(EntityLivingBase target, MarkInstance instance, MarkApplyContext context, int requestedStacks,
        int actualAddedStacks) {
        if (target.worldObj.isRemote) return;
        instance.setDecaying(false);
        if (requestedStacks > 0) {
            instance.setStableUntilWorldTime(context.getWorldTime() + instance.getStableDurationTicks());
        }
        PolarityPhysicsManager.track(target);
    }

    @Override
    public void onEnterDecay(EntityLivingBase target, MarkInstance instance, MarkUpdateContext context) {
        if (target.worldObj.isRemote) return;
        instance.setStacks(0);
    }

    @Override
    public void onRemoved(EntityLivingBase target, MarkInstance instance, MarkUpdateContext context,
        MarkRemovalReason reason) {
        if (!target.worldObj.isRemote) PolarityPhysicsManager.untrackIfUnmarked(target);
    }

    @Override
    public MarkVisualData getVisualData() {
        return visualData;
    }
}
