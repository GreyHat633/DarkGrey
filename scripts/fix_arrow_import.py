import os

path = r'E:\Java\MinecraftMod\DarkGrey\src\main\java\com\greyhat\dark_grey\entity\EntityMadokaArrow.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('import net.minecraft.entity.projectile.EntityThrowable;', 'import net.minecraft.entity.projectile.EntityArrow;')

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)


# For EntityMadokaRing and EntityScythe, let's see if entityInit exists
for name in ['EntityMadokaRing.java', 'EntityScythe.java']:
    p = os.path.join(r'E:\Java\MinecraftMod\DarkGrey\src\main\java\com\greyhat\dark_grey\entity', name)
    with open(p, 'r', encoding='utf-8') as f:
        c = f.read()
    if 'entityInit' not in c:
        print(f"{name} does not have entityInit()!")
        # add a dummy entityInit
        c = c.replace('extends Entity\n{', 'extends Entity\n{\n    @Override protected void entityInit() {}\n')
        with open(p, 'w', encoding='utf-8') as f:
            f.write(c)

print("Done.")
