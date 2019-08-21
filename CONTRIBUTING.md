# :city_sunrise: How to contribute

Hello, and welcome! :smile: Whether you are here to report a bug, to request a new feature or to propose a pull request, this document is for you! This document describes the guidelines for these contributions.

In the current phase of development, it is more like a memo, so may not be considered seriously during the start-up phase.

## Improving the document
// TODO
## Reporting a bug
// TODO

## Submitting a patch
// TODO

**Commit message format**

```
{ [!](<type>) <subject> }
[ <BLANK LINE>
<supplement> ]
```

`{...}` means the part must appear at least once, and `[...]` means the part is optional.

If a change modifies the public API and therefore becomes a breaking change, `!` should be added to the ahead of the `<type>` part.

The `<type>` part of your commit indicates what type of change this commit is about. The
accepted types are:

 - `feat`: A new feature, related issue (if have) should be specified in the `<subject>` part.
 - `fix`: A new bugfix, related issue (if have) should be specified in the `<subject>` part.
 - `test`:  Changes that is purely related to the test suite only, the related test should be specified in the `<subject>` part.
 - `doc`: Changes to the API documentation in code, changes related to the project description documents (`README`, this document, etc) should be categorized to `misc`.
 - `perf`: Changes that significantly improves performance.
 - `style`: Changes that related to the style of the code or the naming of private API.
 - `misc`: Changes that do not directly relate to code, such as CI configuration, project description document, etc.
 - `*`: *(Use only if absolutely necessary)* Changes that can not be categorized due to its wide range of modification, or none of the types above is related.

**We strongly suggest** that changes related to multiple features or issues be split into multiple commits.

The *optional* <supplement> part should contain supplemental material for this PR, such as related discussions, performance test, etc.