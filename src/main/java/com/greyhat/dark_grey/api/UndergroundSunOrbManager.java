package com.greyhat.dark_grey.api;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import com.greyhat.dark_grey.entity.EntityUndergroundSunOrb;

public final class UndergroundSunOrbManager {

    private UndergroundSunOrbManager() {}

    @SuppressWarnings("rawtypes")
    public static List<EntityUndergroundSunOrb> getFollowingOrbs(EntityPlayer player) {
        World world = player.worldObj;
        AxisAlignedBB box = player.boundingBox.expand(8.0D, 8.0D, 8.0D);
        List entities = world.getEntitiesWithinAABB(EntityUndergroundSunOrb.class, box);
        List<EntityUndergroundSunOrb> orbs = new ArrayList<>();

        UUID playerUuid = player.getUniqueID();

        for (Object obj : entities) {
            if (obj instanceof EntityUndergroundSunOrb) {
                EntityUndergroundSunOrb orb = (EntityUndergroundSunOrb) obj;
                if (!orb.isDead && orb.getOrbState() == EntityUndergroundSunOrb.STATE_FOLLOWING) {
                    if (playerUuid.equals(orb.getOwnerUuid())) {
                        orbs.add(orb);
                    }
                }
            }
        }
        return orbs;
    }

    public static int countFollowingOrbs(EntityPlayer player) {
        return getFollowingOrbs(player).size();
    }

    public static EntityUndergroundSunOrb findOldestFollowingOrb(EntityPlayer player) {
        List<EntityUndergroundSunOrb> orbs = getFollowingOrbs(player);
        if (orbs.isEmpty()) return null;

        EntityUndergroundSunOrb oldest = null;
        for (EntityUndergroundSunOrb orb : orbs) {
            if (oldest == null) {
                oldest = orb;
            } else {
                if (orb.getSpawnWorldTime() < oldest.getSpawnWorldTime()) {
                    oldest = orb;
                } else if (orb.getSpawnWorldTime() == oldest.getSpawnWorldTime()
                    && orb.getEntityId() < oldest.getEntityId()) {
                        oldest = orb;
                    }
            }
        }
        return oldest;
    }

    public static int findFreeSlot(EntityPlayer player) {
        List<EntityUndergroundSunOrb> orbs = getFollowingOrbs(player);
        boolean[] used = new boolean[10];
        for (EntityUndergroundSunOrb orb : orbs) {
            int slot = orb.getFormationSlot();
            if (slot >= 0 && slot < used.length) {
                used[slot] = true;
            }
        }
        for (int i = 0; i < used.length; i++) {
            if (!used[i]) return i;
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    public static void removeAllOwnedOrbs(World world, UUID playerUuid) {
        if (world == null || playerUuid == null) return;
        List<net.minecraft.entity.Entity> entities = new ArrayList<>(world.loadedEntityList);
        for (net.minecraft.entity.Entity e : entities) {
            if (e instanceof EntityUndergroundSunOrb) {
                EntityUndergroundSunOrb orb = (EntityUndergroundSunOrb) e;
                if (playerUuid.equals(orb.getOwnerUuid())) {
                    orb.setDead();
                }
            }
        }
    }
}
