Add-Type -AssemblyName System.Drawing

$root = "C:\Users\david\Desktop\cobalt-tv\app\src\main\res"

function New-RoundedPath([single]$x, [single]$y, [single]$w, [single]$h, [single]$r) {
    $p = New-Object System.Drawing.Drawing2D.GraphicsPath
    $d = $r * 2
    $p.AddArc($x, $y, $d, $d, 180, 90)
    $p.AddArc($x + $w - $d, $y, $d, $d, 270, 90)
    $p.AddArc($x + $w - $d, $y + $h - $d, $d, $d, 0, 90)
    $p.AddArc($x, $y + $h - $d, $d, $d, 90, 90)
    $p.CloseFigure()
    return $p
}

function New-Icon([int]$size, [string]$path, [bool]$fullBleed) {
    $bmp = New-Object System.Drawing.Bitmap($size, $size)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = 'AntiAlias'
    $g.TextRenderingHint = 'AntiAliasGridFit'
    $g.Clear([System.Drawing.Color]::Transparent)

    if ($fullBleed) {
        $bg = New-RoundedPath 0 0 $size $size ($size * 0.22)
        $brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
            (New-Object System.Drawing.Point(0, 0)),
            (New-Object System.Drawing.Point($size, $size)),
            [System.Drawing.Color]::FromArgb(255, 12, 20, 38),
            [System.Drawing.Color]::FromArgb(255, 26, 46, 96))
        $g.FillPath($brush, $bg)
        $g.DrawPath((New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(70, 110, 155, 255), [single]($size * 0.012))), $bg)
        $inset = $size * 0.28
    } else {
        $inset = $size * 0.16
    }

    $tile = New-RoundedPath $inset $inset ($size - 2 * $inset) ($size - 2 * $inset) ($size * 0.18)
    $tileBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        (New-Object System.Drawing.Point($inset, $inset)),
        (New-Object System.Drawing.Point(($size - $inset), ($size - $inset))),
        [System.Drawing.Color]::FromArgb(255, 59, 111, 246),
        [System.Drawing.Color]::FromArgb(255, 27, 63, 176))
    $g.FillPath($tileBrush, $tile)

    $fontSize = if ($fullBleed) { $size * 0.40 } else { $size * 0.46 }
    $font = New-Object System.Drawing.Font("Segoe UI", [single]$fontSize, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
    $fmt = New-Object System.Drawing.StringFormat
    $fmt.Alignment = 'Center'
    $fmt.LineAlignment = 'Center'
    $rect = New-Object System.Drawing.RectangleF(0, 0, [single]$size, [single]$size)
    $g.DrawString("C", $font, [System.Drawing.Brushes]::White, $rect, $fmt)

    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose(); $bmp.Dispose(); $font.Dispose()
}

$densities = @{ "mdpi" = 1.0; "hdpi" = 1.5; "xhdpi" = 2.0; "xxhdpi" = 3.0; "xxxhdpi" = 4.0 }
foreach ($d in $densities.Keys) {
    $dir = Join-Path $root "mipmap-$d"
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    $scale = $densities[$d]
    New-Icon ([int](48 * $scale)) (Join-Path $dir "ic_launcher.png") $true
    New-Icon ([int](48 * $scale)) (Join-Path $dir "ic_launcher_round.png") $true
    New-Icon ([int](108 * $scale)) (Join-Path $dir "ic_launcher_foreground.png") $false
}

# TV banner: 320x180 dp, drawn at 3x and downscaled for smooth edges.
$w = 960; $h = 540
$big = New-Object System.Drawing.Bitmap($w, $h)
$g = [System.Drawing.Graphics]::FromImage($big)
$g.SmoothingMode = 'AntiAlias'
$g.TextRenderingHint = 'AntiAliasGridFit'
$rect = New-Object System.Drawing.Rectangle(0, 0, $w, $h)
$bg = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
    $rect,
    [System.Drawing.Color]::FromArgb(255, 7, 11, 20),
    [System.Drawing.Color]::FromArgb(255, 22, 40, 82),
    45.0)
$g.FillRectangle($bg, $rect)

# Soft cobalt glow circle on the right.
$glow = New-Object System.Drawing.Drawing2D.GraphicsPath
$glow.AddEllipse($w * 0.62, -$h * 0.35, $w * 0.62, $w * 0.62)
$glowBrush = New-Object System.Drawing.Drawing2D.PathGradientBrush($glow)
$glowBrush.CenterColor = [System.Drawing.Color]::FromArgb(90, 59, 111, 246)
$glowBrush.SurroundColors = @([System.Drawing.Color]::FromArgb(0, 59, 111, 246))
$g.FillPath($glowBrush, $glow)

$tile = New-RoundedPath 84 168 204 204 44
$tileBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
    (New-Object System.Drawing.Point(84, 168)),
    (New-Object System.Drawing.Point(288, 372)),
    [System.Drawing.Color]::FromArgb(255, 70, 124, 255),
    [System.Drawing.Color]::FromArgb(255, 24, 56, 160))
$g.FillPath($tileBrush, $tile)

$cFont = New-Object System.Drawing.Font("Segoe UI", [single]118, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
$fmt = New-Object System.Drawing.StringFormat
$fmt.Alignment = 'Center'; $fmt.LineAlignment = 'Center'
$g.DrawString("C", $cFont, [System.Drawing.Brushes]::White, (New-Object System.Drawing.RectangleF(84, 156, 204, 204)), $fmt)

$titleFont = New-Object System.Drawing.Font("Segoe UI", [single]96, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
$subFont = New-Object System.Drawing.Font("Segoe UI", [single]34, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
$g.DrawString("COBALT", $titleFont, [System.Drawing.Brushes]::White, 330, 196)
$g.DrawString("T E L E V I S I O N", $subFont, (New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 143, 174, 255))), 336, 300)

$banner = New-Object System.Drawing.Bitmap(320, 180)
$g2 = [System.Drawing.Graphics]::FromImage($banner)
$g2.InterpolationMode = 'HighQualityBicubic'
$g2.DrawImage($big, 0, 0, 320, 180)
New-Item -ItemType Directory -Force -Path (Join-Path $root "drawable") | Out-Null
$banner.Save((Join-Path $root "drawable\tv_banner.png"), [System.Drawing.Imaging.ImageFormat]::Png)

$g.Dispose(); $g2.Dispose(); $big.Dispose(); $banner.Dispose()
Write-Output "icons + banner generated"
