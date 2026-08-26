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

    Everything it writes - the XML, the two reports, and the throwaway config and
    system directories the inspector needs - goes under .inspection/, one folder
    to delete. Those last are isolated because a running IDE holds a lock on its
    own: without them the run fails while IntelliJ is open on the same project,
    and it must never touch the developer's real settings.

    Costs one full indexing pass, so expect 10-20 minutes. A deliberate sweep, not
    a per-commit gate - .github/workflows/inspect.yml runs it every two days.

    Exits non-zero for three findings and no others: DataFlowIssue, ReturnNull and
    WrappedMethodDeclaration. The first two are the standing rule, a null contract
    the checker can prove is broken. The third is this script's own check - a
    method declaration belongs on one line, which no IntelliJ inspection says.
    Everything else is listed for a person to judge.

.EXAMPLE
    pwsh tools/inspect.ps1
#>

param(
    # The one folder this script writes: the XML, the two reports, and the
    # inspector's throwaway IDE directories. Deliberately not under build/:
    # ./gradlew clean deletes that, and the findings list is what you work from
    # for the next hour. Gitignored instead.
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

    # Wherever Gradle keeps its caches. GitHub Actions points GRADLE_USER_HOME
    # at the runner's workspace rather than at the profile, so looking only in
    # the profile found nothing there and the scheduled run could not start.
    $gradleHome = if ($env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME } else { Join-Path $env:USERPROFILE '.gradle' }

    # The Gradle transform path carries a content hash, so it is matched by shape rather than stored.
    $ide = Get-ChildItem -Path "$gradleHome\caches\*\transforms\*\transformed\idea-$version-win" `
        -Directory -ErrorAction SilentlyContinue | Select-Object -First 1

    if (-not $ide) {
        throw "No downloaded IDE $version found under $gradleHome. Run './gradlew compileJava' first - it fetches the platform this script inspects with."
    }

    $inspect = Join-Path $ide.FullName 'bin\inspect.bat'
    if (-not (Test-Path $inspect)) { throw "No inspect.bat in $($ide.FullName)" }

    Write-Host "Inspector: $inspect"
    return $inspect
}

function Invoke-Inspector([string] $inspect, [string] $outPath) {
    # Inside the output folder, so the caller that empties it for a fresh run
    # empties these caches with it. Starting them from empty every time is the
    # point: reusing them is tempting - they hold the indexes and make a second
    # run far quicker - but the VFS snapshot in there can survive a source edit
    # and be analysed instead of the file on disk, which reports findings that
    # were already fixed. A slow run beats a stale one.
    $scratch = Join-Path $outPath 'ide'
    New-Item -ItemType Directory -Force -Path $scratch | Out-Null

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

function Read-WrappedDeclarations([string] $scope) {
    <#
        A method declaration is one line. A signature is one thing to read, and
        split over four lines it is four things to put back together before the
        first question about the method can be asked.

        No IntelliJ inspection says this. The platform has code style settings
        for how to wrap a signature and none for refusing to, so it is checked
        here and reported beside the inspector's own findings - one list to
        read, one gate to pass.

        Matched on the modifiers rather than on the parenthesis, which is what
        keeps a wrapped call - stream() on one line and .filter(..) on the next
        - from being read as a declaration.
    #>
    $declaration = '^\s*(?:(?:public|protected|private|static|final|abstract|synchronized|native|default|strictfp)\s+)+[^;=()]*?\b\w+\s*\('

    foreach ($file in Get-ChildItem -Path $scope -Filter *.java -Recurse -File) {
        $number = 0
        foreach ($text in [System.IO.File]::ReadAllLines($file.FullName)) {
            $number++

            $trimmed = $text.Trim()
            if ($trimmed.StartsWith('*') -or $trimmed.StartsWith('//') -or $trimmed.StartsWith('/*')) { continue }
            if ($text -notmatch $declaration) { continue }

            # Still open at the end of the line, so the signature carries on to
            # the next one. A declaration that closes on its own line is fine,
            # however long it is.
            if ([regex]::Matches($text, '\(').Count -le [regex]::Matches($text, '\)').Count) { continue }

            [pscustomobject]@{
                Path       = $file.FullName.Substring($repo.Length + 1) -replace '\\', '/'
                Line       = $number
                Inspection = 'WrappedMethodDeclaration'
                Severity   = 'ERROR'
                Message    = "A method declaration belongs on one line: $trimmed"
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
    # The single cleanup: a stale XML file would be counted as part of this run,
    # and a stale index would be analysed in place of the source. Both live here.
    # The contents, not the directory: gradle.properties sets org.gradle.vfs.watch,
    # so the daemon holds a handle on it and removing the directory itself fails.
    # A cache file an earlier inspector still holds open is allowed to survive -
    # it costs a slower run - but a surviving report is counted twice, so say so.
    if (Test-Path $outPath) { Get-ChildItem -Path $outPath -Recurse -Force | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue }
    if (Get-ChildItem -Path (Join-Path $outPath '*.xml') -ErrorAction SilentlyContinue) {
        throw "XML from an earlier run is still in $outPath and could not be deleted. Close whatever holds it open and run again."
    }
    Invoke-Inspector (Resolve-Inspector) $outPath
}

# The inspector's findings and this script's own, as one list. Write-Reports
# groups by inspection name and knows nothing about where a finding came from,
# which is why the wrapped-declaration check produces the same shape.
$scope = if ($Subdirectory) { Join-Path $repo $Subdirectory } else { Join-Path $repo 'src' }
$problems = @(Read-Problems $outPath) + @(Read-WrappedDeclarations $scope)

Write-Reports $problems $outPath

# The three that are not allowed to survive a sweep. The first two are the
# project's standing rule - a null contract the checker can prove is broken is a
# defect, not a style note. The third is the one-line signature.
#
# Everything else the inspector reports is a judgement call and needs a person,
# so it is listed and not gated: this exits non-zero for these three only, which
# is what lets the scheduled run in .github/workflows/inspect.yml mean something.
$gate = @('DataFlowIssue', 'ReturnNull', 'WrappedMethodDeclaration')
$breaches = @($problems | Where-Object { $gate -contains $_.Inspection })

if ($breaches) {
    Write-Host ''
    Write-Host "$($breaches.Count) finding(s) the gate does not allow:" -ForegroundColor Red
    $breaches | Sort-Object Path, Line | ForEach-Object {
        Write-Host ('  {0}:{1} - [{2}] {3}' -f $_.Path, $_.Line, $_.Inspection, $_.Message)
    }
    exit 1
}

Write-Host ''
Write-Host "Gate clear: no $($gate -join ', ')." -ForegroundColor Green
