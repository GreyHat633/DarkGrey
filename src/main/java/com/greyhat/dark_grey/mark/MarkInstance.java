package com.greyhat.dark_grey.mark;

import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;

public class MarkInstance {

    private String markId;
    private int stacks;

    private long firstAppliedWorldTime;
    private long lastAppliedWorldTime;

    private long stableUntilWorldTime;
    private int stableDurationTicks;
    private long nextPeriodicTriggerWorldTime;
    private long nextDecayTriggerWorldTime;
    private long lastPeriodicTriggerWorldTime;

    private boolean decaying;

    private UUID sourceUuid;
    private int sourceEntityId;
    private int sourceDimension;

    private NBTTagCompound customData;

    public MarkInstance(String markId) {
        this.markId = markId;
        this.customData = new NBTTagCompound();
    }

    public String getMarkId() {
        return markId;
    }

    public int getStacks() {
        return stacks;
    }

    public void setStacks(int stacks) {
        this.stacks = stacks;
    }

    public long getFirstAppliedWorldTime() {
        return firstAppliedWorldTime;
    }

    public void setFirstAppliedWorldTime(long firstAppliedWorldTime) {
        this.firstAppliedWorldTime = firstAppliedWorldTime;
    }

    public long getLastAppliedWorldTime() {
        return lastAppliedWorldTime;
    }

    public void setLastAppliedWorldTime(long lastAppliedWorldTime) {
        this.lastAppliedWorldTime = lastAppliedWorldTime;
    }

    public long getStableUntilWorldTime() {
        return stableUntilWorldTime;
    }

    public void setStableUntilWorldTime(long stableUntilWorldTime) {
        this.stableUntilWorldTime = stableUntilWorldTime;
    }

    public int getStableDurationTicks() {
        return stableDurationTicks;
    }

    public void setStableDurationTicks(int stableDurationTicks) {
        this.stableDurationTicks = Math.max(0, stableDurationTicks);
    }

    public long getNextPeriodicTriggerWorldTime() {
        return nextPeriodicTriggerWorldTime;
    }

    public void setNextPeriodicTriggerWorldTime(long nextPeriodicTriggerWorldTime) {
        this.nextPeriodicTriggerWorldTime = nextPeriodicTriggerWorldTime;
    }

    public long getNextDecayTriggerWorldTime() {
        return nextDecayTriggerWorldTime;
    }

    public void setNextDecayTriggerWorldTime(long nextDecayTriggerWorldTime) {
        this.nextDecayTriggerWorldTime = nextDecayTriggerWorldTime;
    }

    public long getLastPeriodicTriggerWorldTime() {
        return lastPeriodicTriggerWorldTime;
    }

    public void setLastPeriodicTriggerWorldTime(long lastPeriodicTriggerWorldTime) {
        this.lastPeriodicTriggerWorldTime = lastPeriodicTriggerWorldTime;
    }

    public boolean isDecaying() {
        return decaying;
    }

    public void setDecaying(boolean decaying) {
        this.decaying = decaying;
    }

    public UUID getSourceUuid() {
        return sourceUuid;
    }

    public void setSourceUuid(UUID sourceUuid) {
        this.sourceUuid = sourceUuid;
    }

    public int getSourceEntityId() {
        return sourceEntityId;
    }

    public void setSourceEntityId(int sourceEntityId) {
        this.sourceEntityId = sourceEntityId;
    }

    public int getSourceDimension() {
        return sourceDimension;
    }

    public void setSourceDimension(int sourceDimension) {
        this.sourceDimension = sourceDimension;
    }

    public NBTTagCompound getCustomData() {
        return customData;
    }

    public void setCustomData(NBTTagCompound customData) {
        this.customData = customData;
    }

    public void writeToNBT(NBTTagCompound nbt) {
        nbt.setString("Id", markId);
        nbt.setInteger("Stacks", stacks);
        nbt.setLong("FirstApplied", firstAppliedWorldTime);
        nbt.setLong("LastApplied", lastAppliedWorldTime);
        nbt.setLong("StableUntil", stableUntilWorldTime);
        nbt.setInteger("StableDurationTicks", stableDurationTicks);
        nbt.setLong("NextPeriodic", nextPeriodicTriggerWorldTime);
        nbt.setLong("NextDecay", nextDecayTriggerWorldTime);
        nbt.setLong("LastPeriodic", lastPeriodicTriggerWorldTime);
        nbt.setBoolean("Decaying", decaying);

        if (sourceUuid != null) {
            nbt.setLong("SourceUUIDMost", sourceUuid.getMostSignificantBits());
            nbt.setLong("SourceUUIDLeast", sourceUuid.getLeastSignificantBits());
        }
        nbt.setInteger("SourceEntityId", sourceEntityId);
        nbt.setInteger("SourceDimension", sourceDimension);

        if (customData != null && !customData.hasNoTags()) {
            nbt.setTag("CustomData", customData);
        }
    }

    public static MarkInstance readFromNBT(NBTTagCompound nbt) {
        String id = nbt.getString("Id");
        if (id == null || id.isEmpty()) {
            return null;
        }
        MarkInstance instance = new MarkInstance(id);
        instance.stacks = nbt.getInteger("Stacks");
        instance.firstAppliedWorldTime = nbt.getLong("FirstApplied");
        instance.lastAppliedWorldTime = nbt.getLong("LastApplied");
        instance.stableUntilWorldTime = nbt.getLong("StableUntil");
        instance.stableDurationTicks = nbt.getInteger("StableDurationTicks");
        instance.nextPeriodicTriggerWorldTime = nbt.getLong("NextPeriodic");
        instance.nextDecayTriggerWorldTime = nbt.getLong("NextDecay");
        instance.lastPeriodicTriggerWorldTime = nbt.getLong("LastPeriodic");
        instance.decaying = nbt.getBoolean("Decaying");

        if (nbt.hasKey("SourceUUIDMost") && nbt.hasKey("SourceUUIDLeast")) {
            instance.sourceUuid = new UUID(nbt.getLong("SourceUUIDMost"), nbt.getLong("SourceUUIDLeast"));
        }
        instance.sourceEntityId = nbt.getInteger("SourceEntityId");
        instance.sourceDimension = nbt.getInteger("SourceDimension");

        if (nbt.hasKey("CustomData")) {
            instance.customData = nbt.getCompoundTag("CustomData");
        }
        return instance;
    }
}
