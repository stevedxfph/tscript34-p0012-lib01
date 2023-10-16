# TS34-P12-Lib01

This project is an attempt to make a Maven package.

## FAQ

### Can I publish to Maven Central?

No.  The list of steps is too long.

### Can I publish to JitPack?

No.  It won't let you use your proper `groupId` and `artifactId` or `version`.
It derives those itself using your GitHub username, project name, and tag.
Which arguably is a simpler way to do things, but maybe you don't want to
couple the name of your proect to where it happens to be hosted.

### Can I get GitHub to build and host packages for me?

[Maybe](https://docs.github.com/en/actions/publishing-packages/publishing-java-packages-with-maven)!


## Tasks

### DONE Make proof-of-concept GitHub action or workflow or whatever (M4909T-148)

[Start here](https://docs.github.com/en/actions/learn-github-actions/understanding-github-actions)

[Done](https://github.com/stevedxfph/tscript34-p0012-lib01/actions/runs/6537132384)!

Findings: the existence of a .yml file in .github/workflows indicating
the condition under which to run the workflow (`on: [push]`)
makes it run the `jobs` listed in that same file.
