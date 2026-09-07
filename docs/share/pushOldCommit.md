[Documentation](../README.md) › [Sharing work with the team](main.md) › UC-SHARE-015

# UC-SHARE-015: Push a commit that never left this machine

**As a** tester, **I want** to be told when I have committed something and not
sent it, **so that** work I think the team has is not sitting on my laptop.

There is no key for this. The message appears when the review finds nothing else
to do.

## Rules

- **Rule-SHARE-064** — Testin counts what is committed here and not on the
  remote, and says so rather than reporting no changes.
- **Rule-SHARE-065** — The message counts commits, not files.

Rule-SHARE-001 to Rule-SHARE-006 hold everywhere. They are on
[the sharing page](main.md#rules-that-hold-everywhere).

## Main flow

1. The tester committed yesterday and the push failed.
2. Today the tester chooses **View Pending Commits**.
3. Nothing has changed since, so there is nothing to review.
4. A message titled **Not Pushed** reads the count, then *commits are committed
   here and not on the remote.*
5. The tester chooses **Sync With Remote**, which pushes them.

## What Testin refuses

**If nothing has changed and nothing is unpushed** — a message reads *No
changes*, with no title.

## Why this exists

The review dialog answers the question "what have I not sent". A tester whose
push failed has an answer of nothing to review, and that reads as everything
being sent. So Testin counts the commits the remote has not got, and says so
instead.

---

[Documentation](../README.md) › [Sharing work with the team](main.md)
