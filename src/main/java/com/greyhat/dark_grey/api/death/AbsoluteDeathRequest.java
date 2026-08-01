package com.greyhat.dark_grey.api.death;

import java.lang.ref.WeakReference;
import java.util.UUID;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;

public class AbsoluteDeathRequest {

    private final UUID targetUuid;
    private final WeakReference<EntityPlayerMP> targetRef;
    private final UUID executorUuid;
    private final int executorEntityId;
    private final int dimension;
    private final AbsoluteDeathReason reason;
    private final long requestedWorldTime;

    public AbsoluteDeathRequest(EntityPlayerMP target, EntityLivingBase executor, AbsoluteDeathReason reason,
        long worldTime) {
        this.targetUuid = target.getUniqueID();
        this.targetRef = new WeakReference<>(target);
        if (executor != null) {
            this.executorUuid = executor.getUniqueID();
            this.executorEntityId = executor.getEntityId();
        } else {
            this.executorUuid = null;
            this.executorEntityId = -1;
        }
        this.dimension = target.dimension;
        this.reason = reason;
        this.requestedWorldTime = worldTime;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public EntityPlayerMP getTargetIfOnline() {
        return targetRef.get();
    }

    public UUID getExecutorUuid() {
        return executorUuid;
    }

    public int getExecutorEntityId() {
        return executorEntityId;
    }

    public int getDimension() {
        return dimension;
    }

    public AbsoluteDeathReason getReason() {
        return reason;
    }

    public long getRequestedWorldTime() {
        return requestedWorldTime;
    }
}
