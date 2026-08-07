import json

path1 = 'src/main/resources/assets/dark_grey/data/rpg_items.json'

with open(path1, 'r', encoding='utf-8') as f:
    data = json.load(f)

items = data.get('items', [])
new_items = [item for item in items if item.get('id') != 'meteor']
data['items'] = new_items

with open(path1, 'w', encoding='utf-8') as f:
    json.dump(data, f, ensure_ascii=False, indent=2)

print("Removed meteor from rpg_items.json")
