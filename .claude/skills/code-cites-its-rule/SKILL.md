---
name: code-cites-its-rule
description: Every code change updates the documentation in the same commit, and every method a tester can reach names the rule or use case it implements. Use when writing or changing any Java in Testin, when reviewing a diff, and when a method has no comment saying which documented behavior it carries. Triggers on "write this method", "fix this", "change what this does", "review this change".
---

# Code cites its rule

Testin's documentation is not a description written after the fact. It is the
specification: 151 use cases and 610 rules under `docs/`, each one numbered so
it can be pointed at.

Two things follow, and they are not optional.

> ## The code says which rule it carries, and the rule changes when the code does.

## The two halves

**1. Every method a tester can reach names its rule or its use case.**

```java
// UC-TREE-PANEL-012, Rule-TREE-PANEL-038
public void removeSelected(final @NotNull List<DirectoryDto> nodes) {
```

**2. Every change to what a tester sees changes the document in the same
commit.** A commit that changes what a key does, and does not change the step
that names that key, is incomplete. The review should say so.

This is the whole reason the documentation lives in this repository rather than
beside it.

## How to write the marker

The marker is one line, above the signature. Never on the signature line: a
method declaration is one line, however long, and a marker must not be the thing
that wraps it.

| What the method carries | The marker |
|---|---|
| One use case | `// UC-TREE-PANEL-012` |
| A rule inside one use case | `// UC-TREE-PANEL-012, Rule-TREE-PANEL-038` |
| A rule that holds across a whole part | `// Rule-TREE-PANEL-007` |
| Several use cases | `// UC-EDITOR-PANEL-032, UC-EDITOR-PANEL-033` |

**A rule name carries its part.** `Rule-TREE-PANEL-044` says where it lives, so
the marker needs nothing else to be findable. It was not always so: rules were
bare numbers until 7 September 2026, and there were eight different Rule44s. A
marker still reading `Rule44` is from before that and names nothing.

**The numbers run in reading order, with no gaps.** A part's rules are numbered
down its own `main.md`: the rules that hold everywhere first, then use case by
use case in the order that page lists them. So a rule added to an early page
renumbers the ones after it, and that renumbering is part of the change, not a
tidy-up for later.

Where the method has javadoc, the marker is a line of its own inside it, first:

```java
/**
 * UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-141.
 * <p>
 * Escape records nothing at all, neither the detail nor the verdict, because a
 * half-written failure is worse than no failure.
 */
```

## The eight prefixes

| Part | Its use cases and rules | Its folder |
|---|---|---|
| The tree panel | `UC-TREE-PANEL-001` and `Rule-TREE-PANEL-001` and up | `docs/treePanel/` |
| The editor panel | `UC-EDITOR-PANEL-001` and `Rule-EDITOR-PANEL-001` and up | `docs/editorPanel/` |
| The view panel | `UC-VIEW-PANEL-001` and `Rule-VIEW-PANEL-001` and up | `docs/viewPanel/` |
| The settings page | `UC-SETTING-001` and `Rule-SETTING-001` and up | `docs/setting/` |
| Automation code and the gutter | `UC-CODEGEN-001` and `Rule-CODEGEN-001` and up | `docs/codegen/` |
| Reports | `UC-REPORT-001` and `Rule-REPORT-001` and up | `docs/report/` |
| Sharing work with the team | `UC-SHARE-001` and `Rule-SHARE-001` and up | `docs/share/` |
| Inside Testin | `UC-INTERNAL-001` and `Rule-INTERNAL-001` and up | `docs/internal/` |

The numbering is defined once, in `docs/standard.md`. Read it before inventing
anything.

## The identifier has to exist

A marker naming a use case or a rule that is not in `docs/` is worse than no
marker, because the next reader trusts it and goes looking. Two ways to be sure:

```bash
grep -rn "UC-TREE-PANEL-012" docs/          # the use case page and everything pointing at it
grep -rn "\*\*Rule-TREE-PANEL-038\*\*" docs/   # the rule, on the page that owns it
```

## When there is no rule yet

**Write the rule first.** `docs/standard.md` says it in one line: *if a use case
needs a rule nobody has written, the rule is written first.*

That is not ceremony. Writing the rule is where the decision gets made, and a
rule that cannot be written in one plain sentence is usually a sign the design
is wrong. Three of the six critical defects found in September 2026 were code
that contradicted a rule the codebase had already written down somewhere else.

If the behavior is genuinely new, add the rule to the part's page and give it
the next number. Each part's `main.md` carries a **Last rule** row saying what
that number is; move it on in the same commit, and cite the rule from the code.

## When a method carries no rule

Not every method is a rule. A method carries no marker when:

- it is a helper nobody outside its own class calls, and it implements no
  behavior a tester can name;
- it is framework plumbing: a constructor, a getter, a Swing listener that only
  forwards;
- it is a test.

The test is simple. **Could a tester describe what this method does, without
being shown the code?** If yes, it has a rule, and the marker names it. If no,
it is plumbing, and a marker would be noise.

## When a change needs no document change

A refactor that changes nothing a tester can see changes no document. That is
not an exception to the rule, it is the test of whether it really was a
refactor.

So the question at the end of a change is always: **what would a tester
notice?**

- Nothing → no document change. Say so in the commit body.
- Something → the document changes in this commit, not the next one.
- Something, and no document covers it → write the rule first, then the code.

## What a complete change looks like

A key that used to do one thing and now does another:

1. The step in the use case page that names the key, rewritten.
2. The rule, if the rule itself changed.
3. The part's key table, if the key moved or is new.
4. The `Checked against` line in that part's `main.md`, moved to this commit.
5. The code, carrying the marker.
6. One commit.

## Two things this is not

**It is not a licence to write comments that repeat the code.** The marker says
*which documented behavior this is*. It does not describe what the lines do. A
comment that says why still earns its place; a comment that narrates the next
line still does not.

**It is not a reason to weaken the documentation to match the code.** When the
code and the document disagree, one of them is wrong and the disagreement is
worth stating. Testin's eight `main.md` pages each end with the places the
plugin breaks its own rules, and 97 of those are open issues. Adding to that
list is an honest outcome. Quietly rewriting a rule so today's code satisfies it
is not.

## Checking a diff

Before offering a change, ask three questions in this order:

1. **Which rule or use case does each new or changed method carry?** If a method
   a tester can reach has no marker, add one, or say why it needs none.
2. **Does the marker name something that is really in `docs/`?** Grep it.
3. **Would a tester notice this change?** If yes, is the document in this diff?

A change that answers all three is complete. A change that cannot answer the
first is usually a change nobody asked for.
