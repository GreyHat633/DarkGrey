package com.greyhat.dark_grey.common;

import net.minecraft.item.Item;

public class ClientProxy extends CommonProxy {

    @Override
    public void init(cpw.mods.fml.common.event.FMLInitializationEvent event) {
        super.init(event);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);

        com.greyhat.dark_grey.mark.client.ClientMarkCache cache = new com.greyhat.dark_grey.mark.client.ClientMarkCache();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(cache);
        cpw.mods.fml.common.FMLCommonHandler.instance()
            .bus()
            .register(cache);

        net.minecraftforge.common.MinecraftForge.EVENT_BUS
            .register(new com.greyhat.dark_grey.mark.client.render.MarkEntityOverlayRenderer());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS
            .register(new com.greyhat.dark_grey.mark.client.render.MarkTargetPanelRenderer());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS
            .register(new com.greyhat.dark_grey.mark.client.render.MarkSelfHudRenderer());
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
                    if ("itanis".equals(container.getRpgItemId())
                        || "underground_sun".equals(container.getRpgItemId())) {
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
    public void scheduleMarkSync(final com.greyhat.dark_grey.network.MarkSyncMessage message) {
        runOnClientThread(new Runnable() {

            @Override
            public void run() {
                com.greyhat.dark_grey.mark.client.ClientMarkCache.updateMark(
                    message.entityId,
                    message.data,
                    message.changeReason,
                    message.displayedDelta,
                    message.immediateTriggered);
            }
        });
    }

    @Override
    public void scheduleMarkRemove(final com.greyhat.dark_grey.network.MarkRemoveMessage message) {
        runOnClientThread(new Runnable() {

            @Override
            public void run() {
                com.greyhat.dark_grey.mark.client.ClientEntityMarks marks = com.greyhat.dark_grey.mark.client.ClientMarkCache
                    .get(message.entityId);
                if (marks != null) {
                    marks.removeMark(message.markId);
                }
            }
        });
    }

    @Override
    public void scheduleMarkSnapshot(final com.greyhat.dark_grey.network.MarkSnapshotMessage message) {
        runOnClientThread(new Runnable() {

            @Override
            public void run() {
                com.greyhat.dark_grey.mark.client.ClientMarkCache.clear(message.entityId);
                if (message.marks == null) {
                    return;
                }
                for (com.greyhat.dark_grey.network.MarkSyncMessage.MarkData data : message.marks) {
                    com.greyhat.dark_grey.mark.client.ClientMarkCache
                        .updateMark(message.entityId, data, (byte) 0, 0, false);
                }
            }
        });
    }

    @Override
    public void scheduleMarkClearEntity(final com.greyhat.dark_grey.network.MarkClearEntityMessage message) {
        runOnClientThread(new Runnable() {

            @Override
            public void run() {
                com.greyhat.dark_grey.mark.client.ClientMarkCache.clear(message.entityId);
            }
        });
    }

    @Override
    public void scheduleErebusHit(final com.greyhat.dark_grey.network.ErebusHitMessage message) {
        runOnClientThread(new Runnable() {

            @Override
            public void run() {
                net.minecraft.world.World world = net.minecraft.client.Minecraft.getMinecraft().theWorld;
                if (world == null || message.entityIds == null) {
                    return;
                }
                for (int id : message.entityIds) {
                    net.minecraft.entity.Entity entity = world.getEntityByID(id);
                    if (entity != null) {
                        spawnCurseParticles(world, entity.posX, entity.posY + entity.height / 2.0D, entity.posZ);
                    }
                }
            }
        });
    }

    @Override
    public void scheduleUndergroundSunExplosion(
        final com.greyhat.dark_grey.network.UndergroundSunExplosionMessage message) {
        runOnClientThread(new Runnable() {

            @Override
            public void run() {
                spawnUndergroundSunExplosion(message);
            }
        });
    }

    @Override
    public void scheduleShatteredBoneParticles(
        final com.greyhat.dark_grey.network.ShatteredBoneParticlesMessage message) {
        runOnClientThread(new Runnable() {

            @Override
            public void run() {
                net.minecraft.world.World world = net.minecraft.client.Minecraft.getMinecraft().theWorld;
                if (world == null) {
                    return;
                }
                for (int i = 0; i < com.greyhat.dark_grey.network.ShatteredBoneParticlesMessage.PARTICLE_COUNT; i++) {
                    int offset = i * 3;
                    world.spawnParticle(
                        "iconcrack_352",
                        message.x,
                        message.y,
                        message.z,
                        message.velocities[offset],
                        message.velocities[offset + 1],
                        message.velocities[offset + 2]);
                }
            }
        });
    }

    private static void runOnClientThread(Runnable runnable) {
        net.minecraft.client.Minecraft.getMinecraft()
            .func_152344_a(runnable);
    }

    private static void spawnCurseParticles(net.minecraft.world.World world, double x, double y, double z) {
        for (int i = 0; i < 15; i++) {
            double offsetX = world.rand.nextDouble() - 0.5D;
            double offsetY = world.rand.nextDouble() - 0.5D;
            double offsetZ = world.rand.nextDouble() - 0.5D;
            world.spawnParticle(
                "mobSpell",
                x + offsetX,
                y + offsetY,
                z + offsetZ,
                0.5D + world.rand.nextDouble() * 0.4D,
                0.0D,
                0.7D + world.rand.nextDouble() * 0.3D);
            world.spawnParticle("witchMagic", x + offsetX, y + offsetY, z + offsetZ, 0.0D, 0.0D, 0.0D);
        }
        world.spawnParticle("largeexplode", x, y, z, 0.0D, 0.0D, 0.0D);
    }

    private static void spawnUndergroundSunExplosion(
        com.greyhat.dark_grey.network.UndergroundSunExplosionMessage message) {
        net.minecraft.world.World world = net.minecraft.client.Minecraft.getMinecraft().theWorld;
        if (world == null) {
            return;
        }
        double x = message.x;
        double y = message.y;
        double z = message.z;
        float radius = message.radius;

        world.spawnParticle("hugeexplosion", x, y, z, 0.0D, 0.0D, 0.0D);
        for (int i = 0; i < 5; i++) {
            double ox = (world.rand.nextDouble() - 0.5D) * radius * 0.5D;
            double oy = (world.rand.nextDouble() - 0.5D) * radius * 0.5D;
            double oz = (world.rand.nextDouble() - 0.5D) * radius * 0.5D;
            world.spawnParticle("largeexplode", x + ox, y + oy, z + oz, 0.0D, 0.0D, 0.0D);
        }
        for (int i = 0; i < 600; i++) {
            double angle = world.rand.nextDouble() * Math.PI * 2.0D;
            double dist = world.rand.nextDouble() * radius;
            double px = x + Math.cos(angle) * dist;
            double pz = z + Math.sin(angle) * dist;
            double py = y + (world.rand.nextDouble() - 0.5D) * 4.0D;
            double vx = Math.cos(angle) * 1.5D;
            double vz = Math.sin(angle) * 1.5D;
            double vy = (world.rand.nextDouble() - 0.5D) * 1.5D;
            world.spawnParticle("flame", px, py, pz, vx, vy, vz);
            if (i % 3 == 0) {
                world.spawnParticle("crit", px, py, pz, vx * 1.5D, vy * 1.5D, vz * 1.5D);
            }
        }
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
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
            com.greyhat.dark_grey.entity.EntityUndergroundSunOrb.class,
            new com.greyhat.dark_grey.client.render.RenderUndergroundSunOrb());
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
            com.greyhat.dark_grey.entity.EntityRedSunFireball.class,
            new com.greyhat.dark_grey.client.render.RenderRedSunFireball());
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
            com.greyhat.dark_grey.entity.EntityCorruptionBomb.class,
            new com.greyhat.dark_grey.client.render.RenderCorruptionBomb());
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
            com.greyhat.dark_grey.entity.EntityStarBullet.class,
            new com.greyhat.dark_grey.client.render.RenderStarBullet());
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
            com.greyhat.dark_grey.entity.EntityBoneFlask.class,
            new com.greyhat.dark_grey.client.render.RenderBoneFlask());
        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
            com.greyhat.dark_grey.entity.EntityBoneSpikesField.class,
            new com.greyhat.dark_grey.client.render.RenderInvisible());

        // Register Supernova planet renderer
        com.greyhat.dark_grey.client.render.SupernovaPlanetRenderer planetRenderer = new com.greyhat.dark_grey.client.render.SupernovaPlanetRenderer();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(planetRenderer);
        cpw.mods.fml.common.FMLCommonHandler.instance()
            .bus()
            .register(planetRenderer);
    }

    @Override
    public void registerItemRenderer(Item item, String equippedTextureName) {
        net.minecraftforge.client.MinecraftForgeClient
            .registerItemRenderer(item, new com.greyhat.dark_grey.client.render.RPGItemRenderer(equippedTextureName));
    }

    @Override
    public void registerScaledItemRenderer(Item item, String equippedTextureName, float scale) {
        net.minecraftforge.client.MinecraftForgeClient.registerItemRenderer(
            item,
            new com.greyhat.dark_grey.client.render.RPGItemRenderer(equippedTextureName, scale));
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
