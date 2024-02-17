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

Firs commit of 0.0.6-SNAPSHOT, `622f91323643316ba78dffeeddd0765561905cd9`,
I said `on: workflow_dispatch` (as per https://stackoverflow.com/a/70315883/17653243).
This seems to have prevented either workflow from automatically running.

Also note that when I pushed tag `0.0.5`, before that change,
the build failed because that package had already been built.
I think this is fine.  Just be careful not to have the same `<version>`
appear in multiple commits, and have the tag match it, for clarity.

I manually (through the GitHub web UI) kicked off the `learn-github-actions`
workflow, and [it succeeded](https://github.com/stevedxfph/tscript34-p0012-lib01/actions/runs/6553382624).
This seems to indicate that `on: workflow_dispatch` works as a way to have
a workflow that can be triggered, but does not happen automatically.

In the future maybe I can figure a way to have it run when tags of the form `/\d+\.\d+\.\d+/` are pushed.

[2023-10-17]: I got `v0.0.6-take2` to automatically build with the following `on` bit:

```yaml
on:
  push:
    tags:
      - 'v[0-9]+.[0-9]+'
      - 'v[0-9]+.[0-9]+.[0-9]+'
      - 'v[0-9]+.[0-9]+.[0-9]+-*'
```


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


### TOGO See if you can publish to Maven Central again

There are a lot of steps.

- [X] Create account on https://central.sonatype.com/
- [X] Verify domain by adding a TXT record to nuke24.net
- [X] Generate a GPG key on my laptop
- [X] Configure Maven to call GPG to sign keys
  - Yet more `<plugin>`s required
  - But miraculously it does seem to work, and `mvn deploy`,
    while it didn't manage to actually deploy,
    did get as far as getting GPG to prompt me for my passphrase
  - `mvn verify` might get to the just-about-to-depoloy point 
- [X] Determine that `mvn deploy` doesn't work,
  since it's apparently trying to use some old system
  involving Jira usernames and passwords, which I don't have.
- [X] Upload a zip to https://central.sonatype.com/publishing
  and see that it got rejected.  At least it lists a bunch of reasons why.
  No .sha1s / .md5s.  Why doesn't the dumb Maven plugin generate those, huh?
- [ ] Generate some sha1s and md5s myself

#### Upload Attempt 3

> File path 'net/nuke24/tscript34/p0012' is not valid for file 'tscript34-p0012-lib01-0.0.8-javadoc.jar'

Repeat for all the files.
Does it not want that folder structure?
Oh, or maybe it's because I forgot the version number level.

> Developers information is missing
> Project URL is not defined
> Project description is missing
> SCM URL is not defined

### Upload Attempt 4

I added an additional level of directory named "0.0.8".

`zip -r tscript34-p0012-lib01-0.0.8-take4.zip net` from within `target/`
(under which I have a `net/...`, which contains the `jar`, `pom`, `md5`, `sha1`,
and `asc` files just for this one version) seems to do the job.

> File path 'net/nuke24/tscript34/p0012/0.0.8' is not valid for file 'tscript34-p0012-lib01-0.0.8-javadoc.jar'

So it still doesn't like my directory structure.

### Attempt #5

I had tried to fill in some metadata in pom.xml,
and Sonatype seems to have fewer complaints.

> 1 out of 2 Components Validated
> 
> null

Still doesn't like my paths.

I realize now that the artifact name should be in there, too.

`net/nuke24/tscript34/p0012/tscript34-p0012-lib01/0.0.9/tscript34-p0012-lib01-0.0.9.pom`, etc.

### Attempt #6

Deployment ID `bf4e33ae-2ec4-40d3-a5f8-6ea4ee84ef64`.
Pending.


Still pending.

Well that's different, anyway.

Time for bed.  Check again tomorrow.

https://central.sonatype.com/publishing/deployments


(a few minutes later)

Ooh, it's now 'verified', and I can 'publish'!
So I clicked that.

Now it's taking its time publishing.
