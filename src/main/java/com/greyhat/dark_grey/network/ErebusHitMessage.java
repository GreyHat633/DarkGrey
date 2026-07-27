package com.greyhat.dark_grey.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;

public class ErebusHitMessage implements IMessage {

    private static final int MAX_ENTITY_IDS = 256;

    public int[] entityIds;

    public ErebusHitMessage() {}

    public ErebusHitMessage(int[] entityIds) {
        this.entityIds = entityIds;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int length = buf.readInt();
        if (length < 0 || length > MAX_ENTITY_IDS || buf.readableBytes() < length * 4) {
            throw new DecoderException("Invalid Erebus entity id count: " + length);
        }
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
