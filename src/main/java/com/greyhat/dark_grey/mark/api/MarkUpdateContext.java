package com.greyhat.dark_grey.mark.api;

public final class MarkUpdateContext {

    private final long worldTime;

    public MarkUpdateContext(long worldTime) {
        this.worldTime = worldTime;
    }

    public long getWorldTime() {
        return worldTime;
    }
}
