Add-Type -AssemblyName System.Drawing
$img = [System.Drawing.Image]::FromFile('E:\Java\MinecraftMod\RPGItem\图片1.png')
Write-Output ("Size: {0}x{1}" -f $img.Width, $img.Height)
$bmp = New-Object System.Drawing.Bitmap 64, 64
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.DrawImage($img, 0, 0, 64, 64)
$bmp.Save('E:\Java\MinecraftMod\DarkGrey\src\main\resources\assets\dark_grey\textures\items\corruption_bomb.png', [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose()
$bmp.Dispose()
$img.Dispose()
Write-Output "Resized to 64x64"
