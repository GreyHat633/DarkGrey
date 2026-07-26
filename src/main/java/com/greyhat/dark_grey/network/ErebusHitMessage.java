package com.greyhat.dark_grey.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class ErebusHitMessage implements IMessage {

    public int[] entityIds;

    public ErebusHitMessage() {}

    public ErebusHitMessage(int[] entityIds) {
        this.entityIds = entityIds;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int length = buf.readInt();
        entityIds = new int[length];
        for (int i = 0; i < length; i++) {
            entityIds[i] = buf.readInt();
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityIds.length);
        for (int i = 0; i < entityIds.length; i++) {
            buf.writeInt(entityIds[i]);
        }
    }
}
