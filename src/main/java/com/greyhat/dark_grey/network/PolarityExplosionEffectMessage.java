package com.greyhat.dark_grey.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class PolarityExplosionEffectMessage implements IMessage {

    public double x;
    public double y;
    public double z;
    public boolean special;

    public PolarityExplosionEffectMessage() {}

    public PolarityExplosionEffectMessage(double x, double y, double z, boolean special) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.special = special;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readDouble();
        y = buf.readDouble();
        z = buf.readDouble();
        special = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeBoolean(special);
    }
}
