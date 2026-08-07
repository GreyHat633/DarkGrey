import sys

# 1. Update DarkGrey.java
dg_path = 'src/main/java/com/greyhat/dark_grey/DarkGrey.java'
with open(dg_path, 'r', encoding='utf-8') as f:
    dg_content = f.read()

if 'ComponentMeteor' not in dg_content:
    search_comp = 'ComponentRegistry.register("耀斑", (Supplier<IRPGComponent>) ComponentSolarFlare::new);'
    replace_comp = search_comp + '\n        ComponentRegistry.register("陨星", (Supplier<IRPGComponent>) com.greyhat.dark_grey.component.ComponentMeteor::new);'
    dg_content = dg_content.replace(search_comp, replace_comp)

if 'EntityMeteorFireTrail' not in dg_content:
    search_ent = 'EntityRegistry.registerModEntity(\n            (Class) com.greyhat.dark_grey.entity.EntityHeliosBurningField.class,\n            "helios_burning_field",\n            24,\n            (Object) DarkGrey.instance,\n            64,\n            10,\n            false);'
    replace_ent = search_ent + '\n        cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(\n            (Class) com.greyhat.dark_grey.entity.EntityMeteorFireTrail.class,\n            "meteor_fire_trail",\n            25,\n            (Object) DarkGrey.instance,\n            64,\n            10,\n            false);'
    dg_content = dg_content.replace(search_ent, replace_ent)

with open(dg_path, 'w', encoding='utf-8') as f:
    f.write(dg_content)

# 2. Update CommonProxy.java
cp_path = 'src/main/java/com/greyhat/dark_grey/common/CommonProxy.java'
with open(cp_path, 'r', encoding='utf-8') as f:
    cp_content = f.read()

if 'MeteorFlightTracker' not in cp_content:
    search_pre = 'MinecraftForge.EVENT_BUS.register((Object) new BoneCrusherCombatHandler());'
    replace_pre = search_pre + '\n        com.greyhat.dark_grey.event.MeteorFlightTracker tracker = new com.greyhat.dark_grey.event.MeteorFlightTracker();\n        MinecraftForge.EVENT_BUS.register((Object) tracker);\n        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(tracker);'
    cp_content = cp_content.replace(search_pre, replace_pre)

if 'MeteorExplosionMessage' not in cp_content:
    search_net = 'DarkGrey.NETWORK.registerMessage('
    replace_net = 'DarkGrey.NETWORK.registerMessage(\n            com.greyhat.dark_grey.network.MeteorExplosionMessage.class,\n            com.greyhat.dark_grey.network.MeteorExplosionMessage.class,\n            15,\n            cpw.mods.fml.relauncher.Side.CLIENT);\n        ' + search_net
    cp_content = cp_content.replace(search_net, replace_net, 1)

with open(cp_path, 'w', encoding='utf-8') as f:
    f.write(cp_content)

# 3. Update ClientProxy.java
client_path = 'src/main/java/com/greyhat/dark_grey/common/ClientProxy.java'
with open(client_path, 'r', encoding='utf-8') as f:
    client_content = f.read()

if 'MeteorRenderHandler' not in client_content:
    search_init = 'net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);'
    replace_init = search_init + '\n        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new com.greyhat.dark_grey.client.render.MeteorRenderHandler());'
    client_content = client_content.replace(search_init, replace_init)

with open(client_path, 'w', encoding='utf-8') as f:
    f.write(client_content)

print("Registries injected successfully.")
