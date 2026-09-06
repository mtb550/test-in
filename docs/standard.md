[Documentation](README.md) › How a document is written

# How a document is written

Every module of Testin gets three documents, and each one answers a different
question. This page says which question, what goes in each, and the form it
takes — so the same fact is written once, in the one place a reader would look
for it.

Read this before writing or changing any page under `docs/`.

---

## Three documents, three questions

| | Business requirements | System requirements | Design |
|---|---|---|---|
| **The question** | Why does this exist, and what must always hold? | What exactly happens, step by step? | What does the tester see? |
| **The form** | One user story per use case, then numbered rules | Given / When / Then scenarios, one per behaviour | A sketch of every screen, with its parts numbered |
| **Written for** | A lead deciding whether Testin fits. A tester learning what a thing is *for* | A tester learning how to do it. Anyone checking whether a build is right | Anyone changing a screen, or judging a change to one |
| **Names keys** | **Never** | **Always** — every key, once, in the step that presses it | Only where a key is drawn on a sketch |
| **Holds validations** | **Yes** — as rules. *"A test set cannot be dropped among test runs"* | Shows each rule as a scenario that ends in a refusal, and cites the rule | No |
| **Names screens** | No | Yes — links the design | **Owns** them |
| **Says how it is built** | Never | Never | A class name only where it names who owns a decision |

**One fact, one home.** A key is stated in the system requirements and nowhere
else. A rule is stated in the business requirements and cited everywhere else. A
screen is drawn in the design and linked from everywhere else. When two documents
would say the same thing, one says it and the other links.

---

## Why these three forms

**A user story for the business requirements**, because it forces the *who* and
the *why* into one sentence and leaves no room for the *how*:

> **As a** tester, **I want** to create a test project from the tree, **so that**
> a new product under test has a place for its cases before any are written.

**Given / When / Then for the system requirements**, because it is the language
testers already write tests in, because each scenario is something a person can
check against a build, and because *"When the tester presses `Ctrl+M`"* puts the
key exactly where it is a fact rather than a footnote:

> **Given** a test project is selected in the tree
> **When** the tester presses `Ctrl+M`
> **Then** the create dialog opens, offering a test set package or a test set

**A numbered sketch for the design**, because a screen is a picture, and a
paragraph describing where the buttons are is worse than a drawing of them. The
light mode document is the pattern.

---

## Identifiers

Everything a reader might cite has a number. **Cite the number, never the
sentence** — a number survives the text being reworded.

| Id | Is | Lives in | Example |
|---|---|---|---|
| `UC-PP-01` | A **use case** — one thing a tester does | Business requirements *and* system requirements, **same id in both**. The story is in one, the scenarios in the other, and the shared id is what links them | *Create a test project* |
| `BR-PP-01` | A **business rule** — something that must always hold | Business requirements | *Test Cases and Test Runs are fixed containers* |
| `SR-PP-01` | A **scenario** — one Given / When / Then | System requirements | *Create from the tree* |
| `Q-PP-01` | An **open question** — genuinely undecided, listed rather than guessed | Business requirements | |

The middle letters name the module:

| Code | Module |
|---|---|
| `PP` | Project panel — the tree |
| `TE` | Test case editor |
| `RE` | Test run editor, including light mode |
| `VP` | View panel |
| `ST` | Settings |
| `EX` | Evidence and exchange — reports, export, import, sync |

