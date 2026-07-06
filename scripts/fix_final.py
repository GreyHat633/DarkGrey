import os

path = r'E:\Java\MinecraftMod\DarkGrey\src\main\java\com\greyhat\dark_grey\entity\EntityMadokaArrow.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('super(world, shooter);', 'super(world, shooter, 1.0f);')
content = content.replace('this.field_70129_M', 'this.yOffset')
content = content.replace('EntityLivingBase shooter = this.shootingEntity;', 'EntityLivingBase shooter = (EntityLivingBase) this.shootingEntity;')

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Done fixing EntityMadokaArrow.")
