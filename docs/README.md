# Testin documentation

> Reading this in the repository? The same pages, laid out for reading, are at
> **[mtb550.github.io/test-in](https://mtb550.github.io/test-in/)**.

Everything about the plugin that is not the code: what it is for, what it does,
what every screen looks like, and how to work on it.

New here? Start with the [project README](https://github.com/mtb550/test-in#readme).
It says what Testin is and how to install it. Then read
**[the tree panel](treePanel/main.md)**, which is where everything in Testin
starts.

---

## The documents

Testin has eight parts. Each gets a folder, and every thing a tester does in it
gets a page of its own: the story, its rules, its screens, what happens step by
step, and every way it can be refused.

| Document | What it covers | Use cases | Rules |
|---|---|---|---|
| **[The product](product.md)** | What is true of all of it: who uses Testin, what they work with, every status, and the rules that hold everywhere | — | — |
| **[The tree panel](treePanel/main.md)** | The tree on the left. Test projects, test sets, test runs, and everything done to them | 27 | 88 |
| **[The editor panel](editorPanel/main.md)** | Writing test cases, and executing a test run. Both editors, and [light mode](editorPanel/lightMode.md) | 46 | 189 |
| **[The view panel](viewPanel/main.md)** | The panel on the right. One test case in full, and what a test run recorded about it | 15 | 59 |
| **[The settings page](setting/main.md)** | Everything set once per machine, and where each value is kept | 11 | 39 |
| **[Automation code and the gutter](codegen/main.md)** | The test methods Testin writes, and how they are kept in step with the tree | 20 | 66 |
| **[Reports](report/main.md)** | Writing a test run out as a document, in four formats | 3 | 15 |
| **[Sharing work with the team](share/main.md)** | Export, import, Git and a server. Every way test data leaves and arrives | 23 | 101 |
| **[Inside Testin](internal/main.md)** | The parts that belong to no panel: the search, and the one thing that owns every file | 6 | 51 |

**151 use cases and 608 rules**, each one checked against the code it describes.
Every part also lists where the plugin breaks its own rules, and what nobody has
decided yet.

The plan is [#181](https://github.com/mtb550/test-in/issues/181).

Writing one? Read **[How a document is written](standard.md)** first.

## For testers

You installed the plugin and want to use it well.

| Document | What it answers | Where it stands |
|---|---|---|
| **Keyboard reference** | Every key Testin answers to, on every screen | Not written — [#73](https://github.com/mtb550/test-in/issues/73). Each part lists its own keys today |
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
- Known defects are named, not hidden. Each part ends with the places it breaks its own rules.
- Every rule and use case has a number, so an issue or a commit can point at one. The numbering is explained in [How a document is written](standard.md).
- When the plugin changes, the document changes in the same commit.
- Every document says which version of the plugin it was checked against.
- Not written is a state. Every missing document is listed with the issue that will write it.
- One fact, one place. Nothing is written twice.
