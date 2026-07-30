package com.greyhat.dark_grey.combat;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.MinecraftForge;

import com.greyhat.dark_grey.DarkGrey;
import com.greyhat.dark_grey.api.CombatTargeting;
import com.greyhat.dark_grey.mark.MarkManager;
import com.greyhat.dark_grey.network.ShatteredBoneStaffCastEndMessage;
import com.greyhat.dark_grey.network.ShatteredBoneStaffCastStartMessage;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class ShatteredBoneStaffCastManager {

    public static final ShatteredBoneStaffCastManager INSTANCE = new ShatteredBoneStaffCastManager();

    private final Map<Integer, ShatteredBoneStaffCastState> activeCasts = new ConcurrentHashMap<>();

    private ShatteredBoneStaffCastManager() {
        cpw.mods.fml.common.FMLCommonHandler.instance()
            .bus()
            .register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void startCast(EntityLivingBase caster, ShatteredBoneStaffCastState state) {
        if (caster.worldObj.isRemote) return;

        activeCasts.put(caster.getEntityId(), state);

        long currentTime = caster.worldObj.getTotalWorldTime();
        DarkGrey.NETWORK.sendToAllAround(
            new ShatteredBoneStaffCastStartMessage(
                caster.getEntityId(),
                state.anchorX,
                state.anchorY,
                state.anchorZ,
                state.radius,
                currentTime,
                currentTime + state.maxTicks),
            new cpw.mods.fml.common.network.NetworkRegistry.TargetPoint(
                caster.dimension,
                state.anchorX,
                state.anchorY,
                state.anchorZ,
                64.0));
    }

    public void endCast(EntityLivingBase caster) {
        if (caster.worldObj.isRemote) return;
        if (activeCasts.remove(caster.getEntityId()) != null) {
            DarkGrey.NETWORK.sendToAllAround(
                new ShatteredBoneStaffCastEndMessage(caster.getEntityId()),
                new cpw.mods.fml.common.network.NetworkRegistry.TargetPoint(
                    caster.dimension,
                    caster.posX,
                    caster.posY,
                    caster.posZ,
                    64.0));
        }
    }

    public boolean isCasting(EntityLivingBase caster) {
        return activeCasts.containsKey(caster.getEntityId());
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Iterator<Map.Entry<Integer, ShatteredBoneStaffCastState>> it = activeCasts.entrySet()
            .iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, ShatteredBoneStaffCastState> entry = it.next();
            ShatteredBoneStaffCastState state = entry.getValue();

            EntityLivingBase caster = state.caster;
            if (caster.isDead || !caster.isEntityAlive()) {
                endCastInternal(state);
                it.remove();
                continue;
            }

            // Check teleport or large movement cancel
            double distSq = caster.getDistanceSq(state.anchorX, state.anchorY, state.anchorZ);
            if (distSq > state.teleportCancelThresholdSq) {
                endCastInternal(state);
                it.remove();
                continue;
            }

            // Lock X and Z to prevent horizontal movement completely
            caster.setPosition(state.anchorX, caster.posY, state.anchorZ);
            caster.motionX = 0;
            caster.motionZ = 0;

            if (caster instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) caster;

                // If they were flying initially, but now they aren't, they cancelled flight.
                // Cancel the spell!
                if (state.wasFlying && !player.capabilities.isFlying) {
                    endCastInternal(state);
                    it.remove();
                    continue;
                }

                if (player.capabilities.isFlying) {
                    // Lock Y to prevent flying up/down via space/shift
                    caster.setPosition(state.anchorX, state.anchorY, state.anchorZ);
                    caster.motionY = 0;
                } else {
                    // Allow falling, but prevent upward motion
                    if (caster.motionY > 0) caster.motionY = 0;
                }
            } else {
                if (caster.motionY > 0) caster.motionY = 0;
            }

            // Potion effects for FOV and additional locking
            caster.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 5, 7, true));
            caster.addPotionEffect(new PotionEffect(Potion.jump.id, 5, 128, true));

            state.currentTicks++;
            if (state.currentTicks >= state.maxTicks) {
                endCastInternal(state);
                it.remove();
                continue;
            }

            if (state.currentTicks % state.pulseIntervalTicks == 0) {
                pulseDamage(state);
            }
        }
    }

    private void pulseDamage(ShatteredBoneStaffCastState state) {
        EntityLivingBase caster = state.caster;
        AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(
            state.anchorX - state.radius,
            state.anchorY - 2.5,
            state.anchorZ - state.radius,
            state.anchorX + state.radius,
            state.anchorY + 2.5,
            state.anchorZ + state.radius);

        for (Object obj : caster.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, aabb)) {
            EntityLivingBase target = (EntityLivingBase) obj;
            if (target == caster || !CombatTargeting.canDamage(caster, target, false)) {
                continue;
            }

            // Check radius exactly (cylinder)
            double dx = target.posX - state.anchorX;
            double dz = target.posZ - state.anchorZ;
            if (dx * dx + dz * dz > state.radius * state.radius) {
                continue;
            }

            target.addPotionEffect(
                new PotionEffect(Potion.moveSlowdown.id, state.slownessRefreshTicks, state.slownessAmplifier));

            // Bypass hurt resistant time for this pulse
            int oldHurtResistantTime = target.hurtResistantTime;
            target.hurtResistantTime = 0;

            // Store motion to cancel knockback
            double mx = target.motionX;
            double my = target.motionY;
            double mz = target.motionZ;

            boolean hit = target
                .attackEntityFrom(ShatteredBoneDamageSources.causeShatteredBoneStaffDamage(caster), state.damage);
            target.hurtResistantTime = oldHurtResistantTime;

            if (hit) {
                // Restore motion to effectively cancel knockback
                target.motionX = mx;
                target.motionY = my;
                target.motionZ = mz;
                target.isAirBorne = false;

                Integer currentHits = state.hitCounts.get(target.getEntityId());
                int hits = (currentHits == null ? 0 : currentHits) + 1;
                if (hits >= state.hitsPerFracture) {
                    MarkManager.apply(target, state.fractureMarkId, state.fractureStacksPerTrigger, caster);
                    hits = 0; // Reset counter for this target
                }
                state.hitCounts.put(target.getEntityId(), hits);
            }
        }
    }

    private void endCastInternal(ShatteredBoneStaffCastState state) {
        DarkGrey.NETWORK.sendToAllAround(
            new ShatteredBoneStaffCastEndMessage(state.caster.getEntityId()),
            new cpw.mods.fml.common.network.NetworkRegistry.TargetPoint(
                state.caster.dimension,
                state.anchorX,
                state.anchorY,
                state.anchorZ,
                64.0));
    }

    // Ensure manager initializes by loading class
    public static void init() {}
}
