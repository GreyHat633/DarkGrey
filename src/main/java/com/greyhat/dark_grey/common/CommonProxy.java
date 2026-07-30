package com.greyhat.dark_grey.common;

import net.minecraft.item.Item;
import net.minecraftforge.common.MinecraftForge;

import com.greyhat.dark_grey.DarkGrey;
import com.greyhat.dark_grey.Tags;
import com.greyhat.dark_grey.event.BoneCrusherCombatHandler;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());
        DarkGrey.LOG.info(Config.greeting);
        DarkGrey.LOG.info("DarkGrey mod loaded, version " + Tags.VERSION);
        MinecraftForge.EVENT_BUS.register((Object) new BoneCrusherCombatHandler());
    }

    public void init(FMLInitializationEvent event) {}

    public void postInit(FMLPostInitializationEvent event) {}

    public void serverStarting(FMLServerStartingEvent event) {}

    public void registerItemRenderer(Item item, String equippedTextureName) {}

    public void registerScaledItemRenderer(Item item, String equippedTextureName, float scale) {}

    public void registerLanceRenderer(Item item, String equippedTextureName) {}

    public void registerAnimatedItemRenderer(Item item, String equippedTextureName, int frames, int frameTimeMs) {}

    public void registerRenderers() {}

    public void registerBowRenderer(Item item) {}

    public void registerScytheRenderer(Item item, String equippedTextureName) {}

    public void registerGunRenderer(Item item, String id, String texture) {}

    public void registerNetworkHandlers() {
        DarkGrey.NETWORK.registerMessage(
            com.greyhat.dark_grey.network.ConfigSyncHandler.class,
            com.greyhat.dark_grey.network.ConfigSyncMessage.class,
            0,
            cpw.mods.fml.relauncher.Side.CLIENT);
        DarkGrey.NETWORK.registerMessage(
            com.greyhat.dark_grey.network.SolarFlareImpactHandler.class,
            com.greyhat.dark_grey.network.SolarFlareImpactMessage.class,
            1,
            cpw.mods.fml.relauncher.Side.CLIENT);
        DarkGrey.NETWORK.registerMessage(
            com.greyhat.dark_grey.network.ItanisModeSwitchHandler.class,
            com.greyhat.dark_grey.network.ItanisModeSwitchMessage.class,
            2,
            cpw.mods.fml.relauncher.Side.SERVER);
        DarkGrey.NETWORK.registerMessage(
            com.greyhat.dark_grey.network.UndergroundSunExplosionHandler.class,
            com.greyhat.dark_grey.network.UndergroundSunExplosionMessage.class,
            3,
            cpw.mods.fml.relauncher.Side.CLIENT);
        DarkGrey.NETWORK.registerMessage(
            com.greyhat.dark_grey.network.MarkSyncHandler.class,
            com.greyhat.dark_grey.network.MarkSyncMessage.class,
            4,
            cpw.mods.fml.relauncher.Side.CLIENT);
        DarkGrey.NETWORK.registerMessage(
            com.greyhat.dark_grey.network.MarkRemoveHandler.class,
            com.greyhat.dark_grey.network.MarkRemoveMessage.class,
            5,
            cpw.mods.fml.relauncher.Side.CLIENT);
        DarkGrey.NETWORK.registerMessage(
            com.greyhat.dark_grey.network.MarkSnapshotHandler.class,
            com.greyhat.dark_grey.network.MarkSnapshotMessage.class,
            6,
            cpw.mods.fml.relauncher.Side.CLIENT);
        DarkGrey.NETWORK.registerMessage(
            com.greyhat.dark_grey.network.MarkClearEntityHandler.class,
            com.greyhat.dark_grey.network.MarkClearEntityMessage.class,
            7,
            cpw.mods.fml.relauncher.Side.CLIENT);
        DarkGrey.NETWORK.registerMessage(
            com.greyhat.dark_grey.network.ErebusHitHandler.class,
            com.greyhat.dark_grey.network.ErebusHitMessage.class,
            8,
            cpw.mods.fml.relauncher.Side.CLIENT);
        DarkGrey.NETWORK.registerMessage(
            com.greyhat.dark_grey.network.ShatteredBoneParticlesHandler.class,
            com.greyhat.dark_grey.network.ShatteredBoneParticlesMessage.class,
            9,
            cpw.mods.fml.relauncher.Side.CLIENT);
        DarkGrey.NETWORK.registerMessage(
            com.greyhat.dark_grey.network.ShatteredBoneStaffCastStartHandler.class,
            com.greyhat.dark_grey.network.ShatteredBoneStaffCastStartMessage.class,
            10,
            cpw.mods.fml.relauncher.Side.CLIENT);
        DarkGrey.NETWORK.registerMessage(
            com.greyhat.dark_grey.network.ShatteredBoneStaffCastEndHandler.class,
            com.greyhat.dark_grey.network.ShatteredBoneStaffCastEndMessage.class,
            11,
            cpw.mods.fml.relauncher.Side.CLIENT);
    }

    public void scheduleConfigApply(String json) {}

    public void scheduleSolarFlareImpact(double motionX, double motionY, double motionZ) {}

    public void scheduleMarkSync(com.greyhat.dark_grey.network.MarkSyncMessage message) {}

    public void scheduleMarkRemove(com.greyhat.dark_grey.network.MarkRemoveMessage message) {}

    public void scheduleMarkSnapshot(com.greyhat.dark_grey.network.MarkSnapshotMessage message) {}

    public void scheduleMarkClearEntity(com.greyhat.dark_grey.network.MarkClearEntityMessage message) {}

    public void scheduleErebusHit(com.greyhat.dark_grey.network.ErebusHitMessage message) {}

    public void scheduleUndergroundSunExplosion(com.greyhat.dark_grey.network.UndergroundSunExplosionMessage message) {}

    public void scheduleShatteredBoneParticles(com.greyhat.dark_grey.network.ShatteredBoneParticlesMessage message) {}

    public void scheduleShatteredBoneCastStart(
        com.greyhat.dark_grey.network.ShatteredBoneStaffCastStartMessage message) {}

    public void scheduleShatteredBoneCastEnd(com.greyhat.dark_grey.network.ShatteredBoneStaffCastEndMessage message) {}
}
