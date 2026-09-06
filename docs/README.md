# Testin documentation

> Reading on GitHub? The same pages, laid out for reading, are at
> **[mtb550.github.io/test-in](https://mtb550.github.io/test-in/)**.

Everything about the plugin that is not the code: what it is for, who uses it,
how each screen was decided, and how to work on it.

New here? Start with the [project README](../README.md) — what Testin is, how to
install it and what it does. Then come back for the detail.

Every document lives in this folder, in Markdown, committed beside the code it
describes. A change to the plugin and the change to its documentation are one
commit and one review, which is the only thing that keeps the two from drifting
apart quietly.

---

## For testers

You installed the plugin and want to use it well.

| Document | What it answers | State |
|---|---|---|
| **Keyboard reference** | Every key Testin answers to, on every surface. The plugin is built to be driven from the keyboard, so this is the page that makes it fast. | Not written — [#73](https://github.com/mtb550/test-in/issues/73) |
| **First run** | Ten minutes from installing the plugin to a test case with a verdict on it. | Not written — [#104](https://github.com/mtb550/test-in/issues/104) |

## What Testin is for

The product, described for someone who does not read Java.

| Document | What it answers | State |
|---|---|---|
| **Business requirements** | The actors, the use cases and the rules. Who Testin is for, what problem it solves, and what it deliberately does not do. | Not written — [#72](https://github.com/mtb550/test-in/issues/72) |
| **Formats on disk** | The seven marker formats, `testin.yml` and the sequence store. The document that lets somebody read a test project without the plugin installed. | Not written — [#100](https://github.com/mtb550/test-in/issues/100) |

## Design

One document per screen: what it looks like, and the reasoning behind every
choice on it. Written before the code, then checked against the build
afterward — a design page that quietly stops matching the plugin is worse than
no page, because it is believed.

| Document | What it answers | State |
|---|---|---|
| **[Light mode](design/light-mode.md)** | The standalone always-on-top window that shows one test case at a time, so a tester can work with IntelliJ minimized and still record a verdict. | **Written** — [#13](https://github.com/mtb550/test-in/issues/13), closed |
| **Test case editor** | The grid, the card list, the details panel and the toolbar that drives them. | Not written |
| **The dialog framework** | `ui.framework` is the design system in code — the dialog shell, the status bar strip, the keycap, the prose paragraph, the row striping. What a new dialog gets for free. | Not written — [#11](https://github.com/mtb550/test-in/issues/11), [#69](https://github.com/mtb550/test-in/issues/69) |

## For contributors

What a person needs before their first change.

| Document | What it answers | State |
|---|---|---|
| **Architecture** | The layer map, the indexer-only file access rule and its exempt list, and two walkthroughs a newcomer can follow end to end. | Not written — [#99](https://github.com/mtb550/test-in/issues/99) |
| **Contributing** | Setup, the checks that must pass, the run configurations and the IDE compatibility rule. | Not written — [#102](https://github.com/mtb550/test-in/issues/102) |
| **Standing decisions** | The calls made once that should not be re-argued in every review — one owner for anything shared, absence is an empty value rather than a null, a method declaration is one line. | Not written — [#101](https://github.com/mtb550/test-in/issues/101) |

---

## How this works

**Markdown, in the repository.** Every page here is a `.md` file under `docs/`.
It is versioned, it diffs in a pull request as prose rather than as markup, it
travels between machines with a clone, and it reads on GitHub without anything
being built or published.

**One document, one owner.** A BRD and a BRS are the same document under two
names, so there is one of them. Where two documents would answer the same
question, there is one document and the other links to it.

**A page says where it disagrees with the build.** Design documents are checked
against the code and record any place the two differ, rather than describing an
intention nobody kept.

**Not written is a state, not an omission.** Every document above that does not
exist is listed with the issue that will write it, so this index is the roadmap
as well as the table of contents.
