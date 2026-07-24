Add-Type -AssemblyName System.Drawing
$inputPath = "C:\Users\GreyHat\.gemini\antigravity\brain\4deda908-1fff-43a6-86f4-e22f1d270c2a\underground_sun_v2_1784836657507.jpg"
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
        if ($lum -lt 25) {
            $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(0, 0, 0, 0))
        } else {
            $alpha = 255
            if ($lum -lt 100) { $alpha = [int](($lum - 25) / 75.0 * 205 + 50) }
            $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($alpha, $color.R, $color.G, $color.B))
        }
    }
}
$bmp.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Host "Texture v2 created!"
