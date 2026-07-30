Add-Type -AssemblyName System.Drawing
function Resize-Image($file, $out, $size) {
    $img = [System.Drawing.Image]::FromFile($file)
    $bmp = New-Object System.Drawing.Bitmap($size, $size)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.DrawImage($img, 0, 0, $size, $size)
    $g.Dispose()
    $img.Dispose()
    $bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

Resize-Image 'e:\Java\MinecraftMod\DarkGrey\src\main\resources\assets\dark_grey\textures\items\bone_cannon.png' 'e:\Java\MinecraftMod\DarkGrey\src\main\resources\assets\dark_grey\textures\items\bone_cannon.png' 32
Resize-Image 'e:\Java\MinecraftMod\DarkGrey\src\main\resources\assets\dark_grey\textures\items\bone_cannon_equipped.png' 'e:\Java\MinecraftMod\DarkGrey\src\main\resources\assets\dark_grey\textures\items\bone_cannon_equipped.png' 32
Write-Host "Images resized."
