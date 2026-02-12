WildFly Launcher
============
https://wildfly.org

This project provides a tool than can be used to launch various types of WildFly processes.

Building
-------------------

Prerequisites:

* JDK 11 or newer - check `java -version`
* Maven 3.6.0 or newer - check `mvn -v`

To build with your own Maven installation:

> ./mvnw install




License
-------
* [Apache License Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.html)

Documentation
---
* https://docs.wildfly.org/wildfly-launcher

== Releasing

Releasing the project requires permission to deploy to Maven Central see https://central.sonatype.org/publish/requirements/[Maven Central Release Requirements].
Once everything is setup, you simply need to run the `./release.sh` script. There are two required parameters:

1. `-r` or `--release` which is the version you want to release
2. `-d` or `--development` which is the next development version.

By default, the release version cannot contain `SNAPSHOT` and the development version, must contain `SNAPSHOT`.

[source,bash]
.Example Command
----
./release.sh -r 1.0.0.Final -d 1.0.1.Final-SNAPSHOT
----

=== Supported Arguments

|===
|Argument | Requires Value | Description

| `-d`, `--development`
| Yes
| The next version for the development cycle.

| `-f`, `--force`
| No
| Forces to allow a SNAPSHOT suffix in release version and not require one for the development version.

| `-h`, `--help`
| N/A
| Displays this help

| `--notes-start-tag`
| Unused
| Passes the `--notes-from-tag` and the argument to the `gh create release` command.

| `-p`, `--prerelease`
| Unused
| Passes the `--prerelease` to the `gh create release` command.

| `-r`, `--release`
| Yes
| The version to be released. Also used for the tag.

| `--dry-run`
| No
| Executes the release in as a dry-run. Nothing will be updated or pushed.

| `-v`, `--verbose`
| No
| Prints verbose output.

|===


Any additional arguments are considered arguments for the Maven command.
