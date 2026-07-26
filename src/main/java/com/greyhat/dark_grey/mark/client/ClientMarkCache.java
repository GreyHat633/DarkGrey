package com.greyhat.dark_grey.mark.client;

import java.util.HashMap;
import java.util.Map;

import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.world.WorldEvent;

import com.greyhat.dark_grey.network.MarkSyncMessage;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;

public class ClientMarkCache {

    private static final Map<Integer, ClientEntityMarks> CACHE = new HashMap<>();

    public static ClientEntityMarks get(int entityId) {
        return CACHE.get(entityId);
    }

    public static ClientEntityMarks getOrCreate(int entityId) {
        ClientEntityMarks marks = CACHE.get(entityId);
        if (marks == null) {
            marks = new ClientEntityMarks();
            CACHE.put(entityId, marks);
        }
        return marks;
    }

    public static void clear(int entityId) {
        CACHE.remove(entityId);
    }

    public static void clearAll() {
        CACHE.clear();
    }

    public static void updateMark(int entityId, MarkSyncMessage.MarkData data, byte changeReason, int displayedDelta,
        boolean immediateTriggered) {
        ClientEntityMarks entityMarks = getOrCreate(entityId);
        ClientMarkInstance instance = entityMarks.getMark(data.markId);
        if (instance == null) {
            instance = new ClientMarkInstance(data.markId);
            entityMarks.putMark(instance);
        }

        instance.stacks = data.stacks;
        instance.maxStacks = data.maxStacks;
        instance.decaying = data.decaying;
        instance.maxed = data.maxed;
        instance.stableUntilWorldTime = data.stableUntilWorldTime;
        instance.nextPeriodicTriggerWorldTime = data.nextPeriodicTriggerWorldTime;
        instance.nextDecayTriggerWorldTime = data.nextDecayTriggerWorldTime;
        instance.customData = data.customData;

        instance.changeReason = changeReason;
        instance.displayedDelta = displayedDelta;
        instance.immediateTriggered = immediateTriggered;

    }

    @SubscribeEvent
    public void onEntityDeath(LivingDeathEvent event) {
        if (event.entityLiving.worldObj.isRemote) {
            clear(event.entityLiving.getEntityId());
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world.isRemote) {
            clearAll();
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.player.worldObj.isRemote) {
            clearAll();
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player.worldObj.isRemote) {
            clearAll();
        }
    }
}
