import sys

file_path = 'src/main/java/com/greyhat/dark_grey/DarkGrey.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

comp_str = '        com.greyhat.dark_grey.api.ComponentRegistry\n            .register("狼毒M271", () -> new com.greyhat.dark_grey.component.ComponentWolfsbaneM271());'
if 'ComponentHeliosBlastGun' not in content:
    content = content.replace(comp_str, '        ComponentRegistry.register("赫利俄斯爆破枪", (java.util.function.Supplier<com.greyhat.dark_grey.api.IRPGComponent>) com.greyhat.dark_grey.component.ComponentHeliosBlastGun::new);\n' + comp_str)

entity_str = '        cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(\n            (Class) com.greyhat.dark_grey.entity.EntitySunflameBullet.class,\n            "sunflame_bullet",\n            17,\n            (Object) DarkGrey.instance,\n            64,\n            10,\n            true);'
if 'EntityHeliosBurningField' not in content:
    content = content.replace(entity_str, entity_str + '\n        cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(\n            (Class) com.greyhat.dark_grey.entity.EntityHeliosBurningField.class,\n            "helios_burning_field",\n            24,\n            (Object) DarkGrey.instance,\n            64,\n            10,\n            false);')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Updated DarkGrey.java via python")
