[Documentation](README.md) › How a document is written

# How a document is written

Testin has six parts. Each part gets a folder, and each thing a tester does in
it gets a page of its own. This page says how those pages are written.

Read this before writing or changing any page under `docs/`.

---

## One page per thing the tester does

A part of Testin is a folder. `main.md` is its front page, and every use case is
a page beside it, named after what the tester does: `createTestProject.md`.

**`main.md` holds what is true of the whole part:**

| In this order | Holds |
|---|---|
| **The use cases** | A table linking every page, grouped the way the work groups |
| **What the part is for** | Why it exists, in a few sentences |
| **The words it uses** | Any word the rules lean on, explained before they use it |
| **How to read these pages** | The shape of a use case page, and what the marks in the drawings stand for |
| **The rules that hold everywhere** | Numbered rules that apply to the whole part |
| **Every key, in one place** | One table: the key, what it does, and the page that owns it |
| **The screens that belong to no single use case** | The panel, or the window itself, drawn |
| **Why it is built this way** | The decisions worth not re-arguing |
| **Where the plugin breaks its own rules** | Numbered differences a tester can hit today |
| **Not decided** | Numbered questions, listed instead of guessed |

**A use case page holds one thing the tester does:**

| In this order | Holds |
|---|---|
| **The story** | One sentence: as a tester, I want, so that |
| **Rules** | The numbered rules this use case needs, and a line pointing at the ones that hold everywhere |
| **The screens** | Each one drawn, with its parts numbered |
| **Main flow** | What happens, step by step, when nothing goes wrong |
| **What Testin refuses** | Every way it can go wrong, and what the tester sees each time |

**One fact, one place.** A key is written once, in the step that presses it. A
rule is written once and pointed at by number everywhere else. A screen is drawn
once, on the page that opens it, and pointed at from any other page that shares
it.

---

## The three forms

**A user story opens each page**, because it forces the *who* and the *why* into
one sentence and leaves no room for the *how*:

> **As a** tester, **I want** to create a test project from the tree, **so that**
> a new product under test has a place for its cases before any are written.

**Numbered steps for the main flow**, because that is the order the tester does
it in, and because a step that says *"The tester presses `Ctrl+M`"* puts the key
where it is a fact, not a footnote:

> 1. The tester selects **Test Cases** or a test set package.
> 2. The tester presses `Ctrl+M`, or chooses **Create**.
> 3. The **Create Test Node** dialog opens with two kinds to pick from.

**An "If" line for each refusal**, because a tester meets a refusal by
recognizing the situation, not by reading a numbered flow:

> **If the name is empty** — the dialog stays open, the gray hint text turns red,
> and the cursor stays in the box.

**A numbered drawing for each screen**, because a screen is a picture, and a
paragraph describing where the buttons are is worse than a drawing of them.

---

## How things are numbered

Everything a reader might point at is numbered. **Point at the number, never
the sentence.** A number still means the same thing after the words are
rewritten.

Four things are numbered. Each one is written the way it is read:

| Written | What it is |
|---|---|
| **UC-001** | One thing a tester does, such as creating a test project. It is the title of its own page |
| **Rule 1** | Something that must always be true |
| **Question 1** | Something nobody has answered yet, listed instead of guessed |
| **Difference 1** | A place where the plugin does not do what its own rules say |

**The numbers start again in each part of Testin.** The project panel has a
rule 4, and so will the test case editor. Inside one document, "rule 4" is
enough. Anywhere else, say which part it belongs to: *the project panel's rule
4*. A bug report says *project panel, rule 4*.

