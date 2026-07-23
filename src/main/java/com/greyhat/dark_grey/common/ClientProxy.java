package com.greyhat.dark_grey.common;

import net.minecraft.item.Item;

public class ClientProxy extends CommonProxy {

    @Override
    public void init(cpw.mods.fml.common.event.FMLInitializationEvent event) {
        super.init(event);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
    }

    @cpw.mods.fml.common.eventhandler.SubscribeEvent
    public void onMouseEvent(net.minecraftforge.client.event.MouseEvent event) {
        if (event.button == 0 && event.buttonstate) { // Left click pressed
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
            if (mc.thePlayer != null && mc.currentScreen == null) {
                net.minecraft.item.ItemStack held = mc.thePlayer.getCurrentEquippedItem();
                if (held != null && held.getItem() instanceof com.greyhat.dark_grey.api.IRPGItemContainer) {
                    com.greyhat.dark_grey.api.IRPGItemContainer container = (com.greyhat.dark_grey.api.IRPGItemContainer) held
                        .getItem();
                    if ("itanis".equals(container.getRpgItemId())) {
                        com.greyhat.dark_grey.DarkGrey.NETWORK
                            .sendToServer(new com.greyhat.dark_grey.network.ItanisModeSwitchMessage());
                        event.setCanceled(true);
                    }
                }
            }
        }
    }

    @Override
    public void scheduleSolarFlareImpact(final double motionX, final double motionY, final double motionZ) {
        net.minecraft.client.Minecraft.getMinecraft()
            .func_152344_a(new Runnable() {

                @Override
                public void run() {
                    net.minecraft.entity.player.EntityPlayer player = net.minecraft.client.Minecraft
                        .getMinecraft().thePlayer;
                    if (player == null) {
                        return;
                    }
                    player.getEntityData()
                        .setBoolean("SolarDashHasHit", true);
                    player.motionX = motionX;
                    player.motionY = motionY;
                    player.motionZ = motionZ;
                    player.stepHeight = 0.5F;
                }
            });
    }

    @Override
    public void scheduleConfigApply(final String json) {
        net.minecraft.client.Minecraft.getMinecraft()
            .func_152344_a(new Runnable() {

                @Override
                public void run() {
                    com.greyhat.dark_grey.api.RPGItemDataManager.getInstance()
                        .applyRemoteConfig(json);
                }
            });
    }

    @Override
    public void registerRenderers() {
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
            com.greyhat.dark_grey.entity.EntityMadokaArrow.class,
            new com.greyhat.dark_grey.client.render.RenderMadokaArrow());
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
            com.greyhat.dark_grey.entity.EntityMadokaRing.class,
            new com.greyhat.dark_grey.client.render.RenderMadokaRing());
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
            com.greyhat.dark_grey.entity.EntityScythe.class,
            new com.greyhat.dark_grey.client.render.RenderScythe());
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
            com.greyhat.dark_grey.entity.EntityAuraTorrent.class,
            new com.greyhat.dark_grey.client.render.RenderInvisible());
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
            com.greyhat.dark_grey.entity.EntityPhantomStrike.class,
            new com.greyhat.dark_grey.client.render.RenderPhantomStrike());
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
            com.greyhat.dark_grey.entity.EntityItanisArrow.class,
            new com.greyhat.dark_grey.client.render.RenderItanisArrow());
    }

    @Override
    public void registerItemRenderer(Item item, String equippedTextureName) {
        net.minecraftforge.client.MinecraftForgeClient
            .registerItemRenderer(item, new com.greyhat.dark_grey.client.render.RPGItemRenderer(equippedTextureName));
    }

    public void registerBowRenderer(Item item) {
        net.minecraftforge.client.MinecraftForgeClient
            .registerItemRenderer(item, new com.greyhat.dark_grey.client.render.RenderRPGBow());
    }

    public void registerScytheRenderer(Item item, String equippedTextureName) {
        // Scythes use custom RenderScytheWeapon in first person/third person for scaled sword grip.
        net.minecraftforge.client.MinecraftForgeClient.registerItemRenderer(
            item,
            new com.greyhat.dark_grey.client.render.RenderScytheWeapon(equippedTextureName));
    }

    public void registerLanceRenderer(Item item, String equippedTextureName) {
        net.minecraftforge.client.MinecraftForgeClient
            .registerItemRenderer(item, new com.greyhat.dark_grey.client.render.RenderRPGLance(equippedTextureName));
    }

    public void registerAnimatedItemRenderer(Item item, String equippedTextureName, int frames, int frameTimeMs) {
        net.minecraftforge.client.MinecraftForgeClient.registerItemRenderer(
            item,
            new com.greyhat.dark_grey.client.render.AnimatedRPGItemRenderer(equippedTextureName, frames, frameTimeMs));
    }
}
