import os
import re

def fix_file(filepath, replacements, unicode_fixes=False):
    try:
        with open(filepath, 'r', encoding='utf-8-sig', errors='ignore') as f:
            lines = f.readlines()
        
        with open(filepath, 'w', encoding='utf-8') as f:
            for line in lines:
                skip = False
                if unicode_fixes:
                    # Fix ComponentRegistry.register for chinese names
                    if 'ComponentRegistry.register' in line:
                        if 'ComponentSupernovaSet' in line:
                            line = '        com.greyhat.dark_grey.api.ComponentRegistry.register("\\u661F\\u8F89", com.greyhat.dark_grey.component.ComponentSupernovaSet::new);\n'
                        elif 'ComponentLawOfCycles' in line:
                            line = '        com.greyhat.dark_grey.api.ComponentRegistry.register("\\u5706\\u73AF\\u4E4B\\u7406", com.greyhat.dark_grey.component.ComponentLawOfCycles::new);\n'
                        elif 'ComponentTrueLawOfCycles' in line:
                            line = '        com.greyhat.dark_grey.api.ComponentRegistry.register("\\u771F\\u5706\\u73AF\\u4E4B\\u7406", com.greyhat.dark_grey.component.ComponentTrueLawOfCycles::new);\n'
                        elif 'ComponentBloodSacrifice' in line:
                            line = '        com.greyhat.dark_grey.api.ComponentRegistry.register("\\u8840\\u796D", com.greyhat.dark_grey.api.impl.ComponentBloodSacrifice::new);\n'
                        elif 'ComponentCalamity' in line:
                            line = '        com.greyhat.dark_grey.api.ComponentRegistry.register("\\u52AB\\u96BE", com.greyhat.dark_grey.api.impl.ComponentCalamity::new);\n'
                        else:
                            pass # Leave as is
                    
                    if 'LOG.info' in line:
                        skip = True # Just drop Chinese logging statements
                
                for r in replacements:
                    if r in line:
                        f.write('// ' + line)
                        skip = True
                        break
                
                if not skip:
                    f.write(line)
    except Exception as e:
        print(f"Error processing {filepath}: {e}")

# DarkGrey.java
fix_file(
    r'E:\Java\MinecraftMod\DarkGrey\src\main\java\com\greyhat\dark_grey\DarkGrey.java',
    [
        'ItemTabIcon', 'ItemArrowOfLight', 'CommandRPGHelp', 
        'registerServerCommand'
    ],
    unicode_fixes=True
)

# RPGItemLoader.java
fix_file(
    r'E:\Java\MinecraftMod\DarkGrey\src\main\java\com\greyhat\dark_grey\api\RPGItemLoader.java',
    [
        'ItemRPGScythe', 'ItemRPGAmmo', 'ItemRPGBow', 
        'registerScytheRenderer', 'registerBowRenderer',
        'Dummy materials'
    ]
)

# ClientProxy.java
try:
    with open(r'E:\Java\MinecraftMod\DarkGrey\src\main\java\com\greyhat\dark_grey\common\ClientProxy.java', 'r', encoding='utf-8-sig', errors='ignore') as f:
        content = f.read()
    
    # Just clear out the bodies of the methods
    content = """package com.greyhat.dark_grey.common;
import net.minecraft.item.Item;
public class ClientProxy extends CommonProxy {
    @Override
    public void registerRenderers() {}
    @Override
    public void registerItemRenderer(Item item, String equippedTextureName) {}
    public void registerBowRenderer(Item item) {}
    public void registerScytheRenderer(Item item, String equippedTextureName) {}
}
"""
    with open(r'E:\Java\MinecraftMod\DarkGrey\src\main\java\com\greyhat\dark_grey\common\ClientProxy.java', 'w', encoding='utf-8') as f:
        f.write(content)
except Exception as e:
    pass
