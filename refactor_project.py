import os
import re

root = "C:/Users/mtb/IdeaProjects/testin/src/main/java"

# Files that use Config.getProject()
files_to_fix = []

for dirpath, dirnames, filenames in os.walk(root):
    for f in filenames:
        if f.endswith(".java"):
            path = os.path.join(dirpath, f)
            with open(path, "r", encoding="utf-8") as fh:
                content = fh.read()
                if "Config.getProject()" in content:
                    files_to_fix.append(path)

print(f"Found {len(files_to_fix)} files with Config.getProject()")

# For each file, determine how to replace
# Strategy: For files that already import Project, replace Config.getProject() with a project variable
# For files that don't have Project available, add it as needed

for path in files_to_fix:
    print(f"\n=== {path} ===")
    with open(path, "r", encoding="utf-8") as fh:
        content = fh.read()
    
    # Count occurrences
    count = content.count("Config.getProject()")
    print(f"  Occurrences: {count}")
    
    # Show the lines with Config.getProject()
    for i, line in enumerate(content.split("\n"), 1):
        if "Config.getProject()" in line:
            print(f"  L{i}: {line.strip()}")
