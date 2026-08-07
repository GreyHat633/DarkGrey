import os

path = 'src/main/java/com/greyhat/dark_grey/common/ClientProxy.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

search = 'net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new com.greyhat.dark_grey.client.render.MeteorRenderHandler());'
replace = search + '\n        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(com.greyhat.dark_grey.entity.EntityMeteorFireTrail.class, new com.greyhat.dark_grey.client.render.RenderInvisible());'

if search in content:
    content = content.replace(search, replace)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print("Registered RenderInvisible for EntityMeteorFireTrail.")
else:
    print("Could not find pattern in ClientProxy.")
