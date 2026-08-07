import json
import os

path1 = 'src/main/resources/assets/dark_grey/data/rpg_items.json'

with open(path1, 'r', encoding='utf-8') as f:
    data = json.load(f)

items = data.get('items', [])
found = False
for item in items:
    if item.get('id') == 'meteor':
        found = True
        break

if not found:
    new_item = {
        "id": "meteor",
        "type": "法杖",
        "displayName": {
          "zh_CN": "陨星",
          "en_US": "Meteor"
        },
        "texture": "dark_grey:meteor",
        "durability": 0,
        "damage": 0,
        "components": [
          {
            "name": "陨星",
            "params": {
              "baseDamage": 335.0,
              "tapThresholdTicks": 6,
              "maxChargeTicks": 40,
              "launchMinPower": 1.0,
              "launchMaxSpeed": 3.5,
              "useVanillaGravity": True,
              "landingExplosionRadius": 5.0,
              "landingExplosionMultiplier": 3.0,
              "landingScorchStacks": 1,
              "landingScorchDetonationMultiplier": 1.0,
              "dashDistance": 5.0,
              "dashImpactDamageMultiplier": 1.0,
              "dashReboundHorizontal": 0.8,
              "dashReboundVertical": 0.4,
              "trailWidth": 1.0,
              "trailDurationTicks": 60,
              "trailIntervalTicks": 10,
              "trailDamage": 40.0,
              "trailScorchChance": 0.4,
              "trailIgniteChance": 0.4,
              "trailScorchStacks": 1,
              "trailScorchDetonationMultiplier": 1.0
            }
          }
        ],
        "enchantments": ""
    }
    items.append(new_item)
    
    with open(path1, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    
    print("Added meteor to rpg_items.json")
else:
    print("Already exists")
