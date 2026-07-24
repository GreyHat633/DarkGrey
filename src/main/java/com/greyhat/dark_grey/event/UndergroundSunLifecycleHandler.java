package com.greyhat.dark_grey.event;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerEvent;

import com.greyhat.dark_grey.api.UndergroundSunOrbManager;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;

public class UndergroundSunLifecycleHandler {

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (event.wasDeath) {
            UndergroundSunOrbManager.removeAllOwnedOrbs(event.original.worldObj, event.original.getUniqueID());
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
        UndergroundSunOrbManager.removeAllOwnedOrbs(event.player.worldObj, event.player.getUniqueID());
        MinecraftServer server = MinecraftServer.getServer();
        if (server != null) {
            World oldWorld = server.worldServerForDimension(event.fromDim);
            if (oldWorld != null) {
                UndergroundSunOrbManager.removeAllOwnedOrbs(oldWorld, event.player.getUniqueID());
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
        UndergroundSunOrbManager.removeAllOwnedOrbs(event.player.worldObj, event.player.getUniqueID());
    }
}
