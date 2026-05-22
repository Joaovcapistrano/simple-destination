# Simple Destination

Simple Destination is a lightweight RuneLite external plugin for setting and following a single manual destination from the World Map.

## Features

- Right-click the World Map and choose **Set Destination** to save a destination.
- Use **Clear Destination** to remove the active destination.
- Shows a destination indicator on the World Map.
- Shows a minimap dot when the destination is visible on the minimap.
- Shows a minimap arrow when the destination is outside the visible minimap area.
- Shows a World Map edge arrow when the destination is outside the currently visible World Map area.
- Allows focusing the World Map on the saved destination from the destination indicator.
- Optionally clears the destination automatically when the player reaches it.

## Configuration

- **Indicator color**: color used for the destination dot and arrow fill. Defaults to RuneLite cyan.
- **Border color**: color used for the outline of map and minimap indicators. Defaults to `#FFFF3CFF`.
- **Clear when reached**: automatically clears the destination when the player gets close enough. Enabled by default.
- **Reached distance**: tile distance used by **Clear when reached**. Defaults to `2`.

## Development

This repository follows the RuneLite `example-plugin` / Plugin Hub structure.

Run locally with:

```bash
./gradlew run
```

The development client starts with:

```txt
--developer-mode --debug
```

## Plugin Hub

The plugin metadata is defined in `runelite-plugin.properties`:

```properties
displayName=Simple Destination
author=RovicT
plugins=com.simpledestination.SimpleDestinationPlugin
build=standard
```

