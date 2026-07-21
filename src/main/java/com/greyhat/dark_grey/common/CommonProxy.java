package com.greyhat.dark_grey.common;

import net.minecraft.item.Item;

import com.greyhat.dark_grey.DarkGrey;
import com.greyhat.dark_grey.Tags;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());
        DarkGrey.LOG.info(Config.greeting);
        DarkGrey.LOG.info("DarkGrey mod loaded, version " + Tags.VERSION);
    }

    public void init(FMLInitializationEvent event) {}

    public void postInit(FMLPostInitializationEvent event) {}

    public void serverStarting(FMLServerStartingEvent event) {}

    public void registerItemRenderer(Item item, String equippedTextureName) {}

    public void registerLanceRenderer(Item item, String equippedTextureName) {}

    public void registerAnimatedItemRenderer(Item item, String equippedTextureName, int frames, int frameTimeMs) {}

    public void registerRenderers() {}

    public void registerBowRenderer(Item item) {}

    public void registerScytheRenderer(Item item, String equippedTextureName) {}

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
            com.greyhat.dark_grey.network.C2SToggleItanisMode.Handler.class,
            com.greyhat.dark_grey.network.C2SToggleItanisMode.class,
            2,
            cpw.mods.fml.relauncher.Side.SERVER);
    }

    public void scheduleConfigApply(String json) {}

    public void scheduleSolarFlareImpact(double motionX, double motionY, double motionZ) {}
}
