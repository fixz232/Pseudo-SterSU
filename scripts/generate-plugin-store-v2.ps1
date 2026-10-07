$ErrorActionPreference = 'Stop'
# V1 remains immutable for older Manager builds, which require exactly nine entries.

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')
$v1Path = Join-Path $repoRoot 'plugin-store/catalog-v1.json'
$packagePath = Join-Path $repoRoot 'plugin-store/packages/pathmask-lkm.ksplugin'
$package = Get-Content -LiteralPath $packagePath -Raw -Encoding UTF8 | ConvertFrom-Json -AsHashtable
$v1 = Get-Content -LiteralPath $v1Path -Raw -Encoding UTF8 | ConvertFrom-Json -AsHashtable

if ($v1.plugins.Count -ne 9 -or $v1.plugins.id -contains $package.id) {
    throw 'Unexpected v1 plugin catalog; refusing to replace its compatibility baseline.'
}

$bytes = [IO.File]::ReadAllBytes($packagePath)
$sha256 = [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($bytes)).ToLowerInvariant()
$entry = [ordered]@{
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
    sha256 = $sha256
    sizeBytes = $bytes.Length
}
$catalog = [ordered]@{
    schema = $v1.schema
    version = $v1.version
    generatedAt = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
    plugins = @($v1.plugins) + @($entry)
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
