package com.simpledestination;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class SimpleDestinationPluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(SimpleDestinationPlugin.class);
        RuneLite.main(args);
    }
}
