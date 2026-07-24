package com.greyhat.dark_grey.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class UndergroundSunExplosionMessage implements IMessage {

    public double x;
    public double y;
    public double z;
    public float radius;

    public UndergroundSunExplosionMessage() {}

    public UndergroundSunExplosionMessage(double x, double y, double z, float radius) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.radius = buf.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(radius);
    }
}
