param(
    [string]$InputPath,
    [string]$OutputPath
)
Add-Type -AssemblyName System.Drawing
$img = [System.Drawing.Bitmap]::FromFile($InputPath)
$newImg = New-Object System.Drawing.Bitmap($img.Width, $img.Height)

for ($y = 0; $y -lt $img.Height; $y++) {
    for ($x = 0; $x -lt $img.Width; $x++) {
        $pixel = $img.GetPixel($x, $y)
        if ($pixel.R -gt 240 -and $pixel.G -gt 240 -and $pixel.B -gt 240) {
            $newImg.SetPixel($x, $y, [System.Drawing.Color]::Transparent)
        } else {
            $newImg.SetPixel($x, $y, $pixel)
        }
    }
}

$resizedImg = New-Object System.Drawing.Bitmap($newImg, 256, 256)

$dir = [System.IO.Path]::GetDirectoryName($OutputPath)
if (-not (Test-Path $dir)) {
    New-Item -ItemType Directory -Force -Path $dir
}

$resizedImg.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)

$img.Dispose()
$newImg.Dispose()
$resizedImg.Dispose()
