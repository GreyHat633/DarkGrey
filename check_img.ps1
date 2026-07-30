Add-Type -AssemblyName System.Drawing
$img = [System.Drawing.Image]::FromFile('e:\Java\MinecraftMod\DarkGrey\src\main\resources\assets\dark_grey\textures\items\bone_cannon.png')
Write-Host "Width: $($img.Width), Height: $($img.Height)"
$img.Dispose()
