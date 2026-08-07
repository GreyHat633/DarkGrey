import os

path = 'src/main/resources/assets/dark_grey/lang/zh_CN.lang'
with open(path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

with open(path, 'w', encoding='utf-8') as f:
    for line in lines:
        if 'item.dark_grey:meteor' not in line:
            f.write(line)
print("Removed from zh_CN.lang")
