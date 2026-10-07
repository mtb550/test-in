[Documentation](../README.md) › Task guides

# How to collect Testin's logs

> When something goes wrong and no message explains it, Testin's log usually
> says what happened. Collect it together with the IDE's own log, and send both.

## Before you start

- Know what you did, what you expected, and what happened instead.

## Steps

1. Open **Settings | Tools | Testin**, set **Log level** to **TRACE** and press **Apply**. No restart is needed.
   ([Choose how much Testin writes to its log](../setting/setLogLevel.md))
2. Do again what went wrong.
3. Choose **Help | Collect Logs and Diagnostic Data**. The file it makes holds
   `idea.log` and `testin.log` side by side.
4. Send that file with what you did, what you expected and what happened.
5. Set **Log level** back to **INFO**.

## Where the files are

**Help | Show Log in Explorer** (**Show Log in Finder** on a Mac) opens the folder
holding `idea.log` and `testin.log`.

## If something goes wrong

| What you see                     | What to do                                     |
|----------------------------------|------------------------------------------------|
| `testin.log` says almost nothing | The level is INFO or lower. Set TRACE, step 1. |
| `testin.log` is empty            | The level is DISABLED. Set TRACE, step 1.      |

[Documentation](../README.md) › Task guides
