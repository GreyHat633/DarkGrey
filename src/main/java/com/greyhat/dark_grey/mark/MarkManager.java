package com.greyhat.dark_grey.mark;

import net.minecraft.entity.EntityLivingBase;

import com.greyhat.dark_grey.DarkGrey;
import com.greyhat.dark_grey.mark.api.IMarkType;
import com.greyhat.dark_grey.mark.api.MarkApplyContext;
import com.greyhat.dark_grey.mark.api.MarkApplyResult;
import com.greyhat.dark_grey.mark.api.MarkRemovalReason;
import com.greyhat.dark_grey.mark.api.MarkUpdateContext;
import com.greyhat.dark_grey.network.MarkClearEntityMessage;
import com.greyhat.dark_grey.network.MarkRemoveMessage;
import com.greyhat.dark_grey.network.MarkSyncMessage;

public final class MarkManager {

    private MarkManager() {}

    public static MarkApplyResult apply(EntityLivingBase target, String markId, int amount, EntityLivingBase source) {
        MarkApplyContext.Builder builder = new MarkApplyContext.Builder().source(source)
            .requestedStacks(amount)
            .worldTime(target != null ? target.worldObj.getTotalWorldTime() : 0)
            .applicationId("api")
            .refreshDuration(true)
            .triggerImmediate(true);
        IMarkType type = MarkRegistry.get(markId);
        if (type != null) {
            builder.stableDurationTicks(type.getDefaultStableDurationTicks());
        }
        return apply(target, markId, builder.build());
    }

    public static MarkApplyResult apply(EntityLivingBase target, String markId, MarkApplyContext context) {
        if (target == null || target.isDead) {
            return new MarkApplyResult(
                false,
                false,
                false,
                false,
                0,
                context.getRequestedStacks(),
                0,
                0,
                "Invalid Target");
        }
        if (target.worldObj.isRemote) {
            return new MarkApplyResult(
                false,
                false,
                false,
                false,
                0,
                context.getRequestedStacks(),
                0,
                0,
                "Client Side");
        }
        if (context.getRequestedStacks() <= 0) {
            return new MarkApplyResult(
                false,
                false,
                false,
                false,
                0,
                context.getRequestedStacks(),
                0,
                0,
                "Invalid Amount");
        }

        IMarkType type = MarkRegistry.get(markId);
        if (type == null) {
            return new MarkApplyResult(
                false,
                false,
                false,
                false,
                0,
                context.getRequestedStacks(),
                0,
                0,
                "Unknown Mark");
        }

        int stableDurationTicks = context.hasStableDurationTicks() ? context.getStableDurationTicks()
            : type.getDefaultStableDurationTicks();
        if (stableDurationTicks < 0) {
            stableDurationTicks = 0;
        }

        MarkContainer container = MarkContainer.get(target);
        if (container == null) {
            return new MarkApplyResult(
                false,
                false,
                false,
                false,
                0,
                context.getRequestedStacks(),
                0,
                0,
                "No Container");
        }

        MarkInstance instance = container.getMark(markId);
        boolean firstApplication = false;
        int oldStacks = 0;

        if (instance == null) {
            firstApplication = true;
            instance = new MarkInstance(type.getId());
            instance.setFirstAppliedWorldTime(context.getWorldTime());
            container.put(instance);
        } else {
            oldStacks = instance.getStacks();
        }

        if (!type.canApply(target, instance, context)) {
            return new MarkApplyResult(
                false,
                firstApplication,
                false,
                false,
                oldStacks,
                context.getRequestedStacks(),
                0,
                oldStacks,
                "Rejected By Type");
        }

        int maxStacks = type.getMaxStacks();
        boolean wasAlreadyMax = oldStacks >= maxStacks;

        int effectiveRequestedStacks = context.getRequestedStacks();
        if (context.getSource() instanceof net.minecraft.entity.player.EntityPlayer) {
            effectiveRequestedStacks = com.greyhat.dark_grey.api.SetBonusManager.modifyMarkRequestedStacks(
                (net.minecraft.entity.player.EntityPlayer) context.getSource(),
                target,
                markId,
                context,
                effectiveRequestedStacks);
        }

        int newStacks = Math.min(oldStacks + effectiveRequestedStacks, maxStacks);
        int actualAddedStacks = newStacks - oldStacks;
        boolean reachedMax = newStacks >= maxStacks;

        instance.setStacks(newStacks);
        instance.setStableDurationTicks(stableDurationTicks);
        instance.setLastAppliedWorldTime(context.getWorldTime());

        if (context.getSourceUuid() != null) {
            instance.setSourceUuid(context.getSourceUuid());
            if (context.getSource() != null) {
                instance.setSourceEntityId(
                    context.getSource()
                        .getEntityId());
                instance.setSourceDimension(context.getSource().dimension);
            }
        }

        try {
            if (firstApplication) {
                type.onFirstApplied(target, instance, context);
            }
            type.onApplied(target, instance, context, effectiveRequestedStacks, actualAddedStacks);
        } catch (Exception e) {
            DarkGrey.LOG.error("Error during mark apply logic for " + markId, e);
        }

        syncMark(
            target,
            instance,
            type,
            (byte) (firstApplication ? 1 : (wasAlreadyMax && actualAddedStacks == 0 ? 3 : 2)),
            actualAddedStacks,
            context.isTriggerImmediate());

        return new MarkApplyResult(
            true,
            firstApplication,
            reachedMax,
            wasAlreadyMax,
            oldStacks,
            effectiveRequestedStacks,
            actualAddedStacks,
            newStacks,
            null);
    }

    public static int getStacks(EntityLivingBase target, String markId) {
        if (target == null) return 0;
        MarkContainer container = MarkContainer.get(target);
        if (container == null) return 0;
        MarkInstance instance = container.getMark(markId);
        return instance != null ? instance.getStacks() : 0;
    }

