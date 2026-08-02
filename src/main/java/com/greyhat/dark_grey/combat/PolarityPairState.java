package com.greyhat.dark_grey.combat;

public class PolarityPairState {

    public boolean armed = true;
    public long lastExplosionWorldTime = 0;
    public long lastSeenWorldTime = 0;

    public void rearm() {
        this.armed = true;
    }
}
