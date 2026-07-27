package com.greyhat.dark_grey.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;

/** One packet containing the same 200 bone-fragment particles previously sent as 200 packets. */
public final class ShatteredBoneParticlesMessage implements IMessage {

    public static final int PARTICLE_COUNT = 200;

    public float x;
    public float y;
    public float z;
    public final float[] velocities = new float[PARTICLE_COUNT * 3];

    public ShatteredBoneParticlesMessage() {}

    public ShatteredBoneParticlesMessage(double x, double y, double z, float[] velocities) {
        this.x = (float) x;
        this.y = (float) y;
        this.z = (float) z;
        if (velocities == null || velocities.length != this.velocities.length) {
            throw new IllegalArgumentException("Expected exactly " + this.velocities.length + " velocity values");
        }
        System.arraycopy(velocities, 0, this.velocities, 0, this.velocities.length);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int requiredBytes = (3 + this.velocities.length) * 4;
        if (buf.readableBytes() != requiredBytes) {
            throw new DecoderException("Invalid shattered-bone particle payload length: " + buf.readableBytes());
        }
        this.x = buf.readFloat();
        this.y = buf.readFloat();
        this.z = buf.readFloat();
        for (int i = 0; i < this.velocities.length; i++) {
            this.velocities[i] = buf.readFloat();
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeFloat(this.x);
        buf.writeFloat(this.y);
        buf.writeFloat(this.z);
        for (float velocity : this.velocities) {
            buf.writeFloat(velocity);
        }
    }
}
