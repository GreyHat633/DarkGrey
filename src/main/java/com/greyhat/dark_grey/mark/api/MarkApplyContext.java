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
    private final int stableDurationTicks;

    private MarkApplyContext(Builder builder) {
        this.source = builder.source;
        this.sourceUuid = builder.sourceUuid != null ? builder.sourceUuid
            : (builder.source != null ? builder.source.getUniqueID() : null);
        this.requestedStacks = builder.requestedStacks;
        this.worldTime = builder.worldTime;
        this.applicationId = builder.applicationId;
        this.refreshDuration = builder.refreshDuration;
        this.triggerImmediate = builder.triggerImmediate;
        this.extraData = builder.extraData != null ? (NBTTagCompound) builder.extraData.copy() : null;
        this.stableDurationTicks = builder.stableDurationTicks;
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

    public boolean hasStableDurationTicks() {
        return stableDurationTicks >= 0;
    }

    public int getStableDurationTicks() {
        return stableDurationTicks;
    }

    /**
     * @deprecated Use {@link #getStableDurationTicks()}.
     */
    @Deprecated
    public int getDurationTicks() {
        return getStableDurationTicks();
    }

    public static class Builder {

        private EntityLivingBase source;
        private UUID sourceUuid;
        private int requestedStacks = 1;
        private long worldTime;
        private String applicationId = "api";
        private boolean refreshDuration = true;
        private boolean triggerImmediate = true;
        private NBTTagCompound extraData;
        private int stableDurationTicks = -1;

        public Builder source(EntityLivingBase source) {
            this.source = source;
            return this;
        }

        public Builder sourceUuid(UUID sourceUuid) {
            this.sourceUuid = sourceUuid;
            return this;
        }

        public Builder requestedStacks(int requestedStacks) {
            this.requestedStacks = requestedStacks;
            return this;
        }

        public Builder worldTime(long worldTime) {
            this.worldTime = worldTime;
            return this;
        }

        public Builder applicationId(String applicationId) {
            this.applicationId = applicationId;
            return this;
        }

        public Builder refreshDuration(boolean refreshDuration) {
            this.refreshDuration = refreshDuration;
            return this;
        }

        public Builder triggerImmediate(boolean triggerImmediate) {
            this.triggerImmediate = triggerImmediate;
            return this;
        }

        public Builder extraData(NBTTagCompound extraData) {
            this.extraData = extraData;
            return this;
        }

        public Builder stableDurationTicks(int stableDurationTicks) {
            if (stableDurationTicks < 0) {
                throw new IllegalArgumentException("stableDurationTicks must be >= 0");
            }
            this.stableDurationTicks = stableDurationTicks;
            return this;
        }

        /**
         * @deprecated Use {@link #stableDurationTicks(int)}.
         */
        @Deprecated
        public Builder durationTicks(int durationTicks) {
            return stableDurationTicks(durationTicks);
        }

        public MarkApplyContext build() {
            return new MarkApplyContext(this);
        }
    }
}
