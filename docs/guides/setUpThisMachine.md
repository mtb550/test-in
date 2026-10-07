[Documentation](../README.md) › Task guides

# How to set up Testin on this machine

> Four settings, once per machine. At the end, the Testin panel shows your test
> projects, and everything you save carries your name.

## Before you start

- Testin is installed in this IDE.
- You know where your test projects are, or where you want them. Each test
  project is a folder, and they all sit in one folder: the Testin folder.

## Steps

1. Open the settings. Press the gear button on the Testin panel's toolbar, or
   open **Settings | Tools | Testin**. ([Open the settings page](../setting/openSettings.md))
2. Set **Testin folder** to the folder that holds your test projects. Type the
   full path, or press **...** to choose the folder. ([Set the Testin folder](../setting/setTestinFolder.md))
3. Set **Tester name** to your name. Testin writes it onto everything you save.
   ([Give my name](../setting/setTesterName.md))
4. Set **Default download folder** to where reports and exports should be saved
   first. ([Set the folder that files are saved to](../setting/setDownloadFolder.md))
5. Leave **Log level** as it is. Raise it only when somebody asks you for a
   log. ([Choose how much Testin writes to its log](../setting/setLogLevel.md))
6. Press **OK**. Testin reads the Testin folder, and the Testin panel shows what
   it found.
7. If the Testin folder holds no test project yet, the panel offers **Create
   your first test project**. ([Create a test project](../treePanel/createTestProject.md))

## Next

To make this code project open its test project for everyone who clones it,
read [How to link this repository to its test project](linkThisRepository.md).

## If something goes wrong

| What you see                                | What to do                                                              |
|---------------------------------------------|-------------------------------------------------------------------------|
| The red ? says the Testin folder is not set | Press **Open Settings** in its hint and start at step 2.                |
| *Testin Folder Not Found*                   | No folder is at that path. Check the path, or create the folder first.  |
| *Testin Folder Is Not a Folder*             | The path names a file. Choose the folder that holds your test projects. |
| *Testin Folder Needs a Full Path*           | Type the whole path, from the drive or the root.                        |

[Documentation](../README.md) › Task guides
