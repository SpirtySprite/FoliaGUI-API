# Changelog

All notable changes to FoliaGUI are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/).

## [Unreleased]

### Changed
- Runtime state is no longer process-wide. A `FoliaGUIService` (create one with `FoliaGUI.create(plugin)`)
  owns the open-GUI registry, navigation history, anvil/sign/merchant/chat sessions, theme and listener.
  `FoliaGUI.init` still creates a default service and all existing static helpers keep working on it.
- `BaseGui#service()`, `BaseGui#service(FoliaGUIService)` and `.service(...)` on the GUI builders bind a GUI
  to an explicit service. `ChatPrompt.ask(service, player, ...)` and `hasSession(service, player)` were added.
- `FoliaGUI.init` from a second plugin while a default service exists now logs a warning instead of
  silently doing nothing.
- The item identity tag uses the fixed key `foliagui:item` instead of a key namespaced by the owning plugin.
- Internal `handleClick`/`handleClose`/`handleDrag`/`handleSignChange`/`handleQuit` methods on `AnvilGui`,
  `MerchantGui` and `SignGui` now take the service as their first argument. They are `@ApiStatus.Internal`.

### Removed
- `GuiManager.register` and `GuiManager.unregister` (internal, now on `GuiRegistry`).

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
