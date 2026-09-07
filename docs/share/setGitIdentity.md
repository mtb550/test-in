[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-008

# UC-SHARE-008: Tell Git who I am

**As a** tester, **I want** to give Git my name and email without leaving
Testin, **so that** my first commit is not refused on a machine nobody has set
up.

Nothing starts this. It opens when a commit is refused for want of a name.

## Rules

- **Rule-SHARE-037** — The dialog opens only when Git says it does not know who
  the tester is.
- **Rule-SHARE-038** — The commit is made straight after the identity is set.
  The tester does not have to press commit again.
- **Rule-SHARE-039** — The tester chooses whether this is for this one
  repository or for every repository on this machine.

Rule-SHARE-001 to Rule-SHARE-006 hold everywhere. They are on
[the sharing page](main.md#rules-that-hold-everywhere).

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

1. **The two boxes** — neither has a label, only its gray hint. The name holds
   the cursor.
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

The email is not checked at all. Any text is accepted, and Git makes of it what
it will. That is difference 18 on
[the sharing page](main.md#where-the-plugin-breaks-its-own-rules).

## This is not the tester name

Testin has its own tester name, on the settings page, which it stamps on test
cases and verdicts. This is Git's, and it is used only on commits. They can be
different, and nothing keeps them the same. The Testin one is
[UC-SETTING-004](../setting/setTesterName.md).

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
