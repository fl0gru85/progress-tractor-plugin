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

```powershell
.\gradlew.bat test          # unit tests (geometry + headless rendering)
.\gradlew.bat buildPlugin   # -> build\distributions\progress-tractor-plugin-<version>.zip
                            #    + progress-tractor-plugin-<version>-preview.png (light/dark preview)
.\gradlew.bat runIde        # start a sandbox IDE with the plugin installed
.\gradlew.bat verifyPlugin  # JetBrains Plugin Verifier against platformVersion (gradle.properties)
```

Install the ZIP via *Settings → Plugins → ⚙ → Install Plugin from Disk…* and restart the IDE.

## How it works

`TractorUiInstaller` registers the plugin's UI delegate as Swing's `ProgressBarUI` in the `UIManager`.
`TractorUiListener` re-installs it at application start and after every Look-and-Feel change.
`TractorProgressBarUI` (a `BasicProgressBarUI`) paints the scene via `TractorPainter`; layout math lives in
`TractorGeometry`. A Swing timer repaints the bar for the wheel/smoke animation only while it is showing.

`TractorProgressBarUIFactory` is a tiny Java class because Kotlin cannot declare the static
`createUI(JComponent)` that Swing needs (it clashes with the inherited static in `BasicProgressBarUI`).

The plugin requires an IDE restart to install/uninstall, because the UI class stays referenced from
Swing's UI defaults.