    public static boolean has(EntityLivingBase target, String markId) {
        return getStacks(target, markId) > 0;
    }

    public static void remove(EntityLivingBase target, String markId, MarkRemovalReason reason) {
        if (target == null || target.worldObj.isRemote) return;
        MarkContainer container = MarkContainer.get(target);
        if (container == null) return;

        MarkInstance instance = container.remove(markId);
        if (instance != null) {
            IMarkType type = MarkRegistry.get(markId);
            if (type != null) {
                try {
                    type.onRemoved(
                        target,
                        instance,
                        new MarkUpdateContext(target.worldObj.getTotalWorldTime()),
                        reason);
                } catch (Exception e) {
                    DarkGrey.LOG.error("Error during mark remove logic for " + markId, e);
                }
            }
            DarkGrey.NETWORK.sendToAllAround(
                new MarkRemoveMessage(target.getEntityId(), instance.getMarkId()),
                new cpw.mods.fml.common.network.NetworkRegistry.TargetPoint(
                    target.dimension,
                    target.posX,
                    target.posY,
                    target.posZ,
                    64));
        }
    }

    public static void clearAll(EntityLivingBase target, MarkRemovalReason reason) {
        if (target == null || target.worldObj.isRemote) return;
        MarkContainer container = MarkContainer.get(target);
        if (container == null) return;

        for (MarkInstance instance : new java.util.ArrayList<MarkInstance>(container.getAllMarks())) {
            IMarkType type = MarkRegistry.get(instance.getMarkId());
            if (type != null && container.hasMark(instance.getMarkId())) {
                try {
                    type.onRemoved(
                        target,
                        instance,
                        new MarkUpdateContext(target.worldObj.getTotalWorldTime()),
                        reason);
                } catch (Exception e) {
                    DarkGrey.LOG.error("Error during mark remove logic", e);
                }
            }
        }
        container.clear();
        DarkGrey.NETWORK.sendToAllAround(
            new MarkClearEntityMessage(target.getEntityId()),
            new cpw.mods.fml.common.network.NetworkRegistry.TargetPoint(
                target.dimension,
                target.posX,
                target.posY,
                target.posZ,
                64));
    }

    public static void setStacks(EntityLivingBase target, String markId, int stacks, EntityLivingBase source) {
        if (target == null || target.worldObj.isRemote) return;
        if (stacks <= 0) {
            remove(target, markId, MarkRemovalReason.COMMAND);
            return;
        }
        MarkContainer container = MarkContainer.get(target);
        if (container == null) return;

        IMarkType type = MarkRegistry.get(markId);
        if (type == null) return;

        MarkInstance instance = container.getMark(markId);
        boolean first = false;
        int oldStacks = 0;
        if (instance == null) {
            instance = new MarkInstance(type.getId());
            container.put(instance);
            first = true;
        } else {
            oldStacks = instance.getStacks();
        }

        int newStacks = Math.min(stacks, type.getMaxStacks());
        instance.setStacks(newStacks);

        try {
            if (first) {
                MarkApplyContext context = new MarkApplyContext.Builder().source(source)
                    .requestedStacks(newStacks)
                    .worldTime(target.worldObj.getTotalWorldTime())
                    .applicationId("command")
                    .refreshDuration(false)
                    .triggerImmediate(false)
                    .stableDurationTicks(type.getDefaultStableDurationTicks())
                    .build();
                instance.setStableDurationTicks(type.getDefaultStableDurationTicks());
                type.onFirstApplied(target, instance, context);
            }
            type.onStacksChanged(
                target,
                instance,
                oldStacks,
                newStacks,
                new MarkUpdateContext(target.worldObj.getTotalWorldTime()));
        } catch (Exception e) {
            DarkGrey.LOG.error("Error during mark setStacks logic for " + markId, e);
        }

        syncMark(target, instance, type, (byte) 2, newStacks - oldStacks, false);
    }

    public static int consume(EntityLivingBase target, String markId, int amount) {
        if (target == null || target.worldObj.isRemote || amount <= 0) return 0;
        MarkContainer container = MarkContainer.get(target);
        if (container == null) return 0;

        MarkInstance instance = container.getMark(markId);
        if (instance == null) return 0;

        int current = instance.getStacks();
        int consumed = Math.min(current, amount);
        int remain = current - consumed;

        if (remain <= 0) {
            remove(target, markId, MarkRemovalReason.CONSUMED);
        } else {
            instance.setStacks(remain);
            IMarkType type = MarkRegistry.get(markId);
            syncMark(target, instance, type, (byte) 5, -consumed, false);
        }
        return consumed;
    }

    public static void syncMark(EntityLivingBase target, MarkInstance instance, IMarkType type, byte changeReason,
        int delta, boolean immediateTriggered) {
        if (target.worldObj.isRemote) return;
        MarkSyncMessage msg = new MarkSyncMessage(
            target.getEntityId(),
            instance.getMarkId(),
            instance.getStacks(),
            type != null ? type.getMaxStacks() : 99,
            instance.isDecaying(),
            instance.getStacks() >= (type != null ? type.getMaxStacks() : 99),
            instance.getStableUntilWorldTime(),
            instance.getNextPeriodicTriggerWorldTime(),
            instance.getNextDecayTriggerWorldTime(),
            instance.getCustomData(),
            changeReason,
            delta,
            immediateTriggered);
        DarkGrey.NETWORK.sendToAllAround(
            msg,
            new cpw.mods.fml.common.network.NetworkRegistry.TargetPoint(
                target.dimension,
                target.posX,
                target.posY,
                target.posZ,
                64));
    }
}