Rules that hold for the whole product, not one module, carry no module code:
`BR-11`. Those live in [the product's own document](business-requirements/product.md).

**The chain, read downward:**

```
UC-PP-01   the use case          named once, in both specifications
  │
  ├── BR-PP-nn   what must hold          business-requirements/project-panel.md
  ├── SR-PP-nn   what happens, and when  system-requirements/project-panel.md
  └── a screen   what the tester sees    design/project-panel.md
```

A design cites `UC` and `BR` and creates neither. A scenario cites the `BR` it
exercises. If a scenario needs a rule nobody has written, the rule is written
first, in the business requirements, and the scenario cites it.

---

## The templates

### Business requirements — per use case

```markdown
### UC-PP-01 · Create a test project

**As a** tester, **I want** …, **so that** ….

**Rules**

- **BR-PP-01** — …
- **BR-PP-02** — …

**Not decided** — Q-PP-01: …           ← only if something genuinely is
```

### System requirements — per use case

```markdown
### UC-PP-01 · Create a test project

Screen: [The create dialog](../design/project-panel.md#the-create-dialog)

**SR-PP-01 · From the tree**
> **Given** a test project is selected in the tree
> **When** the tester presses `Ctrl+M`
> **Then** the create dialog opens, offering a test set package or a test set

**SR-PP-02 · A name is required** — BR-PP-02
> **Given** the create dialog is open with an empty name
> **When** the tester presses `Enter`
> **Then** the dialog stays open and the name field is marked
```

One scenario, one behaviour. A refusal is its own scenario, and it names the rule
it enforces in its title line.

### Design — per screen

```markdown
## The create dialog

(sketch, 76 columns wide, box characters)

1. **The kind** — …
2. **The name** — …

Used by UC-PP-01, UC-PP-04.
```

---

## Language

These documents are for testers. Every sentence is checked against that reader.

**Plain words, short sentences.**

- **One idea per sentence.** About 15 words. A sentence with two ideas is two
  sentences.
- **No dashes and no semicolons in a sentence.** A dash joins two ideas. Split
  them. A dash may only separate an identifier from its text: *BR-PP-08 — …*
- **A list, not a sentence with parts.** Three or more things go in a list.
  Steps go in a numbered list.
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

**Exact words for exact things.**

- **"The tester", never "the user".** It is who the product is for.
- **A key is written as the plugin shows it**, in backticks: `Ctrl+M`, `F2`,
  `Shift+F6`, `Delete`, `Enter`. Not "control-M", not "the F2 key".
- **A menu label is written exactly**, in bold: **Create**, **Rename**, **Remove**.
  If the label changes, the document changes in the same commit.
- **A notification is quoted exactly**, in italics: the tester sees *Removed 4*,
  so the scenario says *Removed 4*.
- **No class names** in the business or system requirements. A tester does not
  need them, and they date the document the first time the code is refactored.
- **What is refused is stated as plainly as what is allowed.** *"The two fixed
  containers cannot be removed. The menu does not offer it."*
- **One idea per scenario.** If a **Then** has two unrelated outcomes, it is two
  scenarios.

---

## Every document carries the same header

| | |
|---|---|
| **Area** | Which of the three specifications, linked |
| **Module** | Which module code |
| **Read with** | The same module in the other two specifications, linked |
| **Answers** | One sentence |
| **State** | Written · Draft · Not written — with the issue |
| **Checked against** | The commit on `main`, and the date |

So a reader can tell what the page is, whether it is finished, and how far it
might have drifted — before reading a word of it.

---

## The same way in, and the same way out

A reader is never more than one click from an index.

- **An index is a list of documents, and nothing else.** Each folder has one:
  [`business-requirements.md`](business-requirements/business-requirements.md),
  [`system-requirements.md`](system-requirements/system-requirements.md),
  [`design.md`](design/design.md). It lists every document, written or not,
  with the issue that will write it. [The home page](README.md) lists the three.
- **The line above every title is a breadcrumb.** Each step is a link:
  *Documentation › Business requirements › Project panel*.
- **The last line of every document is the same breadcrumb, plus its twins.**
  The twins are the same module in the other two specifications.

---

## When behaviour changes

The document changes **in the same commit** as the code. A commit that changes
what a key does and does not change the scenario that names it is incomplete,
and the review should say so. This is the whole reason the documentation lives
in this repository rather than beside it.

---

[Documentation](README.md) › **How a document is written**
