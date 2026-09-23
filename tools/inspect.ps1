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
    a per-commit gate - .github/workflows/inspect.yml runs it on every push to
    main. It was on a two-day schedule until 2026-09-12: a calendar runs it over
    code nobody touched and misses the push that mattered.

    Exits non-zero for any finding in the files this repository writes, the
    ones .idea/scopes/Inspected.xml names. A warning the IDE shows there is a
    warning the run fails on. The exceptions are the few rules a headless run
    cannot be trusted with, and $notGated at the foot of this file names each
    one with its reason - as $notGatedMessages does for the two that are right
    about everything but one sentence. DuplicatedDisplayString is counted rather
    than forbidden, against .github/display-string-baseline.txt.

    Nine rules are this script's own, because no IntelliJ inspection makes them
    or the headless run cannot be trusted with the one that does:
    WrappedMethodDeclaration, StaticMutableState,
    HandWrittenPrivateConstructor, NonMarkerComment, UnusedLambdaParameter,
    DriftedCaption, OrphanedJavadoc, MissingCopyright and
    HtmlParagraphInMarkdown.

    They read the source as text, so -Quick runs them alone in seconds with no
    IDE. That is the check to run before handing a change over; the full run is
    CI's, on every push.

.EXAMPLE
    pwsh tools/inspect.ps1
#>

