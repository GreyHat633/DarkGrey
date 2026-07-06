package com.greyhat.dark_grey.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.entity.EntityLivingBase;

public class ScorchedMarkTracker {

    private static final WeakHashMap<EntityLivingBase, Integer> SCORCHED_MARKS = new WeakHashMap<>();

    // [WARNING] DO NOT use a WeakHashMap<EntityPlayer, Boolean> to track player-specific state!
    // In Minecraft singleplayer, EntityClientPlayerMP and EntityPlayerMP have the SAME getEntityId().
    // WeakHashMap uses Object.equals() which on Entity uses getEntityId(). This causes the server and client
    // threads to overwrite each other's keys and values in singleplayer, breaking game logic.
    // Always use player.getEntityData() for temporary booleans/state flags instead!

    public static synchronized void mark(EntityLivingBase entity, int duration) {
        SCORCHED_MARKS.put(entity, duration);
    }

    public static synchronized int getTimer(EntityLivingBase entity) {
        return SCORCHED_MARKS.getOrDefault(entity, 0);
    }

    public static synchronized void clear(EntityLivingBase entity) {
        SCORCHED_MARKS.remove(entity);
    }

    public static synchronized void tick() {
        if (SCORCHED_MARKS.isEmpty()) {
            return;
        }

        List<EntityLivingBase> toRemove = new ArrayList<>();
        Map<EntityLivingBase, Integer> updates = new HashMap<>();

        for (Map.Entry<EntityLivingBase, Integer> entry : SCORCHED_MARKS.entrySet()) {
            EntityLivingBase entity = entry.getKey();
            if (entity == null || entity.isDead) {
                toRemove.add(entity);
                continue;
            }

            int timer = entry.getValue();
            if (timer > 0) {
                int nextTimer = timer - 1;
                updates.put(entity, nextTimer);

                // Spawn scorched mark lava particle on the server side
                if (timer % 5 == 0 && entity.worldObj instanceof net.minecraft.world.WorldServer) {
                    ((net.minecraft.world.WorldServer) entity.worldObj).func_147487_a(
                        "lava",
                        entity.posX,
                        entity.posY + entity.height / 2.0,
                        entity.posZ,
                        1,
                        0.5,
                        0.5,
                        0.5,
                        0.0);
                }
            } else {
                toRemove.add(entity);
            }
        }

        for (EntityLivingBase entity : toRemove) {
            SCORCHED_MARKS.remove(entity);
        }
        SCORCHED_MARKS.putAll(updates);
    }
}
