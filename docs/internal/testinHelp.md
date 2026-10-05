[Documentation](../README.md) › [Inside Testin](main.md) › UC-INTERNAL-009

# UC-INTERNAL-009: Get help where Testin needs it

**As a** tester, **I want** Testin to tell me what is not set up yet, and to
offer the guide for the part I am using, **so that** I can fix it myself without
searching the documentation or asking whoever installed the plugin.

Testin Help is the question mark at the right end of the IDE's status bar. It
holds two lists that the rest of Testin fills: **hints**, for what needs doing
now, and **guides**, for how a part of Testin works.

There is no key for this. Click the question mark.

## Rules

- **Rule-INTERNAL-126** — The Testin Help mark is a question mark, the last
  entry on the right of the IDE's status bar, in every project and over every
  editor. It is gray while nothing waits.
- **Rule-INTERNAL-127** — The mark turns red while a hint waits. A hint names
  one setup step that is missing, with a form to fix it when one fits. Each step
  holds one hint: a newer one replaces it, and it clears when the step is done.
  A missing step Testin finds on its own is a hint and nothing else; a key or
  button the tester pressed still answers, and the hint waits beside it.
- **Rule-INTERNAL-128** — A click on the mark opens one popup in pages: **Hint**
  first while hints wait, then **Testin Guides** while guides are offered. Each
  hint ends with the guide that walks through it. When neither holds anything,
  the click says *Nothing to show* instead.
- **Rule-INTERNAL-129** — A guide is offered where its need shows, and stays for
  the session. *How to set up Testin on this machine* when the Testin folder is
  not set or holds no test project. *How to link this repository to its test
  project* at View Pending Commits and Sync. *How to raise bug reports* when a
  run item is set to Failed. *Test case editor shortcuts* when a test set opens,
  and *Test run editor shortcuts* when a test run opens. A hint offers its own
  guide.
- **Rule-INTERNAL-130** — A guide opens in a Testin window, from the pages the
  plugin carries, so it needs no network and matches the installed version. A
  link to another page opens it in the same window; a web address opens in the
  browser.
- **Rule-INTERNAL-131** — Every guide has its page under `docs/guides`, and
  every page there is a guide Testin can offer.

## The screen

```
                                           ┌──────────────────────────────────────────┐
                                           │  | Hint |   Testin Guides                │
                                           ├──────────────────────────────────────────┤
                                           │  Add bugRepoUrl to testin.yml to say     │
                                           │  which repository bugs are filed in      │
                                           │  bugRepoUrl  [https://github.com/...]    │
                                           │              [ Apply ]                   │
                                           └──────────────────────────────────────────┘
  ... UTF-8    main                                                                (?)
```

1. **The mark** — in every project, gray when nothing waits, red while a hint
   waits. Hovering names the first waiting hint, or reads *Testin Help*.
2. **Hint** — every waiting hint, each with its form when it has one, and the
   guide that walks through it.
3. **Testin Guides** — the guides offered so far, one link each.

## Main flow

1. A part of Testin finds something that is not set up, and fires a hint. The
   mark turns red.
2. The tester clicks the mark. The popup opens on **Hint**.
3. The tester fixes it in the form, or follows the guide. The hint clears and
   the mark turns gray.

**Offering a guide**

1. The tester reaches a part of Testin that has a guide, such as a test set's
   editor.
2. That part offers its guide.
3. The guide is listed under **Testin Guides** for the rest of the session.

## What else happens

**Nothing waits and nothing is offered** — the click shows *Nothing to show*,
and no popup opens.

**The tester presses a key that needs a missing step** — the key answers as it
always did, and the hint waits beside it.

**The tester does not want the mark** — the status bar's own right-click menu
hides it and shows it again. Testin keeps no setting of its own for it.

[Documentation](../README.md) › [Inside Testin](main.md)
