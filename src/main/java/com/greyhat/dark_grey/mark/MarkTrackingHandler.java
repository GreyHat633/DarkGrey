package com.greyhat.dark_grey.mark;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.player.PlayerEvent.StartTracking;

import com.greyhat.dark_grey.DarkGrey;
import com.greyhat.dark_grey.network.MarkSnapshotMessage;
import com.greyhat.dark_grey.network.MarkSyncMessage;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;

public class MarkTrackingHandler {

    @SubscribeEvent
    public void onStartTracking(StartTracking event) {
        if (event.target instanceof EntityLivingBase && event.entityPlayer instanceof EntityPlayerMP) {
            EntityLivingBase target = (EntityLivingBase) event.target;
            EntityPlayerMP player = (EntityPlayerMP) event.entityPlayer;

            MarkContainer container = MarkContainer.get(target);
            if (container != null && !container.isEmpty()) {
                sendSnapshot(player, target, container);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            syncSelfMarks((EntityPlayerMP) event.player);
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            syncSelfMarks((EntityPlayerMP) event.player);
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            syncSelfMarks((EntityPlayerMP) event.player);
        }
    }

    private void syncSelfMarks(EntityPlayerMP player) {
        MarkContainer container = MarkContainer.get(player);
        if (container != null && !container.isEmpty()) {
            sendSnapshot(player, player, container);
        }
    }

    private void sendSnapshot(EntityPlayerMP player, EntityLivingBase target, MarkContainer container) {
        List<MarkSyncMessage.MarkData> list = new ArrayList<>();
        for (MarkInstance instance : container.getAllMarks()) {
            int maxStacks = 99;
            com.greyhat.dark_grey.mark.api.IMarkType type = MarkRegistry.get(instance.getMarkId());
            if (type != null) {
                maxStacks = type.getMaxStacks();
            }
            list.add(
                new MarkSyncMessage.MarkData(
                    instance.getMarkId(),
                    instance.getStacks(),
                    maxStacks,
                    instance.isDecaying(),
                    instance.getStacks() >= maxStacks,
                    instance.getStableUntilWorldTime(),
                    instance.getNextPeriodicTriggerWorldTime(),
                    instance.getNextDecayTriggerWorldTime()));
        }
        if (!list.isEmpty()) {
            DarkGrey.NETWORK.sendTo(new MarkSnapshotMessage(target.getEntityId(), list), player);
        }
    }
}
