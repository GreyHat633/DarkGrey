package com.greyhat.dark_grey.mark.type;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;

import com.greyhat.dark_grey.DarkGrey;
import com.greyhat.dark_grey.api.RPGDamageSources;
import com.greyhat.dark_grey.mark.MarkInstance;
import com.greyhat.dark_grey.mark.MarkManager;
import com.greyhat.dark_grey.mark.api.AbstractMarkType;
import com.greyhat.dark_grey.mark.api.MarkApplyContext;
import com.greyhat.dark_grey.mark.api.MarkDecayMode;
import com.greyhat.dark_grey.mark.api.MarkRemovalReason;
import com.greyhat.dark_grey.mark.api.MarkUpdateContext;
import com.greyhat.dark_grey.mark.api.MarkVisualData;

public class ScorchMarkType extends AbstractMarkType {

    public static final String ID = "scorch";
    private final MarkVisualData visualData;

    public ScorchMarkType() {
        this.visualData = new MarkVisualData(
            new ResourceLocation(DarkGrey.MODID, "textures/gui/marks/scorch.png"),
            "mark.dark_grey.scorch.name",
            "mark.dark_grey.scorch.description",
            0xFFFF4400, // primary (orange/red)
            0xFFFF4400, // decay
            0xFFFF4400, // max
            90, // priority
            true, // showStacks (now stacks up to 7)
            true, // showStableTimer
            false, // showPeriodicTimer
            false, // showDecayTimer
            true, // showOnEntity
            true, // showOnTargetPanel
            true, // showOnSelfHud
            true, // flashWhenNearDecay
            false // glowWhenMaxed
        );
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public int getMaxStacks() {
        return 7;
    }

    @Override
    public MarkDecayMode getDecayMode() {
        return MarkDecayMode.INSTANT;
    }

    @Override
    public int getDefaultStableDurationTicks() {
        return 30; // 1.5 seconds default
    }

    @Override
    public int getDecayIntervalTicks() {
        return 0;
    }

    @Override
    public int getDecayAmount() {
        return 7; // Instantly clear all max 7 stacks
    }

    @Override
    public MarkVisualData getVisualData() {
        return visualData;
    }

    @Override
    public void onFirstApplied(EntityLivingBase target, MarkInstance instance, MarkApplyContext context) {
        if (target.worldObj.isRemote) return;

        instance.setDecaying(false);
        int duration = getStableDurationTicks(instance);
        instance.setStableUntilWorldTime(context.getWorldTime() + duration);
    }

    @Override
    public void onApplied(EntityLivingBase target, MarkInstance instance, MarkApplyContext context, int requestedStacks,
        int actualAddedStacks) {
        if (target.worldObj.isRemote) return;

        instance.setDecaying(false);
        if (requestedStacks > 0) {
            instance.setStableUntilWorldTime(context.getWorldTime() + getStableDurationTicks(instance));
        }
    }

    @Override
    public void onEnterDecay(EntityLivingBase target, MarkInstance instance, MarkUpdateContext context) {
        if (target.worldObj.isRemote) return;

        int oldStacks = instance.getStacks();
        int newStacks = Math.max(0, oldStacks - getDecayAmount());

        if (newStacks > 0) {
            instance.setStacks(newStacks);
            instance.setDecaying(false);
            instance.setStableUntilWorldTime(context.getWorldTime() + getStableDurationTicks(instance));
            MarkManager.syncMark(target, instance, this, (byte) 5, newStacks - oldStacks, false);
        } else {
            instance.setStacks(0);
        }
    }

    @Override
    public void onRemoved(EntityLivingBase target, MarkInstance instance, MarkUpdateContext context,
        MarkRemovalReason reason) {
        // Nothing special to clean up
    }

    @Override
    public void onStacksChanged(EntityLivingBase target, MarkInstance instance, int oldStacks, int newStacks,
        MarkUpdateContext context) {
        if (target.worldObj.isRemote) return;

        instance.setDecaying(false);
        if (newStacks > 0 && instance.getStableUntilWorldTime() <= context.getWorldTime()) {
            instance.setStableUntilWorldTime(context.getWorldTime() + getStableDurationTicks(instance));
        }
    }

    private int getStableDurationTicks(MarkInstance instance) {
        int duration = instance.getStableDurationTicks();
        return duration > 0 ? duration : getDefaultStableDurationTicks();
    }

    public void detonate(EntityLivingBase target, EntityLivingBase detonator, float detonationMultiplier) {
        if (target == null || target.worldObj.isRemote) return;

        int stacks = MarkManager.getStacks(target, ID);
        if (stacks <= 0) return;

        float baseDmg = 20.0f + 10.0f * stacks;
        float explosionDmg = baseDmg * detonationMultiplier;

        target.worldObj.playSoundEffect(
            target.posX,
            target.posY,
            target.posZ,
            "random.explode",
            1.0F,
            (1.0F + (target.worldObj.rand.nextFloat() - target.worldObj.rand.nextFloat()) * 0.2F) * 0.7F);

        if (target.worldObj instanceof net.minecraft.world.WorldServer) {
            ((net.minecraft.world.WorldServer) target.worldObj).func_147487_a(
                "largeexplode",
                target.posX,
                target.posY + target.height / 2.0,
                target.posZ,
                5,
                0.0,
                0.0,
                0.0,
                0.0);
        }

        net.minecraft.util.AxisAlignedBB aabb = target.boundingBox.expand(3.0, 3.0, 3.0);
        @SuppressWarnings("unchecked")
        java.util.List<net.minecraft.entity.Entity> list = target.worldObj
            .getEntitiesWithinAABBExcludingEntity(detonator, aabb);
        for (net.minecraft.entity.Entity e : list) {
            if (e instanceof EntityLivingBase
                && com.greyhat.dark_grey.api.CombatTargeting.canDamage(detonator, (EntityLivingBase) e, false)) {

                DamageSource explosionSource;
                if (detonator instanceof net.minecraft.entity.player.EntityPlayer) {
                    explosionSource = RPGDamageSources
                        .causeScorchDetonationDamage((net.minecraft.entity.player.EntityPlayer) detonator);
                } else {
                    explosionSource = RPGDamageSources.causeMarkDamage(ID, detonator)
                        .setExplosion();
                }

                e.attackEntityFrom(explosionSource, explosionDmg);
            }
        }
    }
}
