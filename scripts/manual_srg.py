import os

replacements = {
    'func_70014_b': 'writeEntityToNBT',
    'func_70037_a': 'readEntityFromNBT',
    'field_70169_q': 'lastTickPosX',
    'field_70167_r': 'lastTickPosY',
    'field_70166_s': 'lastTickPosZ',
    'func_85052_h()': 'shootingEntity',
    'func_72890_a': 'getClosestPlayerToEntity',
    'func_70676_i': 'getLook',
    'func_72433_c': 'lengthVector',
    'func_72438_d': 'distanceTo',
    'field_72308_g': 'entityHit',
    'func_76356_a': 'causeArrowDamage',
    'func_72876_a': 'createExplosion',
    'field_70180_af': 'dataWatcher',
    'func_75692_b': 'updateObject',
    '.createVectorHelper(Vec3.createVectorHelper': '.crossProduct(Vec3.createVectorHelper',
}

src_dirs = [
    r'E:\Java\MinecraftMod\DarkGrey\src\main\java\com\greyhat\dark_grey\component',
    r'E:\Java\MinecraftMod\DarkGrey\src\main\java\com\greyhat\dark_grey\entity',
    r'E:\Java\MinecraftMod\DarkGrey\src\main\java\com\greyhat\dark_grey\api',
]

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

print("Done replacing.")
