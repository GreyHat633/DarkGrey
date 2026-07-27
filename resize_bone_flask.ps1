Add-Type -AssemblyName System.Drawing
$sourceFile = Get-ChildItem "E:\Java\MinecraftMod\RPGItem\*.png" | Select-Object -First 1
$sourcePath = $sourceFile.FullName
$targetPath = "E:\Java\MinecraftMod\DarkGrey\src\main\resources\assets\dark_grey\textures\items\bone_flask.png"
Copy-Item $sourcePath -Destination $targetPath -Force
$img = [System.Drawing.Image]::FromFile($targetPath)
Write-Host "Original Size: $($img.Width)x$($img.Height)"
$bmp = New-Object System.Drawing.Bitmap(64, 64)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.DrawImage($img, 0, 0, 64, 64)
$img.Dispose()
$bmp.Save($targetPath, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose()
$bmp.Dispose()
Write-Host "Resized to 64x64"
