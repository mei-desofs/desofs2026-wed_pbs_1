param(
    [string]$ArtifactsDir = "downloaded-artifacts",
    [string]$EvidenceDir = "Deliverables\Phase 2\Evidence"
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$sourceRoot = Join-Path $repoRoot $ArtifactsDir
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
    @{ Destination = "sca"; Patterns = @("*dependency-check*", "*cyclonedx*", "*sbom*", "*sca*") },
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

$artifactDirs = Get-ChildItem -LiteralPath $sourceRoot -Directory
$copied = New-Object System.Collections.Generic.HashSet[string]

foreach ($artifact in $artifactDirs) {
    foreach ($rule in $rules) {
        $matched = $false
        foreach ($pattern in $rule.Patterns) {
            if ($artifact.Name -like $pattern) {
                $matched = $true
                break
            }
        }

        if ($matched) {
            Copy-ArtifactDirectory -Artifact $artifact -Destination $rule.Destination
            [void]$copied.Add($artifact.FullName)
            break
        }
    }
}

$uncategorizedRoot = Join-Path $targetRoot "pipelines"
foreach ($artifact in $artifactDirs) {
    if (-not $copied.Contains($artifact.FullName)) {
        Copy-ArtifactDirectory -Artifact $artifact -Destination "pipelines"
        Write-Warning "Artifact '$($artifact.Name)' did not match a specific evidence area and was copied to pipelines."
    }
}

Write-Host ""
Write-Host "Evidence collection complete."
Write-Host "Source: $sourceRoot"
Write-Host "Target: $targetRoot"
