# Repository Guidelines

## Initial Proposal and remaining tasks

* See [InitialProposal](promotions/InitialProposal.md)
* See [Todo](todo.md)
* See [Model design bounty](doc/ModelDesignBounty.md) for models/textures to be drawn by someone else; code can ship with a placeholder box, and new blocks that need art go on that list.

The Initial Proposal (2022) is the original inspiration and a guide to general goals. Much of it has since changed or
was never accurate (mechanics, numbers, recipes). Treat it as background, not a spec: when it would decide a design
question, ask the maintainer instead of assuming it is correct.

## Project Structure & Module Organization
RadioCraft is a NeoForge-backed Minecraft mod packaged with Gradle. Source lives in `src/main/java/com/arrl/...`, resources in `src/main/resources`, and generated data also syncs into `src/generated/resources`. Development run configurations output to `runs/`, separating `client`, `server`, and `data` worlds, while build artifacts land in `build/`. Keep shared assets (textures, sounds) versioned under `src/main/resources/assets/radiocraft` and prefer data-driven content via the data generator.

## Build, Test, and Development Commands
- `./gradlew build` compiles, runs checks, and assembles the mod jar under `build/libs/`.
- `./gradlew runClient` launches a dev client with `RADIOCRAFT_DEV_ENV=true` using `runs/client` as the sandbox.
- `./gradlew runServer` starts a headless NeoForge server in `runs/server` for multiplayer testing.
- `./gradlew runData` refreshes generated assets into `src/generated/resources` (commit the diff when intentional).
- `./gradlew printVersion` echoes the git-derived semantic version that will stamp artifacts.

## Coding Style & Naming Conventions
Target Java 21 and four-space indentation. Class and enum names follow `UpperCamelCase`, methods and fields use `lowerCamelCase`, and constants remain `UPPER_SNAKE_CASE`. Keep packages under `com.arrl.radiocraft`. The build enforces `-Xlint:all`; address warnings rather than suppressing them. Document public API surfaces with Javadoc when they feed mod integrations.

## Testing Guidelines
See [doc/Testing.md](doc/Testing.md) for the full guide and the list of known bugs. In short:
- `./gradlew test`: JUnit 5 unit tests in `src/test/java` for pure logic (no game bootstrap). Name test classes after the unit under test with a `Test` suffix (e.g., `BandUtilsTest`).
- `./gradlew runGameTestServer`: in-world GameTests in `src/gametest` (a dev-only source set, never shipped). Known bugs are `required = false` tests; make them required once fixed.
- `./gradlew prodLaunchTest`: the shipped jar on a real NeoForge server and client, outside the dev environment.
- `tools/devserver.sh start` + `python tools/rcon.py "<command>"`: a live headless dev server driven over RCON. Use `/rcdebug` to inspect and drive radios, antennas, networks and solar weather.

Run the unit tests and GameTests before committing. Add a GameTest when fixing a bug or adding gameplay behavior that can be checked server-side. Rendering, GUIs and audio still need `runClient` and a human: list anything that needs that in [doc/NeedsTesting.md](doc/NeedsTesting.md) for the maintainer's next playtest. The maintainer works through that file with `tools/needs_testing.py`: at the start of a session look for items marked `- [!]` (needs work) and their `Note (date):` comments and screenshots under `doc/testing_shots/`, fix them, and set them back to `- [ ]`.

## Commit & Pull Request Guidelines
Use concise, present-tense commit messages (`Fix antenna wire sync`) and keep related changes in a single commit when practical. PRs should outline the player-facing impact, list testing done (`runClient`, `test`), and reference any linked GitHub issues or design docs. Screenshots or clips help reviewers validate UI or in-game changes. Confirm the build is clean and data generator output is committed before requesting review.

## Releasing
Pushing code and releasing are separate: `tools\push-versions.ps1 -PushOnly` pushes the branch and its tags to GitHub
(RadiocraftARRL/RadioCraft, from `tools/modrinth_projects.json`) without a release. Release only when the maintainer asks,
so players are not sent too many versions. Tag the commit with the version (`1.9.1`; the jar version comes from the tag),
then `tools\push-versions.ps1 -Version 1.9.1 -Build` pushes the branch and tag, builds, and publishes it to Modrinth and as
a GitHub release (`-NoGitHubRelease` for Modrinth only) with the matching section of `doc/ReleaseNotes-<major.minor>.md` as
the changelog. Build a release from a clean checkout of the tag (a `git worktree`) when the tree has other work in it. Tokens live in `~/.radiocraft/` or the environment, never in the repo; project ids and dependency lists
are in `tools/modrinth_projects.json`. The shipped jar comes from the `modJar` task: Shadow relocates JOrbis, then the
jar-in-jar mods are added untouched (never feed nested jars through Shadow, it unpacks them). The `-slim` jar is not
for players. Before publishing, push-versions.ps1 launches the jar on a production NeoForge server and client
(`./gradlew prodLaunchTest`, see doc/Testing.md) and stops if it does not load. Never skip that for a release.

## Development Environment Notes
`tools/deploy-prism.sh` builds the mod and installs it plus its runtime dependencies into a Prism Launcher instance named "RadioCraft Dev". Use dedicated Gradle run directories to avoid polluting personal saves. If you adjust configuration files under `run/`, do not commit them unless they are required defaults. When introducing new dependencies, add them through `build.gradle.kts` and cite the source Maven repository.

## Agent Notes
- Do **not** edit files under `src/generated/`; always ask the user to run `./gradlew runData` after making localization or data changes so generated assets stay in sync.
