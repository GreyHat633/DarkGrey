package com.greyhat.dark_grey.common;

import com.greyhat.dark_grey.DarkGrey;
import com.greyhat.dark_grey.Tags;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import net.minecraft.item.Item;

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
    public void registerRenderers() {}
    public void registerBowRenderer(Item item) {}
    public void registerScytheRenderer(Item item, String equippedTextureName) {}
}
