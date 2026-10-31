# GlintForge (Fabric, Minecraft 26.1.2, client-side)
Press G (rebind in Controls) -> pick/hold an item -> tweak Size / Opacity / Speed -> choose overlay texture -> Save.
Custom textures: drop PNGs into  config/glintforge/textures/  (any size; tiles/repeats like the vanilla glint).
Settings: config/glintforge/items.json
Build: fill gradle.properties from https://fabricmc.net/develop, then ./gradlew build (JDK 25).

## Building locally
Install JDK 25 and Gradle 9.4.1+, then in this folder run:  gradle build
The jar appears in build/libs/ (use the one WITHOUT "-sources").
Or push to GitHub: the included Actions workflow builds it and uploads the jar as an artifact.
