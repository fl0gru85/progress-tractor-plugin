# Progress Tractor 🚜

IntelliJ plugin that replaces the IDE's progress bars with a tractor plowing a field.

- **Determinate progress:** the plowed soil behind the tractor = completed work; the crop in front = remaining work.
- **Indeterminate progress:** the tractor drives back and forth across the field.
- Drawn programmatically with Java2D (no image assets), adapts to light/dark themes and HiDPI.
- Respects the `ProgressBar.status` client property (`error` / `warning`) by tinting the plowed soil,
  so e.g. failing tests in the test runner remain visible.

Compatible with IntelliJ-based IDEs **2024.2 (build 242) and newer**.

## Build

Requires JDK 17+ to run Gradle; a JDK 21 toolchain is provisioned automatically (foojay resolver).

```bash
./gradlew test          # unit tests (geometry + headless rendering)
./gradlew buildPlugin   # -> build/distributions/progress-tractor-plugin-<version>.zip
                        #    + progress-tractor-plugin-<version>-preview.png (light/dark preview)
./gradlew runIde        # start a sandbox IDE with the plugin installed
./gradlew verifyPlugin  # JetBrains Plugin Verifier against platformVersion (gradle.properties)
```

On Windows use `.\gradlew.bat` instead of `./gradlew`.

Install the ZIP via *Settings → Plugins → ⚙ → Install Plugin from Disk…* and restart the IDE.

## Release

Every push to `main` and every pull request runs the `Build` workflow (tests + plugin ZIP as artifact).

Pushing a tag `vX.Y.Z` runs the `Release` workflow: tests, Plugin Verifier, signing and upload of
version `X.Y.Z` to JetBrains Marketplace. The tag defines the version; `pluginVersion` in
`gradle.properties` is only the local default.

```bash
git tag v1.0.1
git push origin v1.0.1
```

Required repository secrets:

| Secret | Content |
|---|---|
| `PUBLISH_TOKEN` | JetBrains Marketplace permanent token |
| `CERTIFICATE_CHAIN` | Signing certificate chain (`chain.crt`) |
| `PRIVATE_KEY` | Signing private key (`private.pem`) |
| `PRIVATE_KEY_PASSWORD` | Password of the private key |

The first version of a plugin has to be uploaded manually at <https://plugins.jetbrains.com/plugin/add>;
the Marketplace rejects a version that already exists.

## License

[MIT](LICENSE)

## How it works

`TractorUiInstaller` registers the plugin's UI delegate as Swing's `ProgressBarUI` in the `UIManager`.
`TractorUiListener` re-installs it at application start and after every Look-and-Feel change.
`TractorProgressBarUI` (a `BasicProgressBarUI`) paints the scene via `TractorPainter`; layout math lives in
`TractorGeometry`. A Swing timer repaints the bar for the wheel/smoke animation only while it is showing.

`TractorProgressBarUIFactory` is a tiny Java class because Kotlin cannot declare the static
`createUI(JComponent)` that Swing needs (it clashes with the inherited static in `BasicProgressBarUI`).

The plugin requires an IDE restart to install/uninstall, because the UI class stays referenced from
Swing's UI defaults.
