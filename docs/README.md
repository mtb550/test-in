# Testin documentation

> Reading this in the repository? The same pages, laid out for reading, are at
> **[mtb550.github.io/test-in](https://mtb550.github.io/test-in/)**.

Everything about the plugin that is not the code: what it is for, what it does,
what every screen looks like, and how to work on it.

New here? Start with the [project README](https://github.com/mtb550/test-in#readme).
It says what Testin is and how to install it.

---

## The documents

Testin has six parts. Each part gets a folder, and every thing a tester does in
it gets a page of its own: the story, its rules, its screens, what happens step
by step, and every way it can be refused.

| Document | What it covers | Where it stands |
|---|---|---|
| **[The product](product.md)** | What is true of all six parts: who uses Testin, what they work with, every status, and the rules that hold everywhere | A draft, moved from Notion, not re-checked against today's plugin — [#72](https://github.com/mtb550/test-in/issues/72) |
| **[The project panel](projectPanel/main.md)** | The tree on the left. 20 use cases, 76 rules, 14 screens | **Written** |
| The test case editor | Writing and reading test cases: the grid, the card list and the details panel | Not written |
| The test run editor | Running tests and recording verdicts. **[Light mode](lightMode.md)** is one of its windows, and is written | Not written, except light mode |
| The view panel | Details, history and bugs of the selected test case | Not written |
| The settings page | Everything set once per machine | Not written |
| Reports, export, import and sync | Getting work in and out of Testin | Not written |

The plan, and the order the parts are written in, is
[#181](https://github.com/mtb550/test-in/issues/181).

Writing one? Read **[How a document is written](standard.md)** first.

## For testers

You installed the plugin and want to use it well.

| Document | What it answers | Where it stands |
|---|---|---|
| **Keyboard reference** | Every key Testin answers to, on every screen | Not written — [#73](https://github.com/mtb550/test-in/issues/73) |
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
- Every rule, use case and scenario has a number, so an issue or a commit can point at one. The numbering is explained in [How a document is written](standard.md).
- When the plugin changes, the document changes in the same commit.
- Every document says which version of the plugin it was checked against.
- Not written is a state. Every missing document is listed with the issue that will write it.
- One fact, one place. Nothing is written twice.
