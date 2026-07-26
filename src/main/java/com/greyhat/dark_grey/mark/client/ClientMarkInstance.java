package com.greyhat.dark_grey.mark.client;

public class ClientMarkInstance {

    public String markId;
    public int stacks;
    public int maxStacks;

    public boolean decaying;
    public boolean maxed;

    public long stableUntilWorldTime;
    public long nextPeriodicTriggerWorldTime;
    public long nextDecayTriggerWorldTime;

    public byte changeReason;
    public int displayedDelta;
    public boolean immediateTriggered;

    public ClientMarkInstance(String markId) {
        this.markId = markId;
    }
}
