package com.greyhat.dark_grey.mark.api;

import java.util.UUID;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;

public final class MarkApplyContext {

    private final EntityLivingBase source;
    private final UUID sourceUuid;
    private final int requestedStacks;
    private final long worldTime;
    private final String applicationId;
    private final boolean refreshDuration;
    private final boolean triggerImmediate;
    private final NBTTagCompound extraData;

    public MarkApplyContext(EntityLivingBase source, int requestedStacks, long worldTime, String applicationId,
        boolean refreshDuration, boolean triggerImmediate, NBTTagCompound extraData) {
        this.source = source;
        this.sourceUuid = source != null ? source.getUniqueID() : null;
        this.requestedStacks = requestedStacks;
        this.worldTime = worldTime;
        this.applicationId = applicationId;
        this.refreshDuration = refreshDuration;
        this.triggerImmediate = triggerImmediate;
        this.extraData = extraData != null ? (NBTTagCompound) extraData.copy() : null;
    }

    public EntityLivingBase getSource() {
        return source;
    }

    public UUID getSourceUuid() {
        return sourceUuid;
    }

    public int getRequestedStacks() {
        return requestedStacks;
    }

    public long getWorldTime() {
        return worldTime;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public boolean isRefreshDuration() {
        return refreshDuration;
    }

    public boolean isTriggerImmediate() {
        return triggerImmediate;
    }

    public NBTTagCompound getExtraData() {
        return extraData != null ? (NBTTagCompound) extraData.copy() : null;
    }
}
