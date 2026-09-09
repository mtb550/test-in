---
name: lombok-writes-the-boilerplate
description: Lombok generates the constructors, getters and builders in Testin — never hand-write one. Use when adding a class with a private constructor, a getter, a value type, an enum with fields, or any accessor that Lombok already generates. Triggers on "private X() {}", "public String getY()", "add a field to this enum".
---

# Lombok writes the boilerplate

Testin has Lombok on `compileOnly` + `annotationProcessor` in every module. If
Lombok generates it, do not type it.

The rule is not about characters saved. Hand-written boilerplate is code a
reader has to check, and checking it means reading it against the annotation
that would have written it — which is strictly more work than there was before.

## The one that keeps coming back

A utility class needs a private constructor so nobody instantiates it:

```java
// no
public final class CardTitle {

    private CardTitle() {
    }
}

// yes
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CardTitle {
}
```

Two imports instead of three lines, and it reads as a *property of the class*
sitting with `final` and `public`, rather than as a member the reader has to
look at and dismiss. Every other holder in the codebase is already written this
way — `Fqcn`, `Notifier`, `Badges`, `BackgroundWork` — so a hand-written one is
also the odd file out.

## What Lombok owns here

| Instead of | Write |
|---|---|
| `private Foo() {}` on a holder | `@NoArgsConstructor(access = AccessLevel.PRIVATE)` |
| A constructor assigning every field | `@AllArgsConstructor`, or `@RequiredArgsConstructor` for the `final` ones |
| `public X getY()` | `@Getter` on the field, or on the class |
| `public void setY(X y)` | `@Setter`, and only where a setter is genuinely wanted |
| A field-per-constant enum with a hand-written constructor and getters | `@Getter @AllArgsConstructor` on the enum — see `Done`, `Refused`, `Priority` |
| `equals`/`hashCode`/`toString` on a value | a `record`, first. `@EqualsAndHashCode` only when a record will not do |

## What Lombok does not own

- **`@NonNull` goes on DTO and marker fields only**, where the generated runtime
  check is the point. Everywhere else nullability is carried by the jetbrains
  `@NotNull` / `@Nullable`, which the IDE instruments. Two annotations meaning
  the same thing in one signature is worse than either.
- **`@SneakyThrows` is not used.** A method handles its own failures — catch
  inside, log through `Logger`, notify when a tester action triggered it. There
  are zero in the tree; keep it that way.
- **`@Data` is not used.** It bundles decisions nobody made — a setter for every
  field, `equals` on a mutable object. Say which ones you want.
- **`@Builder` only where a call site really cannot read positionally.** Most
  Testin constructors take two or three arguments and read fine.

## The exception, and why it is the only one

A private constructor that calls `super(...)` cannot be generated:

```java
public final class Bundle extends DynamicBundle {

    private Bundle() {
        super(Bundle.class, BUNDLE);
    }
}
```

`@NoArgsConstructor` writes an empty body, so there is nowhere for the `super`
call to go. `Bundle` is the one class in `src/main` that hand-writes a
constructor, and this is why. If a second one appears, it needs the same kind of
reason written beside it.

## Checking it

`grep -rn "^    private [A-Z]\w*() *{$" --include=*.java src/main` should return
exactly one line, and it should be `Bundle`.
