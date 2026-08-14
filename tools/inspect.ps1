#Requires -Version 7

<#
.SYNOPSIS
    Runs the IntelliJ inspections over this project without opening the IDE (#55).

.DESCRIPTION
    The findings the editor shows exist only inside the IDE - ./gradlew compileJava
    reports zero warnings, because everything the profile finds is an IntelliJ
    inspection, not a javac one. This produces the same list on the command line so
    it can be counted and driven to zero (#48).

    The inspector is the build the Gradle plugin downloaded, pinned by
    intellij.version in gradle.properties, so the result does not depend on which
    IDE happens to be installed on a given machine.

    Config and system directories are isolated under build/, because a running IDE
    holds a lock on its own - without this the run fails while IntelliJ is open on
    the same project, and it must never write to the developer's real settings.

    Costs one full indexing pass, so expect 10-20 minutes. A deliberate sweep, not
    a per-commit gate.

.EXAMPLE
    pwsh tools/inspect.ps1
#>

param(
    # Where the XML output and the two reports are written. Deliberately not
    # under build/: ./gradlew clean deletes that, and the findings list is what
    # you work from for the next hour. Gitignored instead.
    [string] $OutputDir = '.inspection',

    # Restrict the run to a subdirectory. Defaults to the production sources,
    # which is the point: .sandbox holds a whole IDE installation from
    # ./gradlew runIde, and inspecting it produced 74,803 spellcheck findings
    # against 33 in src. Pass '' to inspect the project root instead.
    [string] $Subdirectory = 'src/main',

    # Reuse the XML already in $OutputDir and only rebuild the reports.
    [switch] $ReportOnly
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot

function Resolve-Inspector {
    $props = Get-Content (Join-Path $repo 'gradle.properties')
    $version = ($props | Select-String -Pattern '^intellij\.version=(.+)$').Matches[0].Groups[1].Value

    # The Gradle transform path carries a content hash, so it is matched by shape rather than stored.
    $ide = Get-ChildItem -Path "$env:USERPROFILE\.gradle\caches\*\transforms\*\transformed\idea-$version-win" `
        -Directory -ErrorAction SilentlyContinue | Select-Object -First 1

    if (-not $ide) {
        throw "No downloaded IDE $version found. Run './gradlew compileJava' first - it fetches the platform this script inspects with."
    }

    $inspect = Join-Path $ide.FullName 'bin\inspect.bat'
    if (-not (Test-Path $inspect)) { throw "No inspect.bat in $($ide.FullName)" }

    Write-Host "Inspector: $inspect"
    return $inspect
}

function Invoke-Inspector([string] $inspect, [string] $outPath) {
    # Beside the output and for the same reason: an inspector still running holds
    # this directory open, and a ./gradlew clean in another terminal then fails
    # to delete build/ rather than merely losing the report.
    $scratch = Join-Path $repo '.inspection-ide'

    # Started from empty every time. Reusing the caches is tempting - they hold
    # the indexes and make a second run far quicker - but the VFS snapshot in
    # there can survive a source edit and be analysed instead of the file on
    # disk, which reports findings that were already fixed. A slow run beats a
    # stale one.
    if (Test-Path $scratch) { Get-ChildItem -Path $scratch -Recurse -Force | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue }
    New-Item -ItemType Directory -Force -Path $scratch, $outPath | Out-Null

    # Forward slashes: idea.properties is read as a Java properties file, where a
    # backslash escapes the next character.
    $s = $scratch -replace '\\', '/'
    $propsFile = Join-Path $scratch 'idea.properties'
    @"
idea.config.path=$s/config
idea.system.path=$s/system
idea.log.path=$s/log
"@ | Set-Content -Path $propsFile -Encoding utf8

    $profilePath = Join-Path $repo '.idea\inspectionProfiles\Testin.xml'
    $arguments = @($repo, $profilePath, $outPath, '-v1')
    if ($Subdirectory) { $arguments += @('-d', (Join-Path $repo $Subdirectory)) }

    Write-Host "Inspecting $repo - one indexing pass, expect 10-20 minutes..."
    $env:IDEA_PROPERTIES = $propsFile
    try {
        & $inspect @arguments
    } finally {
        Remove-Item Env:\IDEA_PROPERTIES -ErrorAction SilentlyContinue
    }
}

function Read-Problems([string] $outPath) {
    $files = Get-ChildItem -Path (Join-Path $outPath '*.xml') -ErrorAction SilentlyContinue
    if (-not $files) { throw "No XML in $outPath - the inspector produced nothing. Check its output above." }

    foreach ($file in $files) {
        $doc = [xml](Get-Content -Path $file.FullName -Raw)
        foreach ($problem in $doc.problems.problem) {
            if (-not $problem) { continue }
            [pscustomobject]@{
                # file:// $PROJECT_DIR$ / path - only the repo-relative tail is useful.
                Path       = ($problem.file -replace '^file://\$PROJECT_DIR\$/', '')
                Line       = [int] $problem.line
                Inspection = $problem.problem_class.id
                Severity   = $problem.problem_class.severity
                Message    = ($problem.description -replace '<[^>]+>', '' -replace '\s+', ' ').Trim()
            }
        }
    }
}

function Write-Reports([object[]] $problems, [string] $outPath) {
    # A project model that did not resolve floods the output with unresolved
    # symbols and every other count becomes meaningless. Say so rather than
    # leaving it to the reader to notice.
    $unresolved = @($problems | Where-Object { $_.Message -match 'Cannot resolve|cannot be resolved' }).Count
    if ($unresolved -gt 20) {
        Write-Warning "$unresolved unresolved-symbol findings. The Gradle project model probably did not load, or the bundled Lombok plugin was inactive - treat the counts below as unusable."
    }

    $summary = Join-Path $outPath 'summary.txt'
    $findings = Join-Path $outPath 'findings.txt'

    $lines = @("$($problems.Count) findings", '', 'By inspection:', '')
    $lines += $problems | Group-Object Inspection | Sort-Object Count -Descending |
        ForEach-Object { '{0,6}  {1}' -f $_.Count, $_.Name }

    $lines += @('', 'By package:', '')
    $lines += $problems |
        Group-Object { ($_.Path -replace '^src/(main|test)/java/', '' -replace '/[^/]+$', '') } |
        Sort-Object Count -Descending |
        ForEach-Object { '{0,6}  {1}' -f $_.Count, $_.Name }

    $lines | Set-Content -Path $summary -Encoding utf8

    $problems | Sort-Object Path, Line |
        ForEach-Object { '{0}:{1} - [{2}] {3}' -f $_.Path, $_.Line, $_.Inspection, $_.Message } |
        Set-Content -Path $findings -Encoding utf8

    Write-Host ''
    Get-Content $summary | Select-Object -First 25
    Write-Host ''
    Write-Host "Full list: $findings"
    Write-Host "Summary:   $summary"
}

$outPath = Join-Path $repo $OutputDir

if (-not $ReportOnly) {
    # A stale XML file from an earlier run would be counted as part of this one.
    # The contents, not the directory: gradle.properties sets org.gradle.vfs.watch,
    # so the daemon holds a handle on it and removing the directory itself fails.
    if (Test-Path $outPath) { Get-ChildItem -Path $outPath -Recurse -Force | Remove-Item -Recurse -Force }
    Invoke-Inspector (Resolve-Inspector) $outPath
}

Write-Reports (Read-Problems $outPath) $outPath
