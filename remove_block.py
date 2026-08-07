import os

path = 'src/main/java/com/greyhat/dark_grey/common/ClientProxy.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

search = '''    @Override
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
print("Removed broken block from ClientProxy.java")
