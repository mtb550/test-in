[Documentation](README.md) › How a document is written

# How a document is written

Testin has six parts, and each part gets one document. That document holds
everything about the part. This page says what goes in it, in what order, and
how it is written.

Read this before writing or changing any page under `docs/`.

---

## One part, one document

There used to be three documents per part: what it promises, what it does, and
what it looks like. A reader had to hold all three open to answer one question.
Now there is one, and it answers the question in the order the question comes
up.

| In this order | Holds |
|---|---|
| **What the part is for** | Why it exists, in a few sentences |
| **The words it uses** | Any word the rules lean on, explained before they use it |
| **How to read this document** | What Given, When and Then mean, and what the marks in the drawings stand for |
| **The rules that hold everywhere** | Numbered rules that apply to the whole part |
| **Every key, in one place** | One table: the key, what it does, and the scenario that owns it |
| **The screens that belong to no single use case** | The panel, or the window itself, drawn |
| **One section per use case** | The story, its rules, its screens and its scenarios, together |
| **Why it is built this way** | The decisions worth not re-arguing |
| **Where the plugin breaks its own rules** | Numbered differences a tester can hit today |
| **Not decided** | Numbered questions, listed instead of guessed |

**One fact, one place.** A key is written once, in the scenario that presses it.
A rule is written once and pointed at by number everywhere else. A screen is
drawn once, in the use case that opens it, and pointed at from any other use
case that shares it.

---

## The three forms

**A user story opens each use case**, because it forces the *who* and the *why*
into one sentence and leaves no room for the *how*:

> **As a** tester, **I want** to create a test project from the tree, **so that**
> a new product under test has a place for its cases before any are written.

**Given / When / Then for each scenario**, because it is the language testers
already write tests in, because each scenario is something a person can check
against a build, and because *"When the tester presses `Ctrl+M`"* puts the key
where it is a fact, not a footnote:

> **Given** a test project is selected in the tree
> **When** the tester presses `Ctrl+M`
> **Then** the create dialog opens, offering a test set package or a test set

**A numbered drawing for each screen**, because a screen is a picture, and a
paragraph describing where the buttons are is worse than a drawing of them.

---

## How things are numbered

Everything a reader might point at is numbered. **Point at the number, never
the sentence.** A number still means the same thing after the words are
rewritten.

Five things are numbered. Each one is written the way it is read:

| Written | What it is |
|---|---|
| **Use case 1** | One thing a tester does, such as creating a test project |
| **Rule 1** | Something that must always be true |
| **Scenario 1** | One Given, When, Then |
| **Question 1** | Something nobody has answered yet, listed instead of guessed |
| **Difference 1** | A place where the plugin does not do what its own rules say |

**The numbers start again in each part of Testin.** The project panel has a
rule 4, and so will the test case editor. Inside one document, "rule 4" is
enough. Anywhere else, say which part it belongs to: *the project panel's rule
4*. A bug report says *project panel, rule 4*.

The one exception is [the product's own document](system-requirements/product.md).
Its rules hold everywhere, so they belong to no part, and it says so at the top.

A scenario names the rule it puts to work, on the line under its title: *Keeps
rule 8*. If a scenario needs a rule nobody has written, the rule is written
first.

---

## The template

```markdown
### Use case 1 · Create a test project

**As a** tester, **I want** …, **so that** ….

**Rules**

- **Rule 1** — …
- **Rule 2** — …

#### The create dialog

(drawing, 76 columns wide, box characters)

1. **The kind** — …
2. **The name** — …

**Scenario 1 · From the tree**
> **Given** a test project is selected in the tree
> **When** the tester presses `Ctrl+M`
> **Then** the create dialog opens, offering a test set package or a test set

**Scenario 2 · A name is required**
Keeps rule 2.
> **Given** the create dialog is open with an empty name
> **When** the tester presses `Enter`
> **Then** the dialog stays open and the name field is marked
```

One scenario, one behavior. A refusal is its own scenario.

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
| **Part of Testin** | Which part this document covers |
| **Answers** | One sentence |
| **Numbering** | Where this document's numbers start, and what they count |
| **State** | Written, Draft or Not written, with the issue |
| **Checked against** | The commit on `main`, and the date |

So a reader can tell three things before reading a word of it: what the page
is, whether it is finished, and how far it might have drifted.

---

## The same way in, and the same way out

A reader is never more than one click from the index.

- **[The home page](README.md) is the only index.** It lists every document,
  written or not, with the issue that will write it.
- **The line above every title is a breadcrumb.** Each step is a link:
  *Documentation › System requirements › The project panel*.
- **The last line of every document is the same breadcrumb.**

---

## When behavior changes

The document changes **in the same commit** as the code. A commit that changes
what a key does, and does not change the scenario that names it, is
incomplete. The review should say so. This is the whole reason the
documentation lives in this repository, rather than beside it.

---

[Documentation](README.md) › **How a document is written**
