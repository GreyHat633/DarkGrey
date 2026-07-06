import json, os, re
brains_dir = r'C:\Users\GreyHat\.gemini\antigravity\brain'
code = ''
for b in os.listdir(brains_dir):
    p = os.path.join(brains_dir, b, '.system_generated', 'logs', 'transcript_full.jsonl')
    if os.path.exists(p):
        with open(p, encoding='utf-8', errors='ignore') as f:
            for l in f:
                if 'class DarkGrey' in l:
                    try: d = json.loads(l)
                    except: continue
                    for tc in d.get('tool_calls', []):
                        args = tc.get('args', tc.get('arguments', {}))
                        c = args.get('CodeContent', '')
                        if 'class DarkGrey' in c and 'package com.greyhat.dark_grey;' in c:
                            if len(c) > len(code): code = c
if not code:
    code = open(r'E:\Java\VibeCoding\decompiled_sources\com\greyhat\dark_grey\DarkGrey.java', encoding='utf-8').read()

code = code.replace('Enchantment.field_77331_b', 'Enchantment.enchantmentsList')
code = code.replace('ench.field_77352_x', 'ench.effectId')
code = code.replace('StatCollector.func_74838_a(ench.func_77320_a())', 'StatCollector.translateToLocal(ench.getName())')
code = code.replace('ench.func_77320_a()', 'ench.getName()')
code = code.replace('Launch.blackboard.get("fml.deobfuscatedEnvironment")', '(Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment")')

with open(r'E:\Java\MinecraftMod\DarkGrey\src\main\java\com\greyhat\dark_grey\DarkGrey.java', 'w', encoding='utf-8') as f:
    f.write(code)
print('Fixed DarkGrey.java')
