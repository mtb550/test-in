#Requires -Version 7

<#
.SYNOPSIS
    Compares what the Plugin Verifier reported against the recorded baseline (#144).

.DESCRIPTION
    The verifier exits non-zero while any target IDE reports a problem, which is
    every run today - 159 of them, all the same unresolved-class kind. A check
    that is always red is a check everyone learns to ignore, so verify.yml lets
    that step through and asks this instead.

    The question that matters is not "is there a problem" but "is there a new
    one". This answers that: it fails when a count is above its baseline, says so
    when a count has come down, and fails when an IDE the baseline names produced
    no verdict at all - a check that did not happen must never read as a pass.

    The numbers live in .github/verification-baseline.txt, and every line's target
    is 0. That is what the JetBrains Marketplace shows on the plugin page.

    Run it after 'gradlew verifyPlugin -PverifyAllIdes'.

.EXAMPLE
    ./gradlew verifyPlugin -PverifyAllIdes
    pwsh tools/verification-report.ps1
#>

param(
    [string] $Baseline = '.github/verification-baseline.txt',
    [string] $Reports = 'build/reports/pluginVerifier'
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot

# The prefix of a report folder - IU-261.25134.95 - is what stays the same when
# the IDE version moves, so it is what the baseline is keyed on.
$names = @{ IU = 'IntelliJ IDEA'; PY = 'PyCharm'; GO = 'GoLand'; WS = 'WebStorm' }

function Read-Baseline([string] $path) {
    $expected = [ordered]@{}
    foreach ($line in Get-Content -Path $path) {
        $text = $line.Trim()
        if (-not $text -or $text.StartsWith('#')) { continue }

        $parts = $text -split '\s+'
        $expected[$parts[0]] = [int] $parts[1]
    }
    return $expected
}

function Read-Verdicts([string] $path) {
    $found = @{}
    if (-not (Test-Path $path)) { return $found }

    foreach ($file in Get-ChildItem -Path $path -Filter 'verification-verdict.txt' -Recurse -File) {
        # .../pluginVerifier/<IDE>/plugins/<id>/<version>/verification-verdict.txt
        $ide = $file.FullName.Substring($path.Length).TrimStart('\', '/') -split '[\\/]' | Select-Object -First 1
        $prefix = ($ide -split '-')[0]

        $first = (Get-Content -Path $file.FullName -TotalCount 1)
        $count = if ($first -match '^(\d+) compatibility problems') { [int] $Matches[1] }
                 elseif ($first -match '^Compatible\b') { 0 }
                 else { -1 }

        $found[$prefix] = [pscustomobject]@{ Count = $count; Ide = $ide }
    }
    return $found
}

$baselinePath = Join-Path $repo $Baseline
$reportsPath = Join-Path $repo $Reports

$expected = Read-Baseline $baselinePath
$found = Read-Verdicts $reportsPath

if ($found.Count -eq 0) {
    Write-Host "No verification verdicts under $reportsPath - the verifier produced nothing." -ForegroundColor Red
    exit 1
}

$rows = @()
$worse = @()
$better = @()
$missing = @()
$unreadable = @()

foreach ($prefix in ($expected.Keys + $found.Keys | Sort-Object -Unique)) {
    $was = if ($expected.Contains($prefix)) { $expected[$prefix] } else { $null }
    $now = $found[$prefix]

    if (-not $now) {
        # A target the baseline names and this run did not reach. A verdict that
        # never arrived proves nothing, and reading it as "no problems" is how a
        # gate quietly stops gating.
        $missing += $prefix
        $rows += [pscustomobject]@{ Ide = $prefix; Build = '-'; Was = $was; Now = '-'; Note = 'NOT VERIFIED' }
        continue
    }

    if ($null -eq $was) {
        $rows += [pscustomobject]@{ Ide = $prefix; Build = $now.Ide; Was = '-'; Now = $now.Count; Note = 'new target' }
        continue
    }

    if ($now.Count -lt 0) {
        $unreadable += $prefix
        $rows += [pscustomobject]@{ Ide = $prefix; Build = $now.Ide; Was = $was; Now = '?'; Note = 'verdict not understood' }
    }
    elseif ($now.Count -gt $was) {
        $worse += [pscustomobject]@{ Prefix = $prefix; Was = $was; Now = $now.Count }
        $rows += [pscustomobject]@{ Ide = $prefix; Build = $now.Ide; Was = $was; Now = $now.Count; Note = "WORSE by $($now.Count - $was)" }
    }
    elseif ($now.Count -lt $was) {
        $better += [pscustomobject]@{ Prefix = $prefix; Was = $was; Now = $now.Count }
        $rows += [pscustomobject]@{ Ide = $prefix; Build = $now.Ide; Was = $was; Now = $now.Count; Note = "better by $($was - $now.Count)" }
    }
    else {
        $note = if ($now.Count -eq 0) { 'green' } else { 'unchanged' }
        $rows += [pscustomobject]@{ Ide = $prefix; Build = $now.Ide; Was = $was; Now = $now.Count; Note = $note }
    }
}

$total = ($found.Values | Where-Object { $_.Count -gt 0 } | Measure-Object -Property Count -Sum).Sum
if (-not $total) { $total = 0 }

$lines = @('## Plugin Verifier', '', '| IDE | Build | Baseline | Now | |', '|---|---|---:|---:|---|')
foreach ($row in $rows) {
    $label = if ($names.ContainsKey($row.Ide)) { $names[$row.Ide] } else { $row.Ide }
    $lines += '| {0} | `{1}` | {2} | {3} | {4} |' -f $label, $row.Build, $row.Was, $row.Now, $row.Note
}

$lines += @('', "**$total compatibility problems in total.** The target for every row is 0 - that is what the Marketplace shows on the plugin page.")

if ($better) {
    $moved = ($better | ForEach-Object { '{0} {1} -> {2}' -f $_.Prefix, $_.Was, $_.Now }) -join ', '
    $lines += @('', "Lower than the baseline: $moved. Update ``$Baseline`` so the gate keeps its meaning.")
}

# The run page, so nobody downloads an artifact to read four numbers.
if ($env:GITHUB_STEP_SUMMARY) { $lines | Add-Content -Path $env:GITHUB_STEP_SUMMARY -Encoding utf8 }
$lines | ForEach-Object { Write-Host $_ }

if ($worse) {
    Write-Host ''
    foreach ($item in $worse) {
        $label = if ($names.ContainsKey($item.Prefix)) { $names[$item.Prefix] } else { $item.Prefix }
        Write-Host "$label reports $($item.Now) problems, up from $($item.Was). Read the artifact before lowering the baseline." -ForegroundColor Red
    }
    exit 1
}

if ($missing) {
    Write-Host ''
    $labels = ($missing | ForEach-Object { if ($names.ContainsKey($_)) { $names[$_] } else { $_ } }) -join ', '
    Write-Host "No verdict for: $labels. The verifier did not reach them, so this run proves nothing about them." -ForegroundColor Red
    exit 1
}

if ($unreadable) {
    Write-Host ''
    Write-Host "Could not read the verdict for: $($unreadable -join ', ')" -ForegroundColor Red
    exit 1
}

Write-Host ''
Write-Host 'No target is worse than its baseline.' -ForegroundColor Green
