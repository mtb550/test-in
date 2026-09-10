# Testin documentation

> Reading this in the repository? The same pages, laid out for reading, are at
> **[mtb550.github.io/test-in](https://mtb550.github.io/test-in/)**.

This is everything about the plugin that is not the code. What it is for, what
it does, what every screen looks like, and every way it can say no.

You do not need to read code to use these pages. You do not need to read them in
order either. Read the part you are working in.

---

## How to read this

Four steps. Each one is a link away from the last.

1. **Start here.** This page lists the eight parts of Testin.
2. **Open the part you need.** Each part has a page that lists everything a
   tester can do in it. Every entry says, in one line, what it is for.
3. **Open the thing you want to do.** That is a use case page.
4. **Read the page.** It tells you the whole of that one job.

If you are new and do not know where to start, open
**[the tree panel](treePanel/main.md)**. Everything in Testin starts there.

## What a use case page tells you

Every page has the same shape, so once you have read one you can read them all.

| Section | What it gives you |
|---|---|
| The first line | Where you are: the documentation, the part, the use case number |
| **The story** | Who wants this, what they want, and why |
| **Rules** | Everything that is always true here. Each rule has a number so it can be pointed at |
| **The screen** | A picture of what you see, with every part of it numbered |
| **Main flow** | What happens, step by step, when nothing goes wrong |
| **What Testin refuses** | Every way it can go wrong, and what you see each time |

Some jobs open no screen. Those pages say **What the tester sees** instead, and
tell you what changes and which message appears.

## The eight parts

| Part | What it covers | Use cases | Rules |
|---|---|---|---|
| **[The tree panel](treePanel/main.md)** | The tree on the left. Test projects, test sets, test runs, and everything done to them | 27 | 92 |
| **[The editor panel](editorPanel/main.md)** | Writing test cases, and running a test run. Both editors, and [light mode](editorPanel/lightMode.md) | 47 | 208 |
| **[The view panel](viewPanel/main.md)** | The panel on the right. One test case in full, and what a test run recorded about it | 15 | 62 |
| **[The settings page](setting/main.md)** | Everything set once per machine, and where each value is kept | 11 | 42 |
| **[Automation code and the gutter](codegen/main.md)** | The test methods Testin writes, and how they are kept in step with the tree | 20 | 71 |
| **[Reports](report/main.md)** | Writing a test run out as a document, in four formats | 3 | 16 |
| **[Sharing work with the team](share/main.md)** | Export, import, Git and a server. Every way test data leaves and arrives | 22 | 104 |
| **[Inside Testin](internal/main.md)** | The parts that belong to no panel: the search, and the one thing that owns every file | 7 | 65 |

**152 use cases and 660 rules**, each one checked against the code it describes.

Every part also lists two more things at the end of its page: where the plugin
breaks its own rules today, and what nobody has decided yet. Both are honest
lists, not apologies.

## Pages about all of Testin

| Page | What it gives you |
|---|---|
| **[Every shortcut](shortcuts.md)** | Every key Testin answers to, what it does, and where |
| **[The product](product.md)** | Who uses Testin, what they work with, every status, and the rules that hold everywhere |
| **[Standing decisions](decisions.md)** | Seven designs that look wrong until you know why, and what each one costs to reverse |
| **[The formats on disk](formats.md)** | Every file Testin writes, field by field, and what a version bump promises |
| **[How a document is written](standard.md)** | Read this before writing one |

## For testers

You installed the plugin and want to use it well.

| Document | What it answers | Where it stands |
|---|---|---|
| **[Every shortcut](shortcuts.md)** | Every key Testin answers to, on every screen | Written |
| **Task guides and concepts** | The rest of the tester site | Not written — [#73](https://github.com/mtb550/test-in/issues/73) |
| **[First run](firstRun.md)** | From installing the plugin to a first verdict, in ten minutes | Written |

## For contributors

What a person needs before their first change.

| Document | What it answers | Where it stands |
|---|---|---|
| **Architecture** | The layers, the rule that all file access goes through one place, and two walkthroughs | Not written — [#99](https://github.com/mtb550/test-in/issues/99) |
| **Contributing** | Setup, the checks that must pass, and the run configurations | Not written — [#102](https://github.com/mtb550/test-in/issues/102) |
| **[Standing decisions](decisions.md)** | Seven decisions made once, with the reason each one looks wrong, so they are not argued again in every review | Written |
| **[The formats on disk](formats.md)** | Every marker, the test case and run files, and `testin.yml` — field by field, with the versioning rules | Written |
| **[The indexer's budget](internal/readTestProject.md)** | What reading ten thousand test cases costs, measured, and the test that holds it there | Written |
