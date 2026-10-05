[Documentation](../README.md) › Task guides

# How to share test projects over Git

> A test project is a folder of files. Put it under Git once, and Testin
> commits, pushes and syncs it, so the whole team works on the same test cases.

## Before you start

- The Git plugin is on, in **Settings | Plugins**.
- You know the address of the team's Git repository, for the first push.

## Steps

1. Select the test project in the Testin panel and choose **View Pending
   Commits**. If the folder is not under Git yet, the red ? at the end of the
   status bar offers **Initialize Git (git init)**; click it.
   ([Put the test project under Git](../share/putUnderGit.md))
2. Choose **View Pending Commits** again. Every change is listed, one row for
   each changed field. Untick what is not ready to send.
   ([See what I have not committed](../share/reviewChanges.md))
3. Type a message and press **Commit & Push**. The first time, Git asks who you
   are: type your name and email.
   ([Tell Git who I am](../share/setGitIdentity.md))
4. The first push asks for the remote address. Paste the team's repository
   address. ([Commit and push](../share/commitAndPush.md))
5. From then on, choose **Sync with Remote** to send your commits and take the
   team's in one step. ([Send my changes and take the team's](../share/syncWithGit.md))

## Next

If a sync stops on a conflict, read
[How to resolve Git conflicts](resolveGitConflicts.md).

## If something goes wrong

| What you see                                 | What to do                                                                  |
|----------------------------------------------|-----------------------------------------------------------------------------|
| The red ? says the project is not under Git  | Press **Initialize Git (git init)** in its hint, step 1.                    |
| The red ? says no remote URL is configured   | Commit and push once, step 4. The push asks for the address.               |
| *Git Plugin Not Available*                   | Turn on the Git plugin in **Settings \| Plugins**, then restart the IDE.    |
| A Git step failed                            | Press **Show Git log** on its message to read what Git said.               |

[Documentation](../README.md) › Task guides
