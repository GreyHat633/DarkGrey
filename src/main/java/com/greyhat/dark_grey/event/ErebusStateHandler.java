package com.greyhat.dark_grey.event;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;

public class ErebusStateHandler {

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != Phase.END || event.player.worldObj.isRemote) {
            return;
        }

        EntityPlayer player = event.player;
        NBTTagCompound entityData = player.getEntityData();

        if (!entityData.getBoolean("DarkGreyErebusRangeActive")) {
            return;
        }

        long now = player.worldObj.getTotalWorldTime();
        long resetAt = entityData.getLong("DarkGreyErebusResetAt");

        if (now >= resetAt) {
            entityData.setInteger("DarkGreyErebusNextRadius", 3);
            entityData.setBoolean("DarkGreyErebusRangeActive", false);
            entityData.setLong("DarkGreyErebusResetAt", 0);

            player.addChatMessage(new ChatComponentTranslation("message.erebus.reset", 3));
        }
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.entityLiving instanceof EntityPlayer && !event.entityLiving.worldObj.isRemote) {
            EntityPlayer player = (EntityPlayer) event.entityLiving;
            resetErebusState(player, false);
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerLoggedOutEvent event) {
        if (!event.player.worldObj.isRemote) {
            resetErebusState(event.player, false);
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
        if (!event.player.worldObj.isRemote) {
            resetErebusState(event.player, false);
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!event.player.worldObj.isRemote) {
            resetErebusState(event.player, false);
        }
    }

    private void resetErebusState(EntityPlayer player, boolean sendMsg) {
        NBTTagCompound entityData = player.getEntityData();
        entityData.setInteger("DarkGreyErebusNextRadius", 3);
        entityData.setBoolean("DarkGreyErebusRangeActive", false);
        entityData.setLong("DarkGreyErebusResetAt", 0);
        entityData.setLong("DarkGreyErebusCooldownEnd", 0);

        if (sendMsg) {
            player.addChatMessage(new ChatComponentTranslation("message.erebus.reset", 3));
        }
    }
}
