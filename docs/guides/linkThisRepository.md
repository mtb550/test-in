[Documentation](../README.md) › Task guides

# How to link this repository to its test project

> A code project opens one test project. Link them once and commit the link.
> Everyone who clones the repository then opens the same test project, and
> Testin can write its automation code.

## Before you start

- Testin is set up on this machine. If not, read
  [How to set up Testin on this machine](setUpThisMachine.md).
- The test project is in your Testin folder, or in a Git repository you can
  clone.

## Steps

1. In the Testin panel, press **Select Test Project**, and choose the test
   project. Testin keeps this choice on your machine only.
   ([Choose which test project this code project uses](../treePanel/chooseTestProject.md))
2. Press **Save to testin.yml**, the eighth button at the top of the panel. A
   preview shows the lines Testin will write.
   ([Save the test project to testin.yml](../treePanel/saveTestinYml.md))
3. Press `Enter`. Testin writes `testinProject`, and where the test project is
   cloned from when its folder is a Git repository with a remote. Every other
   line of the file stays as it was.
4. Commit `testin.yml` with the IDE's own commit, and push it. Your team gets
   the same link.
5. Automation code turns on for this test project: generating, **Automate Test
   Case**, **Navigate to Test Method**, **Run Tests** and the gutter icons.

## For a colleague who clones the repository

1. They open the code project. The Testin panel looks for the test project that
   `testin.yml` names.
2. If it is not on their machine yet, the panel says so and offers **Clone
   \<name\>**. ([Import a test project that already exists](../treePanel/importTestProject.md))
3. Clicking it copies the test project into their Testin folder, and the tree
   opens.

## If something goes wrong

| What you see                                                   | What to do                                                                                   |
|----------------------------------------------------------------|----------------------------------------------------------------------------------------------|
| The red ? says testin.yml does not name this test project      | `testin.yml` does not name the open test project. Press **Save to testin.yml**, step 2.      |
| *testin.yml names \<name\>, which is not in the Testin folder* | Clone it, or choose another test project at step 1.                                          |
| *testin.yml names \<name\>, which could not be read*           | The folder is there, but Testin cannot read it. Open it from the Testin folder and check it. |
| *Clone \<name\> (needs the Git plugin)*                        | Turn on the Git plugin in **Settings \| Plugins**, then restart the IDE.                     |
| *No Test Projects*                                             | The Testin folder holds none. Create one, or clone one.                                      |

[Documentation](../README.md) › Task guides
