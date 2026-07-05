import json
import os

transcript_path = r'C:\Users\GreyHat\.gemini\antigravity\brain\7e757ced-6aeb-4d51-9efa-e496ce96551c\.system_generated\logs\transcript_full.jsonl'
files_to_recover = ['ComponentAuraTorrent.java', 'ComponentBloodSacrifice.java', 'ComponentCalamity.java', 'ComponentTorchAfterglow.java']
recovered_files = {}

if os.path.exists(transcript_path):
    with open(transcript_path, 'r', encoding='utf-8') as f:
        for line in f:
            if 'ComponentAuraTorrent' in line or 'ComponentBloodSacrifice' in line or 'ComponentCalamity' in line:
                try:
                    obj = json.loads(line)
                    if obj.get('type') == 'PLANNER_RESPONSE' and 'tool_calls' in obj:
                        for tool in obj['tool_calls']:
                            if tool['name'] == 'write_to_file' or tool['name'] == 'replace_file_content':
                                args = tool.get('args', {})
                                target_file = args.get('TargetFile', '')
                                for target in files_to_recover:
                                    if target in target_file:
                                        if 'CodeContent' in args:
                                            recovered_files[target] = args['CodeContent']
                                        elif 'ReplacementContent' in args:
                                            # If it was a replace, we might need a more sophisticated approach. 
                                            # But usually it's write_to_file first.
                                            pass
                except Exception as e:
                    pass

for k, v in recovered_files.items():
    print(f"RECOVERED: {k}")
    # Write to file
    out_path = os.path.join(r'E:\Java\MinecraftMod\DarkGrey\src\main\java\com\greyhat\dark_grey\component', k)
    with open(out_path, 'w', encoding='utf-8') as out_f:
        out_f.write(v)

print("Recovery script finished.")
