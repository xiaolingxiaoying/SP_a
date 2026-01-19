$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

$javafxLib = $env:JAVA_FX_LIB
if ([string]::IsNullOrWhiteSpace($javafxLib)) {
    $javafxLib = "C:\Program Files\javafx-sdk-21.0.9\lib"
}

if (-not (Test-Path $javafxLib)) {
    Write-Error "JavaFX lib directory not found: $javafxLib"
    exit 1
}

if (-not (Test-Path "out")) {
    New-Item -ItemType Directory -Path "out" | Out-Null
}

Write-Host "Using JavaFX lib: $javafxLib"
Write-Host "Compiling Java sources..."

javac -encoding UTF-8 `
  --module-path "$javafxLib" `
  --add-modules javafx.controls `
  -d out `
  src\spiderfx\model\*.java `
  src\spiderfx\view\*.java `
  src\spiderfx\controller\*.java `
  src\spiderfx\Main.java

if ($LASTEXITCODE -ne 0) {
    Write-Error "Compilation failed, exit code: $LASTEXITCODE"
    exit $LASTEXITCODE
}

if (Test-Path "src\spiderfx\cards") {
    Write-Host "Syncing card resources to out\cards ..."
    $destCards = "out\cards"
    if (Test-Path $destCards) {
        Remove-Item -Recurse -Force $destCards
    }
    Copy-Item "src\spiderfx\cards" $destCards -Recurse
}

# 复制 CSS 和其他资源到 out 目录以供 getResource 使用
Write-Host "Syncing resources to out..."
Copy-Item "src\spiderfx\spider.css" "out\spiderfx\" -Force

Write-Host "Compilation succeeded. Launching game..."

java `
  --module-path "$javafxLib" `
  --add-modules javafx.controls `
  -cp out spiderfx.Main
