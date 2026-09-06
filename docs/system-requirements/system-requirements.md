[Documentation](../README.md) › System requirements

# System Requirement Specification

What the software must do, stated so that each requirement can be checked.

| | |
|---|---|
| **Answers** | What the product must do to keep the promises the business requirements make |
| **For** | Anyone deciding whether a change is correct, and anyone writing a test that proves it |
| **Owns** | `SR` — one identifier per scenario, citable from an issue, a commit or a test |
| **Written to** | [How a document is written](../standard.md) — Given / When / Then, one behaviour per scenario, every key named here and nowhere else |
| **State** | Product-wide sections: **not written** — [#180](https://github.com/mtb550/test-in/issues/180). By module: **1 of 6** |

---

## Documents

Each module has its own document. It has the same use cases, with the same
`UC` ids, as the [business requirements](../business-requirements/business-requirements.md).
Every key is named here, once. Every screen is drawn in the
[design](../design/design.md).

| Document | Holds | State |
|---|---|---|
| The product | Disk formats, failure, performance, compatibility, security | Not written — [#180](https://github.com/mtb550/test-in/issues/180) |
| **[Project panel](project-panel.md)** `PP` | The tree: 75 scenarios, 13 keys | **Written** — [#181](https://github.com/mtb550/test-in/issues/181) |
| Test case editor `TE` | Writing test cases | Not written — [#181](https://github.com/mtb550/test-in/issues/181) |
| Test run editor `RE` | Running tests and recording verdicts. Light mode | Not written — [#181](https://github.com/mtb550/test-in/issues/181) |
| View panel `VP` | Details, history and bugs of one test case | Not written — [#181](https://github.com/mtb550/test-in/issues/181) |
| Settings `ST` | The settings page | Not written — [#181](https://github.com/mtb550/test-in/issues/181) |
| Evidence and exchange `EX` | Reports, export, import, sync | Not written — [#181](https://github.com/mtb550/test-in/issues/181) |

## How this differs from the business requirements

They are easy to confuse, and a document that blurs them ends up saying
everything twice.

| | [Business requirements](../business-requirements/business-requirements.md) | This document |
|---|---|---|
| **Asks** | What does Testin promise, and to whom | What must the software do to keep that promise |
| **Voice** | A tester or a lead reads it | Someone deciding whether a build is correct reads it |
| **Example rule** | *A tester may record exactly three verdicts* | *The verdict control accepts P, F and B, and no other key records a verdict on any surface* |
| **Verifiable by** | Asking whether the product behaves that way | A test that passes or fails |
| **Identifiers** | `BR-nn`, `UC-nn`, `Q-nn` | `SR-nn` |

**Every `SR` cites the `BR` it serves.** A requirement that serves no promise is
either a promise nobody wrote down, or work nobody needs — and both are worth
finding before the code is.

---

## What the product's document will hold

Sections planned, in the order they are worth writing. Each is empty until it is
written; a heading with nothing under it is not a specification.

| Section | What it will state |
|---|---|
| **Functional** | One `SR` per capability: what the input is, what the software does with it, and what must be true afterward. Grouped as the capabilities are — authoring, execution, evidence and exchange |
| **Data** | What is written to disk, in what format, and what must survive a round trip. The byte-identical rule is a business promise; the file formats behind it belong here |
| **Behaviour under failure** | What must happen when a file is missing, a remote refuses, a plugin is absent, or two writers disagree |
| **Performance** | The numbers a build must meet — how many test cases the tree holds before it slows, how long an index takes, what a report costs |
| **Compatibility** | Which IDEs and which platform versions, and what the plugin must do on one it does not support |
| **Security and privacy** | Where credentials live, what leaves the machine, and what must never be written to a file the repository carries |

---

## Traceability

The chain each requirement sits in:

```
BR-nn   the promise            business-requirements/
  │
  ▼
SR-nn   what must be true      here
  │
  ├──▶  a design document      design/          how the tester meets it
  └──▶  a test                 src/test/        what proves it
```

**Cite the number, not the sentence.** `SR-12` in a commit message survives the
requirement being reworded; a quoted sentence does not.

---

> **⚠️ Nothing is written yet.** This page states what the document will be and
> what it owns, so the identifier space and its relationship to the business
> requirements are settled before anybody writes an `SR-01` that means something
> else. Writing it is [#180](https://github.com/mtb550/test-in/issues/180).

---

[Documentation](../README.md) › **System requirements** — the other two: [business requirements](../business-requirements/business-requirements.md) · [design](../design/design.md)
