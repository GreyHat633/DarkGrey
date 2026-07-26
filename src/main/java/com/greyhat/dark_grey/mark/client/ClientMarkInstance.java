package com.greyhat.dark_grey.mark.client;

import net.minecraft.nbt.NBTTagCompound;

public class ClientMarkInstance {

    public String markId;
    public int stacks;
    public int maxStacks;

    public boolean decaying;
    public boolean maxed;

    public long stableUntilWorldTime;
    public long nextPeriodicTriggerWorldTime;
    public long nextDecayTriggerWorldTime;
    public NBTTagCompound customData;

    public byte changeReason;
    public int displayedDelta;
    public boolean immediateTriggered;

    public long localCreationTime = System.currentTimeMillis();

    public ClientMarkInstance(String markId) {
        this.markId = markId;
    }
}
