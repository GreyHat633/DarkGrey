Add-Type -AssemblyName System.Drawing
$imagePath = "E:\Java\MinecraftMod\DarkGrey\src\main\resources\assets\dark_grey\textures\items\corruption_bomb.png"
$img = [System.Drawing.Image]::FromFile($imagePath)
Write-Host "Original Size: $($img.Width)x$($img.Height)"
if ($img.Width -eq 16 -and $img.Height -eq 16) {
    Write-Host "Already 16x16"
} else {
    $bmp = New-Object System.Drawing.Bitmap(16, 16)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.DrawImage($img, 0, 0, 16, 16)
    $img.Dispose()
    $bmp.Save($imagePath, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose()
    $bmp.Dispose()
    Write-Host "Resized to 16x16"
}
