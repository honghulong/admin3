Add-Type -AssemblyName System.Drawing

$projectRoot = 'F:\x项目文档\j未保小程序\miniprogram\miniprogram'
$outPath = 'D:\hongmengProjects\admin3\未保小程序首页预览.png'

function New-Font($name, $size, $style = [System.Drawing.FontStyle]::Regular) {
    return [System.Drawing.Font]::new($name, $size, $style, [System.Drawing.GraphicsUnit]::Pixel)
}

function Draw-CenteredString($graphics, $text, $font, $brush, $rect) {
    $format = [System.Drawing.StringFormat]::new()
    $format.Alignment = [System.Drawing.StringAlignment]::Center
    $format.LineAlignment = [System.Drawing.StringAlignment]::Center
    $graphics.DrawString($text, $font, $brush, $rect, $format)
    $format.Dispose()
}

$scale = 2
$canvasW = 420 * $scale
$canvasH = 860 * $scale
$phoneX = 22 * $scale
$phoneY = 22 * $scale
$phoneW = 375 * $scale
$phoneH = 812 * $scale

$bitmap = [System.Drawing.Bitmap]::new($canvasW, $canvasH)
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit

$bg = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(238, 241, 244))
$white = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::White)
$text = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(51, 51, 51))
$muted = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(102, 102, 102))
$blue = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(25, 137, 250))
$pink = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 228, 225))
$divider = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(176, 196, 222))
$cyan = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(167, 255, 235))
$teal = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(0, 105, 92))
$banner = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(248, 248, 248))
$linePen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(230, 230, 230), 2)
$darkPen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(18, 18, 18), 3)

$graphics.FillRectangle($bg, 0, 0, $canvasW, $canvasH)
$graphics.FillRectangle($white, $phoneX, $phoneY, $phoneW, $phoneH)

$fontStatus = New-Font 'Segoe UI' (12 * $scale) ([System.Drawing.FontStyle]::Bold)
$fontHeader = New-Font 'Microsoft YaHei UI' (15 * $scale) ([System.Drawing.FontStyle]::Bold)
$fontTitle = New-Font 'Microsoft YaHei UI' (20 * $scale) ([System.Drawing.FontStyle]::Bold)
$fontSub = New-Font 'Segoe UI' (12 * $scale)
$fontGrid = New-Font 'Microsoft YaHei UI' (15 * $scale)
$fontEmoji = New-Font 'Segoe UI Emoji' (36 * $scale)
$fontMedia = New-Font 'Microsoft YaHei UI' (30 * $scale) ([System.Drawing.FontStyle]::Bold)
$fontTab = New-Font 'Microsoft YaHei UI' (11 * $scale)

$statusH = 34 * $scale
$graphics.DrawString('9:41', $fontStatus, $text, $phoneX + 24 * $scale, $phoneY + 15 * $scale)
$rightStatus = '5G  100%'
$sizeStatus = $graphics.MeasureString($rightStatus, $fontStatus)
$graphics.DrawString($rightStatus, $fontStatus, $text, $phoneX + $phoneW - 24 * $scale - $sizeStatus.Width, $phoneY + 15 * $scale)

$capsuleRect = [System.Drawing.Rectangle]::new($phoneX + $phoneW - 100 * $scale, $phoneY + 46 * $scale, 86 * $scale, 30 * $scale)
$graphics.DrawEllipse($darkPen, $capsuleRect.X + 57 * $scale, $capsuleRect.Y + 7 * $scale, 16 * $scale, 16 * $scale)
for ($i = 0; $i -lt 3; $i++) {
    $graphics.FillEllipse([System.Drawing.Brushes]::Black, $capsuleRect.X + (17 + $i * 9) * $scale, $capsuleRect.Y + 13 * $scale, 4 * $scale, 4 * $scale)
}

$contentX = $phoneX
$contentY = $phoneY + $statusH
$y = $contentY

$headerRect = [System.Drawing.RectangleF]::new($contentX + 14 * $scale, $y + 10 * $scale, $phoneW - 118 * $scale, 28 * $scale)
Draw-CenteredString $graphics '东莞市人民检察院未保强制报告' $fontHeader $text $headerRect
$graphics.DrawLine($linePen, $contentX, $y + 48 * $scale, $contentX + $phoneW, $y + 48 * $scale)
$y += 48 * $scale

