# Repository Guidelines

## Initial Proposal and remaining tasks

* See [InitialProposal](promotions/InitialProposal.md)
* See [Todo](todo.md)

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
JUnit scaffolding is available via Gradle; add new tests in `src/test/java`. Name test classes after the unit under test with a `Test` suffix (e.g., `RadioControllerTest`). Run `./gradlew test` locally before committing. When adding gameplay features, exercise them with a focused dev-world scenario and capture repro steps in the PR description if automated coverage is impractical.

## Commit & Pull Request Guidelines
Use concise, present-tense commit messages (`Fix antenna wire sync`) and keep related changes in a single commit when practical. PRs should outline the player-facing impact, list testing done (`runClient`, `test`), and reference any linked GitHub issues or design docs. Screenshots or clips help reviewers validate UI or in-game changes. Confirm the build is clean and data generator output is committed before requesting review.

## Development Environment Notes
Use dedicated Gradle run directories to avoid polluting personal saves. If you adjust configuration files under `run/`, do not commit them unless they are required defaults. When introducing new dependencies, add them through `build.gradle.kts` and cite the source Maven repository.
