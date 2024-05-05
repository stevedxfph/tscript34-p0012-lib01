# TS34-P12-Lib01

This project is an attempt to make a Maven package.

## FAQ

### Can I publish to Maven Central?

No.  The list of steps is too long.

2024-02-17 Update: I [managed to do it](#finally-publish-to-maven-central).
It was a huge pain in the butt, but it is possible.

### Can I publish to JitPack?

No.  It won't let you use your proper `groupId` and `artifactId` or `version`.
It derives those itself using your GitHub username, project name, and tag.
Which arguably is a simpler way to do things, but maybe you don't want to
couple the name of your project to where it happens to be hosted.

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

### Make proof-of-concept GitHub action
...or workflow or whatever

(DONE; M4909T-148)

[Start here](https://docs.github.com/en/actions/learn-github-actions/understanding-github-actions)

[Done](https://github.com/stevedxfph/tscript34-p0012-lib01/actions/runs/6537132384)!

Findings: the existence of a .yml file in .github/workflows indicating
the condition under which to run the workflow (`on: [push]`)
makes it run the `jobs` listed in that same file.

### Read more about what the YAML means
(TODO)

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


### Finally Publish to Maven Central
(DONE; M4909T-257)

There are a lot of steps.

- [X] Create account on https://central.sonatype.com/
- [X] Verify domain by adding a TXT record to nuke24.net
- [X] [Generate a GPG key](https://central.sonatype.org/publish/requirements/gpg/) on my laptop
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
- [X] Generate some sha1s and md5s myself
  - I wrote [a small Ruby script](./generate.hashes.rb) to help.
- [X] Add some extra metadata to pom.xml
- [X] A few more attempts at zip files to get the folder structure right

#### Generate a GPG key

```sh
gpg --gen-key
```

Pick a name and password, etc.

Assuming you only have one GPG key in your...local keystore...thing
(Lord help you if you have more than one because I don't know
how to configure `mvn`/`gpg`/whatever to choose one)
you should be able to `mvn verify` with the result that
there are `.asc` files under `target/` corresponding to each
JAR file that was put there from an earlier step.

Relevant section of pom.xml (within project > build > plugins):

```xml
<plugin>
	<groupId>org.apache.maven.plugins</groupId>
	<artifactId>maven-gpg-plugin</artifactId>
	<version>1.5</version>
	<executions>
		<execution>
			<id>sign-artifacts</id>
			<phase>verify</phase>
			<goals>
				<goal>sign</goal>
			</goals>
			<configuration>
				<keyname>${gpg.keyname}</keyname>
				<passphraseServerId>${gpg.keyname}</passphraseServerId>
			</configuration>
		</execution>
	</executions>
</plugin>
```

#### Upload Attempt #3

> File path 'net/nuke24/tscript34/p0012' is not valid for file 'tscript34-p0012-lib01-0.0.8-javadoc.jar'

Repeat for all the files.
Does it not want that folder structure?
Oh, or maybe it's because I forgot the version number level.

> Developers information is missing
> Project URL is not defined
> Project description is missing
> SCM URL is not defined

### Upload Attempt #4

I added an additional level of directory named "0.0.8".

`zip -r tscript34-p0012-lib01-0.0.8-take4.zip net` from within `target/`
(under which I have a `net/...`, which contains the `jar`, `pom`, `md5`, `sha1`,
and `asc` files just for this one version) seems to do the job.

> File path 'net/nuke24/tscript34/p0012/0.0.8' is not valid for file 'tscript34-p0012-lib01-0.0.8-javadoc.jar'

So it still doesn't like my directory structure.

### Upload Attempt #5

I had tried to fill in some metadata in pom.xml,
and Sonatype seems to have fewer complaints.

> 1 out of 2 Components Validated
> 
> null

Still doesn't like my paths.

I realize now that the artifact name should be in there, too.

`net/nuke24/tscript34/p0012/tscript34-p0012-lib01/0.0.9/tscript34-p0012-lib01-0.0.9.pom`, etc.

### Upload Attempt #6

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


[2024-02-17T09]: It is published!

Can we require the new version from app01
and have it download the thing
and all work out?

```sh
mvn test
```

```
Downloading from central: https://repo1.maven.org/maven2/net/nuke24/tscript34/p0012/tscript34-p0012-lib01/0.0.9/tscript34-p0012-lib01-0.0.9.pom
Downloaded from central: https://repo1.maven.org/maven2/net/nuke24/tscript34/p0012/tscript34-p0012-lib01/0.0.9/tscript34-p0012-lib01-0.0.9.pom (4.0 kB at 5.7 kB/s)
Downloading from central: https://repo1.maven.org/maven2/net/nuke24/tscript34/p0012/tscript34-p0012-lib01/0.0.9/tscript34-p0012-lib01-0.0.9.jar
Downloaded from central: https://repo1.maven.org/maven2/net/nuke24/tscript34/p0012/tscript34-p0012-lib01/0.0.9/tscript34-p0012-lib01-0.0.9.jar (3.7 kB at 29 kB/s)
```

:o

## Packaging Automation

Maven's supposed to do all of this, but doesn't.

| Tool               | Task
|--------------------|--------------------------|
| TBD                | Generate POM.xml         |
| `mvn clean verify` | compile Java             |
| `mvn clean verify` | create regular JAR file  |
| `mvn clean verify` | create sources.jar       |
| `mvn clean verify` | create javadoc.jar       |
| `mvn clean verify` | create .asc files        |
| TBD                | generate hash files      |
| TBD                | create zip               |

`mvn clean verify` saves its results as `"target/" + artifact name + "-" + version + extension`,
e.g. `tscript34-p0012-lib01-0.0.10-SNAPSHOT-sources.jar.asc`.

Steps for remaining tasks:
- Derive group ID, artifact ID, version from pom.xml
- Based on those and knowledge of where Maven stores its results,
  generate a list of files, including hashes, to be zipped
- From that list, create a zip file

### Upload attempt #7 / v0.0.10.1

Deployment ID: edbd806d-0da3-4b85-8fac-ed3989cb1882

It validated and is now 'publishing', so I guess it worked!

I suppose the next thing to do is to make the packager into
a self-contained tool so I can use it on other projects.


### Upload attempt #8 / v0.0.10.2

Can we do without the `<distributionManagement>` part of the pom.xml?

Also building on another computer, which means I have a different signing key.

When I attempt to publish, it says

> Invalid signature for file: tscript34-p0012-lib01-0.0.10.2-javadoc.jar
> Invalid signature for file: tscript34-p0012-lib01-0.0.10.2-sources.jar
> Invalid signature for file: tscript34-p0012-lib01-0.0.10.2.jar
> Invalid signature for file: tscript34-p0012-lib01-0.0.10.2.pom

Which is all of them.

Is this because I used a different signature than before,
or because I haven't distributed this key?

### Upload attempt #9

So I did a

```sh
gpg --keyserver keyserver.ubuntu.com --send-key BA43B4AA4A1D82285A68860E1796BC8141D3529A
```
And uploaded again.

Still didn't work.

GPG says the signatures are valid.


### Upload attempt #10

What if I use a new key with a different e-mail address?

```sh
gpg --gen-key
```

With e-mail address `togos00+etf20240504@gmail.com`,
generating `C170189784A1B914EB4B3595A897357685CFB31E`

```
gpg --keyserver keyserver.ubuntu.com --send-key C170189784A1B914EB4B3595A897357685CFB31E
mvn -Dgpg.keyname=C170189784A1B914EB4B3595A897357685CFB31E verify
```

And attempt to publish that.

Pending...Validating...Failed.
It still doesn't like my signatures!


### Upload attempt #11

Signed with good old `8B9E6793B42A6ED7A4382423565A5EBB1DDED0FE`.

Validated!

So it still seems it didn't like my alternate signing keys.
Whether that's just because they're not the first one I signed
with or because I failed to do some other step,
like upload to all 3 of the key servers mentioned
(keyserver.ubuntu.com, keys.openpgp.org, pgp.mit.edu)
I'm still not 100% sure.

What are you supposed to do if you lose access to your original signing key?
GPG doesn't make it easy to back them up!

Anyway, we can apparently do without `<distributionManagement>`.


### Upload attempt #12

```sh
curl --request POST --verbose --header "Authorization: Bearer ${auth_base64}" --form bundle=@target/tscript34-p0012-lib01-0.0.10.3-package.zip   https://central.sonatype.com/api/v1/publisher/upload
```

Server says `201 Created`.

https://central.sonatype.com/publishing shows a new deployment has been created,
with the same name as the file ("tscript34-p0012-lib01-0.0.10.3-package.zip").

So that worked.  I suppose there's another API endpoint
that I could use to actually publish.
(See [the docs](https://central.sonatype.org/publish/publish-portal-api/#uploading-a-deployment-bundle).)
