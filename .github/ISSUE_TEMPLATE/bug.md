---
name: Bug
about: Something Testin does that it should not, or does not do that it should
title: ""
labels: bug
---

<!--
  Two labels before this is ready to pick up: priority: (critical|high|medium|low)
  and cost: (nocost|minor|major|expensive). See CONTRIBUTING.md.

  The title is a specific claim, not a category: "Grid rows render oversized in
  every theme except Darcula", not "Grid bug".
-->

# Bug

One paragraph. What is wrong, and what happens to a tester because of it.

## Observed

- What happens, and where.
- The conditions: plugin version, IDE build, theme, platform.
- What you expected instead.

## Where to look

`package/Class:line` references, with what each one does, and the ranked
suspects for the cause. Say what you checked and ruled out.

## Statistics

**Measured against `main`, not estimated.** How many places show this, how many
callers are affected, how many of them already handle it correctly. A count with
a denominator: "20, which is below the project average" says something that "20"
does not.

Then one sentence on what the numbers mean for the fix. The table is not the
point; the conclusion drawn from it is.

## Acceptance criteria

- [ ] One observable outcome per line, checkable without reading the diff.
- [ ] One line for what must **not** change — the other editor, the other theme,
      the other platform.