param(
    # The one folder this script writes: the XML, the two reports, and the
    # inspector's throwaway IDE directories. Deliberately not under build/:
    # ./gradlew clean deletes that, and the findings list is what you work from
    # for the next hour. Ignored by Git instead.
    [string] $OutputDir = '.inspection',

    # Narrows a run to one place, and passing '' inspects the project root.
    #
    # No default, deliberately. It used to default to 'src/main', which is
    # always truthy - so the branch that reads every production source root was
    # unreachable, the content modules were never checked, and three comments
    # said otherwise (#170). What "no override" means is now asked of
    # PSBoundParameters, which can tell it from a value.
    [string] $Subdirectory,

    # How many display strings written in more than one place this tree is
    # allowed to have. A ratchet rather than a gate: see
    # Test-DisplayStringBaseline.
    [string] $DisplayStringBaseline = '.github/display-string-baseline.txt',

    # Reuse the XML already in $OutputDir and only rebuild the reports. The
    # line numbers in it are the ones the inspector saw, so a source edited
    # since then reports against lines that have moved - fine for re-reading a
    # run, wrong for judging the tree as it is now.
    [switch] $ReportOnly,

    # Only this script's own rules, and no IDE at all. Seconds rather than
    # twenty minutes, because nothing is indexed: the rules below read the
    # source as text.
    #
    # This is what to run before handing a change over. The full run belongs to
    # CI on every push, and the findings it alone can see - dead code, a
    # deprecated call, a redundant cast - are read from that run. What it cannot
    # see is exactly what this mode catches, and what kept arriving in Muteb's
    # IDE one warning at a time.
    [switch] $Quick
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot

# Whether the caller narrowed this run. Asked of PSBoundParameters rather than
# of the value, because "-Subdirectory ''" is a real request - inspect the
# project root - and a value cannot tell that from having no value at all.
$narrowed = $PSBoundParameters.ContainsKey('Subdirectory')

# What the inspector analyses: the whole project unless narrowed, and what is
# kept of it: the files .idea/scopes/Inspected.xml names, which is everything
# this repository writes. The inspector reads everything because a whole-project
# check judged on part of the tree is wrong - run on 'src/main' alone, a throws
# clause testin-java needs was reported redundant, and the content modules, the
# tests and every Markdown file were never read at all. It cannot be told the
# scope instead: its -scope option and the idea.analyze.scope property both
# failed headless, the first matching nothing and the second nothing less than
# the whole repository. So Select-Inspected drops what lies outside - .sandbox,
# which holds an entire IDE and produced 74,803 spelling findings, this script's
# own scratch folder, and the sample data.
$analysisScope = if ($narrowed) { $Subdirectory } else { '' }

function Resolve-Inspector {
    $props = Get-Content (Join-Path $repo 'gradle.properties')
    $version = ($props | Select-String -Pattern '^intellij\.version=(.+)$').Matches[0].Groups[1].Value

    # Wherever Gradle keeps its caches. GitHub Actions points GRADLE_USER_HOME
    # at the runner's workspace rather than at the profile, so looking only in
    # the profile found nothing there and the scheduled run could not start.
    # $HOME is PowerShell's own and is the profile on every platform.
    $gradleHome = if ($env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME } else { Join-Path $HOME '.gradle' }

    # The platform is downloaded per operating system and the folder is named
    # after it, so the suffix is matched rather than assumed (#106).
    $osSuffix = if ($IsWindows) { 'win' } elseif ($IsMacOS) { 'mac' } else { 'linux' }

    # The Gradle transform path carries a content hash, so it is matched by shape rather than stored.
    $ide = Get-ChildItem -Path (Join-Path $gradleHome "caches/*/transforms/*/transformed" "idea-$version-$osSuffix") `
        -Directory -ErrorAction SilentlyContinue | Select-Object -First 1

    if (-not $ide) {
        throw "No downloaded IDE $version found under $gradleHome. Run './gradlew compileJava' first - it fetches the platform this script inspects with."
    }

    # inspect.bat on Windows, inspect.sh everywhere else. Same arguments, different name.
    $launcher = if ($IsWindows) { 'inspect.bat' } else { 'inspect.sh' }
    $inspect = Join-Path $ide.FullName 'bin' $launcher
    if (-not (Test-Path $inspect)) { throw "No $launcher in $($ide.FullName)" }

    Write-Host "Inspector: $inspect"
    return $inspect
}

function Invoke-Inspector([string] $inspect, [string] $outPath) {
    # Outside the repository, and emptied before every run. Both halves matter.
    #
    # Empty, because reusing these caches is tempting - they hold the indexes
    # and make a second run far quicker - but the VFS snapshot in there can
    # survive a source edit and be analysed instead of the file on disk, which
    # reports findings that were already fixed. A slow run beats a stale one.
    #
    # Outside, because they used to live in .inspection, inside the very folder
    # being inspected: 741 MB of an IDE's own caches, indexed by the run that
    # wrote them. The inspector reads the project from disk, so the scratch has
    # no reason to sit in it.
    $scratch = Join-Path ([System.IO.Path]::GetTempPath()) 'testin-inspect'
    if (Test-Path $scratch) { Remove-Item -Path $scratch -Recurse -Force -ErrorAction SilentlyContinue }
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

    $profilePath = Join-Path $repo '.idea' 'inspectionProfiles' 'Testin.xml'
    $arguments = @($repo, $profilePath, $outPath, '-v1')
    if ($analysisScope) { $arguments += @('-d', (Join-Path $repo $analysisScope)) }

    Write-Host "Inspecting $repo - one indexing pass, expect 10-20 minutes..."
    $env:IDEA_PROPERTIES = $propsFile
    try {
        & $inspect @arguments
    } finally {
        Remove-Item Env:\IDEA_PROPERTIES -ErrorAction SilentlyContinue
    }
}

function Select-Inspected([object[]] $problems) {
    <#
        The findings in files this repository writes, which is what the scope
        .idea/scopes/Inspected.xml names - the same scope Code | Inspect Code
        offers in the IDE, so the two lists agree. The pattern is
        (a||b||...)&&!c&&!d: a folder is written as file:folder//* and a single
        file as file:name, and what follows &&! is left out.
    #>
    $pattern = ([xml](Get-Content (Join-Path $repo '.idea/scopes/Inspected.xml') -Raw)).component.scope.pattern
    $parts = @($pattern -split '&&!')
    $excluded = @($parts | Select-Object -Skip 1 | ForEach-Object { $_ -replace '^file:', '' })
    $folders = @()
    $files = @()
    foreach ($part in ($parts[0].Trim('(', ')') -split '\|\|')) {
        $target = $part -replace '^file:', ''
        if ($target.EndsWith('//*')) { $folders += $target.Substring(0, $target.Length - 3) + '/' } else { $files += $target }
    }

    # The spelling and grammar checkers know English, and a translation bundle
    # is not English: 1,746 "typos" in messages_fr and messages_hi were French
    # and Hindi. The Testin profile switches both off in the Translations scope,
    # which is what the IDE reads - and the headless run ignores a scope inside
    # a profile, so the same rule is written here as well. Every other check
    # still reads the translations.
    $translation = '^src/main/resources/messages_[a-z]{2}\.properties$'
    $language = @('SpellCheckingInspection', 'GrazieInspection', 'GrazieStyle')

    $problems | Where-Object {
        $path = $_.Path.TrimEnd('/')
        $inScope = (($files -contains $path) -or @($folders | Where-Object { $path.StartsWith($_) }).Count -gt 0) -and $excluded -notcontains $path
        $inScope -and -not ($language -contains $_.Inspection -and $path -match $translation)
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

function Get-JavaSourceRoots {
    <#
        Every source tree, tests included.

        Get-SourceRoots is the production ones, which is what the design checks
        want: a caption duplicated between two test harnesses is read by nobody.
        The copyright header is the other kind of rule - a test file is as
        readable in a fork, a decompiler or a paste as a main one, and a rule
        with an exception is a rule somebody has to remember (#303).

        Only the ones that exist, for the same reason as above.
    #>
    return @('src/main', 'src/test', 'testin-java/src', 'testin-testng/src') |
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

        # A parameter an implementation elsewhere uses. Its own name is no use
        # for the search below - a parameter called p appears in every file in
        # the plugin - so what is looked for is the type declaring it, which a
        # content module has to name to implement it at all. CodeNavigation's
        # two p parameters were reported unused on every run: the only
        # implementation in scope is NoCodeNavigation, which does nothing by
        # design, and the real one is CodeNavigator in testin-java (#170).
        if ($problem.Message -match '^Parameter .* is not used in any implementation') {
            $type = [System.IO.Path]::GetFileNameWithoutExtension($problem.Path)
            if ($moduleText -match ('\b' + [regex]::Escape($type) + '\b')) { Set-UsedFromContentModule $problem }
            continue
        }

        # A method, a whole class, or an enum constant. It caught only the first
        # to begin with, which left ExecutionPosition reported as a dead class
        # while two files in testin-java were calling it.
        if ($problem.Message -notmatch '^(Method|Class|Enum constant|Constructor) .* never used') { continue }

        $name = Get-DeclaredName $problem
        if (-not $name) { continue }

        # How a module would name it. A type is named outright, so a word match
        # is enough and anything tighter would miss an import or a static call.
        # A method or a constant is reached through something, so the dot or the
        # method reference has to be there - a bare word match on a name like
        # "run" would spare every finding in the report.
        $pattern = if ($problem.Message -match '^Class ') {
            '\b' + [regex]::Escape($name) + '\b'
        } else {
            '(\.|::)\s*' + [regex]::Escape($name) + '\s*[(:,)]'
        }

        if ($moduleText -notmatch $pattern) { continue }

        Set-UsedFromContentModule $problem
    }

    return $problems
}

<#
    Relabels one finding as reached from a content module.

    One place, because there are two ways in now - a declaration nothing in
    scope calls, and a parameter no implementation in scope uses - and the
    sentence a reader gets should not depend on which of them found it.
#>
function Set-UsedFromContentModule([object] $problem) {
    $problem.Inspection = 'UsedFromContentModule'
    $problem.Message = "$($problem.Message) It is called from a content module, which is outside the inspector's analysis scope - not dead code."
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

        # A type first: "public final class ExecutionPosition {" has no
        # parenthesis to find, and "record Moved(..)" has one that would give
        # back the record name anyway - but only by accident, and not for a
        # class or an interface.
        if ($text -match '\b(?:class|interface|enum|record)\s+(\w+)') { return $matches[1] }

        if ($text -match '\b(\w+)\s*\(') { return $matches[1] }
    }

    return ''
}


function Test-MachineString([string] $value) {
    <#
        Whether a literal is machinery rather than something a tester reads.

        Deliberately generous about what it excludes. A check that cries wolf
        gets switched off, and the ones it lets through - a caption, a status, a
        button - are the ones worth arguing about.
    #>
    if ($value.Length -lt 2 -or $value.Length -gt 60) { return $true }
    if ($value -notmatch '[A-Za-z]') { return $true }
    # paths, ids, format strings, css, packages
    if ($value -match '[/\\{}<>%$#=;|\[\]()*+^~`]') { return $true }
    if ($value -match '\.' -and $value -notmatch ' ') { return $true }
    if ($value -ne $value.Trim()) { return $true }
    if ($value -cmatch '^[a-z0-9_-]+$') { return $true }
    if ($value -cmatch '^[A-Z0-9_]+$') { return $true }
    # An identifier, not a sentence. A hump inside a single word is how a JSON
    # field, a config key or an inspection name is spelled - testinProject,
    # RepoUrl, UnstableApiUsage - and none of them is read by a tester. A word
    # they do read is a word, or words: Copy and Details have no hump, Git Error
    # has a space.
    if ($value -notmatch ' ' -and $value -cmatch '[a-z][A-Z]') { return $true }
    return $false
}



