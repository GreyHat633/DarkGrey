Add-Type -AssemblyName System.Drawing
function FlipY-Image($file) {
    $img = [System.Drawing.Image]::FromFile($file)
    $img.RotateFlip([System.Drawing.RotateFlipType]::RotateNoneFlipY)
    
    $temp = $file + ".tmp.png"
    $img.Save($temp, [System.Drawing.Imaging.ImageFormat]::Png)
    $img.Dispose()
    
    Remove-Item $file -Force
    Rename-Item $temp (Split-Path $file -Leaf)
}

FlipY-Image 'e:\Java\MinecraftMod\DarkGrey\src\main\resources\assets\dark_grey\textures\items\bone_cannon.png'
FlipY-Image 'e:\Java\MinecraftMod\DarkGrey\src\main\resources\assets\dark_grey\textures\items\bone_cannon_equipped.png'
Write-Host "Images flipped vertically back to original."
