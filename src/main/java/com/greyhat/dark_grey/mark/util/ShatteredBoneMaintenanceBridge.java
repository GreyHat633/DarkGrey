package com.greyhat.dark_grey.mark.util;

import java.util.UUID;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;

import com.greyhat.dark_grey.mark.MarkContainer;
import com.greyhat.dark_grey.mark.MarkInstance;
import com.greyhat.dark_grey.mark.MarkManager;
import com.greyhat.dark_grey.mark.MarkRegistry;
import com.greyhat.dark_grey.mark.api.MarkApplyContext;
import com.greyhat.dark_grey.mark.api.MarkRemovalReason;
import com.greyhat.dark_grey.mark.type.ShatteredBoneMarkType;

public class ShatteredBoneMaintenanceBridge {

    public static void setMaintainedByFracture(EntityLivingBase target, boolean maintained, UUID sourceUuid) {
        if (target == null || target.worldObj.isRemote) return;

        MarkContainer container = MarkContainer.get(target);
        if (container == null) return;

        MarkInstance instance = container.getMark(ShatteredBoneMarkType.ID);

        if (maintained) {
            if (instance == null) {
                MarkApplyContext context = new MarkApplyContext.Builder().sourceUuid(sourceUuid)
                    .requestedStacks(1)
                    .worldTime(target.worldObj.getTotalWorldTime())
                    .applicationId("fracture_maintenance")
                    .refreshDuration(false)
                    .triggerImmediate(true)
                    .stableDurationTicks(0)
                    .build();
                MarkManager.apply(target, ShatteredBoneMarkType.ID, context);
                instance = container.getMark(ShatteredBoneMarkType.ID);
            }

            if (instance != null) {
                NBTTagCompound data = instance.getCustomData();
                data.setBoolean("MaintainedByFracture", true);
                MarkManager.syncMark(target, instance, MarkRegistry.get(ShatteredBoneMarkType.ID), (byte) 4, 0, false);
            }
        } else {
            if (instance != null) {
                NBTTagCompound data = instance.getCustomData();
                data.setBoolean("MaintainedByFracture", false);

                boolean hasIndependentDuration = data.getBoolean("HasIndependentDuration");
                long expire = data.getLong("IndependentExpireWorldTime");
                long now = target.worldObj.getTotalWorldTime();

                if (!hasIndependentDuration || now >= expire) {
                    MarkManager.remove(target, ShatteredBoneMarkType.ID, MarkRemovalReason.EXPIRED);
                } else {
                    MarkManager
                        .syncMark(target, instance, MarkRegistry.get(ShatteredBoneMarkType.ID), (byte) 4, 0, false);
                }
            }
        }
    }
}