function Write-DisplayStringInventory([string[]] $scopes, [string] $outPath) {
    <#
        Every string a tester might read, and where it is written.

        findings.txt lists the ones written in more than one file, because those
        are the ones to act on. This is the whole set - including the strings
        written once, which are the ones a new duplicate would be created from.
        Read it when deciding whether a word already exists before adding it.

        Grouped by the call the string is handed to, because the call says what
        kind of word it is: one passed to softShow is a confirmation, one passed
        to .column is a table heading, one passed to StatusBarShortcut.build is a
        dialog key. A group with no owning enum is a vocabulary waiting for one -
        which is how Done was found.
    #>
    $rows = @()

    foreach ($scope in $scopes) {
        foreach ($file in Get-ChildItem -Path $scope -Filter *.java -Recurse -File) {
            $short = $file.FullName.Substring($repo.Length + 1) -replace '\\', '/'
            $number = 0

            foreach ($text in [System.IO.File]::ReadAllLines($file.FullName)) {
                $number++

                $trimmed = $text.Trim()
                if ($trimmed.StartsWith('*') -or $trimmed.StartsWith('//') -or $trimmed.StartsWith('/*')) { continue }
                if ($text -match 'Logger\.(trace|debug|info|warn|error|fatal)') { continue }

                foreach ($match in [regex]::Matches($text, '"((?:[^"\\]|\\.)*)"')) {
                    $value = $match.Groups[1].Value
                    if (Test-MachineString $value) { continue }

                    # The nearest identifier followed by '(' before the string.
                    # Single-line calls only, which is nearly all of them; the
                    # rest land under (unknown) and are still listed.
                    $before = $text.Substring(0, $match.Index)
                    $calls = [regex]::Matches($before, '([\w.]+)\s*\(')
                    $call = if ($calls.Count -gt 0) { $calls[$calls.Count - 1].Groups[1].Value } else { '(unknown)' }

                    $rows += [pscustomobject]@{ Value = $value; Call = $call; Where = "$short`:$number" }
                }
            }
        }
    }

    $inventory = Join-Path $outPath 'display-strings.txt'
    $lines = @(
        'Every string a tester might read, and where it is written.',
        '',
        'Grouped by the call it is handed to - the call says what kind of word it',
        'is, and a group with no owning enum is a vocabulary waiting for one.',
        '',
        "$($rows.Count) uses of $(@($rows | Select-Object -ExpandProperty Value -Unique).Count) distinct strings.",
        '',
        'The ones written in more than one file are also in findings.txt, as',
        'DuplicatedDisplayString, which is what the ratchet counts.',
        '')

    foreach ($group in ($rows | Group-Object Call | Sort-Object Count -Descending)) {
        $distinct = @($group.Group | Select-Object -ExpandProperty Value -Unique)
        $lines += ''
        $lines += ('=' * 78)
        $lines += ('{0}   -   {1} use(s), {2} distinct' -f $group.Name, $group.Count, $distinct.Count)
        $lines += ('=' * 78)

        foreach ($value in ($distinct | Sort-Object)) {
            $places = @($group.Group | Where-Object { $_.Value -eq $value } | Select-Object -ExpandProperty Where)
            $lines += ('  "{0}"' -f $value)
            foreach ($place in ($places | Sort-Object)) { $lines += ('        ' + $place) }
        }
    }

    $lines | Set-Content -Path $inventory -Encoding utf8
    Write-Host "Strings:   $inventory"
}

function Read-DriftedCaptions([string[]] $enums) {
    <#
        One concept, two words, in front of the same tester.

        The enums that name a test case's fields declare the same constants -
        EXPECTED_RESULT, STEPS, MODULE - and each used to spell its own caption.
        Two of them said "Expected Results" while the grid, the run editor and
        the details panel said "Expected Result", and it stayed that way for as
        long as each was written out separately.

        Read-DuplicatedDisplayStrings cannot see this. It matches literals that
        are the same, and a caption that has already drifted is two different
        strings - the check that exists to stop drift is blind to drift that has
        already happened. So this one compares by constant name instead of by
        text.

        A constant whose caption comes from another enum is skipped: asking an
        owner is the fix, not the finding.
    #>
    $captions = @{}

    foreach ($file in $enums) {
        # Loud, not skipped. A path that is not there used to be passed over in
        # silence, so this check shrank from four enums to two on the day #111
        # moved the other two - and printed "Gate clear" either way for
        # everything it had stopped looking at (#66, finding 98).
        if (-not (Test-Path $file)) {
            throw "Read-DriftedCaptions was given a file that is not there: $file"
        }

        $lines = [System.IO.File]::ReadAllLines($file)
        $short = (Split-Path $file -Leaf) -replace '\.java$', ''

        for ($i = 0; $i -lt $lines.Count - 1; $i++) {
            if ($lines[$i] -notmatch '^    ([A-Z][A-Z0-9_]+)\($') { continue }
            $constant = $matches[1]

            # The caption is the first argument, on the line below: a literal, or
            # the bundle key it is looked up under since the captions were
            # translated. Either form is the one thing two enums naming the same
            # constant have to agree on, and reading only the literal meant this
            # saw nothing at all once every caption became a key.
            $argument = $lines[$i + 1].Trim()
            if ($argument -notmatch '^(?:Bundle\.message\()?"((?:[^"\\]|\\.)*)"') { continue }

            $value = $matches[1]
            if (-not $captions.ContainsKey($constant)) { $captions[$constant] = @() }
            $captions[$constant] += [pscustomobject]@{
                Value = $value
                Where = $short
                Path  = $file.Substring($repo.Length + 1) -replace '\\', '/'
                Line  = $i + 2
            }
        }
    }

    foreach ($constant in $captions.Keys) {
        $places = $captions[$constant]
        $distinct = @($places | Select-Object -ExpandProperty Value -Unique)
        if ($distinct.Count -lt 2) { continue }

        $names = ($distinct | ForEach-Object { '"' + $_ + '"' }) -join ' and '

        foreach ($place in $places) {
            [pscustomobject]@{
                Path       = $place.Path
                Line       = $place.Line
                Inspection = 'DriftedCaption'
                Severity   = 'ERROR'
                Message    = "$constant is called $names in different places. One concept, one word - ask the enum that owns it."
            }
        }
    }
}

