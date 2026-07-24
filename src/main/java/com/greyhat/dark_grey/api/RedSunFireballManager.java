package com.greyhat.dark_grey.api;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;

import com.greyhat.dark_grey.entity.EntityRedSunFireball;

public class RedSunFireballManager {

    public static EntityRedSunFireball findChargingFireball(EntityPlayer player) {
        if (player == null || player.worldObj == null) return null;

        AxisAlignedBB aabb = player.boundingBox.expand(16.0D, 16.0D, 16.0D);
        List<EntityRedSunFireball> list = player.worldObj.getEntitiesWithinAABB(EntityRedSunFireball.class, aabb);

        for (EntityRedSunFireball fireball : list) {
            if (fireball.getState() == EntityRedSunFireball.STATE_CHARGING && !fireball.isDead) {
                if (player.getUniqueID()
                    .equals(fireball.getOwnerUuid())) {
                    return fireball;
                }
            }
        }
        return null;
    }

    public static void removeChargingFireball(EntityPlayer player) {
        EntityRedSunFireball fireball = findChargingFireball(player);
        if (fireball != null) {
            fireball.setDead();
        }
    }
}
