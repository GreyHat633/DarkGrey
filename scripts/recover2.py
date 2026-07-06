import json
import os
import re

brains_dir = r'C:\Users\GreyHat\.gemini\antigravity\brain'
output_dir = r'E:\Java\MinecraftMod\DarkGrey\src\main\java\com\greyhat\dark_grey'
missing_classes = ['ItemRPGBow', 'ComponentRegistry', 'ComponentLawOfCycles', 'ComponentTrueLawOfCycles', 'EntityMadokaArrow', 'EntityMadokaRing', 'SetBonusManager', 'RenderMadokaRing', 'RenderMadokaArrow', 'RenderRPGBow']

file_map = {}

def get_balanced_class(code):
    brace_count = 0
    in_class = False
    in_string = False
    in_char = False
    in_line_comment = False
    in_block_comment = False
    
    i = 0
    while i < len(code):
        char = code[i]
        if in_line_comment:
            if char == '\n': in_line_comment = False
        elif in_block_comment:
            if char == '*' and i + 1 < len(code) and code[i+1] == '/':
                in_block_comment = False
                i += 1
        elif in_string:
            if char == '\\': i += 1
            elif char == '"': in_string = False
        elif in_char:
            if char == '\\': i += 1
            elif char == "'": in_char = False
        else:
            if char == '/' and i + 1 < len(code) and code[i+1] == '/':
                in_line_comment = True; i += 1
            elif char == '/' and i + 1 < len(code) and code[i+1] == '*':
                in_block_comment = True; i += 1
            elif char == '"': in_string = True
            elif char == "'": in_char = True
            elif char == '{':
                if not in_class: in_class = True
                brace_count += 1
            elif char == '}':
                if in_class:
                    brace_count -= 1
                    if brace_count == 0: return code[:i+1]
        i += 1
    return code

def extract_from_string(text):
    # Match more broadly for the missing classes, specifically we allow 'abstract', 'final', etc.
    matches = re.finditer(r'(package com\.greyhat\.dark_grey.*?;.*?public\s+(?:abstract\s+)?(?:final\s+)?(?:class|interface)\s+(\w+)[\s\S]*?{.*)', text, re.DOTALL)
    for m in matches:
        code = m.group(1)
        name = m.group(2)
        
        if name not in missing_classes:
            continue
            
        clean_lines = []
        for l in code.split('\n'):
            clean_line = re.sub(r'^\d+:\s', '', l)
            clean_line = re.sub(r'^\s*>?\s*\d+:\s?', '', clean_line)
            clean_line = re.sub(r'^.*?:\d+:\s?', '', clean_line)
            clean_lines.append(clean_line)
        clean_code = '\n'.join(clean_lines)
        
        balanced_code = get_balanced_class(clean_code)
        
        if name not in file_map or len(balanced_code) > len(file_map.get(name, '')):
            file_map[name] = balanced_code

def scan_file(path):
    try:
        with open(path, 'r', encoding='utf-8', errors='ignore') as f:
            for line in f:
                try:
                    step = json.loads(line)
                except Exception:
                    continue
                
                stack = [step]
                while stack:
                    curr = stack.pop()
                    if isinstance(curr, dict):
                        for v in curr.values():
                            if isinstance(v, (dict, list)):
                                stack.append(v)
                            elif isinstance(v, str):
                                if 'package com.greyhat.dark_grey' in v:
                                    extract_from_string(v)
                    elif isinstance(curr, list):
                        for v in curr:
                            if isinstance(v, (dict, list)):
                                stack.append(v)
                            elif isinstance(v, str):
                                if 'package com.greyhat.dark_grey' in v:
                                    extract_from_string(v)
    except Exception as e:
        pass

for brain in os.listdir(brains_dir):
    transcript_path = os.path.join(brains_dir, brain, '.system_generated', 'logs', 'transcript_full.jsonl')
    if os.path.exists(transcript_path):
        scan_file(transcript_path)

print(f'Found {len(file_map)} missing classes across all brains.')

for class_name, clean_code in file_map.items():
    pkg_match = re.search(r'package (.*?);', clean_code)
    if pkg_match:
        pkg = pkg_match.group(1)
        rel_path = pkg.replace('.', '\\') + '\\' + class_name + '.java'
        full_path = os.path.join(output_dir, rel_path.replace('com\\greyhat\\dark_grey\\', ''))
        
        os.makedirs(os.path.dirname(full_path), exist_ok=True)
        with open(full_path, 'w', encoding='utf-8') as out_f:
            out_f.write(clean_code)
        print(f'Recovered {class_name}')
