import codecs

path = r'e:\Java\MinecraftMod\DarkGrey\src\main\java\com\greyhat\dark_grey\item\ItemRPGGun.java'
with codecs.open(path, 'r', 'utf-8', errors='ignore') as f:
    lines = f.readlines()

for i in range(len(lines)):
    if 'Item container for the' in lines[i] and 'GUN' in lines[i]:
        lines[i] = ' * Item container for the "枪械" (GUN) weapon category.\n'
    if 'First let IOnRightClick handlers run' in lines[i] and 'they may intercept for firing' in lines[i]:
        lines[i] = '        // First let IOnRightClick handlers run —— they may intercept for firing.\n'

with codecs.open(path, 'w', 'utf-8') as f:
    f.writelines(lines)
