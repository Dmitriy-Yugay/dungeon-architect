# Dungeon Architect

A planned dungeon-builder tower defense game built with Kotlin and libGDX.
The player designs a dungeon, places defenses, and survives waves of invading
heroes.

## Status

The initial project scaffold is in place. It opens an empty desktop window;
gameplay systems have not been implemented yet.

## Requirements

- JDK 21

## Run

```shell
./gradlew lwjgl3:run
```

On macOS, use the Gradle run task so the LWJGL process starts on the first
thread.

## Build

```shell
./gradlew build
```

## Modules

- `core`: platform-independent game application and future shared game code.
- `lwjgl3`: desktop launcher and LWJGL3-specific window configuration.
- `assets`: shared game assets; currently empty.

## Documentation

- [Project context](docs/PROJECT_CONTEXT.md)
- [Game design](docs/game-design.md)
- [Architecture](docs/architecture.md)

## Technology

- Kotlin
- libGDX
- Gradle
