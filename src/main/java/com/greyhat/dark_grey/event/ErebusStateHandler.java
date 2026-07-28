package com.greyhat.dark_grey.event;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import com.greyhat.dark_grey.api.CooldownHelper;
import com.greyhat.dark_grey.component.ComponentErebus;

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

        long maximumResetMillis = CooldownHelper.ticksToMillis(72000L);
        if (CooldownHelper.isReady(
            entityData,
            ComponentErebus.RESET_AT_MILLIS_KEY,
            maximumResetMillis,
            ComponentErebus.LEGACY_RESET_AT_KEY)) {
            entityData.setInteger("DarkGreyErebusNextRadius", 3);
            entityData.setBoolean("DarkGreyErebusRangeActive", false);
            CooldownHelper.clear(entityData, ComponentErebus.RESET_AT_MILLIS_KEY, ComponentErebus.LEGACY_RESET_AT_KEY);

            player.addChatMessage(new ChatComponentTranslation("message.erebus.reset", 3));
        }
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.entityLiving instanceof EntityPlayer && !event.entityLiving.worldObj.isRemote) {
            EntityPlayer player = (EntityPlayer) event.entityLiving;
            resetErebusState(player, false, true);
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerLoggedOutEvent event) {
        if (!event.player.worldObj.isRemote) {
            resetErebusState(event.player, false, true);
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
        if (!event.player.worldObj.isRemote) {
            resetErebusState(event.player, false, false);
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!event.player.worldObj.isRemote) {
            resetErebusState(event.player, false, true);
        }
    }

    private void resetErebusState(EntityPlayer player, boolean sendMsg, boolean clearCooldown) {
        NBTTagCompound entityData = player.getEntityData();
        entityData.setInteger("DarkGreyErebusNextRadius", 3);
        entityData.setBoolean("DarkGreyErebusRangeActive", false);
        CooldownHelper.clear(entityData, ComponentErebus.RESET_AT_MILLIS_KEY, ComponentErebus.LEGACY_RESET_AT_KEY);
        if (clearCooldown) {
            CooldownHelper
                .clear(entityData, ComponentErebus.COOLDOWN_END_MILLIS_KEY, ComponentErebus.LEGACY_COOLDOWN_END_KEY);
        }

        if (sendMsg) {
            player.addChatMessage(new ChatComponentTranslation("message.erebus.reset", 3));
        }
    }
}
