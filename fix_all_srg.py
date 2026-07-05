import os

src_dirs = [
    r'E:\Java\MinecraftMod\DarkGrey\src\main\java\com\greyhat\dark_grey\component',
    r'E:\Java\MinecraftMod\DarkGrey\src\main\java\com\greyhat\dark_grey\entity',
    r'E:\Java\MinecraftMod\DarkGrey\src\main\java\com\greyhat\dark_grey\api',
    r'E:\Java\MinecraftMod\DarkGrey\src\main\java\com\greyhat\dark_grey\client\render'
]

replacements = {
    'field_70460_b': 'armorInventory',
    'func_70092_e': 'getDistanceSq',
    'func_76061_a': 'setDamageBypassesArmor',
    'func_76063_c': 'setDamageAllowedInCreativeMode',
    'func_70105_a': 'setSize',
    'func_70071_h_': 'onUpdate',
    'func_70106_y': 'setDead',
    'func_74760_g': 'getFloat',
    'func_74776_a': 'setFloat',
    'field_70128_L': 'isDead',
    'field_70173_aa': 'ticksExisted',
    'field_70121_D': 'boundingBox',
    'func_72314_b': 'expand',
    'func_72839_b': 'getEntitiesWithinAABBExcludingEntity',
    'field_76377_j': 'magic',
    'func_70068_e': 'getDistanceSqToEntity',
    'func_74771_c': 'getByte',
    'func_74774_a': 'setByte',
    'field_70146_Z': 'rand',
    'func_74768_a': 'setInteger',
    'func_74762_e': 'getInteger',
}

count = 0
for src_dir in src_dirs:
    for root, dirs, files in os.walk(src_dir):
        for f in files:
            if f.endswith('.java'):
                path = os.path.join(root, f)
                with open(path, 'r', encoding='utf-8', errors='ignore') as fh:
                    content = fh.read()
                original = content
                for srg, mcp in replacements.items():
                    content = content.replace(srg, mcp)
                
                if content != original:
                    with open(path, 'w', encoding='utf-8') as fh:
                        fh.write(content)
                    count += 1

print(f'Total files fixed: {count}')
