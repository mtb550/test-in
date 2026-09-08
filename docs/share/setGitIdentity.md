[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-008

# UC-SHARE-008: Tell Git who I am

**As a** tester, **I want** to give Git my name and email without leaving
Testin, **so that** my first commit is not refused on a machine nobody has set
up.

Git will not record a commit until it knows a name and an email address. This
dialog asks for them, then makes the commit.

Nothing starts this. It opens when a commit is refused because Git has no
name.

## Rules

- **Rule-SHARE-001** — An export never changes any test case. It only reads.
- **Rule-SHARE-002** — An import never overwrites an existing test case. Every
  imported test case is new.
- **Rule-SHARE-003** — A sync sends and takes in one gesture, so a sync that
  succeeded never leaves the tester's work only on this machine.
- **Rule-SHARE-004** — A password is never written to a file Testin writes, and
  never to the log.
- **Rule-SHARE-005** — Long work runs under a progress bar. A Git step that
  only reads can be canceled; one that is writing to the repository or to the
  remote cannot, because a push or a rebase stopped half way leaves the
  repository in a state nobody asked for. The export, import and report ones can
  be canceled: none of them leaves anything the tester cannot see.
- **Rule-SHARE-006** — Nothing here is in the IDE's keymap, so none of these
  keys can be changed there.
- **Rule-SHARE-039** — The dialog opens only when Git says it does not know who
  the tester is.
- **Rule-SHARE-040** — The commit is made straight after the identity is set.
  The tester does not have to press commit again.
- **Rule-SHARE-041** — The tester chooses whether this is for this one
  repository or for every repository on this machine.

## The screen

```
┌──────────────────────────────────────────────────────────────┐
│  Set Git Identity and Commit                                 │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  Git records who made a commit, and has no name or email     │
│  to record yet.                                              │
│                                                              │
│  [ your name...                                           ]  │
│  [ your email address...                                  ]  │
│                                                              │
│  Apply to    (x) This repository                             │
│              ( ) Every repository on this machine            │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│  [k]  Enter Confirm       Escape Cancel                      │
└──────────────────────────────────────────────────────────────┘
```

1. **The two boxes** — neither has a label. Each shows a gray hint instead. The
   name box holds the cursor.
2. **Apply to** — this repository is chosen.

## Main flow

1. The tester presses **Commit** and Git does not know who they are.
2. The **Set Git Identity and Commit** dialog opens.
3. The tester types their name and email and presses `Enter`.
4. Testin tells Git, for this repository.
5. A message reads *Identity set*.
6. The commit is made.

## What Testin refuses

**If the name is empty** — its gray hint turns red and the box takes the cursor.

**If the email is empty** — the same.

**If Git will not take the identity** — a message titled **Config Failed** reads
*Failed to set Git identity:* and then the reason.

## Where the plugin breaks its own rules

The email is not checked at all. Any text is accepted. Git then makes of it
what it will. That is difference 18 on
[the sharing page](main.md#where-the-plugin-breaks-its-own-rules).

## This is not the tester name

Testin has a tester name of its own, on the settings page. It stamps that name
on test cases and verdicts. The name here is Git's, and it is used only on
commits. The two can be different, and nothing keeps them the same. The Testin
one is [UC-SETTING-004](../setting/setTesterName.md).

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
