# Testin documentation

> Reading this in the repository? The same pages, laid out for reading, are at
> **[mtb550.github.io/test-in](https://mtb550.github.io/test-in/)**.

Everything about the plugin that is not the code: what it is for, what it does,
how each screen looks, and how to work on it.

New here? Start with the [project README](https://github.com/mtb550/test-in#readme).
It says what Testin is and how to install it.

---

## The three specifications

Read them in this order. Each has an index that lists its documents.

| | Answers | Owns | State |
|---|---|---|---|
| **[Business requirements](business-requirements/business-requirements.md)** | Why each thing exists, and the rules that always hold | `BR` `UC` `Q` | The product: **[Draft 1](business-requirements/product.md)** — [#72](https://github.com/mtb550/test-in/issues/72). By module: **1 of 6** |
| **[System requirements](system-requirements/system-requirements.md)** | What happens, step by step, and every key | `SR` | The product: not written — [#180](https://github.com/mtb550/test-in/issues/180). By module: **1 of 6** |
| **[Design](design/design.md)** | Every screen, drawn, and why it looks that way | cites the above | By module: **1 of 6**, plus light mode |

Writing one? Read **[How a document is written](standard.md)** first.

**By module.** Each module gets all three. The same use case has the same `UC`
id in the first two, and the design cites it.

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
| **Keyboard reference** | Every key Testin answers to, on every surface | Not written — [#73](https://github.com/mtb550/test-in/issues/73) |
| **First run** | From installing the plugin to a first verdict, in ten minutes | Not written — [#104](https://github.com/mtb550/test-in/issues/104) |

## For contributors

What a person needs before their first change.

| Document | What it answers | State |
|---|---|---|
| **Architecture** | The layers, the indexer-only file access rule, and two walkthroughs | Not written — [#99](https://github.com/mtb550/test-in/issues/99) |
| **Contributing** | Setup, the checks that must pass, the run configurations | Not written — [#102](https://github.com/mtb550/test-in/issues/102) |
| **Standing decisions** | Decisions made once, so they are not argued again in every review | Not written — [#101](https://github.com/mtb550/test-in/issues/101) |
| **Formats on disk** | The seven markers, `testin.yml` and the sequence store. How to read a test project without the plugin | Not written — [#100](https://github.com/mtb550/test-in/issues/100) |

---

## Identifiers

Anything worth citing has a number. **Cite the number, not the sentence.** A
number survives a reworded sentence.

| Prefix | Is | Lives in |
|---|---|---|
| `UC-nn` | A use case. One thing a tester does | business requirements and system requirements, same id in both |
| `BR-nn` | A business rule. Something that must always hold | business requirements |
| `Q-nn` | An open question. Undecided, so listed instead of guessed | business requirements |
| `SR-nn` | A scenario. What happens, step by step, for one use case | system requirements |

A module's identifiers carry its code: `BR-PP-04`. The codes are in
[the standard](standard.md#identifiers).

```
UC-PP-01   the use case          the same id in both specifications
  │
  ├── BR-PP-nn   what must hold          business-requirements/project-panel.md
  ├── SR-PP-nn   what happens, and when  system-requirements/project-panel.md
  └── a screen   what the tester sees    design/project-panel.md
```

A design cites a rule. It never invents one. If a screen needs a new promise,
the promise is written in the business requirements first.

---

## How these documents stay true

- Every sentence can be checked against the plugin. Planned behavior is marked as planned.
- Known defects are named, not hidden.
- Rules and use cases are numbered, so an issue or a commit can cite one.
- When the plugin changes, the document changes in the same commit.
- Every document says which commit it was checked against.
- Not written is a state. Every missing document is listed with the issue that will write it.
- One fact, one home. When two documents would say the same thing, one says it and the other links to it.
