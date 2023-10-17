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


#### [223-10-17]: Findings:

v0.0.5, `7b59b85cf2e81a42d47d14c72ada334f9b07942c`, works, and appears to have gnerated
a package (which I still need to test that I can auto-download it),
though GitHub complained about the hello world workflow having 'No event triggers defined in `on`'

Next commit, `cefd7407c4b72704230560fda75764b6e57d996e`,
it still complained about that, when I left out the `on: ` line entirely.

[Docs indicate complex rules for 'on'](https://docs.github.com/en/actions/using-workflows/workflow-syntax-for-github-actions#onpushpull_requestpull_request_targetpathspaths-ignore)


## Tasks

### DONE Make proof-of-concept GitHub action or workflow or whatever (M4909T-148)

[Start here](https://docs.github.com/en/actions/learn-github-actions/understanding-github-actions)

[Done](https://github.com/stevedxfph/tscript34-p0012-lib01/actions/runs/6537132384)!

Findings: the existence of a .yml file in .github/workflows indicating
the condition under which to run the workflow (`on: [push]`)
makes it run the `jobs` listed in that same file.

### TODO Read more about what the YAML means

There are 'workflows', and 'workflow runs'.
Each time you run a workflow is a 'workflow run', I suppose.

`name` gives the name of the workflow,
while `run-name` is an expression that is evaluated to produce the name of each run.

By 'expression' I mean that it can contain variable references like `${{ github.actor }}`.
Currently I know not at what stage of processing
(before the YAML is parsed? On a per-field basis when the value is needed?)
those expressions are evaluated, nor what can go in them.

[Workflow syntax documentation](https://docs.github.com/en/actions/using-workflows/workflow-syntax-for-github-actions#run-name)
says that "this value can include expressions" (referring to `run-name`)
"and can reference the `github` and `inputs` contexts".
Which hints that each field has its own rules for whether
those expressions are evaluated at all, and what they may contain.
