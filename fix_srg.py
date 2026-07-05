import os

src_dir = r'E:\Java\MinecraftMod\DarkGrey\src\main\java'

replacements = {
    # Item methods
    'func_77653_i': 'getItemStackDisplayName',
    'func_77655_b': 'setUnlocalizedName',
    'func_111206_d': 'setTextureName',
    'func_77637_a': 'setCreativeTab',
    'func_77624_a': 'addInformation',
    # NBTTagCompound methods
    'func_74762_e': 'getInteger',
    'func_74768_a': 'setInteger',
    'func_74775_l': 'getCompoundTag',
    'func_74764_b': 'hasKey',
    'func_74782_a': 'setTag',
    'func_82580_o': 'removeTag',
    'func_74777_a': 'setShort',
    'func_74765_d': 'getShort',
    'func_74737_b': 'copy',
    # NBTTagList methods
    'func_74745_c': 'tagCount',
    'func_150295_c': 'getTagList',
    'func_150305_b': 'getCompoundTagAt',
    'func_74742_a': 'appendTag',
    # ItemStack methods
    'func_77978_p': 'getTagCompound',
    'func_77982_d': 'setTagCompound',
    'func_77942_o': 'hasTagCompound',
    # World fields
    'field_72995_K': 'isRemote',
    'func_82737_E': 'getTotalWorldTime',
    # ICommandSender
    'func_145747_a': 'addChatMessage',
    'func_71517_b': 'getCommandName',
    'func_82362_a': 'getRequiredPermissionLevel',
    'func_71518_a': 'getCommandUsage',
    # Enchantment
    'func_77320_a': 'getName',
    'func_74838_a': 'translateToLocal',
    'field_77331_b': 'enchantmentsList',
    'field_77352_x': 'effectId',
    # Entity
    'func_70005_c_': 'getCommandSenderName',
    # Multimap / Item methods
    'func_77640_w': 'getItemAttributeModifiers',
    'func_111205_h': 'getItemAttributeModifiers',
    
    # Rendering-specific mappings for RenderRPGBow
    'func_70620_b': 'getItemIcon',
    'func_71410_x': 'getMinecraft',
    'field_71446_o': 'renderEngine',
    'func_110577_a': 'bindTexture',
    'func_130087_a': 'getResourceLocation',
    'func_94608_d': 'getItemSpriteNumber',
    'field_78398_a': 'instance',
    'func_78439_a': 'renderItemIn2D',
    'func_94212_f': 'getMaxU',
    'func_94206_g': 'getMinV',
    'func_94209_e': 'getMinU',
    'func_94210_h': 'getMaxV',
    'func_94211_a': 'getIconWidth',
    'func_94216_b': 'getIconHeight',
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
                print(f'Fixed: {f}')

print(f'Total files fixed: {count}')
