# Testin documentation

> Reading this in the repository? The same pages, laid out for reading, are at
> **[mtb550.github.io/test-in](https://mtb550.github.io/test-in/)**.

Everything about the plugin that is not the code: what it is for, what it does,
how each screen looks, and how to work on it.

New here? Start with the [project README](https://github.com/mtb550/test-in#readme).
It says what Testin is and how to install it.

---

## The three documents

Testin is described three times. Each one answers a different question. Read
them in this order.

| | Answers | Where it stands |
|---|---|---|
| **[Business requirements](business-requirements/business-requirements.md)** | Why each thing exists, and the rules that always hold | One part of six is written. The product-wide document is a draft |
| **[System requirements](system-requirements/system-requirements.md)** | What happens, step by step, and every key | One part of six is written |
| **[Design](design/design.md)** | Every screen, drawn, and why it looks that way | One part of six, plus light mode |

Each of the three has an index page that lists its documents.

Writing one? Read **[How a document is written](standard.md)** first.

## What is written so far

Testin has six parts. Each part gets all three documents. One part is finished.

| Part of Testin | Business requirements | System requirements | Design |
|---|---|---|---|
| **The project panel**, the tree on the left | [Written](business-requirements/project-panel.md) | [Written](system-requirements/project-panel.md) | [Written](design/project-panel.md) |
| The test case editor | Not written | Not written | Not written |
| The test run editor | Not written | Not written | [Light mode](design/light-mode.md) |
| The view panel | Not written | Not written | Not written |
| The settings page | Not written | Not written | Not written |
| Reports, export, import and sync | Not written | Not written | Not written |

There is one more document above the six: **[the product](business-requirements/product.md)**,
which holds what is true of all of them. It is a draft, moved from Notion, and it
has not been checked against today's plugin.

The plan, and the order the six are written in, is
[#181](https://github.com/mtb550/test-in/issues/181).

## For testers

You installed the plugin and want to use it well.

| Document | What it answers | Where it stands |
|---|---|---|
| **Keyboard reference** | Every key Testin answers to, on every surface | Not written — [#73](https://github.com/mtb550/test-in/issues/73) |
| **First run** | From installing the plugin to a first verdict, in ten minutes | Not written — [#104](https://github.com/mtb550/test-in/issues/104) |

## For contributors

What a person needs before their first change.

| Document | What it answers | Where it stands |
|---|---|---|
| **Architecture** | The layers, the rule that all file access goes through one place, and two walkthroughs | Not written — [#99](https://github.com/mtb550/test-in/issues/99) |
| **Contributing** | Setup, the checks that must pass, and the run configurations | Not written — [#102](https://github.com/mtb550/test-in/issues/102) |
| **Standing decisions** | Decisions made once, so they are not argued again in every review | Not written — [#101](https://github.com/mtb550/test-in/issues/101) |
| **Formats on disk** | The seven markers, the project file and the sequence store. How to read a test project without the plugin | Not written — [#100](https://github.com/mtb550/test-in/issues/100) |

---

## How these documents stay true

- Every sentence can be checked against the plugin. Planned behavior is marked as planned.
- Known defects are named, not hidden.
- Every rule and every use case has a number, so an issue or a commit can point at one. The numbering is explained in [How a document is written](standard.md).
- When the plugin changes, the document changes in the same commit.
- Every document says which version of the plugin it was checked against.
- Not written is a state. Every missing document is listed with the issue that will write it.
- One fact, one home. When two documents would say the same thing, one says it and the other links to it.