function Read-DuplicatedDisplayStrings([string[]] $scopes) {
    <#
        A user-facing string written in more than one file has no owner.

        This is the check the rest of the design rests on. A caption, a status
        name, a button - each belongs to the type that owns the concept, asked
        for as TestStatus.getLabel() or TestRunConfiguration.getDisplayName().
        A second file spelling it out is a divergence that nothing fails over:
        both read correctly alone, and they stop agreeing the day one is
        renamed.

        Reported once per extra file rather than once per literal, so the count
        is "how many places should be asking an owner instead" - which is the
        number that has to come down.

        Log lines are skipped. What a log says is not read by a tester and is
        not worth centralizing.
    #>
    $found = @{}

    foreach ($scope in $scopes) {
        foreach ($file in Get-ChildItem -Path $scope -Filter *.java -Recurse -File) {
            $number = 0
            foreach ($text in [System.IO.File]::ReadAllLines($file.FullName)) {
                $number++

                $trimmed = $text.Trim()
                if ($trimmed.StartsWith('*') -or $trimmed.StartsWith('//') -or $trimmed.StartsWith('/*')) { continue }
                if ($text -match 'Logger\.(trace|debug|info|warn|error|fatal)') { continue }

                foreach ($match in [regex]::Matches($text, '"((?:[^"\\]|\\.)*)"')) {
                    $value = $match.Groups[1].Value
                    if (Test-MachineString $value) { continue }

                    if (-not $found.ContainsKey($value)) { $found[$value] = @() }
                    $found[$value] += [pscustomobject]@{
                        Path = $file.FullName.Substring($repo.Length + 1) -replace '\\', '/'
                        Line = $number
                    }
                }
            }
        }
    }

    foreach ($value in $found.Keys) {
        $places = $found[$value]
        $files = @($places | Select-Object -ExpandProperty Path -Unique)
        if ($files.Count -lt 2) { continue }

        # Every place after the first: the first one is allowed to be where it
        # lives, and the rest are the ones that should be asking it.
        foreach ($place in ($places | Select-Object -Skip 1)) {
            [pscustomobject]@{
                Path       = $place.Path
                Line       = $place.Line
                Inspection = 'DuplicatedDisplayString'
                Severity   = 'WARNING'
                Message    = "`"$value`" is written in $($files.Count) files. A string a tester reads belongs to the type that owns the concept - ask it instead."
            }
        }
    }
}

function Read-ModelStatics([string[]] $scopes) {
    <#
        Static mutable state, in the packages that model the data.

        Never legitimate there. A static that is not final is one slot for the
        whole IDE holding a value that belongs to something - Config kept the
        cached Java test source root that way, so opening a second project
        overwrote the first and generation could write into the wrong source
        tree without saying anything.

        A per-project value goes on a @Service(Service.Level.PROJECT), which the
        caller gets and then reads or sets.

        Only non-final statics. A static final is a constant or an empty value
        of its own type, which is the pattern this codebase is built on.
    #>
    foreach ($scope in $scopes) {
        if (-not (Test-Path $scope)) { continue }

        foreach ($file in Get-ChildItem -Path $scope -Filter *.java -Recurse -File) {
            $number = 0
            foreach ($text in [System.IO.File]::ReadAllLines($file.FullName)) {
                $number++

                $trimmed = $text.Trim()
                if ($trimmed.StartsWith('*') -or $trimmed.StartsWith('//') -or $trimmed.StartsWith('/*')) { continue }

                # An import declares nothing, so it holds nothing. An
                # 'import static a.b.C.took;' has no parentheses and no
                # 'final', which is every test below - so it read as a mutable
                # static and failed the gate over a line that is not a member.
                if ($trimmed.StartsWith('import ')) { continue }

                if ($text -notmatch '\bstatic\b') { continue }
                if ($text -match '\bfinal\b') { continue }
                if ($text -match '\bstatic\s+(final\s+)?(class|interface|enum|record)\b') { continue }
                # a method, not a field
                if ($text -match '\(') { continue }

                [pscustomobject]@{
                    Path       = $file.FullName.Substring($repo.Length + 1) -replace '\\', '/'
                    Line       = $number
                    Inspection = 'StaticMutableState'
                    Severity   = 'ERROR'
                    Message    = "One slot for the whole IDE: $trimmed - a value that belongs to a project goes on a project service the caller gets and sets."
                }
            }
        }
    }
}

function Read-HandWrittenPrivateConstructors([string[]] $scopes) {
    <#
        An empty private constructor, where the Lombok annotation says it better.

        @NoArgsConstructor(access = AccessLevel.PRIVATE) is what this codebase
        uses to say "not instantiable", and it says it on the class rather than
        four lines down.

        Only empty ones. A constructor with a real body - a super(..) call,
        Swing setup - is a different thing entirely, and Lombok cannot express
        it.
    #>
    foreach ($scope in $scopes) {
        foreach ($file in Get-ChildItem -Path $scope -Filter *.java -Recurse -File) {
            $lines = [System.IO.File]::ReadAllLines($file.FullName)

            for ($i = 0; $i -lt $lines.Count; $i++) {
                $text = $lines[$i]
                if ($text -notmatch '^\s*private\s+[A-Z]\w*\s*\(\s*\)\s*\{') { continue }

                # Empty when the brace closes on this line, or the next line is
                # only the closing brace.
                $empty = $text -match '\{\s*\}\s*$'
                if (-not $empty -and $i + 1 -lt $lines.Count) { $empty = $lines[$i + 1].Trim() -eq '}' }
                if (-not $empty) { continue }

                [pscustomobject]@{
                    Path       = $file.FullName.Substring($repo.Length + 1) -replace '\\', '/'
                    Line       = $i + 1
                    Inspection = 'HandWrittenPrivateConstructor'
                    Severity   = 'ERROR'
                    Message    = 'An empty private constructor: use @NoArgsConstructor(access = AccessLevel.PRIVATE) instead.'
                }
            }
        }
    }
}

function Test-DisplayStringBaseline([object[]] $problems, [string] $baselinePath) {
    <#
        The duplicated-string count can go down and must not go up.

        Gating it outright would paint the build red over work nobody has
        scheduled. How many there were when the check was written is recorded
        once, in the baseline file itself (#66, finding 107). A ratchet is the
        same answer verify.yml already gives the Plugin Verifier:
        the number is recorded, a rise fails, and a fall is reported so the
        recorded number can follow it down.
    #>
    $now = @($problems | Where-Object { $_.Inspection -eq 'DuplicatedDisplayString' }).Count

    if (-not (Test-Path $baselinePath)) {
        Write-Host "No display-string baseline at $baselinePath - $now found. Write that number there to start the ratchet." -ForegroundColor Yellow
        return $true
    }

    $expected = [int](((Get-Content $baselinePath) | Where-Object { $_ -notmatch '^\s*#' -and $_.Trim() } | Select-Object -First 1).Trim())

    if ($now -gt $expected) {
        Write-Host ''
        Write-Host "Display strings written in more than one place: $now, up from $expected." -ForegroundColor Red
        Write-Host 'A string a tester reads belongs to the type that owns the concept. See the findings list.'
        return $false
    }

    if ($now -lt $expected) {
        Write-Host "Display strings down to $now from $expected. Update $baselinePath so the ratchet keeps its meaning." -ForegroundColor Green
    } else {
        Write-Host "Display strings: $now, unchanged."
    }

    return $true
}

function Hide-StringsAndComments([string] $source) {
    <#
        The same file with every string literal, char literal, text block and
        comment blanked to spaces, offsets unchanged.

        Written because the first sweep for unused lambda parameters read
        Logger.debug("[details] selectedDetails changed -> ") as a lambda and
        renamed the word inside the message. Any rule that matches on Java
        punctuation has to see the code alone.
    #>
    $out = [System.Text.StringBuilder]::new($source)
    $i = 0
    $n = $source.Length

    while ($i -lt $n) {
        $c = $source[$i]

        if ($c -eq '"' -and $i + 3 -le $n -and $source.Substring($i, 3) -eq '"""') {
            $j = $source.IndexOf('"""', $i + 3)
            $j = if ($j -lt 0) { $n } else { $j + 3 }
            for ($k = $i; $k -lt $j; $k++) { if ($source[$k] -ne "`n") { $out[$k] = ' ' } }
            $i = $j
            continue
        }

        if ($c -eq '"' -or $c -eq "'") {
            $j = $i + 1
            while ($j -lt $n) {
                if ($source[$j] -eq '\') { $j += 2; continue }
                if ($source[$j] -eq $c) { $j++; break }
                $j++
            }
            for ($k = $i; $k -lt [Math]::Min($j, $n); $k++) { $out[$k] = ' ' }
            $i = $j
            continue
        }

        if ($c -eq '/' -and $i + 1 -lt $n -and $source[$i + 1] -eq '/') {
            $j = $source.IndexOf("`n", $i)
            if ($j -lt 0) { $j = $n }
            for ($k = $i; $k -lt $j; $k++) { $out[$k] = ' ' }
            $i = $j
            continue
        }

        if ($c -eq '/' -and $i + 1 -lt $n -and $source[$i + 1] -eq '*') {
            $j = $source.IndexOf('*/', $i + 2)
            $j = if ($j -lt 0) { $n } else { $j + 2 }
            for ($k = $i; $k -lt $j; $k++) { if ($source[$k] -ne "`n") { $out[$k] = ' ' } }
            $i = $j
            continue
        }

        $i++
    }

    return $out.ToString()
}

function Get-LambdaBody([string] $masked, [int] $after) {
    $k = $after
    while ($k -lt $masked.Length -and [char]::IsWhiteSpace($masked[$k])) { $k++ }
    if ($k -ge $masked.Length) { return '' }

    $depth = 0

    if ($masked[$k] -eq '{') {
        for ($j = $k; $j -lt $masked.Length; $j++) {
            if ($masked[$j] -eq '{') { $depth++ }
            elseif ($masked[$j] -eq '}') { $depth--; if ($depth -eq 0) { break } }
        }
        return $masked.Substring($k + 1, [Math]::Max(0, [Math]::Min($j, $masked.Length) - $k - 1))
    }

    for ($j = $k; $j -lt $masked.Length; $j++) {
        $ch = $masked[$j]
        if ('([{'.Contains($ch)) { $depth++ }
        elseif (')]}'.Contains($ch)) { if ($depth -eq 0) { break }; $depth-- }
        elseif (($ch -eq ';' -or $ch -eq ',') -and $depth -eq 0) { break }
    }

    return $masked.Substring($k, [Math]::Min($j, $masked.Length) - $k)
}

function Read-UnusedLambdaParameters([string[]] $scopes) {
    <#
        A lambda parameter nothing in the body reads. Java has a name for one
        since 21 - the unnamed variable, _ - so there is a fix that needs no
        suppression, and the IDE offers it.

        The headless inspector cannot be asked for these: the global "unused"
        inspection under-reports without Lombok's generated code and the content
        modules' callers, and parameters never appear in its list at all. They
        reached Muteb's IDE one at a time instead, which is the whole reason
        this rule exists: 106 of them were sitting in the tree when it was
        written.

        Read off the masked source, so a log message holding an arrow is code to
        nobody. Switch arms - case 'n' -> ... - are skipped: the literal is
        masked away and what is left looks exactly like a lambda.
    #>
    foreach ($scope in $scopes) {
        foreach ($file in Get-ChildItem -Path $scope -Filter *.java -Recurse -File) {
            $source = [System.IO.File]::ReadAllText($file.FullName)
            $masked = Hide-StringsAndComments $source
            $path = $file.FullName.Substring($repo.Length + 1) -replace '\\', '/'

            $unused = [System.Collections.Generic.List[object]]::new()

            foreach ($match in [regex]::Matches($masked, '\(([^()]*)\)\s*->')) {
                $inner = $match.Groups[1].Value
                if ($inner -notmatch '^\s*\w+(\s*,\s*\w+)*\s*$') { continue }

                $body = Get-LambdaBody $masked ($match.Index + $match.Length)
                foreach ($name in [regex]::Matches($inner, '\w+')) {
                    if ($name.Value -eq '_') { continue }
                    if ($body -match ('\b' + [regex]::Escape($name.Value) + '\b')) { continue }
                    $unused.Add(@{ At = $match.Groups[1].Index + $name.Index; Name = $name.Value })
                }
            }

            foreach ($match in [regex]::Matches($masked, '(?<![\w.)])(\w+)\s*->')) {
                $name = $match.Groups[1].Value
                if ($name -in @('_', 'default', 'case')) { continue }

                $lineStart = $masked.LastIndexOf("`n", [Math]::Max(0, $match.Index - 1)) + 1
                if ($masked.Substring($lineStart, $match.Index - $lineStart) -match '\bcase\b') { continue }

                $body = Get-LambdaBody $masked ($match.Index + $match.Length)
                if ($body -match ('\b' + [regex]::Escape($name) + '\b')) { continue }
                $unused.Add(@{ At = $match.Groups[1].Index; Name = $name })
            }

            foreach ($found in $unused) {
                [pscustomobject]@{
                    Path       = $path
                    Line       = ($masked.Substring(0, $found.At) -split "`n").Count
                    Inspection = 'UnusedLambdaParameter'
                    Severity   = 'ERROR'
                    Message    = "Parameter '$($found.Name)' is never used. Java 21 named it: write _ instead"
                }
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
        - from being read as a declaration. A record header is matched with or
        without them, because "record Name(" is never a call: six package-private
        records sat wrapped while the gate read clear.
    #>
    $declaration = '^\s*(?:(?:public|protected|private|static|final|abstract|synchronized|native|default|strictfp)\s+)+[^;=()]*?\b\w+\s*\('
    $record = '^\s*(?:\w+\s+)*record\s+\w+(?:<[^>]*>)?\s*\('

    foreach ($file in Get-ChildItem -Path $scope -Filter *.java -Recurse -File) {
        $number = 0
        foreach ($text in [System.IO.File]::ReadAllLines($file.FullName)) {
            $number++

            $trimmed = $text.Trim()
            if ($trimmed.StartsWith('*') -or $trimmed.StartsWith('//') -or $trimmed.StartsWith('/*')) { continue }
            if ($text -notmatch $declaration -and $text -notmatch $record) { continue }

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

function Test-DocComment([string[]] $lines, [int] $close) {
    <#
        Whether the block comment that ends at $close is a doc comment.

        Only /** is documentation. Every file opens with the copyright notice in
        a plain /* block, and javac never held that as documentation - so the
        package javadoc below it in a package-info.java is still attached to the
        package, and nothing is thrown away. Asking only whether a line ends in
        "*/" read those three files as orphaned the day the headers landed
        (#303).

        Walks back to the line that opened the block, which is the line itself
        for a one-line /** .. */.
    #>
    for ($i = $close; $i -ge 0; $i--) {
        if (-not $lines[$i].Trim().StartsWith('/*')) { continue }
        return $lines[$i].Trim().StartsWith('/**')
    }

    return $false
}

function Read-OrphanedJavadoc([string[]] $scopes) {
    <#
        A javadoc block the compiler throws away.

        Java keeps only the last doc comment before a declaration, so a block
        followed by another block documents nothing, and the member the first
        one was written for is left bare. Twenty were found on 12 September
        2026 and eight of them carried a rule marker - so a grep from the
        documentation landed on the wrong method, and the method that carries
        the rule cited nothing (#66, finding 76).

        Mechanically checkable, which is what makes it worth a rule rather than
        a sweep: the shape is exact. A block whose next line that is neither
        blank nor an annotation of its own opens another block.

        "Of its own" is the whole subtlety. An annotation on a line by itself
        is skipped and one carrying a declaration after it is not - "@NotNull
        String cardTitle(..)" is the member, and reading that as an annotation
        made every documented method of every interface in the tree look
        orphaned.

        One file is frozen by name, the way ArchitectureTest freezes a known
        violation: TestRunDto's block separates a @JsonIgnore from the method
        it was written for, which is a data bug rather than a documentation
        one - the derived field reaches every run file a tester commits. It is
        #73's, and deleting that name is how that story closes.
    #>
    $frozen = @('src/main/java/org/testin/model/dto/TestRunDto.java')

    foreach ($scope in $scopes) {
        if (-not (Test-Path $scope)) { continue }

        foreach ($file in Get-ChildItem -Path $scope -Filter *.java -Recurse -File) {
            $path = $file.FullName.Substring($repo.Length + 1) -replace '\\', '/'
            if ($frozen -contains $path) { continue }

            $lines = [System.IO.File]::ReadAllLines($file.FullName)

            for ($i = 0; $i -lt $lines.Count; $i++) {
                if ($lines[$i].Trim() -notmatch '\*/$') { continue }
                if (-not (Test-DocComment $lines $i)) { continue }

                $j = $i + 1
                while ($j -lt $lines.Count -and ($lines[$j].Trim() -eq '' -or $lines[$j].Trim() -match '^@[A-Za-z_][\w.]*(\(.*\))?$')) { $j++ }

                if ($j -ge $lines.Count -or $lines[$j].Trim() -notmatch '^/\*\*') { continue }

                [pscustomobject]@{
                    Path       = $path
                    Line       = $i + 1
                    Inspection = 'OrphanedJavadoc'
                    Severity   = 'ERROR'
                    Message    = "A second block opens at line $($j + 1), so javac keeps that one and this documents nothing."
                }
            }
        }
    }
}

function Read-HtmlParagraphInMarkdown {
    <#
        A paragraph break written as HTML in a Markdown file.

        A line holding only <p> opens a raw HTML block that runs to the next
        blank line. Inside it backticks stop marking code, so a placeholder
        such as git show af5f3013:<path> is read as an element that is never
        closed, and the IDE says so. CLAUDE.md carried seven of them until
        22 September 2026. A blank line is the Markdown for the same break,
        indented under a list item when the paragraph belongs to one.

        Every tracked Markdown file is read, the repository's root included,
        which is where CLAUDE.md sits and where the inspector does not look.
    #>
    foreach ($relative in @(git -C $repo ls-files '*.md')) {
        # A page deleted in the working tree is still tracked until the delete is staged,
        # and a gate that throws on it reports nothing at all.
        $path = Join-Path $repo $relative
        if (-not (Test-Path $path)) { continue }

        $lines = [System.IO.File]::ReadAllLines($path)

        for ($i = 0; $i -lt $lines.Count; $i++) {
            if ($lines[$i].Trim() -ne '<p>') { continue }

            [pscustomobject]@{
                Path       = $relative
                Line       = $i + 1
                Inspection = 'HtmlParagraphInMarkdown'
                Severity   = 'ERROR'
                Message    = 'A bare <p> opens a raw HTML block, where backticks stop being code. Separate the paragraphs with a blank line.'
            }
        }
    }
}

function Find-CommentStart([string] $line) {
    <#
        Where a comment starts on a line of code, or -1. A // inside a string -
        an address, a regular expression - is not a comment, so the quotes are
        followed rather than the slashes.
    #>
    $quote = ''

    for ($j = 0; $j -lt $line.Length; $j++) {
        $c = $line[$j]

        if ($quote) {
            if ($c -eq '\') { $j++; continue }
            if ($c -eq $quote) { $quote = '' }
            continue
        }

        if ($c -eq '"' -or $c -eq "'") { $quote = $c; continue }
        if ($c -eq '/' -and $j + 1 -lt $line.Length -and ($line[$j + 1] -eq '/' -or $line[$j + 1] -eq '*')) { return $j }
    }

    return -1
}

function Read-NonMarkerComments([string[]] $scopes) {
    <#
        A comment that is not a marker.

        The code carries a UC- or Rule- marker and nothing else. The marker says
        which documented behavior this is, docs/ says what that behavior is, and
        the commit message says why the code is shaped this way - where git
        blame reaches it and where it cannot drift from the code.

        27,483 comment lines came out of src/main in af5f3013, and 4,256 came
        out of the tests on 22 September 2026. Nothing stopped the next one
        until this rule, and a javadoc block was back in src/test the same week.

        Three kinds stay. The Apache notice every file opens with, which
        MissingCopyright checks instead. The ones a machine reads - //noinspection,
        // @formatter:off - because they are part of the build rather than prose.
        And the markers themselves. A // inside a text block or a string is not
        a comment and is left alone.
    #>
    $keep = '^//\s*(UC-|Rule-|noinspection|@formatter)'

    foreach ($scope in $scopes) {
        foreach ($file in Get-ChildItem -Path $scope -Filter *.java -Recurse -File) {
            $lines = [System.IO.File]::ReadAllLines($file.FullName)
            $inHeader = $lines.Count -gt 0 -and $lines[0].StartsWith('/*')
            $inBlock = $false
            $inTextBlock = $false

            for ($i = 0; $i -lt $lines.Count; $i++) {
                $text = $lines[$i]
                $trimmed = $text.Trim()

                if ($inHeader) {
                    if ($trimmed.EndsWith('*/')) { $inHeader = $false }
                    continue
                }

                if ($inBlock) {
                    if ($trimmed.Contains('*/')) { $inBlock = $false }
                    continue
                }

                if ($inTextBlock) {
                    if ((($text.Split('"""', [System.StringSplitOptions]::None).Count - 1) % 2) -eq 1) { $inTextBlock = $false }
                    continue
                }

                $at = -1

                if ($trimmed.StartsWith('//')) {
                    if ($trimmed -match $keep) { continue }
                    $at = $text.IndexOf('//')
                } elseif ($trimmed.StartsWith('/*')) {
                    $at = $text.IndexOf('/*')
                    if (-not $trimmed.Contains('*/')) { $inBlock = $true }
                } else {
                    $at = Find-CommentStart $text
                    if ($at -lt 0) {
                        if ((($text.Split('"""', [System.StringSplitOptions]::None).Count - 1) % 2) -eq 1) { $inTextBlock = $true }
                        continue
                    }
                    if ($text.Substring($at).StartsWith('/*') -and -not $text.Substring($at).Contains('*/')) { $inBlock = $true }
                }

                [pscustomobject]@{
                    Path       = $file.FullName.Substring($repo.Length + 1) -replace '\\', '/'
                    Line       = $i + 1
                    Inspection = 'NonMarkerComment'
                    Severity   = 'ERROR'
                    Message    = "The code carries a UC- or Rule- marker and nothing else: $trimmed"
                }
            }
        }
    }
}

function Read-MissingCopyright([string[]] $scopes) {
    <#
        A file that does not say who owns it or on what terms.

        The license used to live in LICENSE at the repository root and nowhere
        else, so a file that traveled out of the repository - into a jar, a
        decompiler, a search result, a fork - traveled without it. All 656 got
        the Apache 2.0 notice from LICENSE's own appendix in #303, and this is
        what keeps the 657th from being the one that has none: a header nothing
        checks is the header missing from the next file somebody adds.

        The expected first two lines are read from LICENSE rather than written
        out here, so the year and the owner have one owner. Changing the year
        there is meant to be loud: every file disagreeing with it is reported.

        The first non-blank line, not the first line, so a stray blank at the
        top is a header in the wrong place rather than no header at all - the
        finding says the same thing either way.
    #>
    $appendix = [System.IO.File]::ReadAllLines((Join-Path $repo 'LICENSE')) |
        Where-Object { $_ -cmatch '^\s*Copyright \d{4} \S' } |
        Select-Object -First 1

    if (-not $appendix) {
        throw "No 'Copyright <year> <owner>' line in LICENSE, so there is nothing to check the file headers against."
    }

    $expected = '* ' + $appendix.Trim()

    foreach ($scope in $scopes) {
        if (-not (Test-Path $scope)) { continue }

        foreach ($file in Get-ChildItem -Path $scope -Filter *.java -Recurse -File) {
            $lines = [System.IO.File]::ReadAllLines($file.FullName)

            $first = 0
            while ($first -lt $lines.Count -and $lines[$first].Trim() -eq '') { $first++ }

            $opens = ($first -lt ($lines.Count - 1)) -and ($lines[$first].Trim() -eq '/*')
            if ($opens -and $lines[$first + 1].Trim() -ceq $expected) { continue }

            [pscustomobject]@{
                Path       = $file.FullName.Substring($repo.Length + 1) -replace '\\', '/'
                Line       = 1
                Inspection = 'MissingCopyright'
                Severity   = 'ERROR'
                Message    = "No copyright header. A file opens with the Apache 2.0 notice from LICENSE - '/*' and then '$expected' - above everything else, one blank line before package."
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

if (-not $ReportOnly -and -not $Quick) {
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
$scopes = if ($narrowed) { @(Join-Path $repo $Subdirectory) } else { Get-SourceRoots }

$problems = if ($Quick) { @() } else { @(Read-Problems $outPath) }
if (-not $narrowed -and -not $Quick) { $problems = @(Select-Inspected $problems) }

# Two kinds of rule, and they read different trees.
#
# How code is written - one-line signatures, Lombok for the boilerplate, a doc
# block javac keeps, the licence header - holds for every Java file, tests
# included: CLAUDE.md states them for the whole tree, and a rule that looks at
# only part of it is a rule the rest can quietly break. Until #66 finding 106
# only the header read the tests, so a hand-written private constructor sat in
# src/test with nothing to see it.
#
# What a tester reads - a caption written in two places - is production alone.
# A string duplicated between two test harnesses is read by nobody.
#
# An explicit -Subdirectory still wins over both, for the caller narrowing a run.
$everyTree = if ($narrowed) { $scopes } else { Get-JavaSourceRoots }

foreach ($scope in $everyTree) { $problems += @(Read-WrappedDeclarations $scope) }
$problems += @(Read-HandWrittenPrivateConstructors $everyTree)
$problems += @(Read-OrphanedJavadoc $everyTree)
$problems += @(Read-MissingCopyright $everyTree)
$problems += @(Read-NonMarkerComments $everyTree)
$problems += @(Read-UnusedLambdaParameters $everyTree)
$problems += @(Read-HtmlParagraphInMarkdown)

$problems += @(Read-DuplicatedDisplayStrings $scopes)
$problems += @(Read-ModelStatics @((Join-Path $repo 'src/main/java/org/testin/model')))

# The enums that name a test case's fields, compared against each other. Same
# constant, same concept, so the caption is the same question.
# A path that no longer exists is skipped in silence, so two of these four went
# on being listed after #111 moved them and the check quietly shrank to comparing
# one pair - printing "Gate clear" either way (#66, finding 98).
$problems += @(Read-DriftedCaptions @(
        (Join-Path $repo 'src/main/java/org/testin/testcase/TestEditorAttributes.java'),
        (Join-Path $repo 'src/main/java/org/testin/testrun/RunEditorAttributes.java'),
        (Join-Path $repo 'src/main/java/org/testin/testcase/CreateTestCaseFields.java'),
        (Join-Path $repo 'src/main/java/org/testin/testcase/UpdateTestCaseFields.java')))

# A quick run writes nothing: its list is partial by design, and findings.txt
# is what the last full run left to work from. Resolving a content module's
# callers and taking the vocabulary both read that full list, so both wait for
# it too.
if (-not $Quick) {
    $problems = Resolve-CrossModuleUsages $problems

    Write-Reports $problems $outPath

    # The whole vocabulary, beside the findings. findings.txt says what to act on;
    # this says what already exists, which is what stops the next duplicate.
    Write-DisplayStringInventory $scopes $outPath
}
else {
    Write-Host ''
    Write-Host "Quick run: this script's own rules only, over $($everyTree.Count) source root(s). The IDE's own findings come from CI." -ForegroundColor Cyan
    $problems | Sort-Object Path, Line | ForEach-Object {
        Write-Host ('  {0}:{1} - [{2}] {3}' -f $_.Path, $_.Line, $_.Inspection, $_.Message)
    }
}

# Nothing survives a sweep except what is named here, each with the reason a
# headless run cannot be trusted with it. Everything else fails the run: a
# warning the IDE shows in these files is a defect, not a note for later.
$notGated = [ordered]@{
    'SameReturnValue'         = 'judged across every implementation, and the enums'' getters are Lombok''s, which the headless run cannot see'
    'RedundantThrows'         = 'the Java module implements JavaSourceRoot''s interfaces and throws what they declare'
    'UsedFromContentModule'   = 'this script''s note that an unused finding has a caller in a content module'
    'UnusedProperty'          = 'the platform reads action, group and tool window keys by name; BundleKeysTest checks every other key has a reader'
    'UndefinedParamsPresent'  = 'an action''s inputs come from its metadata online, which the headless run does not fetch'
    'JSUnresolvedLibraryURL'  = 'asks whether this machine has downloaded a library a page loads from a CDN'
    'DuplicatedDisplayString' = 'counted against .github/display-string-baseline.txt below instead'
}

# Two inspections are right about everything except one sentence each, so they
# are excused by what the finding says rather than by its name. An "unused"
# method "not reachable from the entry points" is one the platform reaches
# through an interface it implements, and a constructor "never used" is one
# Lombok's @Builder calls; a private member, a parameter or a local that nothing
# reads is judged correctly and fails the run. "URI is not registered" asks
# whether this machine holds the schema for a namespace: both plugin icons
# declare the SVG one, and a headless IDE with no catalog and no network cannot
# look it up.
$notGatedMessages = [ordered]@{
    'unused'          = @('not reachable from the entry points', 'Constructor is never used')
    'XmlHighlighting' = @('URI is not registered')
}

# A finding whose whole description is #loc says nothing at all, and a gate
# cannot demand a fix it cannot name. The Markdown annotator writes one for
# every [*] in a Mermaid state diagram - four in docs/product.md - which is the
# start and end state the syntax is built on.
$noDescription = '#loc'

function Test-Gated([object] $problem) {
    if ($notGated.Contains($problem.Inspection)) { return $false }
    if ($problem.Message.Trim() -eq $noDescription) { return $false }
    return -not @($notGatedMessages[$problem.Inspection] | Where-Object { $problem.Message.Contains($_) }).Count
}

$breaches = @($problems | Where-Object { Test-Gated $_ })

if ($breaches) {
    Write-Host ''
    Write-Host "$($breaches.Count) finding(s) the gate does not allow:" -ForegroundColor Red
    $breaches | Sort-Object Path, Line | ForEach-Object {
        Write-Host ('  {0}:{1} - [{2}] {3}' -f $_.Path, $_.Line, $_.Inspection, $_.Message)
    }
    exit 1
}

Write-Host ''
Write-Host "Gate clear: no finding outside $($notGated.Keys -join ', '), the sentences $($notGatedMessages.Keys -join ' and ') are excused for, and findings with no description." -ForegroundColor Green

# And the one that is counted rather than forbidden. Reported after the gate so
# a hard breach is the first thing read, and it fails the run in its own right:
# the point of a ratchet is that it holds.
if (-not (Test-DisplayStringBaseline $problems (Join-Path $repo $DisplayStringBaseline))) { exit 1 }
