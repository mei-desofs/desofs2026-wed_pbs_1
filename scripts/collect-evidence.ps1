param(
    [string]$ArtifactsDir = "downloaded-artifacts",
    [string]$EvidenceDir = "Deliverables\Phase 2\Evidence",
    [switch]$IncludeUnmatched
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$sourceRoot = if ([System.IO.Path]::IsPathRooted($ArtifactsDir)) {
    $ArtifactsDir
} else {
    Join-Path $repoRoot $ArtifactsDir
}
$targetRoot = Join-Path $repoRoot $EvidenceDir

if (-not (Test-Path -LiteralPath $sourceRoot)) {
    throw "Artifacts directory not found: $sourceRoot. Download GitHub Actions artifacts into '$ArtifactsDir' first."
}

$destinations = @(
    "asvs",
    "dast",
    "pipelines",
    "sast",
    "sca",
    "secret-scanning",
    "testing"
)

foreach ($destination in $destinations) {
    New-Item -ItemType Directory -Force -Path (Join-Path $targetRoot $destination) | Out-Null
}

$rules = @(
    @{ Destination = "testing"; Patterns = @("*surefire*", "*junit*", "*jacoco*", "*pit*", "*mutation*", "*iast-runtime*", "*runtime-security*") },
    @{ Destination = "sast"; Patterns = @("*spotbugs*", "*codeql*", "*sast*") },
    @{ Destination = "sca"; Patterns = @("*dependency-check*", "*cyclonedx*", "*sbom*", "*sca*", "*bom*") },
    @{ Destination = "secret-scanning"; Patterns = @("*gitleaks*", "*secret*") },
    @{ Destination = "dast"; Patterns = @("*zap*", "*dast*") },
    @{ Destination = "pipelines"; Patterns = @("*workflow*", "*summary*", "*actions*", "*pipeline*", "*screenshot*", "*job*") },
    @{ Destination = "asvs"; Patterns = @("*asvs*") }
)

function Copy-ArtifactDirectory {
    param(
        [System.IO.DirectoryInfo]$Artifact,
        [string]$Destination
    )

    $destinationPath = Join-Path (Join-Path $targetRoot $Destination) $Artifact.Name
    Copy-Item -LiteralPath $Artifact.FullName -Destination $destinationPath -Recurse -Force
    Write-Host "Copied '$($Artifact.Name)' -> '$Destination'"
}

function Get-EvidenceDestination {
    param([string]$Name)

    foreach ($rule in $rules) {
        foreach ($pattern in $rule.Patterns) {
            if ($Name -like $pattern) {
                return $rule.Destination
            }
        }
    }

    return $null
}

function Copy-ArtifactFile {
    param(
        [System.IO.FileInfo]$Artifact,
        [string]$Destination
    )

    $destinationPath = Join-Path (Join-Path $targetRoot $Destination) $Artifact.Name
    Copy-Item -LiteralPath $Artifact.FullName -Destination $destinationPath -Force
    Write-Host "Copied '$($Artifact.Name)' -> '$Destination'"
}

$zipFiles = Get-ChildItem -LiteralPath $sourceRoot -File -Filter "*.zip"
foreach ($zipFile in $zipFiles) {
    $expandedPath = Join-Path $sourceRoot ([System.IO.Path]::GetFileNameWithoutExtension($zipFile.Name))
    if (-not (Test-Path -LiteralPath $expandedPath)) {
        New-Item -ItemType Directory -Force -Path $expandedPath | Out-Null
        Expand-Archive -LiteralPath $zipFile.FullName -DestinationPath $expandedPath -Force
        Write-Host "Expanded '$($zipFile.Name)'"
    }
}

$artifactDirs = Get-ChildItem -LiteralPath $sourceRoot -Directory
$copied = New-Object System.Collections.Generic.HashSet[string]

foreach ($artifact in $artifactDirs) {
    $destination = Get-EvidenceDestination -Name $artifact.Name
    if ($destination -ne $null) {
        Copy-ArtifactDirectory -Artifact $artifact -Destination $destination
        [void]$copied.Add($artifact.FullName)
    } elseif ($IncludeUnmatched) {
        Copy-ArtifactDirectory -Artifact $artifact -Destination "pipelines"
        [void]$copied.Add($artifact.FullName)
        Write-Warning "Artifact '$($artifact.Name)' did not match a specific evidence area and was copied to pipelines."
    } else {
        Write-Host "Skipped unmatched directory '$($artifact.Name)'"
    }
}

$artifactFiles = Get-ChildItem -LiteralPath $sourceRoot -File | Where-Object { $_.Extension -ne ".zip" }
foreach ($artifact in $artifactFiles) {
    $destination = Get-EvidenceDestination -Name $artifact.Name
    if ($destination -ne $null) {
        Copy-ArtifactFile -Artifact $artifact -Destination $destination
    } elseif ($IncludeUnmatched) {
        Copy-ArtifactFile -Artifact $artifact -Destination "pipelines"
        Write-Warning "Artifact '$($artifact.Name)' did not match a specific evidence area and was copied to pipelines."
    } else {
        Write-Host "Skipped unmatched file '$($artifact.Name)'"
    }
}

Write-Host ""
Write-Host "Evidence collection complete."
Write-Host "Source: $sourceRoot"
Write-Host "Target: $targetRoot"
