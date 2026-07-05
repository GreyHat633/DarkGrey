import json
import os

transcript_path = r'C:\Users\GreyHat\.gemini\antigravity\brain\68753665-d6cf-4535-81c2-c4ef4f8cfd50\.system_generated\logs\transcript_full.jsonl'
files_to_recover = ['ComponentAuraTorrent.java', 'EntityAuraTorrent.java', 'ItemRPGWand.java']
recovered_files = {}

if os.path.exists(transcript_path):
    with open(transcript_path, 'r', encoding='utf-8') as f:
        for line in f:
            for target in files_to_recover:
                if target in line:
                    try:
                        obj = json.loads(line)
                        if obj.get('type') == 'PLANNER_RESPONSE' and 'tool_calls' in obj:
                            for tool in obj['tool_calls']:
                                if tool['name'] == 'write_to_file' or tool['name'] == 'replace_file_content':
                                    args = tool.get('args', {})
                                    target_file = args.get('TargetFile', '')
                                    if target in target_file:
                                        if 'CodeContent' in args:
                                            recovered_files[target] = args['CodeContent']
                                        elif 'ReplacementContent' in args:
                                            if target not in recovered_files:
                                                print(f"Warning: replace_file_content found for {target} but we don't have the base file yet!")
                                            else:
                                                # Applying replace
                                                target_content = args.get('TargetContent', '')
                                                replacement_content = args.get('ReplacementContent', '')
                                                recovered_files[target] = recovered_files[target].replace(target_content, replacement_content)
                    except Exception as e:
                        pass

for k, v in recovered_files.items():
    print(f"RECOVERED: {k}")
    if 'Entity' in k:
        out_path = os.path.join(r'E:\Java\MinecraftMod\DarkGrey\src\main\java\com\greyhat\dark_grey\entity', k)
    elif 'Item' in k:
        out_path = os.path.join(r'E:\Java\MinecraftMod\DarkGrey\src\main\java\com\greyhat\dark_grey\item', k)
    else:
        out_path = os.path.join(r'E:\Java\MinecraftMod\DarkGrey\src\main\java\com\greyhat\dark_grey\component', k)
    
    with open(out_path, 'w', encoding='utf-8') as out_f:
        out_f.write(v)

print("Current session recovery script finished.")
