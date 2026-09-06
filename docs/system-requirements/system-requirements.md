[Documentation](../README.md) › System requirements

# System Requirement Specification

What the software must do. Every requirement here can be checked against a
build.

| | |
|---|---|
| **Answers** | What the product must do to keep the promises the business requirements make |
| **For** | Anyone deciding whether a change is correct, and anyone writing a test that proves it |
| **Owns** | The scenarios. Each one has a number, so an issue, a commit or a test can point at it |
| **Written to** | [How a document is written](../standard.md). Given, When, Then, one behavior per scenario. Every key is named here, and nowhere else |
| **Where it stands** | The product-wide sections are not written, [#180](https://github.com/mtb550/test-in/issues/180). One part of six is written |

---

## Documents

Each part of Testin has its own document. It covers the same use cases, under
the same names and numbers, as the
[business requirements](../business-requirements/business-requirements.md).
Every key is named here, once. Every screen is drawn in the
[design](../design/design.md).

| Document | Holds | Where it stands |
|---|---|---|
| The product | What is on disk, what happens when something fails, performance, compatibility, security | Not written — [#180](https://github.com/mtb550/test-in/issues/180) |
| **[The project panel](project-panel.md)** | The tree on the left: 75 scenarios, 13 keys | **Written** — [#181](https://github.com/mtb550/test-in/issues/181) |
| The test case editor | Writing test cases | Not written — [#181](https://github.com/mtb550/test-in/issues/181) |
| The test run editor | Running tests and recording verdicts, including light mode | Not written — [#181](https://github.com/mtb550/test-in/issues/181) |
| The view panel | Details, history and bugs of one test case | Not written — [#181](https://github.com/mtb550/test-in/issues/181) |
| The settings page | Everything set once per machine | Not written — [#181](https://github.com/mtb550/test-in/issues/181) |
| Reports, export, import and sync | Getting work in and out of Testin | Not written — [#181](https://github.com/mtb550/test-in/issues/181) |

## How this differs from the business requirements

The two are easy to confuse. A document that blurs them says everything twice.

| | [Business requirements](../business-requirements/business-requirements.md) | This document |
|---|---|---|
| **Asks** | What does Testin promise, and to whom | What must the software do to keep that promise |
| **Voice** | A tester or a lead reads it | Someone deciding whether a build is correct reads it |
| **Example rule** | *A tester may record exactly three verdicts* | *Only `P`, `F` and `B` record a verdict, and they do it on every screen that offers one* |
| **Verifiable by** | Asking whether the product behaves that way | A test that passes or fails |
| **What it numbers** | Rules, use cases and open questions | Scenarios |

**Every scenario names the rule it serves.** A scenario that serves no promise
is one of two things. It is a promise nobody wrote down, or it is work nobody
needs. Both are worth finding before the code is written.

---

## What the product's document will hold

These sections are planned, in the order they are worth writing. Each one is
empty until it is written. A heading with nothing under it says nothing.

| Section | What it will state |
|---|---|
| **Functional** | One scenario per capability. What the input is, what the software does with it, and what must be true afterward. Grouped the way the capabilities are: writing, running, and getting work in and out |
| **Data** | What is written to disk, in what format, and what must survive a round trip. The byte-identical rule is a business promise. The file formats behind it belong here |
| **Behavior under failure** | What must happen when a file is missing, a remote refuses, a plugin is absent, or two writers disagree |
| **Performance** | The numbers a build must meet. How many test cases the tree holds before it slows, how long an index takes, and what a report costs |
| **Compatibility** | Which IDEs and which platform versions, and what the plugin must do on one it does not support |
| **Security and privacy** | Where credentials live, what leaves the machine, and what must never be written to a file the repository carries |

---

## Following one scenario

A scenario sits in the middle of a chain. Above it is the promise it keeps,
which is a rule in the
[business requirements](../business-requirements/business-requirements.md).
Below it are two things: the screen it happens on, drawn in the
[design](../design/design.md), and the test that proves it, under `src/test`.

**Point at the number, not the sentence.** A number in a commit message
survives the scenario being reworded. A quoted sentence does not.

---

> **⚠️ The product's own sections are not written yet.** This page says what that
> document will be, and what it owns. So the numbering is settled before anybody
> starts writing, and two scenarios cannot end up with the same number. Writing
> it is [#180](https://github.com/mtb550/test-in/issues/180).

---

[Documentation](../README.md) › **System requirements** — the other two: [business requirements](../business-requirements/business-requirements.md) · [design](../design/design.md)
