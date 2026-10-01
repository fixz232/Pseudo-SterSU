param(
    [Parameter(Mandatory = $true)]
    [string]$OutputDirectory,
    [Parameter(Mandatory = $true)]
    [string]$CatalogAsset,
    [string]$SourceDirectory = (Join-Path $PSScriptRoot '..\interface-style-resources')
)

$ErrorActionPreference = 'Stop'
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
$zipTimestamp = [DateTimeOffset]::new(1980, 1, 1, 0, 0, 0, [TimeSpan]::Zero)
$repositoryBase = 'https://raw.githubusercontent.com/fixz232/SterSU-ThemeStore/main/interface-styles'
$packageDirectory = Join-Path $OutputDirectory 'interface-styles/packages'
$catalogDirectory = Join-Path $OutputDirectory 'interface-styles'
[System.IO.Directory]::CreateDirectory($packageDirectory) | Out-Null
[System.IO.Directory]::CreateDirectory((Split-Path -Parent $CatalogAsset)) | Out-Null

function ConvertTo-Utf8Bytes([object]$Value) {
    return $utf8NoBom.GetBytes(($Value | ConvertTo-Json -Depth 8 -Compress))
}

function Get-Sha256Hex([byte[]]$Bytes) {
    return [Convert]::ToHexString([System.Security.Cryptography.SHA256]::HashData($Bytes)).ToLowerInvariant()
}

function Format-Argb([long]$Color, [int]$Alpha = -1) {
    $a = if ($Alpha -ge 0) { $Alpha } else { [int](($Color -shr 24) -band 0xff) }
    $r = [int](($Color -shr 16) -band 0xff)
    $g = [int](($Color -shr 8) -band 0xff)
    $b = [int]($Color -band 0xff)
    return '#{0:X2}{1:X2}{2:X2}{3:X2}' -f $a, $r, $g, $b
}

function Mix-Argb([long]$From, [long]$To, [double]$Ratio, [int]$Alpha = 255) {
    $ratio = [Math]::Clamp($Ratio, 0.0, 1.0)
    $r = [int][Math]::Round((($From -shr 16) -band 0xff) * (1.0 - $ratio) + (($To -shr 16) -band 0xff) * $ratio)
    $g = [int][Math]::Round((($From -shr 8) -band 0xff) * (1.0 - $ratio) + (($To -shr 8) -band 0xff) * $ratio)
    $b = [int][Math]::Round(($From -band 0xff) * (1.0 - $ratio) + ($To -band 0xff) * $ratio)
    return '#{0:X2}{1:X2}{2:X2}{3:X2}' -f $Alpha, $r, $g, $b
}

function New-StylePalette([long]$Accent, [bool]$Dark) {
    $white = 0xffffffffL
    $black = 0xff000000L
    $warm = 0xffff9b63L
    $cool = 0xff82d8e3L
    if ($Dark) {
        return [ordered]@{
            background = Mix-Argb $black $Accent 0.12
            backgroundAlt = Mix-Argb 0xff080b10L $Accent 0.22
            surface = Mix-Argb 0xff14191eL $Accent 0.28
            surfaceAlt = Mix-Argb 0xff0d1115L $Accent 0.20
            primary = Mix-Argb $Accent $white 0.38
            secondary = Mix-Argb $Accent $warm 0.54
            outline = Mix-Argb $Accent $cool 0.48
            highlight = '#FFF4FAFF'
            shadow = '#FF000000'
            muted = Mix-Argb 0xff35424bL $Accent 0.26
            content = '#FFE8F2F7'
        }
    }
    return [ordered]@{
        background = Mix-Argb $white $Accent 0.10
        backgroundAlt = Mix-Argb $white $Accent 0.22
        surface = Mix-Argb $white $Accent 0.04
        surfaceAlt = Mix-Argb $white $Accent 0.15
        primary = Format-Argb $Accent
        secondary = Mix-Argb $Accent 0xff9e4e2aL 0.46
        outline = Mix-Argb $Accent 0xff5d6670L 0.52
        highlight = '#FFFFFFFF'
        shadow = Mix-Argb $Accent $black 0.64
        muted = Mix-Argb $white $Accent 0.34
        content = Mix-Argb $black $Accent 0.20
    }
}

