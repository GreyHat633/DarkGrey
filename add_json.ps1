$jsonAdd = @'
    {
      "id": "bone_flask",
      "type": "usable_item",
      "displayName": {
        "zh_CN": "碎骨瓶",
        "en_US": "Bone Flask"
      },
      "texture": "dark_grey:bone_flask",
      "maxStackSize": 16,
      "components": [
        {
          "name": "碎骨瓶",
          "params": {
            "directDamage": 12.0,
            "lingeringDamage": 2.0,
            "fieldDuration": 1200,
            "projectileVelocity": 0.75,
            "projectileInaccuracy": 1.0,
            "projectileGravity": 0.03
          }
        }
      ]
    },
'@
$files = @(
    "E:\Java\MinecraftMod\DarkGrey\src\main\resources\assets\dark_grey\data\rpg_items.json",
    "E:\Java\MinecraftMod\RPGItem\rpg_items.json",
    "E:\Java\MinecraftMod\DarkGrey\run\config\dark_grey\rpg_items.json"
)

foreach ($file in $files) {
    if (Test-Path $file) {
        $content = Get-Content $file -Raw -Encoding UTF8
        # Make sure not to add it twice
        if (-not ($content -match '"id": "bone_flask"')) {
            $content = $content -replace '("items"\s*:\s*\[)', "`$1`r`n$jsonAdd"
            Set-Content -Path $file -Value $content -Encoding UTF8 -NoNewline
            Write-Host "Updated $file"
        } else {
            Write-Host "Already exists in $file"
        }
    }
}
