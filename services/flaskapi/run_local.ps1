$ErrorActionPreference = "Stop"
Push-Location $PSScriptRoot

# 1) .env 로드
if (Test-Path ".env") {
  Get-Content ".env" | ForEach-Object {
    if ($_ -match "^\s*#") { return }
    if ($_ -match "^\s*$") { return }
    $k,$v = $_.Split("=",2)
    [System.Environment]::SetEnvironmentVariable($k.Trim(), $v.Trim())
  }
}

# 2) 가상환경 준비
if (!(Test-Path ".venv")) {
  py -3 -m venv .venv
}

# 3) 활성화
$venvActivate = ".\.venv\Scripts\Activate.ps1"
& $venvActivate

# 4) 의존성 설치
python -m pip install --upgrade pip
pip install -r requirements.txt
# .env 사용을 위해 (없다면) python-dotenv도 설치
pip install python-dotenv

# 5) 실행
$port = $env:PORT; if (-not $port) { $port = 7001 }
python main.py --port $port
Pop-Location
