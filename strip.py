import os
import re

files = [
    r'E:\Java\MinecraftMod\DarkGrey\src\main\java\com\greyhat\dark_grey\api\RPGItemLoader.java',
    r'E:\Java\MinecraftMod\DarkGrey\src\main\java\com\greyhat\dark_grey\DarkGrey.java'
]

for p in files:
    with open(p, 'rb') as f:
        content = f.read().decode('utf-8', errors='ignore')
    
    # Strip comments to remove Chinese characters in comments
    content = re.sub(r'//.*', '', content)
    
    # Replace the registration keys explicitly using their unicode equivalents
    content = content.replace('"星辉"', '"\\u661F\\u8F89"')
    content = content.replace('"圆环之理"', '"\\u5706\\u73AF\\u4E4B\\u7406"')
    content = content.replace('"真圆环之理"', '"\\u771F\\u5706\\u73AF\\u4E4B\\u7406"')
    content = content.replace('"血祭"', '"\\u8840\\u796D"')
    content = content.replace('"劫难"', '"\\u52AB\\u96BE"')
    
    # Also in case they are already mangled as '??' or something, replace the exact line
    content = re.sub(r'ComponentRegistry\.register\(".*?", ComponentSupernovaSet::new\);', 'ComponentRegistry.register("\\u661F\\u8F89", ComponentSupernovaSet::new);', content)
    content = re.sub(r'ComponentRegistry\.register\(".*?", ComponentLawOfCycles::new\);', 'ComponentRegistry.register("\\u5706\\u73AF\\u4E4B\\u7406", ComponentLawOfCycles::new);', content)
    content = re.sub(r'ComponentRegistry\.register\(".*?", ComponentTrueLawOfCycles::new\);', 'ComponentRegistry.register("\\u771F\\u5706\\u73AF\\u4E4B\\u7406", ComponentTrueLawOfCycles::new);', content)
    content = re.sub(r'ComponentRegistry\.register\(".*?", ComponentBloodSacrifice::new\);', 'ComponentRegistry.register("\\u8840\\u796D", ComponentBloodSacrifice::new);', content)
    content = re.sub(r'ComponentRegistry\.register\(".*?", ComponentCalamity::new\);', 'ComponentRegistry.register("\\u52AB\\u96BE", ComponentCalamity::new);', content)

    # Strip any remaining non-ascii characters
    clean_chars = []
    for c in content:
        if ord(c) > 127:
            pass
        else:
            clean_chars.append(c)
            
    clean_content = ''.join(clean_chars)
    
    with open(p, 'wb') as f:
        f.write(clean_content.encode('utf-8'))
