package com.greyhat.dark_grey.common;

import net.minecraft.item.Item;

public class ClientProxy extends CommonProxy {

    @Override
    public void registerRenderers() {
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(com.greyhat.dark_grey.entity.EntityMadokaArrow.class, new com.greyhat.dark_grey.client.render.RenderMadokaArrow());
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(com.greyhat.dark_grey.entity.EntityMadokaRing.class, new com.greyhat.dark_grey.client.render.RenderMadokaRing());
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(com.greyhat.dark_grey.entity.EntityScythe.class, new com.greyhat.dark_grey.client.render.RenderScythe());
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(com.greyhat.dark_grey.entity.EntityAuraTorrent.class, new com.greyhat.dark_grey.client.render.RenderInvisible());
    }
    
    @Override
    public void registerItemRenderer(Item item, String equippedTextureName) {
        net.minecraftforge.client.MinecraftForgeClient.registerItemRenderer(item, new com.greyhat.dark_grey.client.render.RPGItemRenderer(equippedTextureName));
    }
    
    public void registerBowRenderer(Item item) {
        net.minecraftforge.client.MinecraftForgeClient.registerItemRenderer(item, new com.greyhat.dark_grey.client.render.RenderRPGBow());
    }
    
    public void registerScytheRenderer(Item item, String equippedTextureName) {
        // Scythes use standard RPGItemRenderer in first person/third person, and EntityScythe for the spinning effect.
        net.minecraftforge.client.MinecraftForgeClient.registerItemRenderer(item, new com.greyhat.dark_grey.client.render.RPGItemRenderer(equippedTextureName));
    }
}