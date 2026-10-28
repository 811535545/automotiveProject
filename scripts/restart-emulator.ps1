<#
.SYNOPSIS
    一键重启指定 AVD 的 Android 模拟器，并等待系统开机完成。

.DESCRIPTION
    执行流程：
      1. 找到 emulator.exe / qemu-system-x86_64.exe 中命令行包含指定 AVD 名的进程；
      2. 结束进程树（优先杀 emulator.exe 父进程，/T 会一并结束 qemu 子进程）；
      3. 用 -no-snapshot-load 冷启动，避免恢复到黑屏或异常快照；
      4. 通过 adb 等待新设备上线，并等待 sys.boot_completed=1。

    只结束命令行匹配到 AVD 名的进程，不会误杀其他正在运行的模拟器。
    如果命令行匹配不到，只在“当前只有一台模拟器”时兜底结束它，否则直接报错退出。

.PARAMETER AvdName
    要重启的 AVD 名称，默认 Automotive_Portrait。

.PARAMETER BootTimeoutSeconds
    等待设备上线并完成开机的总超时，默认 240 秒。

.PARAMETER NoPause
    结束时不等待回车，方便命令行或自动化调用。

.PARAMETER CreateShortcut
    只在桌面创建快捷方式，不重启模拟器。

.EXAMPLE
    powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\restart-emulator.ps1

.EXAMPLE
    powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\restart-emulator.ps1 -AvdName Automotive_Portrait -NoPause

.EXAMPLE
    powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\restart-emulator.ps1 -CreateShortcut
#>

param(
    [string]$AvdName = "Automotive_Portrait",
    [int]$BootTimeoutSeconds = 240,
    [switch]$NoPause,
    [switch]$CreateShortcut
)

$ErrorActionPreference = "Stop"