function New-Motif(
    [string]$Type,
    [string]$Color,
    [double]$X,
    [double]$Y,
    [double]$Width,
    [double]$Height,
    [double]$Alpha,
    [double]$StrokeDp,
    [int]$RepeatX,
    [int]$RepeatY,
    [double]$DriftX,
    [double]$DriftY
) {
    return [ordered]@{
        type = $Type
        color = $Color
        x = $X
        y = $Y
        width = $Width
        height = $Height
        alpha = $Alpha
        strokeDp = $StrokeDp
        repeatX = $RepeatX
        repeatY = $RepeatY
        driftX = $DriftX
        driftY = $DriftY
    }
}

function New-ThemeResource([object]$Style, [int]$StyleIndex) {
    $forceDark = ($Style.engine -eq 'rain' -and $Style.variant -in @('heavy_rain', 'thunderstorm')) -or
        ($Style.engine -eq 'pixel' -and $Style.variant -eq 'cyber_hacker')
    $lightPalette = New-StylePalette $Style.accent $false
    $darkPalette = New-StylePalette $Style.accent $true
    if ($Style.id -eq 'kernel-elite') {
        $lightPalette = [ordered]@{
            background = '#FFF4F8FA'
            backgroundAlt = '#FFFFFFFF'
            surface = '#FFFFFFFF'
            surfaceAlt = '#FFE5EEF1'
            primary = '#FF006A70'
            secondary = '#FF8A5700'
            outline = '#FF587177'
            highlight = '#FFFFFFFF'
            shadow = '#FF203038'
            muted = '#FF53676E'
            content = '#FF132329'
        }
        $darkPalette = [ordered]@{
            background = '#FF10131A'
            backgroundAlt = '#FF0B0E15'
            surface = '#FF191C23'
            surfaceAlt = '#FF272A31'
            primary = '#FF00DCE6'
            secondary = '#FFFFB300'
            outline = '#FF3A494B'
            highlight = '#FFE0FDFF'
            shadow = '#FF080D13'
            muted = '#FF849495'
            content = '#FFE0E2EC'
        }
    }
    $motifs = @()
    $scene = [ordered]@{
        cycleMillis = 10000
        primaryCount = 24
        secondaryCount = 8
        speed = 1.0
        angle = 0.0
        minLengthDp = 2.0
        maxLengthDp = 8.0
        minStrokeDp = 0.5
        maxStrokeDp = 1.2
        minAlpha = 0.16
        maxAlpha = 0.48
        gridDp = 18.0
        clearOnCycle = $false
        lightning = $false
        motifs = @()
    }

    if ($Style.engine -eq 'rain') {
        $rainSpecs = @{
            light_rain = @(68, 6, 15000, 8.0, 17.0, 0.38, 0.72, 0.08, 0.18, 0.42)
            medium_rain = @(104, 10, 11000, 12.0, 24.0, 0.46, 0.90, 0.13, 0.18, 0.48)
            heavy_rain = @(142, 15, 8500, 18.0, 34.0, 0.54, 1.08, 0.20, 0.20, 0.54)
            thunderstorm = @(168, 18, 7200, 20.0, 38.0, 0.58, 1.18, 0.24, 0.22, 0.58)
            after_rain = @(58, 8, 14000, 8.0, 18.0, 0.38, 0.76, 0.07, 0.16, 0.38)
        }
        $spec = $rainSpecs[$Style.variant]
        $scene.primaryCount = $spec[0]
        $scene.secondaryCount = $spec[1]
        $scene.cycleMillis = $spec[2]
        $scene.minLengthDp = $spec[3]
        $scene.maxLengthDp = $spec[4]
        $scene.minStrokeDp = $spec[5]
        $scene.maxStrokeDp = $spec[6]
        $scene.angle = $spec[7]
        $scene.minAlpha = $spec[8]
        $scene.maxAlpha = $spec[9]
        $scene.speed = 1.0 + (($StyleIndex % 5) * 0.12)
        $scene.clearOnCycle = $Style.variant -eq 'after_rain'
        $scene.lightning = $Style.variant -eq 'thunderstorm'
        $motifs = @(
            (New-Motif 'line' 'muted' 0.02 0.18 0.18 0.02 0.16 0.7 4 3 0.02 0.0),
            (New-Motif 'circle' 'highlight' 0.12 0.72 0.012 0.012 0.18 0.5 6 2 -0.01 0.0)
        )
    } elseif ($Style.engine -eq 'pixel') {
        $seed = $StyleIndex + 1
        $scene.cycleMillis = 7000 + (($seed % 6) * 1300)
        $scene.primaryCount = 18 + ($seed % 9)
        $scene.secondaryCount = 5 + ($seed % 7)
        $scene.speed = 0.55 + (($seed % 5) * 0.18)
        $scene.angle = (($seed % 5) - 2) * 0.04
        $scene.gridDp = 12.0 + (($seed % 6) * 2.0)
        $motifs = @(
            (New-Motif 'rect' 'primary' (0.03 + (($seed % 4) * 0.025)) (0.08 + (($seed % 5) * 0.035)) 0.025 0.008 0.34 1.0 (3 + ($seed % 5)) (2 + ($seed % 3)) 0.025 0.0),
            (New-Motif 'rect' 'secondary' (0.11 + (($seed % 3) * 0.04)) (0.48 + (($seed % 4) * 0.055)) 0.014 0.014 0.42 1.0 (4 + ($seed % 4)) 2 -0.018 0.0),
            (New-Motif 'line' 'outline' 0.04 (0.24 + (($seed % 6) * 0.045)) 0.10 (0.02 + (($seed % 3) * 0.02)) 0.26 (0.6 + (($seed % 4) * 0.2)) (2 + ($seed % 4)) (2 + ($seed % 2)) 0.012 -0.008),
            (New-Motif 'circle' 'highlight' (0.16 + (($seed % 5) * 0.035)) (0.18 + (($seed % 4) * 0.06)) 0.010 0.010 0.30 0.5 (3 + ($seed % 6)) (2 + ($seed % 3)) -0.010 0.012)
        )
    } elseif ($Style.engine -eq 'alpha') {
        $scene.cycleMillis = 16000
        $scene.primaryCount = 0
        $scene.secondaryCount = 0
        $scene.speed = 0.0
        $scene.gridDp = 24.0
        $motifs = @(
            (New-Motif 'line' 'outline' 0.02 0.16 0.20 0.01 0.16 0.5 4 2 0.0 0.0),
            (New-Motif 'circle' 'primary' 0.08 0.72 0.012 0.012 0.22 0.5 5 2 0.0 0.0)
        )
    }
    $scene.motifs = @($motifs)

    $isPixel = $Style.engine -eq 'pixel'
    $isGlass = $Style.engine -eq 'liquid_glass'
    $isElite = $Style.id -eq 'kernel-elite'
    return [ordered]@{
        schema = 'io.github.fixz.apkesu.interface-style-theme'
        version = 3
        engine = $Style.engine
        variant = $Style.variant
        accent = $Style.accent
        forceDark = $forceDark
        palette = [ordered]@{ light = $lightPalette; dark = $darkPalette }
        scene = $scene
        chrome = [ordered]@{
            cardAlpha = if ($isGlass) { 0.54 } elseif ($isPixel) { 0.94 } elseif ($isElite) { 0.96 } else { 0.76 }
            borderAlpha = if ($isGlass) { 0.70 } elseif ($isPixel) { 0.72 } elseif ($isElite) { 0.72 } else { 0.60 }
            cornerDp = if ($isGlass) { 20.0 } elseif ($isPixel) { 0.0 } elseif ($isElite) { 8.0 } else { 14.0 }
            unitDp = if ($isPixel) { 2.0 } else { 1.5 }
            topBarAlpha = if ($isGlass) { 0.18 } elseif ($isElite) { 0.92 } else { 0.62 }
            navigationAlpha = if ($isGlass) { 0.54 } elseif ($isPixel) { 0.94 } elseif ($isElite) { 0.96 } else { 0.62 }
        }
        glass = [ordered]@{
            surfaceAlpha = if ($isGlass) { 0.54 } elseif ($isElite) { 0.96 } else { 0.70 }
            blurDp = if ($isGlass) { 18.0 } else { 12.0 }
            strokeAlpha = if ($isGlass) { 0.70 } elseif ($isElite) { 0.72 } else { 0.55 }
            refraction = $isGlass
            refractionHeightDp = if ($isGlass) { 16.0 } else { 0.0 }
            refractionAmountDp = if ($isGlass) { 9.0 } else { 0.0 }
            chromaticAberration = if ($isGlass) { 0.18 } else { 0.0 }
        }
    }
}

