$ErrorActionPreference = 'Stop'
# V1 remains immutable for older Manager builds, which require exactly nine entries.

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')
$activeIds = @(
    'rescue-protection'
    'device-identity'
    'remote-management-suite'
    'app-id-manager'
    'app-freeze'
    'pathmask-lkm'
)

$entries = foreach ($id in $activeIds) {
    $packagePath = Join-Path $repoRoot "plugin-store/packages/$id.ksplugin"
    $package = Get-Content -LiteralPath $packagePath -Raw -Encoding UTF8 | ConvertFrom-Json -AsHashtable
    $expectedUrl = "https://raw.githubusercontent.com/fixz232/SterSU-ThemeStore/main/plugin-store/packages/$id.ksplugin"
    if ($package.id -ne $id -or $package.downloadUrl -ne $expectedUrl) {
        throw "Invalid package ID or download URL in $packagePath"
    }
    $bytes = [IO.File]::ReadAllBytes($packagePath)
    [ordered]@{
        id = $package.id
        version = $package.version
        name = $package.name
        summary = $package.summary
        description = $package.description
        instructions = @($package.instructions)
        slots = @($package.slots)
        minManagerVersionCode = $package.minManagerVersionCode
        minKsudVersionCode = $package.minKsudVersionCode
        downloadUrl = $package.downloadUrl
        sha256 = [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($bytes)).ToLowerInvariant()
        sizeBytes = $bytes.Length
    }
}
$catalog = [ordered]@{
    schema = 'io.github.fixz.apkesu.plugin-catalog'
    version = 1
    generatedAt = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
    plugins = @($entries)
}
$json = ($catalog | ConvertTo-Json -Depth 10).Replace("`r`n", "`n") + "`n"
$encoding = [Text.UTF8Encoding]::new($false)
foreach ($destination in @(
    (Join-Path $repoRoot 'plugin-store/catalog-v2.json'),
    (Join-Path $repoRoot 'manager/app/src/main/assets/plugin-store/catalog-v2.json')
)) {
    [IO.File]::WriteAllText($destination, $json, $encoding)
}
Write-Output "Generated v2 plugin catalog with $($catalog.plugins.Count) entries."
