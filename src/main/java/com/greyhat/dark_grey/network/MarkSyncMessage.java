package com.greyhat.dark_grey.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class MarkSyncMessage implements IMessage {

    public static class MarkData {

        public String markId;
        public int stacks;
        public int maxStacks;
        public boolean decaying;
        public boolean maxed;
        public long stableUntilWorldTime;
        public long nextPeriodicTriggerWorldTime;
        public long nextDecayTriggerWorldTime;

        public MarkData() {}

        public MarkData(String markId, int stacks, int maxStacks, boolean decaying, boolean maxed, long stableUntil,
            long nextPeriodic, long nextDecay) {
            this.markId = markId;
            this.stacks = stacks;
            this.maxStacks = maxStacks;
            this.decaying = decaying;
            this.maxed = maxed;
            this.stableUntilWorldTime = stableUntil;
            this.nextPeriodicTriggerWorldTime = nextPeriodic;
            this.nextDecayTriggerWorldTime = nextDecay;
        }

        public void fromBytes(ByteBuf buf) {
            this.markId = ByteBufUtils.readUTF8String(buf);
            this.stacks = buf.readInt();
            this.maxStacks = buf.readInt();
            this.decaying = buf.readBoolean();
            this.maxed = buf.readBoolean();
            this.stableUntilWorldTime = buf.readLong();
            this.nextPeriodicTriggerWorldTime = buf.readLong();
            this.nextDecayTriggerWorldTime = buf.readLong();
        }

        public void toBytes(ByteBuf buf) {
            ByteBufUtils.writeUTF8String(buf, this.markId);
            buf.writeInt(this.stacks);
            buf.writeInt(this.maxStacks);
            buf.writeBoolean(this.decaying);
            buf.writeBoolean(this.maxed);
            buf.writeLong(this.stableUntilWorldTime);
            buf.writeLong(this.nextPeriodicTriggerWorldTime);
            buf.writeLong(this.nextDecayTriggerWorldTime);
        }
    }

    public int entityId;
    public MarkData data;
    public byte changeReason;
    public int displayedDelta;
    public boolean immediateTriggered;

    public MarkSyncMessage() {}

    public MarkSyncMessage(int entityId, String markId, int stacks, int maxStacks, boolean decaying, boolean maxed,
        long stableUntil, long nextPeriodic, long nextDecay, byte changeReason, int displayedDelta,
        boolean immediateTriggered) {
        this.entityId = entityId;
        this.data = new MarkData(markId, stacks, maxStacks, decaying, maxed, stableUntil, nextPeriodic, nextDecay);
        this.changeReason = changeReason;
        this.displayedDelta = displayedDelta;
        this.immediateTriggered = immediateTriggered;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityId = buf.readInt();
        this.data = new MarkData();
        this.data.fromBytes(buf);
        this.changeReason = buf.readByte();
        this.displayedDelta = buf.readInt();
        this.immediateTriggered = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityId);
        this.data.toBytes(buf);
        buf.writeByte(this.changeReason);
        buf.writeInt(this.displayedDelta);
        buf.writeBoolean(this.immediateTriggered);
    }
}
