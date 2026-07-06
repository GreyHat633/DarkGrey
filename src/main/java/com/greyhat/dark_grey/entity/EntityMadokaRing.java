//
// Decompiled by Procyon v0.6.0
//

package com.greyhat.dark_grey.entity;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class EntityMadokaRing extends Entity {

    public int maxLife;
    public int age;
    public float initialScale;
    public float pitchRot;
    public float yawRot;

    public EntityMadokaRing(final World world) {
        super(world);
        this.maxLife = 20;
        this.age = 0;
        this.initialScale = 1.0f;
        this.pitchRot = 0.0f;
        this.yawRot = 0.0f;
        this.setSize(0.1f, 0.1f);
    }

    public EntityMadokaRing(final World world, final double x, final double y, final double z, final float scale,
        final float pitch, final float yaw) {
        this(world);
        this.setPosition(x, y, z);
        this.initialScale = scale;
        this.pitchRot = pitch;
        this.yawRot = yaw;
    }

    protected void entityInit() {}

    public void onUpdate() {
        super.onUpdate();
        ++this.age;
        if (this.age >= this.maxLife && !this.worldObj.isRemote) {
            this.setDead();
        }
    }

    protected void readEntityFromNBT(final NBTTagCompound tag) {
        this.age = tag.getInteger("Age");
        this.initialScale = tag.getFloat("InitScale");
        this.pitchRot = tag.getFloat("PitchRot");
        this.yawRot = tag.getFloat("YawRot");
    }

    protected void writeEntityToNBT(final NBTTagCompound tag) {
        tag.setInteger("Age", this.age);
        tag.setFloat("InitScale", this.initialScale);
        tag.setFloat("PitchRot", this.pitchRot);
        tag.setFloat("YawRot", this.yawRot);
    }
}
