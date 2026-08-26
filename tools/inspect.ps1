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

    # Reuse the XML already in $OutputDir and only rebuild the reports. The
    # line numbers in it are the ones the inspector saw, so a source edited
    # since then reports against lines that have moved - fine for re-reading a
    # run, wrong for judging the tree as it is now.
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

function Get-SourceRoots {
    <#
        Every production source tree, not just the core one.

        The plugin is one module plus a content module per language, and the
        checks below have to see all of them. A wrapped signature in
        testin-java is exactly as unreadable as one in src, and it used to pass
        the gate because nothing looked at it.

        Only the ones that exist: a checkout mid-refactor, or a future module,
        should not fail the run.
    #>
    return @('src/main', 'testin-java/src/main', 'testin-testng/src/main') |
        ForEach-Object { Join-Path $repo $_ } |
        Where-Object { Test-Path $_ }
}

function Resolve-CrossModuleUsages([object[]] $problems) {
    <#
        A method the inspector calls dead because the only calls to it are in a
        content module.

        The inspector is given src/main as its analysis scope - the whole
        project is indexed, but the reference graph it builds for "is this ever
        used" covers the scope only. So every method the core exposes for
        testin-java or testin-testng to call reads as unused: four of them
        today, all on TestNGExecution, all called from TestNGRunner.

        They are relabelled rather than dropped. Nothing should vanish out of a
        report silently - that is how the wildcard import in #61 turned 94
        findings into 93 false ones without anyone noticing - and a group of its
        own in the summary says the thing worth knowing: these are not dead, and
        the unused count beside them is now about code that really is.

        Matched by name, so a method sharing a name with one a module genuinely
        calls would be spared wrongly. That is the safe direction to be wrong
        in, and the finding is still in the list to read.
    #>
    $moduleRoots = @('testin-java/src/main', 'testin-testng/src/main') |
        ForEach-Object { Join-Path $repo $_ } |
        Where-Object { Test-Path $_ }

    if (-not $moduleRoots) { return $problems }

    $moduleText = (Get-ChildItem -Path $moduleRoots -Filter *.java -Recurse -File |
        ForEach-Object { [System.IO.File]::ReadAllText($_.FullName) }) -join "`n"

    foreach ($problem in $problems) {
        if ($problem.Inspection -ne 'unused') { continue }
        if ($problem.Message -notmatch '^Method .* never used') { continue }

        $name = Get-DeclaredName $problem
        if (-not $name) { continue }

        # A call or a method reference, either way the module names it.
        if ($moduleText -notmatch ("(\.|::)\s*" + [regex]::Escape($name) + "\s*[(:,)]")) { continue }

        $problem.Inspection = 'UsedFromContentModule'
        $problem.Message = "$($problem.Message) It is called from a content module, which is outside the inspector's analysis scope - not dead code."
    }

    return $problems
}

function Get-DeclaredName([object] $problem) {
    <#
        The method name a finding points at. Read from the source rather than
        parsed out of the message, which names the method only in some of its
        wordings.

        Scanned forward a few lines rather than read off the one line, because
        the line number can point just above the declaration - at the close of
        its javadoc - when the file has been edited since the inspector ran.
        The next declaration below a javadoc is the one that javadoc documents,
        so this is the right answer for the case that actually happens, and a
        few lines is not enough drift to reach a different method.

        Empty when nothing there looks like a declaration, which leaves the
        finding labelled as the inspector labelled it. Failing to recognize a
        cross-module call is a report that says too much; recognizing one that
        is not there would be a report that says too little.
    #>
    $file = Join-Path $repo $problem.Path
    if (-not (Test-Path $file)) { return '' }

    $lines = [System.IO.File]::ReadAllLines($file)
    if ($problem.Line -lt 1 -or $problem.Line -gt $lines.Count) { return '' }

    $last = [Math]::Min($problem.Line + 4, $lines.Count)
    for ($n = $problem.Line; $n -le $last; $n++) {
        $text = $lines[$n - 1]
        $trimmed = $text.Trim()
        if ($trimmed.StartsWith('*') -or $trimmed.StartsWith('//') -or $trimmed.StartsWith('/*')) { continue }
        if ($trimmed.EndsWith('*/')) { continue }

        if ($text -match '\b(\w+)\s*\(') { return $matches[1] }
    }

    return ''
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
# Every production source root, so the one-line-signature gate covers the
# content modules too. An explicit -Subdirectory still wins, for the caller
# narrowing a run to one place.
$scopes = if ($Subdirectory) { @(Join-Path $repo $Subdirectory) } else { Get-SourceRoots }

$problems = @(Read-Problems $outPath)
foreach ($scope in $scopes) { $problems += @(Read-WrappedDeclarations $scope) }

$problems = Resolve-CrossModuleUsages $problems

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
