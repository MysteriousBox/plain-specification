# Installer for Git pre-commit hook that runs the repository Javadoc author check.
# Run from repo root in PowerShell: .\scripts\install-git-hook.ps1

$repoRoot = (Get-Location).Path
$gitHooksDir = Join-Path $repoRoot ".git\hooks"
$preCommitPath = Join-Path $gitHooksDir "pre-commit"
$scriptToRun = "$(Join-Path $repoRoot 'scripts\check-javadoc-author.ps1')"

if (-not (Test-Path $gitHooksDir)) {
    Write-Host ".git/hooks not found. Are you in the repository root and is this a git repo?" -ForegroundColor Red
    exit 1
}

$hookContent = "#!/bin/sh\n# Auto-generated pre-commit hook to run Javadoc author check\nexec powershell -NoProfile -ExecutionPolicy Bypass -File \"$scriptToRun\"\n"

Set-Content -Path $preCommitPath -Value $hookContent -Encoding Ascii
# Ensure executable bit (Git on Windows respects the file but ensure by using Git for Windows)
try {
    & git update-index --add --chmod=+x $preCommitPath 2>$null
} catch {
    # ignore if git not available in PATH
}

Write-Host "Installed pre-commit hook at $preCommitPath" -ForegroundColor Green
Write-Host "Note: You may need to run this script with repository root as current directory." -ForegroundColor Yellow

