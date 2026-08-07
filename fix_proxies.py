import os
import re

# 1. Add spawnParticle to CommonProxy
path = 'src/main/java/com/greyhat/dark_grey/common/CommonProxy.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()
if 'public void spawnParticle' not in content:
    content = content.replace('}\n', '''
    public void spawnParticle(net.minecraft.world.World world, String particleName, double x, double y, double z, double velX, double velY, double velZ) {
    }

    public void spawnParticle(net.minecraft.world.World world, String particleName, double x, double y, double z, int count, double radius, int speed) {
    }
}
''', 1)
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

# 2. Add spawnParticle to ClientProxy
path = 'src/main/java/com/greyhat/dark_grey/common/ClientProxy.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()
if 'public void spawnParticle' not in content:
    content = content.replace('}\n', '''
    @Override
    public void spawnParticle(net.minecraft.world.World world, String particleName, double x, double y, double z, double velX, double velY, double velZ) {
        if (world != null) {
            world.spawnParticle(particleName, x, y, z, velX, velY, velZ);
        }
    }

    @Override
    public void spawnParticle(net.minecraft.world.World world, String particleName, double x, double y, double z, int count, double radius, int speed) {
        if (world != null) {
            for(int i=0; i<count; i++) {
                double rx = x + (world.rand.nextDouble() - 0.5) * radius * 2;
                double ry = y + (world.rand.nextDouble() - 0.5) * radius * 2;
                double rz = z + (world.rand.nextDouble() - 0.5) * radius * 2;
                world.spawnParticle(particleName, rx, ry, rz, 0, 0, 0);
            }
        }
    }
}
''', 1)
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

# 3. Clean up meteor stuff from DarkGrey.java
path = 'src/main/java/com/greyhat/dark_grey/DarkGrey.java'
with open(path, 'r', encoding='utf-8') as f:
    lines = f.readlines()
with open(path, 'w', encoding='utf-8') as f:
    skip = False
    for line in lines:
        if 'ComponentMeteor::new' in line:
            continue
        if skip:
            skip = False
            continue
        if 'ComponentRegistry' in line and 'ComponentMeteor::new' in ''.join(lines):
            # actually we don't know if the NEXT line is meteor, let's just do it cleanly
            pass
        f.write(line)

# 4. Clean up meteor from CommonProxy
path = 'src/main/java/com/greyhat/dark_grey/common/CommonProxy.java'
with open(path, 'r', encoding='utf-8') as f:
    lines = f.readlines()
with open(path, 'w', encoding='utf-8') as f:
    skip = False
    for i, line in enumerate(lines):
        if 'MeteorFlightTracker tracker' in line:
            skip = 2
            continue
        if skip > 0:
            skip -= 1
            continue
        if 'MeteorExplosionMessage' in line:
            continue
        if 'DarkGrey.NETWORK.registerMessage(' in line and i+1<len(lines) and 'MeteorExplosionMessage' in lines[i+1]:
            skip = 4
            continue
        if 'public void scheduleMeteorExplosion' in line:
            skip = 1
            continue
        f.write(line)

# 5. Clean up meteor from ClientProxy
path = 'src/main/java/com/greyhat/dark_grey/common/ClientProxy.java'
with open(path, 'r', encoding='utf-8') as f:
    lines = f.readlines()
with open(path, 'w', encoding='utf-8') as f:
    skip = False
    for i, line in enumerate(lines):
        if 'MeteorRenderHandler' in line:
            continue
        if 'net.minecraftforge.common.MinecraftForge.EVENT_BUS' in line and i+1<len(lines) and 'MeteorRenderHandler' in lines[i+1]:
            skip = 1
            continue
        if skip:
            skip = False
            continue
        if 'EntityMeteorFireTrail.class' in line:
            continue
        if 'cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(' in line and i+1<len(lines) and 'EntityMeteorFireTrail' in lines[i+1]:
            skip = 2
            continue
        f.write(line)

print("Applied fixes")
