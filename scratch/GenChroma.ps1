Add-Type -AssemblyName System.Drawing
$inputPath = "C:\Users\GreyHat\.gemini\antigravity\brain\dee0fb31-a186-4065-a0d9-1a998cb9bbc5\shattered_bone_1785071848409.jpg"
$outputPath = "e:\Java\MinecraftMod\DarkGrey\src\main\resources\assets\dark_grey\textures\gui\marks\shattered_bone.png"
$img = [System.Drawing.Image]::FromFile($inputPath)
$bmp = New-Object System.Drawing.Bitmap 32, 32
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.DrawImage($img, 0, 0, 32, 32)
$g.Dispose()
$img.Dispose()

for ($y = 0; $y -lt 32; $y++) {
    for ($x = 0; $x -lt 32; $x++) {
        $color = $bmp.GetPixel($x, $y)
        if ($color.G -gt 150 -and $color.R -lt 100 -and $color.B -lt 100) {
            $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(0, 0, 0, 0))
        } elseif ($color.G -gt $color.R * 1.5 -and $color.G -gt $color.B * 1.5) {
            $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(0, 0, 0, 0))
        } else {
            $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $color.R, $color.G, $color.B))
        }
    }
}

$dir = [System.IO.Path]::GetDirectoryName($outputPath)
if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
$bmp.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Host "Texture created with transparent background!"