function Write-ZipEntry(
    [System.IO.Compression.ZipArchive]$Archive,
    [string]$Name,
    [byte[]]$Bytes
) {
    $entry = $Archive.CreateEntry($Name, [System.IO.Compression.CompressionLevel]::Optimal)
    $entry.LastWriteTime = $zipTimestamp
    $stream = $entry.Open()
    try {
        $stream.Write($Bytes, 0, $Bytes.Length)
    } finally {
        $stream.Dispose()
    }
}

$styles = @(
    [ordered]@{ id='liquid-glass'; name='毛玻璃 / Frosted glass'; summary='半透明毛玻璃界面'; engine='liquid_glass'; variant=$null; accent=4283987338L },
    [ordered]@{ id='kernel-elite'; name='内核精英 / Kernel Elite'; summary='终端 HUD 风格的内核、授权和模块管理界面，支持日间与夜间调色板。'; engine='alpha'; variant=$null; accent=4278258918L },
    [ordered]@{ id='season-spring'; name='春日 / Spring'; summary='四季主题：春日'; engine='snow'; variant='spring'; accent=4283404098L },
    [ordered]@{ id='season-summer'; name='盛夏 / Summer'; summary='四季主题：盛夏'; engine='snow'; variant='summer'; accent=4279663744L },
    [ordered]@{ id='season-autumn'; name='金秋 / Autumn'; summary='四季主题：金秋'; engine='snow'; variant='autumn'; accent=4288309803L },
    [ordered]@{ id='season-winter'; name='冬雪 / Winter'; summary='四季主题：冬雪'; engine='snow'; variant='winter'; accent=4280848006L },
    [ordered]@{ id='rain-light'; name='细雨 / Light rain'; summary='雨境：细雨'; engine='rain'; variant='light_rain'; accent=4284380326L },
    [ordered]@{ id='rain-medium'; name='中雨 / Medium rain'; summary='雨境：中雨'; engine='rain'; variant='medium_rain'; accent=4283397015L },
    [ordered]@{ id='rain-heavy'; name='骤雨 / Heavy rain'; summary='雨境：骤雨'; engine='rain'; variant='heavy_rain'; accent=4282087023L },
    [ordered]@{ id='rain-thunderstorm'; name='雷暴 / Thunderstorm'; summary='雨境：雷暴'; engine='rain'; variant='thunderstorm'; accent=4284907432L },
    [ordered]@{ id='rain-after'; name='雨后 / After rain'; summary='雨境：雨后'; engine='rain'; variant='after_rain'; accent=4285631643L },
    [ordered]@{ id='pixel-classic-handheld'; name='经典掌机 / Classic handheld'; summary='像素风：经典掌机'; engine='pixel'; variant='classic_handheld'; accent=4283783999L },
    [ordered]@{ id='pixel-neon-arcade'; name='霓虹街机 / Neon arcade'; summary='像素风：霓虹街机'; engine='pixel'; variant='neon_arcade'; accent=4278222715L },
    [ordered]@{ id='pixel-pastoral-fields'; name='田园原野 / Pastoral fields'; summary='像素风：田园原野'; engine='pixel'; variant='pastoral_fields'; accent=4284380752L },
    [ordered]@{ id='pixel-star-voyage'; name='星际航行 / Star voyage'; summary='像素风：星际航行'; engine='pixel'; variant='star_voyage'; accent=4283595463L },
    [ordered]@{ id='pixel-ink-jade'; name='墨玉 / Ink jade'; summary='像素风：墨玉'; engine='pixel'; variant='ink_jade'; accent=4282216542L },
    [ordered]@{ id='pixel-rust-wasteland'; name='锈蚀荒原 / Rust wasteland'; summary='像素风：锈蚀荒原'; engine='pixel'; variant='rust_wasteland'; accent=4286730296L },
    [ordered]@{ id='pixel-ocean-depths'; name='深海 / Ocean depths'; summary='像素风：深海'; engine='pixel'; variant='ocean_depths'; accent=4279696010L },
    [ordered]@{ id='pixel-cyber-hacker'; name='赛博黑客 / Cyber hacker'; summary='像素风：赛博黑客'; engine='pixel'; variant='cyber_hacker'; accent=4288371967L },
    [ordered]@{ id='pixel-three-kingdoms'; name='三国 / Three kingdoms'; summary='像素风：三国'; engine='pixel'; variant='three_kingdoms'; accent=4284242255L },
    [ordered]@{ id='pixel-bianliang-market'; name='汴梁市集 / Bianliang market'; summary='像素风：汴梁市集'; engine='pixel'; variant='bianliang_market'; accent=4285744527L },
    [ordered]@{ id='pixel-fishing-harbor'; name='渔港 / Fishing harbor'; summary='像素风：渔港'; engine='pixel'; variant='fishing_harbor'; accent=4285093797L },
    [ordered]@{ id='pixel-tribal-jungle'; name='部落丛林 / Tribal jungle'; summary='像素风：部落丛林'; engine='pixel'; variant='tribal_jungle'; accent=4283592005L },
    [ordered]@{ id='pixel-lava-valley'; name='熔岩谷 / Lava valley'; summary='像素风：熔岩谷'; engine='pixel'; variant='lava_valley'; accent=4290266156L },
    [ordered]@{ id='pixel-dunhuang-desert'; name='敦煌沙海 / Dunhuang desert'; summary='像素风：敦煌沙海'; engine='pixel'; variant='dunhuang_desert'; accent=4286278799L },
    [ordered]@{ id='pixel-viking-snowfield'; name='维京雪原 / Viking snowfield'; summary='像素风：维京雪原'; engine='pixel'; variant='viking_snowfield'; accent=4283395978L },
    [ordered]@{ id='pixel-jiangnan-watertown'; name='江南水乡 / Jiangnan watertown'; summary='像素风：江南水乡'; engine='pixel'; variant='jiangnan_watertown'; accent=4283987311L },
    [ordered]@{ id='pixel-cloud-town'; name='云上小镇 / Cloud town'; summary='像素风：云上小镇'; engine='pixel'; variant='cloud_town'; accent=4285308569L }
)

