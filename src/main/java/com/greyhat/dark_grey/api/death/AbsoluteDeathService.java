package com.greyhat.dark_grey.api.death;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;

import com.greyhat.dark_grey.DarkGrey;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;

public class AbsoluteDeathService {

    private static final ConcurrentLinkedQueue<AbsoluteDeathRequest> QUEUE = new ConcurrentLinkedQueue<>();
    private static final Set<UUID> EXECUTING = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
    public static final Map<UUID, AbsoluteDeathRequest> ACTIVE_CONTEXT = new ConcurrentHashMap<>();

    public static void requestAbsoluteDeath(EntityPlayerMP target, EntityLivingBase executor,
        AbsoluteDeathReason reason) {
        if (target == null || target.worldObj == null || target.worldObj.isRemote) return;
        long time = target.worldObj.getTotalWorldTime();
        QUEUE.add(new AbsoluteDeathRequest(target, executor, reason, time));
    }

    public static void requestAbsoluteDeath(EntityPlayerMP target, AbsoluteDeathReason reason) {
        requestAbsoluteDeath(target, null, reason);
    }

    public static boolean isAbsoluteDeathSource(DamageSource source) {
        return source instanceof AbsoluteDeathSource;
    }

    public static boolean isExecuting(UUID playerUuid) {
        return EXECUTING.contains(playerUuid) || ACTIVE_CONTEXT.containsKey(playerUuid);
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent event) {
        if (event.phase != Phase.END) return;
        if (QUEUE.isEmpty()) return;

        int count = QUEUE.size();
        for (int i = 0; i < count; i++) {
            AbsoluteDeathRequest req = QUEUE.poll();
            if (req != null) {
                executeNow(req);
            }
        }
    }

    private static void executeNow(AbsoluteDeathRequest request) {
        EntityPlayerMP target = request.getTargetIfOnline();
        if (target == null) return;

        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) return;
        boolean found = false;
        for (Object obj : server.getConfigurationManager().playerEntityList) {
            if (obj instanceof EntityPlayerMP) {
                EntityPlayerMP p = (EntityPlayerMP) obj;
                if (p.getUniqueID()
                    .equals(request.getTargetUuid()) && p == target
                    && !p.isDead
                    && p.getHealth() > 0) {
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            DarkGrey.LOG.debug(
                "AbsoluteDeath: Target {} is invalid, dead, or offline, ignoring request.",
                request.getTargetUuid());
            return;
        }

        UUID uuid = request.getTargetUuid();
        if (!EXECUTING.add(uuid)) {
            return;
        }

        try {
            ACTIVE_CONTEXT.put(uuid, request);

            target.setAbsorptionAmount(0.0F);

            EntityLivingBase executor = null;
            if (request.getExecutorUuid() != null) {
                Entity e = target.worldObj.getEntityByID(request.getExecutorEntityId());
                if (e instanceof EntityLivingBase && e.getUniqueID()
                    .equals(request.getExecutorUuid())) {
                    executor = (EntityLivingBase) e;
                }
            }

            AbsoluteDeathSource source = new AbsoluteDeathSource(request.getReason(), executor);

            float healthBefore = Math.max(1.0F, target.getHealth());
            try {
                target.func_110142_aN()
                    .func_94547_a(source, healthBefore, healthBefore);
            } catch (Throwable t) {
                // Ignore if method names are wrong
            }

            target.setHealth(0.0F);
            target.attackEntityFrom(source, 1.0F);

        } finally {
            ACTIVE_CONTEXT.remove(uuid);
            EXECUTING.remove(uuid);
        }
    }
}
