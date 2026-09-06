# Testin documentation

> Reading this in the repository? The same pages, laid out for reading, are at
> **[mtb550.github.io/test-in](https://mtb550.github.io/test-in/)**.

Everything about the plugin that is not the code: what it is for, who uses it,
how each screen was decided, and how to work on it.

New here? Start with the [project README](../README.md) — what Testin is, how to
install it and what it does. Then come back for the detail.

---

## The three specifications

Each is a folder with its own index. Read them in this order: the first says what
is promised, the second what must be true for the promise to hold, the third what
the tester actually sees.

**Writing one?** Read **[How a document is written](standard.md)** first. It says
which of the three a fact belongs in, the form each takes, and the identifiers
that link them — so the same fact is never written twice.

| | Answers | Owns | State |
|---|---|---|---|
| **[Business requirements](business-requirements/business-requirements.md)** | What Testin promises, and to whom | `BR` `UC` `Q` | The product: **[Draft 1](business-requirements/product.md)** — [#72](https://github.com/mtb550/test-in/issues/72). By module: **1 of 6** |
| **[System requirements](system-requirements/system-requirements.md)** | What the software must do to keep those promises | `SR` | Product-wide: not written — [#180](https://github.com/mtb550/test-in/issues/180). By module: **1 of 6** |
| **[Design](design/design.md)** | What the tester sees on each screen, and why | cites the above | By module: **1 of 6**, plus light mode |

**By module**, three documents each — the same use case carries the same `UC`
id across the first two, and the design cites both:

| Module | Business requirements | System requirements | Design |
|---|---|---|---|
| **Project panel** `PP` | [Written](business-requirements/project-panel.md) | [Written](system-requirements/project-panel.md) | [Written](design/project-panel.md) |
| Test case editor `TE` | — | — | — |
| Test run editor `RE` | — | — | [Light mode](design/light-mode.md) |
| View panel `VP` | — | — | — |
| Settings `ST` | — | — | — |
| Evidence and exchange `EX` | — | — | — |

The plan and the order are [#181](https://github.com/mtb550/test-in/issues/181).

## For testers

You installed the plugin and want to use it well.

| Document | What it answers | State |
|---|---|---|
| **Keyboard reference** | Every key Testin answers to, on every surface. The plugin is built to be driven from the keyboard, so this is the page that makes it fast. | Not written — [#73](https://github.com/mtb550/test-in/issues/73) |
| **First run** | Ten minutes from installing the plugin to a test case with a verdict on it. | Not written — [#104](https://github.com/mtb550/test-in/issues/104) |

## For contributors

What a person needs before their first change.

| Document | What it answers | State |
|---|---|---|
| **Architecture** | The layer map, the indexer-only file access rule and its exempt list, and two walkthroughs a newcomer can follow end to end. | Not written — [#99](https://github.com/mtb550/test-in/issues/99) |
| **Contributing** | Setup, the checks that must pass, the run configurations and the IDE compatibility rule. | Not written — [#102](https://github.com/mtb550/test-in/issues/102) |
| **Standing decisions** | The calls made once that should not be re-argued in every review — one owner for anything shared, absence is an empty value rather than a null, a method declaration is one line. | Not written — [#101](https://github.com/mtb550/test-in/issues/101) |
| **Formats on disk** | The seven marker formats, `testin.yml` and the sequence store. What lets somebody read a test project without the plugin installed. | Not written — [#100](https://github.com/mtb550/test-in/issues/100) |

---

## Identifiers, and how to trace one

Anything worth citing has a number. **Cite the number, never the sentence** — a
number survives the text being reworded, and a quoted sentence does not.

| Prefix | Is | Lives in |
|---|---|---|
| `BR-nn` | A business rule. Something the product promises | business requirements |
| `UC-nn` | A use case. One capability, in full, with the key that starts it | business requirements |
| `Q-nn` | An open question. Something genuinely undecided, listed rather than guessed | business requirements |
| `SR-nn` | A system requirement. Something that must be true for a `BR` to hold | system requirements |

The chain, and what each link adds:

```
BR-nn   the promise              business-requirements/
  │     "a tester may record exactly three verdicts"
  ▼
SR-nn   what must be true        system-requirements/
  │     "P, F and B record a verdict; no other key does, on any surface"
  │
  ├──▶  a design document        design/       what the tester sees, and why
  ├──▶  a test                   src/test/     what proves it
  └──▶  the javadoc              src/main/     how it is done, and why that way
```

**It runs downward only.** A design document may cite a rule and must not invent
one: if a screen needs a promise nothing has made, that is a change to the
business requirements and the screen waits for it. Likewise an `SR` that serves no
`BR` is either a promise nobody wrote down or work nobody needs — both worth
finding before the code is.

---

## How these documents are kept honest

**Every statement is checkable against the product.** No aspiration in a main
body — planned behaviour is quarantined in an appendix and labelled as not
existing.

**Known defects are named, not hidden.** A specification that omits its own
exceptions stops being worth reading.

**Rules and use cases are numbered**, so an issue or a commit can cite one.

**When behaviour changes, the document changes in the same breath.** A rule that
quietly stops being true is worse than no rule, because someone will plan against
it. This is the whole reason the documentation lives in this repository: a change
to the plugin and the change to its documentation are one commit and one review.

**Each document says which commit it was written against**, so a reader can tell
whether it is still true rather than having to guess.

**Not written is a state, not an omission.** Every document above that does not
exist is listed with the issue that will write it, so this index is the roadmap as
well as the table of contents.

**One document, one owner.** A BRD and a BRS are the same document under two
names, so there is one of them. Where two documents would answer the same
question, there is one document and the other cites it. Every page here is
Markdown, so there is no second format for a document to exist in.
