import json, os, re
brains_dir = r'C:\Users\GreyHat\.gemini\antigravity\brain'
code = ''
for b in os.listdir(brains_dir):
    p = os.path.join(brains_dir, b, '.system_generated', 'logs', 'transcript_full.jsonl')
    if os.path.exists(p):
        with open(p, encoding='utf-8', errors='ignore') as f:
            for l in f:
                if 'class ItemRPGScythe' in l:
                    try:
                        d = json.loads(l)
                    except: continue
                    for tc in d.get('tool_calls', []):
                        args = tc.get('args', tc.get('arguments', {}))
                        c = args.get('CodeContent', '')
                        if 'class ItemRPGScythe' in c and 'package com.greyhat.dark_grey.item' in c:
                            if len(c) > len(code): code = c

if code:
    with open(r'E:\Java\MinecraftMod\DarkGrey\src\main\java\com\greyhat\dark_grey\item\ItemRPGScythe.java', 'w', encoding='utf-8') as f:
        f.write(code)
    print('Found and wrote ItemRPGScythe.java')
else:
    print('Not found')