$styleIndex = 0
$catalogStyles = foreach ($style in $styles) {
    if ($style.engine -eq 'snow') {
        $resourceName = 'wallpaper'
        $resourcePath = 'wallpaper.jpg'
        $resourceMime = 'image/jpeg'
        $sourcePath = Join-Path (Join-Path $SourceDirectory $style.id) $resourcePath
        if (-not [System.IO.File]::Exists($sourcePath)) {
            throw "Missing external wallpaper source: $sourcePath"
        }
        $resourceBytes = [System.IO.File]::ReadAllBytes($sourcePath)
    } else {
        $resourceName = 'theme'
        $resourcePath = 'theme.json'
        $resourceMime = 'application/json'
        $resourceBytes = ConvertTo-Utf8Bytes (New-ThemeResource $style $styleIndex)
    }
    $resource = [ordered]@{
        name = $resourceName
        path = $resourcePath
        mimeType = $resourceMime
        sha256 = Get-Sha256Hex $resourceBytes
        sizeBytes = $resourceBytes.LongLength
    }
    $manifestBytes = ConvertTo-Utf8Bytes ([ordered]@{
        schema = 'io.github.fixz.apkesu.interface-style-bundle'
        version = 3
        id = $style.id
        engine = $style.engine
        variant = $style.variant
        resources = @($resource)
    })
    $packagePath = Join-Path $packageDirectory "$($style.id).ksstyle"
    $fileStream = [System.IO.File]::Open(
        $packagePath,
        [System.IO.FileMode]::Create,
        [System.IO.FileAccess]::ReadWrite,
        [System.IO.FileShare]::None
    )
    try {
        $archive = [System.IO.Compression.ZipArchive]::new(
            $fileStream,
            [System.IO.Compression.ZipArchiveMode]::Create,
            $true
        )
        try {
            Write-ZipEntry $archive 'manifest.json' $manifestBytes
            Write-ZipEntry $archive $resourcePath $resourceBytes
        } finally {
            $archive.Dispose()
        }
    } finally {
        $fileStream.Dispose()
    }
    $bytes = [System.IO.File]::ReadAllBytes($packagePath)
    $sha256 = Get-Sha256Hex $bytes
    [ordered]@{
        id = $style.id
        name = $style.name
        summary = $style.summary
        engine = $style.engine
        variant = $style.variant
        version = 3
        downloadUrl = "$repositoryBase/packages/$($style.id).ksstyle"
        sha256 = $sha256
        sizeBytes = $bytes.LongLength
        accent = $style.accent
    }
    $styleIndex += 1
}

