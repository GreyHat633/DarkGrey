import os

path = 'src/main/java/com/greyhat/dark_grey/DarkGrey.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('        ComponentRegistry\n            .register("陨星", (Supplier<IRPGComponent>) com.greyhat.dark_grey.component.ComponentMeteor::new);\n', '')
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

path = 'src/main/java/com/greyhat/dark_grey/common/CommonProxy.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('        com.greyhat.dark_grey.event.MeteorFlightTracker tracker = new com.greyhat.dark_grey.event.MeteorFlightTracker();\n        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(tracker);\n        cpw.mods.fml.common.FMLCommonHandler.instance()\n            .bus()\n            .register(tracker);\n', '')
content = content.replace('        DarkGrey.NETWORK.registerMessage(\n            com.greyhat.dark_grey.network.MeteorExplosionMessage.Handler.class,\n            com.greyhat.dark_grey.network.MeteorExplosionMessage.class,\n            15,\n            cpw.mods.fml.relauncher.Side.CLIENT);\n', '')
content = content.replace('    public void scheduleMeteorExplosion(final com.greyhat.dark_grey.network.MeteorExplosionMessage message) {}\n', '')
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

path = 'src/main/java/com/greyhat/dark_grey/common/ClientProxy.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('        net.minecraftforge.common.MinecraftForge.EVENT_BUS\n            .register(new com.greyhat.dark_grey.client.render.MeteorRenderHandler());\n', '')
content = content.replace('        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(\n            com.greyhat.dark_grey.entity.EntityMeteorFireTrail.class,\n            new com.greyhat.dark_grey.client.render.RenderInvisible());\n', '')

search = '''    @Override
    public void scheduleMeteorExplosion(final com.greyhat.dark_grey.network.MeteorExplosionMessage message) {
        runOnClientThread(new Runnable() {

            @Override
            public void run() {
                net.minecraft.world.World world = net.minecraft.client.Minecraft.getMinecraft().theWorld;
                if (world != null) {
                    double r = message.radius;
                    for (int i = 0; i < 200; i++) {
                        double px = message.x + (world.rand.nextDouble() - 0.5) * r * 2.0;
                        double py = message.y + (world.rand.nextDouble() - 0.5) * r * 2.0;
                        double pz = message.z + (world.rand.nextDouble() - 0.5) * r * 2.0;
                        double distSq = (px - message.x) * (px - message.x) + (py - message.y) * (py - message.y)
                            + (pz - message.z) * (pz - message.z);
                        if (distSq <= r * r) {
                            world.spawnParticle("largeexplode", px, py, pz, 0, 0, 0);
                            world.spawnParticle(
                                "flame",
                                px,
                                py,
                                pz,
                                (world.rand.nextDouble() - 0.5) * 0.5,
                                (world.rand.nextDouble() - 0.5) * 0.5,
                                (world.rand.nextDouble() - 0.5) * 0.5);
                            if (world.rand.nextInt(3) == 0) {
                                world.spawnParticle("lava", px, py, pz, 0, 0, 0);
                            }
                        }
                    }
                    world.spawnParticle("hugeexplosion", message.x, message.y, message.z, 0, 0, 0);
                }
            }
        });
    }

'''
content = content.replace(search, '')

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Done")
