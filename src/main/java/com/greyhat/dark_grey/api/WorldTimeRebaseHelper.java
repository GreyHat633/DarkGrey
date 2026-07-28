package com.greyhat.dark_grey.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

/** Rebases dimension-local absolute tick timestamps when a player changes worlds. */
public final class WorldTimeRebaseHelper {

    private WorldTimeRebaseHelper() {}

    public static long getDimensionTimeDelta(EntityPlayer player, int fromDimension) {
        if (player == null || player.worldObj == null) return 0L;
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) return 0L;

        World oldWorld = server.worldServerForDimension(fromDimension);
        if (oldWorld == null) return 0L;
        return player.worldObj.getTotalWorldTime() - oldWorld.getTotalWorldTime();
    }

    public static long shiftPositiveTimestamp(long timestamp, long delta) {
        if (timestamp <= 0L || delta == 0L) return timestamp;
        if (delta > 0L && timestamp > Long.MAX_VALUE - delta) return Long.MAX_VALUE;
        if (delta < 0L && timestamp < Long.MIN_VALUE - delta) return 1L;
        return Math.max(1L, timestamp + delta);
    }
}
