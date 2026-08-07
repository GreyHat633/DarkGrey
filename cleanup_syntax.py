import os

# ClientProxy
path = 'src/main/java/com/greyhat/dark_grey/common/ClientProxy.java'
with open(path, 'r', encoding='utf-8') as f:
    lines = f.readlines()
with open(path, 'w', encoding='utf-8') as f:
    for line in lines:
        if 'cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(' in line:
            continue
        if 'new com.greyhat.dark_grey.client.render.RenderInvisible());' in line:
            continue
        f.write(line)

# CommonProxy
path = 'src/main/java/com/greyhat/dark_grey/common/CommonProxy.java'
with open(path, 'r', encoding='utf-8') as f:
    lines = f.readlines()
with open(path, 'w', encoding='utf-8') as f:
    skip = False
    for line in lines:
        if 'MeteorFlightTracker tracker = new' in line:
            continue
        if 'net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(tracker);' in line:
            continue
        if 'cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(tracker);' in line:
            continue
        if 'DarkGrey.NETWORK.registerMessage(' in line and 'cpw.mods.fml.relauncher.Side.CLIENT);' in line:
            continue
        f.write(line)

print("Cleaned up remaining meteor syntax errors.")
