Add-Type -AssemblyName System.Drawing
$paths = @(
    "E:\Java\MinecraftMod\DarkGrey\src\main\resources\assets\dark_grey\textures\items\shattered_bone_staff.png",
    "E:\Java\MinecraftMod\DarkGrey\src\main\resources\assets\dark_grey\textures\items\hardened_bone_marrow.png"
)

foreach ($p in $paths) {
    if (Test-Path $p) {
        $img = [System.Drawing.Image]::FromFile($p)
        if ($img.Width -ne 32 -or $img.Height -ne 32) {
            $newImg = New-Object System.Drawing.Bitmap(32, 32)
            $g = [System.Drawing.Graphics]::FromImage($newImg)
            $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
            $g.DrawImage($img, 0, 0, 32, 32)
            $g.Dispose()
            $img.Dispose()
            $newImg.Save($p, [System.Drawing.Imaging.ImageFormat]::Png)
            $newImg.Dispose()
            Write-Output "Resized $p to 32x32"
        } else {
            $img.Dispose()
            Write-Output "Skipped $p, already 32x32"
        }
    }
}
