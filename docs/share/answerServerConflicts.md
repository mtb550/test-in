[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-021

# UC-SHARE-021: Answer the conflicts the server sync could not settle

**As a** tester, **I want** to say which version of a test case wins,
**so that** a sync that found two changes does not choose for me.

There is no key for this. The questions come after the sync.

## Rules

- **Rule-SHARE-093** — A file both sides changed is kept as it is here, and
  nothing is sent for it, until the tester answers.
- **Rule-SHARE-094** — The tester is asked only about the fields that really
  disagree.
- **Rule-SHARE-095** — The answers are sent on the same sync, if nobody else has
  taken the lock in the meantime.

Rule-SHARE-001 to Rule-SHARE-006 hold everywhere. They are on
[the sharing page](main.md#rules-that-hold-everywhere).

## Main flow

1. The tester syncs, and two test cases were changed on both sides.
2. A message titled **Synced, with 2 left to you** says so, and stays in the
   notification list.
3. The **Both Changed** window opens for the first test case.
4. The tester picks a value for each field that disagrees, and presses `Enter`.
5. The window opens for the second test case.
6. When both are answered, Testin sends them.
7. A message reads *Settled 2*.

The window itself is drawn on
[UC-SHARE-018](answerMergeQuestions.md).

## What Testin refuses

**If the tester presses `Escape`** — nothing is written for that test case, and
nothing more is asked. Everything already answered is thrown away. The same
questions come again on the next sync.

**If somebody else has started a sync in the meantime** — a message titled
**Nothing Settled** reads *Somebody else is syncing this project, so your
answers were not sent. You will be asked again on the next sync.*

## What the message before it said

The message after the sync reads what was sent, taken and merged, then *Both
sides changed*, the files, then *This machine kept its copies and sent nothing
for them; you'll be asked about anything that can be merged field by field.*

That message stays in the notification list rather than fading, because a sync
can finish while the tester is reading something else.

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
