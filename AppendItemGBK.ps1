$path = "E:\Java\MinecraftMod\DarkGrey\src\main\resources\assets\dark_grey\data\rpg_items.json"
$gbk = [System.Text.Encoding]::GetEncoding("gbk")
$content = [System.IO.File]::ReadAllText($path, $gbk)

# Verify original content
$origCount = ([regex]::Matches($content, '"id"')).Count
Write-Host "Original item count: $origCount"

# The file ends with:  ...enchantments": "34:3, 51:1"\n    }\n  ]\n}\n
# We want to replace the final     }\n  ]\n}\n  with     },\n    { new item }\n  ]\n}

# Build the new item text with Chinese characters directly in the string
$newItemText = ",`n    {`n"
$newItemText += "      `"id`": `"underground_sun`",`n"
$newItemText += "      `"type`": `"" + [char]0x9053 + [char]0x5177 + "`",`n"  # 道具
$newItemText += "      `"displayName`": {`n"
$newItemText += "        `"zh_CN`": `"" + [char]0x5730 + [char]0x5E95 + [char]0x592A + [char]0x9633 + "`",`n"  # 地底太阳
$newItemText += "        `"en_US`": `"Underground Sun`"`n"
$newItemText += "      },`n"
$newItemText += "      `"texture`": `"dark_grey:underground_sun`",`n"
$newItemText += "      `"durability`": 0,`n"
$newItemText += "      `"damage`": 90,`n"
$newItemText += "      `"components`": [`n"
$newItemText += "        {`n"
$newItemText += "          `"name`": `"" + [char]0x5730 + [char]0x5E95 + [char]0x592A + [char]0x9633 + "`",`n"  # 地底太阳
$newItemText += "          `"params`": {`n"
$newItemText += "            `"chargeTicks`": 40,`n"
$newItemText += "            `"maxStoredOrbs`": 3,`n"
$newItemText += "            `"damageMultiplier`": 5.0,`n"
$newItemText += "            `"explosionRadius`": 20.0,`n"
$newItemText += "            `"explosionHalfHeight`": 10.0,`n"
$newItemText += "            `"projectileSpeed`": 1.8,`n"
$newItemText += "            `"projectileLifetime`": 100,`n"
$newItemText += "            `"launchCooldownTicks`": 5,`n"
$newItemText += "            `"ignoreHurtResistance`": true,`n"
$newItemText += "            `"respectWalls`": false,`n"
$newItemText += "            `"orbitRadius`": 1.25,`n"
$newItemText += "            `"orbitHeight`": -0.2,`n"
$newItemText += "            `"orbitSpeed`": 2.0`n"
$newItemText += "          }`n"
$newItemText += "        }`n"
$newItemText += "      ],`n"
$newItemText += "      `"enchantments`": `"`"`n"
$newItemText += "    }`n"
$newItemText += "  ]`n"
$newItemText += "}`n"

# Replace the ending pattern
$searchPattern = "    }`r?`n  ]`r?`n}`r?`n?"
$content = [regex]::Replace($content, $searchPattern + "$", ("    }" + $newItemText.Substring(1)))

# Write back as GBK
[System.IO.File]::WriteAllText($path, $content, $gbk)

# Verify
$verify = [System.IO.File]::ReadAllText($path, $gbk)
$newCount = ([regex]::Matches($verify, '"id"')).Count
Write-Host "New item count: $newCount"
$has1 = $verify.Contains([char]0x5730 + [string][char]0x5E95 + [string][char]0x592A + [string][char]0x9633)
$has2 = $verify.Contains([char]0x4F0A + [string][char]0x5854 + [string][char]0x5C3C + [string][char]0x65AF)
$has3 = $verify.Contains([char]0x91CD + [string][char]0x51FB + [string][char]0x5DE8 + [string][char]0x5251)
Write-Host "Has 地底太阳: $has1"
Write-Host "Has 伊塔尼斯: $has2"
Write-Host "Has 重击巨剑: $has3"
Write-Host "Has underground_sun: $($verify.Contains('underground_sun'))"
