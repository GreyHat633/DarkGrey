import os
import re

path_dg = 'src/main/java/com/greyhat/dark_grey/DarkGrey.java'
with open(path_dg, 'r', encoding='utf-8') as f:
    lines = f.readlines()
with open(path_dg, 'w', encoding='utf-8') as f:
    for line in lines:
        if 'ComponentMeteor' not in line:
            f.write(line)

path_cp = 'src/main/java/com/greyhat/dark_grey/common/CommonProxy.java'
with open(path_cp, 'r', encoding='utf-8') as f:
    lines = f.readlines()
with open(path_cp, 'w', encoding='utf-8') as f:
    for line in lines:
        if 'MeteorExplosionMessage' not in line and 'scheduleMeteorExplosion' not in line:
            f.write(line)

path_clp = 'src/main/java/com/greyhat/dark_grey/common/ClientProxy.java'
with open(path_clp, 'r', encoding='utf-8') as f:
    lines = f.readlines()
with open(path_clp, 'w', encoding='utf-8') as f:
    skip = 0
    for line in lines:
        if skip > 0:
            skip -= 1
            continue
        if 'EntityMeteorFireTrail.class' in line:
            continue
        if 'MeteorRenderHandler' in line:
            continue
        if 'public void scheduleMeteorExplosion' in line:
            # We must skip the whole method body
            skip = 22 # safe estimate, or we can just regex replace
        if 'MeteorExplosionMessage' in line and skip == 0:
            continue
        if skip == 0:
            f.write(line)
