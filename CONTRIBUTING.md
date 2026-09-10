# Contributing to Testin

> This page carries the **terms** contributions are accepted under, decided in
> [#121](https://github.com/mtb550/test-in/issues/121). How to set the project
> up, what checks to run and what a pull request should look like is
> [#102](https://github.com/mtb550/test-in/issues/102), and will be added here.

## The license

Testin's source is licensed under the **Apache License 2.0** — see
[LICENSE](LICENSE). You may use, change and redistribute it under those terms.

[EULA.md](EULA.md) is a separate document. It is the end user agreement for the
plugin as distributed through JetBrains Marketplace, and applies only if and when
Testin is offered there as a paid or freemium plugin. It grants nothing over the
source and takes nothing away from the Apache license.

## The terms your contribution is accepted under

Two things, and the second one is the one people do not expect:

1. **Your contribution is licensed under Apache 2.0**, the same as the rest of
   the source.
2. **A contributor license agreement is required before an outside contribution
   is merged.** Open the pull request and it will be discussed there; nothing is
   lost by starting work first.

### Why the second one

Because a licence is not ownership, and the difference decides what can happen
to Testin later.

Apache 2.0 says what *you* may do with the code. It does not move the copyright:
a patch sent under it leaves **you** owning your patch, and Testin holding a
licence to it. That is fine for using the code and awkward for everything else —
relicensing a future version, offering a commercial licence, or any arrangement
with a company that wants clear title. Each of those would need every past
contributor to agree, one at a time, forever.

A contributor license agreement settles that once, at the start, in writing. It
is what almost every company-backed open source project uses and it is not a
trap: the code stays Apache 2.0, published, and yours to use like anyone else's.

There are no outside contributors yet, so this is written down before it is
needed rather than after.

## Where the rules live

The conventions this project holds itself to are in the repository, beside the
code they govern, so they travel with a clone:

| | |
|---|---|
| [`CLAUDE.md`](CLAUDE.md) | The architecture rules and the code conventions |
| [`docs/`](docs/README.md) | What Testin does, one use case at a time, with the rules numbered |
| [`docs/standard.md`](docs/standard.md) | How those documents are written, and what a machine checks about them |
| [`docs/decisions.md`](docs/decisions.md) | The decisions that shaped the plugin, and why |

## The one thing to know before you start

`./gradlew compileJava test` must pass. A green build is not proof of a working
plugin — `@NotNull` is not a compile-time contract and most of this code is only
exercised by a running IDE — but a red one is proof of a broken change.
