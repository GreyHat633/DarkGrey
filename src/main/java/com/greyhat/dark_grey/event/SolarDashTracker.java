package com.greyhat.dark_grey.event;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;

/** Short-lived server/client state for the current SolarFlare dash only. */
public final class SolarDashTracker {

    private static final Map<EntityPlayer, Boolean> HIT_PLAYERS = new WeakHashMap<>();
    private static final SolarDashTracker INSTANCE = new SolarDashTracker();

    private SolarDashTracker() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(INSTANCE);
        FMLCommonHandler.instance()
            .bus()
            .register(INSTANCE);
    }

    public static synchronized void clear(EntityPlayer player) {
        if (player != null) {
            HIT_PLAYERS.remove(player);
        }
    }

    public static synchronized boolean hasHit(EntityPlayer player) {
        return player != null && Boolean.TRUE.equals(HIT_PLAYERS.get(player));
    }

    public static synchronized void markHit(EntityPlayer player) {
        if (player != null) {
            HIT_PLAYERS.put(player, Boolean.TRUE);
        }
    }

    private static void clearLifecycle(EntityPlayer player) {
        clear(player);
        if (player != null) {
            player.stepHeight = 0.5F;
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (event.entityLiving instanceof EntityPlayer) {
            clearLifecycle((EntityPlayer) event.entityLiving);
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        clearLifecycle(event.original);
        clearLifecycle(event.entityPlayer);
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
        clearLifecycle(event.player);
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
        clearLifecycle(event.player);
    }
}
