---
name: testin-story
description: Write or update a Testin backlog item as a GitHub issue on mtb550/test-in in the house format. Use when the user describes a feature, bug, refactor or chore for the Testin plugin and wants it captured, or asks to rewrite/tidy an existing issue. Triggers on "make this a story", "open an issue for this", "add to the backlog", "write this up".
allowed-tools: Bash(gh issue create:*), Bash(gh issue edit:*), Bash(gh issue view:*), Bash(gh issue list:*), Bash(gh search issues:*), Read, Grep, Glob, Write
---

# Testin story format

Backlog items for Testin live as GitHub issues on `mtb550/test-in`. Never write
them to a local file — Muteb works across several machines and a local file does
not travel.

Always pass `--repo mtb550/test-in` explicitly. The git remote still reads
`TestGit.git`; GitHub redirects, but `gh` inference is unreliable through a
rename.

## Before writing

1. `gh issue list --repo mtb550/test-in --state open --limit 100` — check whether
   this already exists. If it does, edit that issue rather than opening a second.
2. Ground the write in the code. Read the classes involved and name real paths;
   an issue that names a class that does not exist costs more time than it saves.
   If the backlog refers to a class by an old name, say so and give the real one.
3. If recent commits may already have fixed it, add a **Check first** section
   listing the commit hashes and say to close the issue rather than implement it
   if the work is already done.

## Structure

Feature, refactor or chore:

```markdown
# Story

As a <role>, I want <capability>, so that <benefit>.

## Problem

What is wrong today, concretely. Skip for pure new features.

## Scope

What this issue covers, as prose or a short numbered list. Be explicit about
what is deliberately excluded.

## Implementation notes

Mechanism, gotchas, threading, ordering. Include a code snippet when a specific
API or pattern is the point.

## Touches

`package/Class`, `package/OtherClass` — the real paths.

## Acceptance criteria

- [ ] One observable outcome per line.
- [ ] Written so it can be checked without reading the diff.
```

Bug:

```markdown
# Bug

One-paragraph statement of the defect.

## Observed

- What happens, and where.
- Reproduction conditions: version, IDE build, theme, platform.

## Where to look

`Class:line` references with what each does, plus ranked suspects for the cause.

## Acceptance criteria

- [ ] ...
```

## Rules

- Acceptance criteria are observable outcomes, not implementation steps. "Rows
  fit their content in every theme" — not "change `setRowHeight`".
- Include a criterion for what must **not** change. Regressions in the other
  editor, the other theme, or the other platform are the usual failure.
- When a real design decision is unresolved, add an **Open question** or
  **Design decision required** section stating the options and a recommendation.
  Do not resolve it silently in the issue text.
- Respect the architecture rules in `CLAUDE.md` — indexer-only file access,
  Swing on the EDT, display-only formatting. An issue that proposes violating
  one of them should say why the exception is justified.
- Keep the body in Markdown, plain prose, no emoji.

## Writing it

Stage the body in a scratch file, then:

```bash
gh issue create --repo mtb550/test-in --title "<title>" --body-file <path>
gh issue edit <n> --repo mtb550/test-in --body-file <path>
```

Titles are a specific claim, not a category: "Grid rows render oversized in all
themes except Darcula", not "Grid bug". Report the issue number and URL back.
