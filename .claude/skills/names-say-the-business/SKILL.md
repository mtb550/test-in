---
name: names-say-the-business
description: Every class, method, field, variable and heading is named with the business word, in full - a test case is a test case, never a "case". Use when adding or renaming anything in Testin, when reading a name that shortens a domain word, and when writing docs or an issue about one. Triggers on "case", "item", "run", "set", "node" standing alone in a name.
---

# Names say the business word, in full

Testin's vocabulary is fixed, and `docs/standard.md` writes it down: a **test
case**, a **test set**, a **test run**, a **run item**, a **test project**. Every
name in the code, the docs and the issues uses the whole word.

> ## `CaseDetails` is not a thing. `TestCaseDetails` is.

Muteb, 22 September 2026: *"[I] found class called CaseDetails, it is
TestCaseDetails not CaseDetails... this is not acceptable, as it belongs to test
case. correct name will allow me and contributors to understand easily and any
name should match our correct names in business."*

## The rule

**Name it after the thing it is about, with the business word spelled out.**

| Not this                    | This                              | Why                                                                          |
|-----------------------------|-----------------------------------|------------------------------------------------------------------------------|
| `CaseDetails`               | `TestCaseDetails`                 | A case is a court case. The plugin has test cases                            |
| `caseId`, `caseIds`         | `testCaseId`, `testCaseIds`       | The id of what?                                                              |
| `cases`                     | `testCases`                       | Even where the type says `List<TestCaseDto>`: the reader is reading the name |
| `shownCase`, `liveCase`     | `shownTestCase`, `liveTestCase`   | Same word, same thing, everywhere                                            |
| `item` on its own           | `runItem`                         | A run item is a test case's row in a test run                                |
| `A_CASE`, `JUDGED_CASE`     | `A_TEST_CASE`, `JUDGED_TEST_CASE` | Constants are names too                                                      |
| `aCaseThatArrivesIsPending` | `aTestCaseThatArrivesIsPending`   | A test's name is a sentence about the business                               |

It holds for the documentation and for issues as well: *test case id*, never
*case id*; *run item*, never *item*.

## The two short names, and there are only two

`p` is always the `Project`, and `tc` is always one `TestCaseDto`. Both are
written down: `p` in CLAUDE.md, `tc` in the code everywhere. They are the
exception because they are universal - every file uses them the same way - and
because they name the type rather than shorten the business word.

Anything else gets its full name. A local called `dto`, `obj`, `data` or `list`
says nothing that the line above it did not already say.

## Why the whole word

A name is read far more often than it is written, and by people who did not
write it. **The reader cannot look up what "case" means here** - they have to
find the declaration, read the type, and carry the answer back. A shortened
domain word also collides: `case` is a Java keyword, letter case, a use case and
a court case, so every reader pays for the ambiguity.

The other half is search. `grep -rn "TestCase"` finds every place the concept
lives. Every name that says `Case` instead is a place the search misses.

## What a rename costs, and how to do it

The sweep on 22 September 2026 renamed 10 types and about 600 identifiers across
137 files - `CaseDetails`, `CaseList`, `CaseFont`, `CopiedCase`, `MovedCase`,
`EditShownCase`, `ShownCaseAction` and the rest. The compiler and 767 tests
verified it, and no string, bundle key or stored value changed.

Renaming is a three-step check, not a find-and-replace. CLAUDE.md states it:

1. **Never touch a string.** A bundle key, a marker file name, a stored JSON
   field and a `@State` name all read like code and are none of it.
2. **Check the new name is free in that file.** `cases` renamed to `testCases`
   where a `testCases` already exists is two variables with one name.
3. **Compile and run the tests.** A rename that compiles and passes is done; a
   rename that needs a comment explaining it is not.

## Checks before you name something

1. Which business word is this? Write it out in full.
2. Would a contributor who has never read this file know what the name holds?
3. Does the name repeat what the type already says - `dto`, `list`, `obj`? Say
   the business word instead.
4. Is it one of the two short names, `p` and `tc`? Then it is fine. If you are
   inventing a third, you are naming something that deserves its own word.
