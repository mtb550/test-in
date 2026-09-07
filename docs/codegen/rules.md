[Documentation](../README.md) › [Automation code and the gutter](main.md) › Rules that hold everywhere

# Rules that hold everywhere

These hold wherever this part is used, so they are here rather than repeated
on every page that keeps them. Each use case page names the range and links
back to this one.

- **Rule-CODEGEN-001** — A method is found by the identity in `testName`, never
  by its name. Renaming a test case never loses its method.
- **Rule-CODEGEN-002** — A test case with no description gets no method. A
  description is what names a method.
- **Rule-CODEGEN-003** — Testin writes only the parts listed above. The body is
  the tester's, and Testin never touches it.
- **Rule-CODEGEN-004** — A rename or a move happens before the tree changes,
  while the old name still finds the code.
- **Rule-CODEGEN-005** — Test management works without any of this. A missing
  Java plugin or a missing test folder is a skip, never a failure.
- **Rule-CODEGEN-006** — Nearly everything that goes wrong here is written only
  to the log.

---

[Documentation](../README.md) › [Automation code and the gutter](main.md)
