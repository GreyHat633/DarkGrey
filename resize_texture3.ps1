Add-Type -AssemblyName System.Drawing
$sourceDir = "E:\Java\MinecraftMod\RPGItem"
$files = Get-ChildItem -Path $sourceDir -Filter "*.png"
$sourceFile = $files | Where-Object { $_.Name -like "*1.png" } | Select-Object -First 1

if ($sourceFile) {
    Write-Host "Found file: $($sourceFile.FullName)"
    $img = [System.Drawing.Image]::FromFile($sourceFile.FullName)
    Write-Host "Original Size: $($img.Width)x$($img.Height)"
    
    $bmp = New-Object System.Drawing.Bitmap(16, 16)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.DrawImage($img, 0, 0, 16, 16)
    $img.Dispose()
    
    $targetPath = "E:\Java\MinecraftMod\DarkGrey\src\main\resources\assets\dark_grey\textures\items\corruption_bomb.png"
    $bmp.Save($targetPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose()
    $bmp.Dispose()
    Write-Host "Successfully resized and saved to $targetPath"
} else {
    Write-Host "Source file not found!"
}
