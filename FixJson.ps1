$path = "E:\Java\MinecraftMod\DarkGrey\src\main\resources\assets\dark_grey\data\rpg_items.json"
$gbk = [System.Text.Encoding]::GetEncoding("gbk")
$content = [System.IO.File]::ReadAllText($path, $gbk)

$newItem = @'
    },
    {
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
            "ignoreHurtResistance": true,
            "respectWalls": false,
            "orbitRadius": 1.25,
            "orbitHeight": -0.2,
            "orbitSpeed": 2.0
          }
        }
      ],
      "enchantments": ""
    }
  ]
}
'@

$content = $content -replace "\}\s*\]\s*\}\s*$", $newItem
[System.IO.File]::WriteAllText($path, $content, $gbk)
Write-Host "Replaced successfully."
