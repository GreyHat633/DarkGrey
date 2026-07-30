package com.greyhat.dark_grey.combat;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.EntityLivingBase;

public class ShatteredBoneStaffCastState {

    public final EntityLivingBase caster;
    public final double anchorX;
    public final double anchorY;
    public final double anchorZ;
    public final float radius;
    public final float damage;
    public final int maxTicks;
    public final int pulseIntervalTicks;
    public final String fractureMarkId;
    public final int hitsPerFracture;
    public final int fractureStacksPerTrigger;
    public final int slownessAmplifier;
    public final int slownessRefreshTicks;
    public final double teleportCancelThresholdSq;
    public final boolean wasFlying;

    public int currentTicks = 0;

    // Track hits per entity to calculate fracture applications
    public final Map<Integer, Integer> hitCounts = new HashMap<>();

    public ShatteredBoneStaffCastState(EntityLivingBase caster, float radius, float damage, int maxTicks,
        int pulseIntervalTicks, String fractureMarkId, int hitsPerFracture, int fractureStacksPerTrigger,
        int slownessAmplifier, int slownessRefreshTicks, double teleportCancelThresholdSq) {
        this.caster = caster;
        this.anchorX = caster.posX;
        this.anchorY = caster.posY;
        this.anchorZ = caster.posZ;
        this.radius = radius;
        this.damage = damage;
        this.maxTicks = maxTicks;
        this.pulseIntervalTicks = pulseIntervalTicks;
        this.fractureMarkId = fractureMarkId;
        this.hitsPerFracture = hitsPerFracture;
        this.fractureStacksPerTrigger = fractureStacksPerTrigger;
        this.slownessAmplifier = slownessAmplifier;
        this.slownessRefreshTicks = slownessRefreshTicks;
        this.teleportCancelThresholdSq = teleportCancelThresholdSq;
        if (caster instanceof net.minecraft.entity.player.EntityPlayer) {
            this.wasFlying = ((net.minecraft.entity.player.EntityPlayer) caster).capabilities.isFlying;
        } else {
            this.wasFlying = false;
        }
    }
}