$graphics.FillRectangle($banner, $contentX, $y, $phoneW, 100 * $scale)
$graphics.DrawLine($linePen, $contentX, $y + 100 * $scale, $contentX + $phoneW, $y + 100 * $scale)
$logo = [System.Drawing.Image]::FromFile((Join-Path $projectRoot 'images\logo.png'))
$logoRect = [System.Drawing.Rectangle]::new($contentX + 68 * $scale, $y + 15 * $scale, 70 * $scale, 70 * $scale)
$graphics.DrawImage($logo, $logoRect)
$graphics.DrawString('东莞市人民检察院', $fontTitle, $text, $contentX + 148 * $scale, $y + 27 * $scale)
$graphics.DrawString('www.dongguan.jcy.gov.cn', $fontSub, $muted, $contentX + 148 * $scale, $y + 56 * $scale)
$y += 100 * $scale

$gridY = $y
$gridH = 202 * $scale
$graphics.FillRectangle($divider, $contentX, $gridY, $phoneW, $gridH)
$items = @(
    @('📋', "强制专项`n报告"),
    @('🔍', '线索举报'),
    @('⚖️', '法律法规'),
    @('📖', '案例解读')
)
for ($row = 0; $row -lt 2; $row++) {
    for ($col = 0; $col -lt 2; $col++) {
        $idx = $row * 2 + $col
        $x = $contentX + $col * (($phoneW + 2 * $scale) / 2)
        $itemY = $gridY + $row * (101 * $scale)
        $rect = [System.Drawing.RectangleF]::new($x, $itemY, ($phoneW - 2 * $scale) / 2, 100 * $scale)
        $graphics.FillRectangle($pink, $rect)
        $graphics.DrawString($items[$idx][0], $fontEmoji, $text, $rect.X + 37 * $scale, $rect.Y + 28 * $scale)
        $graphics.DrawString($items[$idx][1], $fontGrid, $text, $rect.X + 85 * $scale, $rect.Y + 31 * $scale)
    }
}
$y += $gridH + 2 * $scale

$tabH = 64 * $scale
$mediaH = ($phoneY + $phoneH) - $y - $tabH
$graphics.FillRectangle($cyan, $contentX, $y, $phoneW, $mediaH)
$media = [System.Drawing.Image]::FromFile((Join-Path $projectRoot 'images\media.png'))
$mediaText = '多媒体课堂'
$mediaTextSize = $graphics.MeasureString($mediaText, $fontMedia)
$groupW = 60 * $scale + 10 * $scale + $mediaTextSize.Width
$groupX = $contentX + ($phoneW - $groupW) / 2
$groupY = $y + ($mediaH - 60 * $scale) / 2
$graphics.DrawImage($media, [System.Drawing.Rectangle]::new([int]$groupX, [int]$groupY, 60 * $scale, 60 * $scale))
$graphics.DrawString($mediaText, $fontMedia, $teal, $groupX + 70 * $scale, $groupY + 10 * $scale)
$y += $mediaH

$graphics.FillRectangle($white, $contentX, $y, $phoneW, $tabH)
$graphics.DrawLine($linePen, $contentX, $y, $contentX + $phoneW, $y)
$tabCenters = @(($contentX + ($phoneW / 4)), ($contentX + ($phoneW * 3 / 4)))
$tabIconFont = New-Font 'Segoe UI Symbol' (22 * $scale)
$graphics.DrawString('⌂', $tabIconFont, $blue, [float]($tabCenters[0] - 10 * $scale), [float]($y + 7 * $scale))
$graphics.DrawString('▤', $tabIconFont, $muted, [float]($tabCenters[1] - 10 * $scale), [float]($y + 8 * $scale))
Draw-CenteredString $graphics '首页' $fontTab $blue ([System.Drawing.RectangleF]::new($contentX, $y + 37 * $scale, $phoneW / 2, 18 * $scale))
Draw-CenteredString $graphics '我的举报' $fontTab $muted ([System.Drawing.RectangleF]::new($contentX + $phoneW / 2, $y + 37 * $scale, $phoneW / 2, 18 * $scale))

$bitmap.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)

$logo.Dispose()
$media.Dispose()
$tabIconFont.Dispose()
$graphics.Dispose()
$bitmap.Dispose()

Write-Output $outPath
