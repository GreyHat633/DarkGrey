import os

path = 'src/main/java/com/greyhat/dark_grey/common/ClientProxy.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

search = '    @Override\n    @Override\n    public void scheduleMeteorExplosion'
replace = '    @Override\n    public void scheduleMeteorExplosion'
if search in content:
    content = content.replace(search, replace)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print("Fixed double @Override")
else:
    print("Not found")
