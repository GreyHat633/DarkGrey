package com.greyhat.dark_grey.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class MarkRemoveMessage implements IMessage {

    public int entityId;
    public String markId;

    public MarkRemoveMessage() {}

    public MarkRemoveMessage(int entityId, String markId) {
        this.entityId = entityId;
        this.markId = markId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityId = buf.readInt();
        this.markId = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityId);
        ByteBufUtils.writeUTF8String(buf, this.markId);
    }
}
