import json

path = 'E:/Java/MinecraftMod/DarkGrey/src/main/resources/assets/dark_grey/data/rpg_items.json'

with open(path, 'r', encoding='utf-8') as f:
    data = json.load(f)

# check if underground_sun exists
found = False
for item in data:
    if item.get('id') == 'underground_sun':
        item['components'][0]['params']['orbitHeight'] = -0.2
        found = True
        break

if not found:
    new_item = {
      "id": "underground_sun",
      "type": "道具",
      "displayName": {
        "zh_CN": "地底太阳",
        "en_US": "Underground Sun"
      },
      "texture": "dark_grey:underground_sun",
      "durability": 0,
      "damage": 90,
      "components": [
        {
          "name": "地底太阳",
          "params": {
            "chargeTicks": 40,
            "maxStoredOrbs": 3,
            "damageMultiplier": 5.0,
            "explosionRadius": 20.0,
            "explosionHalfHeight": 10.0,
            "projectileSpeed": 1.8,
            "projectileLifetime": 100,
            "launchCooldownTicks": 5,
            "ignoreHurtResistance": True,
            "respectWalls": False,
            "orbitRadius": 1.25,
            "orbitHeight": -0.2,
            "orbitSpeed": 2.0
          }
        }
      ],
      "enchantments": ""
    }
    data.append(new_item)

with open(path, 'w', encoding='utf-8') as f:
    json.dump(data, f, ensure_ascii=False, indent=2)

print("JSON updated successfully")
