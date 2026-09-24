$ErrorActionPreference = 'Stop'
# Run with PowerShell 7 (`pwsh`) so UTF-8 source text is parsed consistently.

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')
$assetRoot = Join-Path $repoRoot 'manager/app/src/main/assets/plugin-store'
$packageRoot = Join-Path $repoRoot 'plugin-store/packages'
New-Item -ItemType Directory -Force -Path $assetRoot, $packageRoot | Out-Null

$plugins = @(
    @{ id='rescue-protection'; name='救砖保护'; summary='备份、验证并恢复启动镜像，降低连续启动失败风险。'; description='提供启动镜像备份、深度校验、恢复事务和救援日志。所有操作仍由版本化 ksud 接口执行。'; slots=@('toolbox.rescue'); instructions=@('先确认 boot/init_boot 备份完整并保留可用恢复路径。','启用保护后重启验证；恢复操作会显示校验结果和事务状态。'); minKsud=32800 },
    @{ id='image-tools'; name='镜像工具'; summary='检查、导入和刷写受支持的 boot/init_boot 镜像。'; description='面向受支持分区的镜像检查与安全操作工具，执行前会校验路径、大小、哈希和当前模式。'; slots=@('toolbox.image-tools'); instructions=@('选择来源镜像并先运行检查。','仅对明确匹配当前设备和槽位的镜像执行操作。'); minKsud=32800 },
    @{ id='cpu-spoof'; name='CPU 自定义伪装'; summary='配置并验证持久化 CPU 型号伪装。'; description='使用受限的 ksud CPU 属性接口配置目标值，支持启动后验证、失败回滚和恢复默认。'; slots=@('toolbox.cpu-spoof'); instructions=@('先保存当前真实 CPU 值。','应用后检查验证结果；失败时使用恢复默认并重启。'); minKsud=32800 },
    @{ id='device-identity'; name='标识修改'; summary='安全修改受支持的设备标识并保留恢复备份。'; description='管理序列号、Android ID、MAC 和 OAID 等受支持标识，带格式校验、备份、读回验证和回滚。'; slots=@('toolbox.device-identity'); instructions=@('导出或确认自动备份已成功。','一次只修改一种标识，验证通过后再重启。'); minKsud=32800 },
    @{ id='graphics-renderer'; name='内置图像渲染器'; summary='在系统默认、Skia Vulkan 和 Skia OpenGL 之间切换。'; description='检测设备 Vulkan 能力和 HWUI 属性，只写入白名单属性并在报告成功前读回验证。'; slots=@('toolbox.graphics-renderer'); instructions=@('先刷新环境诊断并确认 Vulkan 能力。','应用后按提示重启，出现异常时恢复系统默认。'); minKsud=32800 },
    @{ id='ai-chat'; name='AI 聊天'; summary='使用用户配置的 HTTPS AI API 进行对话和模块分析。'; description='支持加密保存 API 配置、流式回答、附件分析和受控模块建议；不会自动执行模型生成的命令。'; slots=@('toolbox.ai-chat'); instructions=@('在 AI 设置中配置 HTTPS API 和密钥。','发送涉及 root 的建议前人工检查，工具调用必须逐项确认。'); minKsud=32800 },
    @{ id='remote-management-suite'; name='网页管理器与隐身模式'; summary='本机网页管理器、PWA、签名鉴权和隐身模式配套套件。'; description='网页资源由 ksud 本机服务提供，默认只监听回环地址；隐身状态持久化在 /data/adb/ksu/，管理器退出后仍有效。网页管理器和隐身模式必须同时安装。'; slots=@('maintenance.web-manager','maintenance.stealth-mode'); instructions=@('首次使用先确认服务只绑定 127.0.0.1 并设置签名密钥。','开启隐身后只能通过网页密令或拨号密令关闭。','卸载前必须关闭隐身模式并停止网页服务。'); minKsud=32800 },
    @{ id='app-id-manager'; name='应用 ID 修改'; summary='查看、暂存、应用和恢复应用 SSAID。'; description='为选定应用管理 App ID，提供 XML 校验、备份、启动脚本暂存和重启后验证。'; slots=@('superuser.app-id-manager'); instructions=@('先选择目标应用并读取当前 ID。','应用修改前确认备份存在，完成后按提示重启。'); minKsud=32800 },
    @{ id='app-freeze'; name='应用 ID 冻结'; summary='冻结或解冻应用并查看当前状态。'; description='通过受限的包管理操作冻结应用，保留状态查询、失败原因和恢复入口。'; slots=@('superuser.app-freeze'); instructions=@('确认目标包名和应用名称一致。','系统关键包不要冻结；出现异常时立即解冻并重启。'); minKsud=32800 }
)

foreach ($plugin in $plugins) {
    $package = [ordered]@{
        schema='io.github.fixz.apkesu.plugin'
        version=1
        id=$plugin.id
        name=$plugin.name
        summary=$plugin.summary
        description=$plugin.description
        instructions=$plugin.instructions
        slots=$plugin.slots
        minManagerVersionCode=32800
        minKsudVersionCode=$plugin.minKsud
        downloadUrl="https://raw.githubusercontent.com/fixz232/ApkeSU-PluginStore/main/packages/$($plugin.id).ksplugin"
        sha256=''
        sizeBytes=0
    }
    $path = Join-Path $packageRoot "$($plugin.id).ksplugin"
    $package | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $path -Encoding utf8NoBOM
}

$catalogPlugins = foreach ($plugin in $plugins) {
    $path = Join-Path $packageRoot "$($plugin.id).ksplugin"
    $bytes = [IO.File]::ReadAllBytes($path)
    [ordered]@{
        id=$plugin.id
        version=1
        name=$plugin.name
        summary=$plugin.summary
        description=$plugin.description
        instructions=$plugin.instructions
        slots=$plugin.slots
        minManagerVersionCode=32800
        minKsudVersionCode=$plugin.minKsud
        downloadUrl="https://raw.githubusercontent.com/fixz232/ApkeSU-PluginStore/main/packages/$($plugin.id).ksplugin"
        sha256=([Security.Cryptography.SHA256]::HashData($bytes) | ForEach-Object { $_.ToString('x2') }) -join ''
        sizeBytes=$bytes.Length
    }
}

$catalog = [ordered]@{
    schema='io.github.fixz.apkesu.plugin-catalog'
    version=1
    generatedAt=[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
    plugins=@($catalogPlugins)
}
$catalogJson = $catalog | ConvertTo-Json -Depth 10
Set-Content -LiteralPath (Join-Path $assetRoot 'catalog-v1.json') -Value $catalogJson -Encoding utf8NoBOM
Set-Content -LiteralPath (Join-Path $repoRoot 'plugin-store/catalog-v1.json') -Value $catalogJson -Encoding utf8NoBOM
$catalogDestination = Join-Path $repoRoot 'plugin-store/catalog-v1.json'
if ((Resolve-Path (Join-Path $assetRoot 'catalog-v1.json')).Path -ne $catalogDestination) {
    Copy-Item -LiteralPath (Join-Path $assetRoot 'catalog-v1.json') -Destination $catalogDestination -Force
}

Write-Output "Generated plugin catalog and $($plugins.Count) packages."
