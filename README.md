• # Zefirrat. Wallpaper Background Sync.

  Sync your IDE background with the current Windows wallpaper.

  Do you spend time choosing beautiful desktop wallpapers, only to never see them because your IDE stays fullscreen all
  day? Do you want your IDE background to follow your desktop wallpaper without manually duplicating the same image in
  IDE settings?

  This plugin is for you. It reads the current Windows wallpaper and syncs it with the IDE background so your workspace
  looks more like your actual desktop.

  ## Features

  - Syncs the current Windows desktop wallpaper into the IDE background
  - Works with JetBrains IDEs built on the IntelliJ Platform
  - Supports `Rider` and `WebStorm`
  - Manual sync action
  - Clear synced background action
  - Settings page for:
    - enabling/disabling sync
    - opacity
    - refresh interval
    - fill mode
    - anchor position

  ## Important limitation

  Wallpaper Engine animation is not supported.

  This plugin reads the current system wallpaper through the Windows wallpaper API. That API usually exposes only a
  static snapshot of the wallpaper file, not the live rendered frame stream produced by Wallpaper Engine.

  So if your wallpaper is animated in Wallpaper Engine, the IDE background will still use a static image.

  ## Requirements

  - Windows
  - JetBrains IDE based on IntelliJ Platform
  - `Rider` or `WebStorm`

  ## Installation

  ### From JetBrains Marketplace

  Install the plugin from the Marketplace inside your IDE.

  ### From source

  1. Build the plugin:
     ```powershell
     .\gradlew.bat build

  2. Start the sandbox IDE:

     .\gradlew.bat runIde
  3. Or install the generated ZIP from:

     build/distributions/

  ## Usage

  After installation, open:

  - Settings | Tools | Zefirrat. Wallpaper Background Sync.

  You can also use the actions in the Tools menu:

  - Sync Windows Wallpaper Now
  - Clear Synced IDE Background

  ## Development

  .\gradlew.bat clean build
  .\gradlew.bat runIde

  ## Notes

  - The plugin currently targets Windows only.
  - The wallpaper sync interval is configurable.
  - The background is applied at the IDE level, not per editor tab.

  ## License

  See the repository license file.
