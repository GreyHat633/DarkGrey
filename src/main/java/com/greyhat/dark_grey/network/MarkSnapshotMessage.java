package com.greyhat.dark_grey.network;

import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class MarkSnapshotMessage implements IMessage {

    public int entityId;
    public List<MarkSyncMessage.MarkData> marks;

    public MarkSnapshotMessage() {}

    public MarkSnapshotMessage(int entityId, List<MarkSyncMessage.MarkData> marks) {
        this.entityId = entityId;
        this.marks = marks;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityId = buf.readInt();
        int count = buf.readInt();
        this.marks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            MarkSyncMessage.MarkData data = new MarkSyncMessage.MarkData();
            data.fromBytes(buf);
            this.marks.add(data);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeInt(this.marks != null ? this.marks.size() : 0);
        if (this.marks != null) {
            for (MarkSyncMessage.MarkData data : this.marks) {
                data.toBytes(buf);
            }
        }
    }
}