The one exception is [the product's own document](product.md).
Its rules hold everywhere, so they belong to no part, and it says so at the top.

A use case page lists the rules it needs under **Rules**. If it needs a rule
nobody has written, the rule is written first.

---

## The template

`projectPanel/createTestProject.md` looks like this:

```markdown
[Documentation](../README.md) › [The project panel](main.md) › UC-002

# UC-002: Create a test project

**As a** tester, **I want** …, **so that** ….

## Rules

- **Rule 16** — …
- **Rule 17** — …

Rules 1 to 13 hold everywhere in the panel. They are on
[the project panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## The create dialog

(drawing, 76 columns wide, box characters)

1. **The kind** — …
2. **The name** — …

## Main flow

1. The tester presses …
2. Testin creates …

## What Testin refuses

**If the name is empty** — …

---

[Documentation](../README.md) › [The project panel](main.md)
```

One page, one thing the tester does. Every refusal is an **If** line.

---

## Language

These documents are for testers. Every sentence is checked against that reader.

**Plain words, short sentences.**

- **One idea per sentence.** About 15 words. A sentence with two ideas is two
  sentences. The one exception is the user story, which is one sentence in
  three fixed parts however long it runs.
- **No dashes and no semicolons in a sentence.** A dash joins two ideas. Split
  them. A dash may only separate a number from the text after it: *Rule 8 — …*
- **A list, not a sentence with parts.** Three or more things go in a list when
  each one needs more than a word or two. Steps go in a numbered list. A short
  series stays in the sentence: *create, name, group, order, retire and
  remove*.
- **Say the thing.** No metaphors, no figures of speech. *"The panel is the tree
  on the left."* Not *"The panel is that tree, drawn where the IDE keeps every
  other tree."*
- **The same word for the same thing.** *Test set*, never *set*. *Test run*,
  never *run* on its own. *Test case*, *test project*, *package*.
- **Say who does what.** *"The tester presses `Enter`. The editor opens."* Not
  *"Pressing `Enter` results in the editor being opened."*
- **Present tense, active voice.** *"The dialog opens."* Not *"The dialog will
  be opened."*
- **Explain a word the first time, then use it.** *"Retired means a Deprecated
  test set or an Archived package."*
- **A heading says what the section is, in plain words.** *What it is for*. Not
  *Why it exists*.
- **Numbers: words below 10, digits from 10 up.** *five statuses*, *50 rows*,
  *13 keys*. A key is written as the key: `1` `2` `3`. A quoted message keeps
  the product's own digits: *Removed 4*.
- **American English.** *Behavior*, not *behaviour*. *Gray*, not *grey*. The
  platform the plugin is written against is American, so anything else puts two
  dialects in one page.
- **No class, method or package names.** A tester does not need them, and they
  date the document the first time the code is refactored. Name one only where
  it is the shortest way to say which thing owns a decision.
- **No history.** The document says what the plugin does now. It does not say
  what an earlier design drew, or what was tried and dropped.

**Exact words for exact things.**

- **"The tester", never "the user".** It is who the product is for.
- **A key is written as the plugin shows it**, in backticks: `Ctrl+M`, `F2`,
  `Shift+F6`, `Delete`, `Enter`. Not "control-M", not "the F2 key".
- **A menu label is written exactly**, in bold: **Create**, **Rename**, **Remove**.
  If the label changes, the document changes in the same commit.
- **A notification is quoted exactly**, in italics: the tester sees *Removed 4*,
  so the scenario says *Removed 4*.
- **What is refused is stated as plainly as what is allowed.** *"The two fixed
  containers cannot be removed. The menu does not offer it."*
- **One idea per scenario.** If a **Then** has two unrelated outcomes, it is two
  scenarios.

---

## Every document carries the same header

| | |
|---|---|
| **Part of Testin** | Which part this covers |
| **Answers** | One sentence |
| **Numbering** | Where this part's numbers start, and what they count |
| **State** | Written, Draft or Not written, with the issue |
| **Checked against** | The commit on `main`, and the date |

Only `main.md` carries the header. A use case page starts with its story.

So a reader can tell three things before reading a word of it: what the page
is, whether it is finished, and how far it might have drifted.

---

## The same way in, and the same way out

A reader is never more than one click from the index.

- **[The home page](README.md) lists the parts.** Every part, written or not,
  with the issue that will write it.
- **Each part's `main.md` lists its use cases.** Every one, in a table at the
  top.
- **The line above every title is a breadcrumb.** Each step is a link:
  *Documentation › The project panel › UC-002*.
- **The last line of every page is the same breadcrumb.**

---

## When behavior changes

The page changes **in the same commit** as the code. A commit that changes what
a key does, and does not change the step that names it, is incomplete. The review should say so. This is the whole reason the
documentation lives in this repository, rather than beside it.

---

[Documentation](README.md) › **How a document is written**
