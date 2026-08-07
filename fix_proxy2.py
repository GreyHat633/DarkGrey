import os

path = 'src/main/java/com/greyhat/dark_grey/common/ClientProxy.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

search = 'public void scheduleErebusHit(final com.greyhat.dark_grey.network.ErebusHitMessage message) {'
replace = '''@Override
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
                        double distSq = (px - message.x)*(px - message.x) + (py - message.y)*(py - message.y) + (pz - message.z)*(pz - message.z);
                        if (distSq <= r * r) {
                            world.spawnParticle("largeexplode", px, py, pz, 0, 0, 0);
                            world.spawnParticle("flame", px, py, pz, (world.rand.nextDouble()-0.5)*0.5, (world.rand.nextDouble()-0.5)*0.5, (world.rand.nextDouble()-0.5)*0.5);
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
    
    @Override
    ''' + search

if search in content:
    content = content.replace(search, replace)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