$catalog = [ordered]@{
    schema = 'io.github.fixz.apkesu.interface-style-catalog'
    version = 3
    generatedAt = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
    styles = @($catalogStyles)
}
$catalogJson = ($catalog | ConvertTo-Json -Depth 6) -replace "`r`n", "`n"
$catalogPath = Join-Path $catalogDirectory 'catalog-v1.json'
[System.IO.File]::WriteAllText($catalogPath, $catalogJson, $utf8NoBom)
[System.IO.File]::WriteAllText($CatalogAsset, $catalogJson, $utf8NoBom)

$readme = @'
# ApkeSU Interface Styles

This directory is generated by `scripts/generate-interface-style-store.ps1` in the ApkeSU source tree.

Each `.ksstyle` file is a data-only ZIP bundle. Four-season bundles contain their wallpaper. Rain, pixel, and frosted-glass bundles contain their full palette, scene, animation, motif, chrome, and material parameters in a strict v3 declarative theme resource. Packages cannot contain scripts, DEX, native libraries, or arbitrary Web content. The Manager verifies the signed catalog, fixed GitHub URL, package and resource SHA-256 values, byte limits, ZIP entry allowlist, exact schema fields, numeric limits, engine, and variant before atomically installing a style.
'@
$readme = $readme -replace "`r`n", "`n"
[System.IO.File]::WriteAllText((Join-Path $catalogDirectory 'README.md'), $readme, $utf8NoBom)
Write-Host "Generated $($styles.Count) interface style packages at $catalogDirectory"
