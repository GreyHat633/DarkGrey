Add-Type -AssemblyName System.Drawing
$inputPath = "C:\Users\GreyHat\.gemini\antigravity\brain\4deda908-1fff-43a6-86f4-e22f1d270c2a\underground_sun_icon_1784831731399.jpg"
$outputPath = "E:\Java\MinecraftMod\DarkGrey\src\main\resources\assets\dark_grey\textures\items\underground_sun.png"
$img = [System.Drawing.Image]::FromFile($inputPath)
$bmp = New-Object System.Drawing.Bitmap 32, 32
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.DrawImage($img, 0, 0, 32, 32)
$g.Dispose()
$img.Dispose()

for ($y = 0; $y -lt 32; $y++) {
    for ($x = 0; $x -lt 32; $x++) {
        $color = $bmp.GetPixel($x, $y)
        $lum = [Math]::Max($color.R, [Math]::Max($color.G, $color.B))
        if ($lum -lt 10) {
            $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(0, 0, 0, 0))
        } else {
            $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($lum, $color.R, $color.G, $color.B))
        }
    }
}
$bmp.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Host "Image generated successfully."
