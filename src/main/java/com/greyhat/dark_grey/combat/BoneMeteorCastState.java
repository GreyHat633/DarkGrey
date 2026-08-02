package com.greyhat.dark_grey.combat;

import java.util.UUID;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

public class BoneMeteorCastState {

    public final EntityLivingBase caster;
    public final UUID casterUuid;
    public final World castStartWorld;
    public final int castStartDimension;
    public final long castStartWorldTime;

    public long lastConsumeWorldTime;

    public final int consumeIntervalTicks;
    public final int meteorsPerConsume;
    public final String materialItemId;
    public final int materialCost;

    public final double summonHeight;
    public final double summonRadius;

    public final float meteorFallSpeed;
    public final float meteorGravity;
    public final int meteorLifetimeTicks;

    public final double impactBoxSize;
    public final float impactDamage;

    public final String fractureMarkId;
    public final int fractureStacks;

    public final double castingMoveSpeedMultiplier;

    public final boolean consumeInCreative;
    public final boolean requireMaterialInCreative;

    public BoneMeteorCastState(EntityLivingBase caster, long castStartWorldTime, int consumeIntervalTicks,
        int meteorsPerConsume, String materialItemId, int materialCost, double summonHeight, double summonRadius,
        float meteorFallSpeed, float meteorGravity, int meteorLifetimeTicks, double impactBoxSize, float impactDamage,
        String fractureMarkId, int fractureStacks, double castingMoveSpeedMultiplier, boolean consumeInCreative,
        boolean requireMaterialInCreative) {

        this.caster = caster;
        this.casterUuid = caster.getUniqueID();
        this.castStartWorld = caster.worldObj;
        this.castStartDimension = caster.dimension;
        this.castStartWorldTime = castStartWorldTime;
        this.lastConsumeWorldTime = castStartWorldTime;

        this.consumeIntervalTicks = consumeIntervalTicks;
        this.meteorsPerConsume = meteorsPerConsume;
        this.materialItemId = materialItemId;
        this.materialCost = materialCost;

        this.summonHeight = summonHeight;
        this.summonRadius = summonRadius;

        this.meteorFallSpeed = meteorFallSpeed;
        this.meteorGravity = meteorGravity;
        this.meteorLifetimeTicks = meteorLifetimeTicks;

        this.impactBoxSize = impactBoxSize;
        this.impactDamage = impactDamage;

        this.fractureMarkId = fractureMarkId;
        this.fractureStacks = fractureStacks;

        this.castingMoveSpeedMultiplier = castingMoveSpeedMultiplier;

        this.consumeInCreative = consumeInCreative;
        this.requireMaterialInCreative = requireMaterialInCreative;
    }
}
