# Scans the repository for Java top-level types missing an @author Javadoc tag.
# Usage: .\scripts\check-javadoc-author.ps1 -Path . -Verbose
param(
    [string]$Path = '.',
    [string[]]$ExcludeDirs = @('target', '.git')
)

Write-Host "Scanning Java files under $Path for missing @author tag..."

$files = Get-ChildItem -Path $Path -Recurse -Include *.java | Where-Object {
    foreach ($d in $ExcludeDirs) { if ($_.FullName -like "*\\$d\\*") { return $false } }
    return $true
}

$missing = @()

foreach ($f in $files) {
    $content = Get-Content -Raw -Path $f.FullName
    # Check only for top-level Javadoc before the first 'package' or 'import' or 'public' declaration of a type
    if ($content -match "(?s)/\*\*.*?\*/\s*?(public|class|interface|enum|@interface)") {
        if ($content -notmatch "@author") {
            $missing += $f.FullName
        }
    } else {
        # No Javadoc present at top of file
        $missing += $f.FullName
    }
}

if ($missing.Count -gt 0) {
    Write-Host "Files missing @author in Javadoc:`n" -ForegroundColor Yellow
    $missing | ForEach-Object { Write-Host $_ }
    exit 1
} else {
    Write-Host "All Java files contain @author in top-level Javadoc." -ForegroundColor Green
    exit 0
}

