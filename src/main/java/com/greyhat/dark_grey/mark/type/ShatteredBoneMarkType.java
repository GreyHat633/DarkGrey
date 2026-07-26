package com.greyhat.dark_grey.mark.type;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import com.greyhat.dark_grey.DarkGrey;
import com.greyhat.dark_grey.common.Config;
import com.greyhat.dark_grey.mark.MarkInstance;
import com.greyhat.dark_grey.mark.MarkManager;
import com.greyhat.dark_grey.mark.api.AbstractMarkType;
import com.greyhat.dark_grey.mark.api.MarkApplyContext;
import com.greyhat.dark_grey.mark.api.MarkRemovalReason;
import com.greyhat.dark_grey.mark.api.MarkUpdateContext;
import com.greyhat.dark_grey.mark.api.MarkVisualData;

public class ShatteredBoneMarkType extends AbstractMarkType {

    public static final String ID = "shattered_bone";
    private final MarkVisualData visualData;

    public ShatteredBoneMarkType() {
        this.visualData = new MarkVisualData(
            new ResourceLocation(DarkGrey.MODID, "textures/gui/marks/shattered_bone.png"),
            "mark.dark_grey.shattered_bone.name",
            "mark.dark_grey.shattered_bone.description",
            0xFFDDDDDD, // Same bone color
            0xFFDDDDDD,
            0xFFDDDDDD,
            110, // Higher priority than fracture
            false, // showStacks (max 1, so no 1/1)
            false, // showStableTimer (custom HUD)
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
    public MarkVisualData getVisualData() {
        return visualData;
    }

    @Override
    public void onFirstApplied(EntityLivingBase target, MarkInstance instance, MarkApplyContext context) {
        if (target.worldObj.isRemote) return;
        applyShatteredBone(target, instance, context);
    }

    @Override
    public void onApplied(EntityLivingBase target, MarkInstance instance, MarkApplyContext context, int requestedStacks,
        int actualAddedStacks) {
        if (target.worldObj.isRemote) return;
        applyShatteredBone(target, instance, context);
    }

    private void applyShatteredBone(EntityLivingBase target, MarkInstance instance, MarkApplyContext context) {
        NBTTagCompound data = instance.getCustomData();

        int durationTicks = context.getDurationTicks() > 0 ? context.getDurationTicks()
            : Config.shatteredBoneDefaultIndependentDurationTicks;

        if (durationTicks > 0 && !"fracture_maintenance".equals(context.getApplicationId())) {
            data.setBoolean("HasIndependentDuration", true);
            data.setLong("IndependentExpireWorldTime", context.getWorldTime() + durationTicks);
            if (context.getSourceUuid() != null) {
                data.setLong(
                    "IndependentSourceUuidMost",
                    context.getSourceUuid()
                        .getMostSignificantBits());
                data.setLong(
                    "IndependentSourceUuidLeast",
                    context.getSourceUuid()
                        .getLeastSignificantBits());
            }
        }

        instance.setDecaying(false);
        instance.setNextPeriodicTriggerWorldTime(context.getWorldTime() + 1);

        MarkManager.syncMark(target, instance, this, (byte) 4, 0, false);
    }

    @Override
    public void onPeriodicTrigger(EntityLivingBase target, MarkInstance instance, MarkUpdateContext context) {
        if (target.worldObj.isRemote) return;

        long now = context.getWorldTime();
        NBTTagCompound data = instance.getCustomData();

        boolean maintainedByFracture = data.getBoolean("MaintainedByFracture");
        boolean hasIndependentDuration = data.getBoolean("HasIndependentDuration");
        long independentExpireWorldTime = data.getLong("IndependentExpireWorldTime");

        boolean independentValid = hasIndependentDuration && now < independentExpireWorldTime;

        if (!maintainedByFracture && !independentValid) {
            MarkManager.remove(target, ID, MarkRemovalReason.EXPIRED);
            return;
        }

        if (hasIndependentDuration && now >= independentExpireWorldTime) {
            data.setBoolean("HasIndependentDuration", false);
            MarkManager.syncMark(target, instance, this, (byte) 4, 0, false);
        }

        instance.setNextPeriodicTriggerWorldTime(now + 1);
    }
}
