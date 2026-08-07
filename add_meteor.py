import json
import os

paths = [
    'src/main/resources/assets/dark_grey/data/rpg_items.json',
    'run/client/config/dark_grey/rpg_items.json'
]

new_item = {
    "id": "meteor",
    "type": "wand",
    "displayName": {
        "zh_CN": "陨星",
        "en_US": "Meteor"
    },
    "texture": "dark_grey:meteor",
    "durability": 0,
    "damage": 335.0,
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
                "lockAirControlDuringMeteorFlight": True,
                "trajectoryPreview": True,
                "trajectoryPreviewStepTicks": 1,
                "trajectoryPreviewMaxSteps": 200,
                "landingExplosionRadius": 5.0,
                "landingExplosionMultiplier": 3.0,
                "landingScorchStacks": 1,
                "landingScorchDetonationMultiplier": 1.0,
                "landingRespectWalls": False,
                "cancelMeteorFallDamage": True,
                "dashDistance": 5.0,
                "dashImpactDamageMultiplier": 0.10,
                "dashScorchDetonationMultiplier": 1.0,
                "dashReboundHorizontal": 0.8,
                "dashReboundVertical": 0.4,
                "dashStepHeight": 1.0,
                "trailWidth": 1.0,
                "trailDurationTicks": 60,
                "trailIntervalTicks": 10,
                "trailDamage": 40.0,
                "trailScorchChance": 0.40,
                "trailScorchStacks": 1,
                "trailIgniteChance": 0.40,
                "trailScorchDetonationMultiplier": 1.0
            }
        }
    ],
    "enchantments": ""
}

for path in paths:
    if os.path.exists(path):
        with open(path, 'r', encoding='utf-8') as f:
            data = json.load(f)
        
        # Check if meteor already exists
        exists = False
        for item in data.get('items', []):
            if item.get('id') == 'meteor':
                exists = True
                break
        
        if not exists:
            data['items'].append(new_item)
            with open(path, 'w', encoding='utf-8') as f:
                json.dump(data, f, ensure_ascii=False, indent=2)
            print(f"Added meteor to {path}")
