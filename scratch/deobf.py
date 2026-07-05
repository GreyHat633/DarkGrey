import os
import re

mapping = {
    "func_74777_a": "setShort",
    "func_74742_a": "appendTag",
    "func_74768_a": "setInteger",
    "func_74745_c": "tagCount",
    "func_74782_a": "setTag",
    "func_82580_o": "removeTag",
    "field_77331_b": "enchantmentsList",
    "field_70170_p": "worldObj",
    "func_77626_a": "getMaxItemUseDuration",
    "func_70040_Z": "getLookVec",
    "field_70165_t": "posX",
    "field_72450_a": "xCoord",
    "field_70163_u": "posY",
    "func_70047_e": "getEyeHeight",
    "field_72448_b": "yCoord",
    "field_70161_v": "posZ",
    "field_72449_c": "zCoord",
    "field_73012_v": "rand",
    "func_72869_a": "spawnParticle",
    "func_77653_i": "getItemStackDisplayName",
    "func_77659_f": "onPlayerStoppedUsing",
    "func_77615_a": "onItemRightClick",
    "func_77655_b": "setUnlocalizedName",
    "func_111206_d": "setTextureName",
    "func_77637_a": "setCreativeTab",
    "field_70159_w": "motionX",
    "field_70181_x": "motionY",
    "field_70179_y": "motionZ",
    "field_72995_K": "isRemote",
    "func_70031_b": "setSprinting",
    "func_72838_d": "spawnEntityInWorld",
    "func_85030_a": "playSoundAtEntity",
    "field_71071_by": "inventory",
    "func_70448_g": "consumeInventoryItem",
    "field_71075_bZ": "capabilities",
    "field_75098_d": "isCreativeMode",
    "func_77654_b": "addInformation"
}

files = [
    r"E:\Java\MinecraftMod\DarkGrey\src\main\java\com\greyhat\dark_grey\item\ItemRPGAmmo.java",
    r"E:\Java\MinecraftMod\DarkGrey\src\main\java\com\greyhat\dark_grey\item\ItemRPGBow.java",
    r"E:\Java\MinecraftMod\DarkGrey\src\main\java\com\greyhat\dark_grey\item\ItemRPGArmor.java"
]

for filepath in files:
    if not os.path.exists(filepath):
        print(f"Skipping {filepath}, does not exist")
        continue
    
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    for srg, mcp in mapping.items():
        # Match word boundaries to prevent partial matches
        content = re.sub(r'\b' + re.escape(srg) + r'\b', mcp, content)
        
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
    print(f"Deobfuscated {filepath}")