# 让中文日志在 Git Bash、重定向输出等场景下也按 UTF-8 编码，避免乱码。
try {
    [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
    $OutputEncoding = [System.Text.Encoding]::UTF8
}
catch {
    # 某些宿主不支持修改编码，忽略即可，不影响重启流程。
}

# ---------------------------------------------------------------------------
# 输出辅助：统一带时间戳，方便判断卡在哪一步
# ---------------------------------------------------------------------------
function Write-Step([string]$Message) {
    Write-Host ("[{0}] {1}" -f (Get-Date -Format "HH:mm:ss"), $Message) -ForegroundColor Cyan
}

function Write-Ok([string]$Message) {
    Write-Host ("[{0}] {1}" -f (Get-Date -Format "HH:mm:ss"), $Message) -ForegroundColor Green
}

function Write-Warn([string]$Message) {
    Write-Host ("[{0}] {1}" -f (Get-Date -Format "HH:mm:ss"), $Message) -ForegroundColor Yellow
}

function Write-Err([string]$Message) {
    Write-Host ("[{0}] {1}" -f (Get-Date -Format "HH:mm:ss"), $Message) -ForegroundColor Red
}

# ---------------------------------------------------------------------------
# 用 Start-Process 调 taskkill，而不是直接执行原生命令。
# 原因：进程已经退出时 taskkill 会往 stderr 写“无法终止”，
# 在 $ErrorActionPreference = "Stop" 下可能被当成脚本异常。
# 这里只返回退出码，由调用方决定是否继续。
# ---------------------------------------------------------------------------
function Invoke-Taskkill([int]$ProcessId, [switch]$Tree) {
    $arguments = @("/F")
    if ($Tree) {
        $arguments += "/T"
    }
    $arguments += @("/PID", "$ProcessId")

    try {
        $result = Start-Process -FilePath "taskkill.exe" `
            -ArgumentList $arguments `
            -Wait -PassThru -WindowStyle Hidden
        return $result.ExitCode
    }
    catch {
        return -1
    }
}

# ---------------------------------------------------------------------------
# 路径探测
# 优先使用环境变量，其次使用本机常见的 D:\SDK，最后从 PATH 查找。
# 这样换机器或改 SDK 目录时，不需要改脚本主体。
# ---------------------------------------------------------------------------
function Find-FirstExisting([string[]]$Paths) {
    foreach ($path in $Paths) {
        if ($path -and (Test-Path -LiteralPath $path)) {
            return $path
        }
    }
    return $null
}

$sdkRoots = @(
    $env:ANDROID_SDK_ROOT,
    $env:ANDROID_HOME,
    "D:\SDK",
    $(if ($env:LOCALAPPDATA) { Join-Path $env:LOCALAPPDATA "Android\Sdk" })
) | Where-Object { $_ }

$emulatorCandidates = @()
foreach ($root in $sdkRoots) {
    $emulatorCandidates += (Join-Path $root "emulator\emulator.exe")
}
$emulatorFromPath = Get-Command "emulator.exe" -ErrorAction SilentlyContinue
if ($emulatorFromPath) {
    $emulatorCandidates += $emulatorFromPath.Source
}
$emulator = Find-FirstExisting $emulatorCandidates

$adbCandidates = @()
foreach ($root in $sdkRoots) {
    $adbCandidates += (Join-Path $root "platform-tools\adb.exe")
}
$adbFromPath = Get-Command "adb.exe" -ErrorAction SilentlyContinue
if ($adbFromPath) {
    $adbCandidates += $adbFromPath.Source
}
$adb = Find-FirstExisting $adbCandidates

# ---------------------------------------------------------------------------
# 在桌面创建快捷方式
# 快捷方式调用当前 PowerShell 解释器执行本脚本；脚本路径用引号包住，避免空格问题。
# ---------------------------------------------------------------------------
function New-DesktopShortcut {
    $desktop = [Environment]::GetFolderPath("Desktop")
    if (-not $desktop) {
        $desktop = Join-Path $env:USERPROFILE "Desktop"
    }

    $shortcutPath = Join-Path $desktop "重启 Automotive 模拟器.lnk"

    # 当前如果是 Windows PowerShell，$PSHOME 下就有 powershell.exe；
    # 如果是 PowerShell 7，则退回当前进程自己的可执行文件。
    $hostExe = Join-Path $PSHOME "powershell.exe"
    if (-not (Test-Path -LiteralPath $hostExe)) {
        $hostExe = (Get-Process -Id $PID).Path
    }

    $shell = New-Object -ComObject WScript.Shell
    $shortcut = $shell.CreateShortcut($shortcutPath)
    $shortcut.TargetPath = $hostExe
    $shortcut.Arguments = ('-NoProfile -ExecutionPolicy Bypass -File "{0}"' -f $PSCommandPath)
    $shortcut.WorkingDirectory = Split-Path -Parent $PSCommandPath
    $shortcut.Description = "冷启动 $AvdName 并等待开机完成"
    if ($emulator) {
        $shortcut.IconLocation = "$emulator,0"
    }
    $shortcut.Save()

    Write-Ok "快捷方式已创建：$shortcutPath"
}

# ---------------------------------------------------------------------------
# 结束目标模拟器
# 关键点：只匹配命令行里带 AVD 名的进程，避免把其他 AVD 一起杀掉。
# ---------------------------------------------------------------------------
function Stop-TargetEmulator {
    $processes = @(Get-CimInstance Win32_Process | Where-Object {
        $_.Name -in @("emulator.exe", "qemu-system-x86_64.exe") -and $_.CommandLine
    })

    if ($processes.Count -eq 0) {
        Write-Warn "没有检测到运行中的模拟器，跳过结束步骤。"
        return
    }

    # 精确匹配 "-avd <名字>"，避免 AVD 名是另一个 AVD 名前缀时误杀。
    $avdPattern = '(?:^|\s)-avd\s+"?' + [regex]::Escape($AvdName) + '(?:"|\s|$)'
    $matched = @($processes | Where-Object { $_.CommandLine -match $avdPattern })

    if ($matched.Count -eq 0) {
        # 兜底：只有一台模拟器时才结束它；有多台就停下，防止误杀别人的 AVD。
        $launchers = @($processes | Where-Object { $_.Name -eq "emulator.exe" })
        $qemus = @($processes | Where-Object { $_.Name -ne "emulator.exe" })

        if ($launchers.Count -eq 1) {
            $matched = $launchers
        }
        elseif ($launchers.Count -eq 0 -and $qemus.Count -eq 1) {
            $matched = $qemus
        }
        else {
            throw "没有找到 AVD '$AvdName' 对应的模拟器进程，且当前有多台模拟器在运行。为避免误杀，已停止。"
        }
    }

    # 优先结束 emulator.exe 父进程；taskkill /T 会连同 qemu 子进程一起结束。
    $roots = @($matched | Where-Object { $_.Name -eq "emulator.exe" })
    if ($roots.Count -eq 0) {
        $roots = $matched
    }

    foreach ($proc in $roots) {
        Write-Step ("结束进程 {0} (PID {1})" -f $proc.Name, $proc.ProcessId)
        [void](Invoke-Taskkill -ProcessId $proc.ProcessId -Tree)
    }

    # 父进程结束后，清理可能还活着的 qemu 进程（正常情况已经随进程树退出）。
    # 退出码忽略：这里的目标是“尽力清理”，后面的存活检查才是最终判据。
    foreach ($proc in $matched) {
        if (Get-Process -Id $proc.ProcessId -ErrorAction SilentlyContinue) {
            [void](Invoke-Taskkill -ProcessId $proc.ProcessId)
        }
    }

    # 最多等 15 秒，确认端口和进程都释放后再启动新实例。
    $deadline = (Get-Date).AddSeconds(15)
    while ((Get-Date) -lt $deadline) {
        $alive = @($matched | Where-Object {
            Get-Process -Id $_.ProcessId -ErrorAction SilentlyContinue
        })
        if ($alive.Count -eq 0) {
            break
        }
        Start-Sleep -Milliseconds 300
    }

    $stillAlive = @($matched | Where-Object {
        Get-Process -Id $_.ProcessId -ErrorAction SilentlyContinue
    })
    if ($stillAlive.Count -gt 0) {
        $pids = ($stillAlive | ForEach-Object { $_.ProcessId }) -join ", "
        throw "模拟器进程未能在 15 秒内退出：$pids"
    }

    Write-Ok "目标模拟器已结束。"
}

# ---------------------------------------------------------------------------
# 启动模拟器并等待开机
# -no-snapshot-load：冷启动，不恢复快照；不会清除用户数据。
# ---------------------------------------------------------------------------
function Start-Emulator {
    # 记录启动前已经在线的设备，后面只认“新出现的 serial”，
    # 避免另一台模拟器正在运行时长错设备。
    $preDevices = @()
    if ($adb) {
        $preDevices = @(& $adb devices 2>$null | Select-Object -Skip 1 | ForEach-Object {
            if ($_ -match '^(emulator-\d+)\s+device') { $matches[1] }
        })
    }

    Write-Step ("冷启动 {0}（不加载快照）" -f $AvdName)
    Start-Process -FilePath $emulator `
        -ArgumentList @("-avd", $AvdName, "-no-snapshot-load") `
        -WorkingDirectory (Split-Path -Parent $emulator) | Out-Null

    if (-not $adb) {
        Write-Warn "未找到 adb.exe，无法等待开机完成；模拟器已启动。"
        return
    }

    $deadline = (Get-Date).AddSeconds($BootTimeoutSeconds)
    $serial = $null

    Write-Step "等待新设备以 device 状态上线..."
    while ((Get-Date) -lt $deadline) {
        $devices = @(& $adb devices 2>$null | Select-Object -Skip 1 | ForEach-Object {
            if ($_ -match '^(emulator-\d+)\s+device') { $matches[1] }
        })
        $serial = $devices | Where-Object { $preDevices -notcontains $_ } | Select-Object -First 1
        if ($serial) {
            break
        }
        Start-Sleep -Seconds 2
    }

    if (-not $serial) {
        throw "模拟器在 $BootTimeoutSeconds 秒内没有以 device 状态上线。"
    }

    Write-Step "设备已上线：$serial，等待 sys.boot_completed=1..."
    while ((Get-Date) -lt $deadline) {
        $bootCompleted = (& $adb -s $serial shell getprop sys.boot_completed 2>$null | Out-String).Trim()
        if ($bootCompleted -eq "1") {
            $release = (& $adb -s $serial shell getprop ro.build.version.release 2>$null | Out-String).Trim()
            Write-Ok ("启动完成：{0}，Android {1}" -f $serial, $release)
            return
        }
        Start-Sleep -Seconds 2
    }

    throw "设备 $serial 已上线，但在 $BootTimeoutSeconds 秒内没有完成开机。"
}

# ---------------------------------------------------------------------------
# 主流程
# ---------------------------------------------------------------------------
$exitCode = 0

try {
    if (-not $emulator) {
        throw "没有找到 emulator.exe。请检查 ANDROID_SDK_ROOT / ANDROID_HOME，或修改脚本里的 D:\SDK 路径。"
    }

    Write-Step "AVD：$AvdName"
    Write-Step "emulator：$emulator"
    if ($adb) {
        Write-Step "adb：$adb"
    }
    else {
        Write-Warn "没有找到 adb.exe，将只负责启动，不等待开机。"
    }

    # 先确认 AVD 存在，避免启动到一个不存在的名字后才报错。
    $avds = @(& $emulator -list-avds 2>$null | ForEach-Object { $_.Trim() } | Where-Object { $_ })
    if ($avds -notcontains $AvdName) {
        throw "找不到 AVD '$AvdName'。当前可用 AVD：$($avds -join ', ')"
    }

    if ($CreateShortcut) {
        New-DesktopShortcut
    }
    else {
        Stop-TargetEmulator
        Start-Emulator
    }
}
catch {
    $exitCode = 1
    Write-Err ("失败：{0}" -f $_.Exception.Message)
}

# 双击快捷方式运行时保留窗口，方便看日志；命令行可加 -NoPause 跳过。
if (-not $NoPause) {
    Write-Host ""
    Read-Host "按回车退出" | Out-Null
}

exit $exitCode
