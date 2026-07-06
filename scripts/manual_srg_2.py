import os

replacements = {
    'extends EntityThrowable': 'extends EntityArrow',
    'func_76134_b': 'cos',
    'func_76126_a': 'sin',
    'func_70088_a': 'entityInit',
    'func_75682_a': 'addObject',
    'func_75683_a': 'getWatchableObjectByte',
    'func_76133_a': 'sqrt_double',
    'func_70105_a': 'setSize',
    'func_70071_h_': 'onUpdate',
    'func_70106_y': 'setDead',
    'func_74762_e': 'getInteger',
    'func_74760_g': 'getFloat',
    'func_74768_a': 'setInteger',
    'func_74776_a': 'setFloat',
    'field_70128_L': 'isDead',
    'field_70173_aa': 'ticksExisted',
    'field_70121_D': 'boundingBox',
    'func_72314_b': 'expand',
    'func_72839_b': 'getEntitiesWithinAABBExcludingEntity',
    'field_76377_j': 'magic',
    'func_70068_e': 'getDistanceSqToEntity',
    'field_70460_b': 'armorInventory',
    'func_85052_h()': 'shootingEntity',
    'func_85052_h': 'shootingEntity',
    'DamageSource.causeArrowDamage((Entity)this, (Entity)this.shootingEntity)': 'DamageSource.causeArrowDamage(this, this.shootingEntity)',
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
                
                # Fix constructor for EntityArrow
                if 'EntityMadokaArrow' in f and 'public EntityMadokaArrow(final World world, final EntityLivingBase shooter, final int level, final float baseDamage)' in content:
                    content = content.replace('super(world);', 'super(world, shooter, 1.0f);')
                
                if content != original:
                    with open(path, 'w', encoding='utf-8') as fh:
                        fh.write(content)

print("Done replacing 2.")
