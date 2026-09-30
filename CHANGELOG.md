# Changelog

All notable changes to FoliaGUI are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/).

## [Unreleased]

### Fixed
- A `GuiClickEvent` listener could un-cancel a click on a protected slot. Protected slots now stay
  locked regardless of listeners.
- Asking a new `ChatPrompt` for a player who already had one pending now completes the old prompt's
  callback with `null` instead of leaving it waiting forever.
- `BaseGui.open` no longer reads the inventory's viewers from the caller's thread.
- Made two async-content tests deterministic (they failed intermittently).

### Added
- Continuous integration with a Paper API version matrix, CodeQL analysis and Dependabot.
- Release workflow: pushing a `vX.Y.Z` tag builds the project and publishes a GitHub release.
- `CONTRIBUTING.md`, `SECURITY.md`, issue and pull request templates, and `.editorconfig`.
- Sources and javadoc jars are built with every `mvn verify`.

## [1.0.0]

Initial tracked release. See the git history for earlier changes.
