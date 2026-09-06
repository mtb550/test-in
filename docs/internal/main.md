[Documentation](../README.md) › Inside Testin

# Inside Testin

Two things in Testin belong to no panel. The search opens from anywhere in the
IDE. The indexer is the one part that reads and writes the files on disk, so
everything every panel shows has come through it.

A tester meets both without going looking for them, which is why they are
written up here rather than left to the code.

| | |
|---|---|
| **Part of Testin** | The parts that belong to no panel |
| **Answers** | What the search finds, and what the indexer does with the files under the Testin folder |
| **Numbering** | Use cases are `UC-INTERNAL-001` and up. Rules are numbered 1 and up, and belong to this part |
| **State** | **Being written** — [#181](https://github.com/mtb550/test-in/issues/181) |
| **Checked against** | `main` at `c96c5c2f`, 7 September 2026 |
| **Written to** | [How a document is written](../standard.md) |

---

## The use cases

| | What the tester does | |
|---|---|---|
| **UC-INTERNAL-001** | [Find anything in the test project](globalSearch.md) | |

---

## What is still to write

The indexer's own use cases. It is the one part of Testin allowed to touch a
file, and a tester meets it every time the tree is slow, a change made outside
the IDE appears or does not, or a removed node comes back.

| Not written yet | What it will cover |
|---|---|
| Reading a test project from disk | What starts a scan, what it walks, and what the tester waits for |
| Noticing a change made outside the IDE | What Testin picks up on its own, and what needs **Refresh** |
| Numbering a test case | Where the next number comes from, and what happens when two are made at once |
| Keeping a removed node | Where it goes, what is kept aside for undo, and when that is thrown away |
| Counting what a node holds | When the counts are worked out, and why they are never saved |

---

[Documentation](../README.md) › **Inside Testin**
