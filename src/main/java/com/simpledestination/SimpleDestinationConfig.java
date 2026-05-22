package com.simpledestination;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(SimpleDestinationConfig.GROUP)
public interface SimpleDestinationConfig extends Config
{
    String GROUP = "simpledestination";

    @Alpha
    @ConfigItem(
        keyName = "indicatorColor",
        name = "Indicator color",
        description = "Color used for the world map marker, minimap dot and minimap arrow.",
        position = 0
    )
    default Color indicatorColor()
    {
        return new Color(0, 255, 255, 220);
    }

    @Alpha
    @ConfigItem(
        keyName = "borderColor",
        name = "Border color",
        description = "Color used for the outline of the minimap and world map indicators.",
        position = 1
    )
    default Color borderColor()
    {
        return new Color(255, 255, 60, 255);
    }

    @ConfigItem(
        keyName = "clearWhenReached",
        name = "Clear when reached",
        description = "Clear the current destination when the player gets close to it.",
        position = 2
    )
    default boolean clearWhenReached()
    {
        return true;
    }

    @Range(min = 0, max = 10)
    @ConfigItem(
        keyName = "reachedDistance",
        name = "Reached distance",
        description = "Tile distance used by Clear when reached.",
        position = 3
    )
    default int reachedDistance()
    {
        return 2;
    }
}
