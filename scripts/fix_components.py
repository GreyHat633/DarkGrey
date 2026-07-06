import os

src_dir = r'E:\Java\MinecraftMod\DarkGrey\src\main\java\com\greyhat\dark_grey\component'

replacements = {
    'field_75098_d': 'isCreativeMode',
    'field_71075_bZ': 'capabilities',
    'field_71071_by': 'inventory',
    'field_70462_a': 'mainInventory',
    'func_77973_b': 'getItem',
    'func_77506_a': 'getEnchantmentLevel',
    'field_77342_w': 'infinity',
    'field_77352_x': 'effectId',
    'field_72995_K': 'isRemote',
    'field_73012_v': 'rand',
    'func_70186_c': 'setThrowableHeading',
    'field_70159_w': 'motionX',
    'field_70181_x': 'motionY',
    'field_70179_y': 'motionZ',
    'func_72838_d': 'spawnEntityInWorld',
    'func_72956_a': 'playSoundAtEntity',
    'func_70298_a': 'decrStackSize',
    'func_77988_m': 'getMaxItemUseDuration',
    'func_145747_a': 'addChatMessage',
    'func_70040_Z': 'getLookVec',
    'field_70165_t': 'posX',
    'field_70163_u': 'posY',
    'field_70161_v': 'posZ',
    'func_70047_e': 'getEyeHeight',
    'field_72450_a': 'xCoord',
    'field_72448_b': 'yCoord',
    'field_72449_c': 'zCoord',
    'func_72869_a': 'spawnParticle',
    'field_70122_E': 'onGround',
    'func_85030_a': 'playSound',
    'field_70170_p': 'worldObj',
    'func_70690_d': 'addPotionEffect',
    'field_70133_I': 'isDead',
    'func_110138_aP': 'getMaxHealth',
    'func_70606_j': 'setHealth',
    'func_72431_c': 'createVectorHelper',
}

count = 0
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
