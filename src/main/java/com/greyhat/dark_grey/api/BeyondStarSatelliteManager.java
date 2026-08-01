package com.greyhat.dark_grey.api;

import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.Clone;

import com.greyhat.dark_grey.DarkGrey;
import com.greyhat.dark_grey.network.BeyondStarSatelliteSyncMessage;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;

public class BeyondStarSatelliteManager implements IExtendedEntityProperties {

    public static final String PROP_NAME = "BeyondStarSatellites";
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("1f2a3c4b-5d6e-7f8a-9b0c-1d2e3f4a5b6c");
    private static final AttributeModifier SPEED_MODIFIER = new AttributeModifier(
        SPEED_MODIFIER_UUID,
        "BeyondStarSatelliteSpeed",
        0.20,
        2).setSaved(false);

    private final EntityPlayer player;
    private int satellites = 0;

    public BeyondStarSatelliteManager(EntityPlayer player) {
        this.player = player;
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new EventHandler());
        FMLCommonHandler.instance()
            .bus()
            .register(new EventHandler());
    }

    public static BeyondStarSatelliteManager get(EntityPlayer player) {
        return (BeyondStarSatelliteManager) player.getExtendedProperties(PROP_NAME);
    }

    public static int getCount(EntityPlayer player) {
        BeyondStarSatelliteManager mgr = get(player);
        return mgr != null ? mgr.satellites : 0;
    }

    public static void addSatellites(EntityPlayer player, int amount) {
        BeyondStarSatelliteManager mgr = get(player);
        if (mgr != null) {
            mgr.setSatellites(Math.min(8, mgr.satellites + amount));
        }
    }

    public static void consumeAll(EntityPlayer player) {
        BeyondStarSatelliteManager mgr = get(player);
        if (mgr != null) {
            mgr.setSatellites(0);
        }
    }

    public void setSatellites(int count) {
        if (this.satellites != count) {
            this.satellites = count;
            updateSpeedModifier();
            sync();
        }
    }

    private void updateSpeedModifier() {
        IAttributeInstance attr = player.getEntityAttribute(SharedMonsterAttributes.movementSpeed);
        if (attr != null) {
            attr.removeModifier(SPEED_MODIFIER);
            if (this.satellites > 0) {
                attr.applyModifier(SPEED_MODIFIER);
            }
        }
    }

    public void sync() {
        if (player instanceof EntityPlayerMP) {
            DarkGrey.NETWORK.sendTo(new BeyondStarSatelliteSyncMessage(this.satellites), (EntityPlayerMP) player);
        }
    }

    @Override
    public void saveNBTData(NBTTagCompound compound) {
        compound.setInteger("Satellites", this.satellites);
    }

    @Override
    public void loadNBTData(NBTTagCompound compound) {
        this.satellites = compound.getInteger("Satellites");
        updateSpeedModifier();
    }

    @Override
    public void init(Entity entity, World world) {}

    public static class EventHandler {

        @SubscribeEvent
        public void onEntityConstructing(EntityEvent.EntityConstructing event) {
            if (event.entity instanceof EntityPlayer && event.entity.getExtendedProperties(PROP_NAME) == null) {
                event.entity
                    .registerExtendedProperties(PROP_NAME, new BeyondStarSatelliteManager((EntityPlayer) event.entity));
            }
        }

        @SubscribeEvent
        public void onPlayerClone(Clone event) {
            if (!event.wasDeath) {
                BeyondStarSatelliteManager oldMgr = get(event.original);
                BeyondStarSatelliteManager newMgr = get(event.entityPlayer);
                if (oldMgr != null && newMgr != null) {
                    newMgr.setSatellites(oldMgr.satellites);
                }
            } else {
                BeyondStarSatelliteManager newMgr = get(event.entityPlayer);
                if (newMgr != null) {
                    newMgr.setSatellites(0);
                }
            }
        }

        @SubscribeEvent
        public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            BeyondStarSatelliteManager mgr = get(event.player);
            if (mgr != null) {
                mgr.sync();
                mgr.updateSpeedModifier();
            }
        }

        @SubscribeEvent
        public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
            BeyondStarSatelliteManager mgr = get(event.player);
            if (mgr != null) {
                mgr.sync();
                mgr.updateSpeedModifier();
            }
        }

        @SubscribeEvent
        public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
            BeyondStarSatelliteManager mgr = get(event.player);
            if (mgr != null) {
                mgr.sync();
                mgr.updateSpeedModifier();
            }
        }
    }
}
