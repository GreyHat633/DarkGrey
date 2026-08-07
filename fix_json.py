import json
import codecs

path = 'src/main/resources/assets/dark_grey/data/rpg_items.json'
with codecs.open(path, 'r', 'utf-8') as f:
    data = json.load(f)

ids = [item['id'] for item in data]
if 'sunflame_round' not in ids:
    data.append({
      'id': 'sunflame_round',
      'type': '材料',
      'displayName': { 'zh_CN': '阳炎弹', 'en_US': 'Sunflame Round' },
      'texture': 'dark_grey:sunflame_round',
      'durability': 0,
      'damage': 0,
      'components': [],
      'maxStackSize': 64,
      'enchantments': ''
    })

if 'slag_eruptor' not in ids:
    data.append({
      'id': 'slag_eruptor',
      'type': '铳',
      'displayName': { 'zh_CN': '熔渣喷发器', 'en_US': 'Slag Eruptor' },
      'texture': 'dark_grey:slag_eruptor',
      'durability': 0,
      'damage': 0,
      'components': [{
          'name': '熔渣喷发器',
          'params': {
            'loadTicksRequired': 40,
            'baseDamage': 50.0,
            'magazineCapacity': 30
          }
      }],
      'maxStackSize': 1,
      'enchantments': ''
    })

with codecs.open(path, 'w', 'utf-8') as f:
    json.dump(data, f, indent=2, ensure_ascii=False)
print('JSON updated via Python.')
