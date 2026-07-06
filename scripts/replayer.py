import json
import os

brains_dir = r'C:\Users\GreyHat\.gemini\antigravity\brain'
output_dir = r'E:\Java\MinecraftMod\DarkGrey\src\main\java\com\greyhat\dark_grey'

# Chronological order of brains (approximate based on what we know, but we should probably sort them by some timestamp if possible. Or we can just read the logs in order.
# Actually, the file system state was wiped right before this current brain (7e75). The current brain contains the server restart.
# So we can replay everything from the very beginning.
# Wait, each line has a timestamp? We don't need it. The jsonl logs are in chronological order. We can just sort the brains by folder creation time or assume a chronological order.
brains = []
for brain in os.listdir(brains_dir):
    try:
        t = os.path.getmtime(os.path.join(brains_dir, brain, '.system_generated', 'logs', 'transcript_full.jsonl'))
        brains.append((t, brain))
    except: pass
brains.sort()

file_state = {}

for _, brain in brains:
    transcript_path = os.path.join(brains_dir, brain, '.system_generated', 'logs', 'transcript_full.jsonl')
    if not os.path.exists(transcript_path): continue
    with open(transcript_path, 'r', encoding='utf-8', errors='ignore') as f:
        for line in f:
            try:
                step = json.loads(line)
            except: continue
            
            if step.get('type') == 'PLANNER_RESPONSE':
                for tc in step.get('tool_calls', []):
                    name = tc.get('name')
                    args = tc.get('args', tc.get('arguments', {}))
                    if name == 'write_to_file' or name == 'default_api:write_to_file':
                        target = args.get('TargetFile', '')
                        code = args.get('CodeContent', '')
                        if 'src/main/java' in target.replace('\\', '/'):
                            file_state[target] = code
                    elif name == 'replace_file_content' or name == 'default_api:replace_file_content':
                        target = args.get('TargetFile', '')
                        if target in file_state:
                            start = args.get('StartLine', 1)
                            end = args.get('EndLine', start)
                            replacement = args.get('ReplacementContent', '')
                            
                            lines = file_state[target].split('\n')
                            new_lines = lines[:start-1] + replacement.split('\n') + lines[end:]
                            file_state[target] = '\n'.join(new_lines)
                    elif name == 'multi_replace_file_content' or name == 'default_api:multi_replace_file_content':
                        target = args.get('TargetFile', '')
                        if target in file_state:
                            chunks = args.get('ReplacementChunks', [])
                            chunks.sort(key=lambda x: x.get('StartLine', 1), reverse=True)
                            
                            lines = file_state[target].split('\n')
                            for chunk in chunks:
                                start = chunk.get('StartLine', 1)
                                end = chunk.get('EndLine', start)
                                rep = chunk.get('ReplacementContent', '')
                                lines = lines[:start-1] + rep.split('\n') + lines[end:]
                            file_state[target] = '\n'.join(lines)

for target, code in file_state.items():
    if not code: continue
    
    # only care about com.greyhat.dark_grey classes
    if 'com/greyhat/dark_grey' not in target.replace('\\', '/') and 'com\\greyhat\\dark_grey' not in target:
        continue
        
    full_path = target
    if not full_path.startswith('E:'):
        # some paths might be weird, force them into the right dir
        basename = os.path.basename(target)
        # find package in code to build right path
        import re
        pkg_match = re.search(r'package (.*?);', code)
        if pkg_match:
            pkg = pkg_match.group(1)
            rel = pkg.replace('.', '\\') + '\\' + basename
            full_path = os.path.join(r'E:\Java\MinecraftMod\DarkGrey\src\main\java', rel)

    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    with open(full_path, 'w', encoding='utf-8') as f:
        f.write(code)
    print(f'Recovered from tool replay: {os.path.basename(full_path)}')
