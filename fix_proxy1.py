import os

path = 'src/main/java/com/greyhat/dark_grey/common/CommonProxy.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

search = 'public void registerGunRenderer(Item item, String id, String texture) {}'
replace = search + '\n\n    public void scheduleMeteorExplosion(com.greyhat.dark_grey.network.MeteorExplosionMessage message) {}'

if search in content:
    content = content.replace(search, replace)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
