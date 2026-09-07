#Requires -Version 7

<#
.SYNOPSIS
    Writes a new rule into the documents, on every page it governs.

.DESCRIPTION
    A rule that holds for a whole part is written out on every page in that part,
    so adding one by hand means editing up to 46 files and getting the same words
    into all of them. RuleNumbersTest catches a copy that drifts, but it cannot do
    the typing - this can.

    It takes the number after the part's last, which is what the Numbering row of
    the part's main.md says, and moves that row on. No existing rule is renumbered:
    a number is a name and names do not move, which is what docs/standard.md sets
    out.

    Where the rule goes on a page:

      -Everywhere   after the last rule the part's pages all share, which is where
                    the shared block ends. The pages that share a rule are worked
                    out rather than listed, so the tool cannot go stale.
      -Page         at the end of that one page's rules, because a rule about one
                    use case belongs with that use case's own.

.PARAMETER Part
    The folder under docs, as it is named there: editorPanel, treePanel, share,
    viewPanel, setting, codegen, report, internal.

.PARAMETER Text
    The rule itself, in one sentence or several. Wrapped to 80 columns here, so it
    can be typed as one line.

.PARAMETER Everywhere
    The rule holds for the whole part, so it is written on every page in it.

.PARAMETER Page
    The file name of the one use case page the rule belongs to, such as
    editGridCell.md.

.PARAMETER WhatIf
    Says what it would write and changes nothing.

.EXAMPLE
    ./tools/add-rule.ps1 -Part editorPanel -Page editGridCell.md `
        -Text "A cell that no longer shows what was typed into it says so."

.EXAMPLE
    ./tools/add-rule.ps1 -Part treePanel -Everywhere `
        -Text "A node that cannot be reached from the tree is not drawn in it."
#>

[CmdletBinding(DefaultParameterSetName = 'OnePage', SupportsShouldProcess)]
param(
    [Parameter(Mandatory)][string] $Part,
    [Parameter(Mandatory)][string] $Text,
    [Parameter(Mandatory, ParameterSetName = 'Everywhere')][switch] $Everywhere,
    [Parameter(Mandatory, ParameterSetName = 'OnePage')][string] $Page
)

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$folder = Join-Path $root "docs/$Part"
$main = Join-Path $folder 'main.md'

if (-not (Test-Path $main)) { throw "No such part: $folder has no main.md" }

# The prefix a part's rules carry, read from the part rather than from a table
# here - a second list of these is a second thing to keep in step.
$mainText = Get-Content $main -Raw
if ($mainText -notmatch 'Rules are `Rule-([A-Z][A-Z-]*)-\d+` to `Rule-[A-Z][A-Z-]*-(\d+)`') {
    throw "$main has no Numbering row saying the range its rules cover"
}

$prefix = $Matches[1]
$last = [int]$Matches[2]
$next = $last + 1
$name = 'Rule-{0}-{1:D3}' -f $prefix, $next

# One bullet, wrapped the way the documents are: 80 columns, two spaces under the
# dash so the continuation lines up with the words rather than the marker.
function Format-Rule([string] $ruleName, [string] $words) {
    $line = "- **$ruleName** — $words"
    $out = @()
    $current = ''

    foreach ($word in ($line -split '\s+')) {
        $indent = if ($out.Count -eq 0) { '' } else { '  ' }
        if ($current -eq '') { $current = $word; continue }
        if (($indent + $current + ' ' + $word).Length -gt 80) {
            $out += $current
            $current = $word
        }
        else { $current = $current + ' ' + $word }
    }
    if ($current -ne '') { $out += $current }

    return (($out | Select-Object -First 1) + "`n" + (($out | Select-Object -Skip 1 | ForEach-Object { "  $_" }) -join "`n")).TrimEnd()
}

$bullet = Format-Rule $name $Text

# Which pages get it. Everywhere means every use case page in the part; the shared
# block is where those pages agree, which is read off the pages themselves.
$pages = Get-ChildItem $folder -Filter '*.md' | Where-Object { $_.Name -ne 'main.md' }

if ($PSCmdlet.ParameterSetName -eq 'OnePage') {
    $pages = $pages | Where-Object { $_.Name -eq $Page }
    if (-not $pages) { throw "No such page: $Page under docs/$Part" }
}

# The numbers every page carries, which is the shared block by definition.
$shared = $null
foreach ($file in $pages) {
    $numbers = [regex]::Matches((Get-Content $file.FullName -Raw), '(?m)^- \*\*Rule-[A-Z][A-Z-]*-(\d+)\*\*') |
        ForEach-Object { [int]$_.Groups[1].Value }

    if ($null -eq $shared) { $shared = [System.Collections.Generic.HashSet[int]]::new([int[]]$numbers) }
    else { $shared.IntersectWith([int[]]$numbers) }
}

$lastShared = if ($Everywhere -and $shared.Count -gt 0) { ($shared | Measure-Object -Maximum).Maximum } else { -1 }

$written = 0
foreach ($file in $pages) {
    $text = Get-Content $file.FullName -Raw
    $rules = [regex]::Matches($text, '(?m)^- \*\*Rule-[A-Z][A-Z-]*-(\d+)\*\*[\s\S]*?(?=\r?\n- \*\*Rule-|\r?\n\r?\n)')

    if ($rules.Count -eq 0) {
        Write-Warning "$($file.Name) has no rules to add to, so it was left alone"
        continue
    }

    # After the last shared rule for a part-wide one, and after the page's own
    # last rule otherwise.
    $after = if ($lastShared -ge 0) {
        ($rules | Where-Object { [int]$_.Groups[1].Value -le $lastShared } | Select-Object -Last 1)
    }
    else { $rules[$rules.Count - 1] }

    $at = $after.Index + $after.Length
    $updated = $text.Substring(0, $at) + "`n" + $bullet + $text.Substring($at)

    if ($PSCmdlet.ShouldProcess($file.Name, "add $name")) {
        Set-Content -Path $file.FullName -Value $updated -NoNewline
    }
    $written++
}

# The Numbering row moves on, which is what the next writer reads.
$movedOn = $mainText -replace "(Rules are ``Rule-$prefix-\d+`` to ``Rule-$prefix-)\d+(``)", "`${1}$('{0:D3}' -f $next)`$2"
if ($PSCmdlet.ShouldProcess('main.md', "say the rules now end at $name")) {
    Set-Content -Path $main -Value $movedOn -NoNewline
}

Write-Host ""
Write-Host $bullet
Write-Host ""
Write-Host "Written on $written page(s) in docs/$Part. main.md now says the rules end at $name."
Write-Host "Run ./gradlew :test --tests '*RuleNumbersTest*' to check every copy agrees."
