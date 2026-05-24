package com.simpledestination;

import com.google.inject.Provides;
import java.awt.Rectangle;
import java.util.Arrays;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Point;
import net.runelite.api.RenderOverview;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapOverlay;

@Slf4j
@PluginDescriptor(
    name = "Simple Destination",
    description = "Set a destination from the World Map and follow it with simple map and minimap indicators.",
    tags = {"map", "minimap", "destination", "navigation"}
)
public class SimpleDestinationPlugin extends Plugin
{
    private static final String DESTINATION_TARGET = "<col=00ffff>Destination</col>";
    private static final String SET_DESTINATION = "Set";
    private static final String CLEAR_DESTINATION = "Clear";
    private static final String FOCUS_ON_DESTINATION = "Focus on";
    private static final int WORLD_MAP_EDGE_PADDING = 18;
    private static final int WORLD_MAP_INDICATOR_CLICK_RADIUS = 14;

    @Inject
    private Client client;

    @Inject
    private SimpleDestinationConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private SimpleDestinationMinimapOverlay minimapOverlay;

    @Inject
    private SimpleDestinationWorldMapOverlay destinationWorldMapOverlay;

    @Inject
    private WorldMapOverlay worldMapOverlay;

    @Getter
    private WorldPoint destination;

    private Point lastMenuOpenedPoint;

    @Override
    protected void startUp()
    {
        overlayManager.add(minimapOverlay);
        overlayManager.add(destinationWorldMapOverlay);
        log.debug("Simple Destination started");
    }

    @Override
    protected void shutDown()
    {
        overlayManager.remove(minimapOverlay);
        overlayManager.remove(destinationWorldMapOverlay);
        clearDestination();
        log.debug("Simple Destination stopped");
    }

    @Subscribe
    public void onMenuOpened(MenuOpened event)
    {
        lastMenuOpenedPoint = client.getMouseCanvasPosition();
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event)
    {
        Widget worldMapView = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);

        if (worldMapView == null)
        {
            return;
        }

        Point mousePosition = client.getMouseCanvasPosition();

        if (!worldMapView.getBounds().contains(mousePosition.getX(), mousePosition.getY()))
        {
            return;
        }

        addSetDestinationMenuEntry();

        if (destination != null)
        {
            addClearDestinationMenuEntry();

            if (isMouseOverWorldMapIndicator(mousePosition, worldMapView.getBounds()))
            {
                addFocusOnDestinationMenuEntry();
            }
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (!config.clearWhenReached() || destination == null || client.getLocalPlayer() == null)
        {
            return;
        }

        WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();

        if (playerLocation.getPlane() == destination.getPlane()
            && playerLocation.distanceTo(destination) <= config.reachedDistance())
        {
            clearDestination();
        }
    }

    public void setDestination(WorldPoint newDestination)
    {
        if (newDestination == null)
        {
            return;
        }

        destination = newDestination;
    }

    public void clearDestination()
    {
        destination = null;
    }

    private void addSetDestinationMenuEntry()
    {
        if (hasMenuOption(SET_DESTINATION))
        {
            return;
        }

        createRightClickOnlyMenuEntry(SET_DESTINATION)
            .onClick(entry ->
            {
                Point clickPoint = client.isMenuOpen() && lastMenuOpenedPoint != null
                    ? lastMenuOpenedPoint
                    : client.getMouseCanvasPosition();

                setDestination(calculateWorldMapPoint(clickPoint));
            });
    }

    private void addClearDestinationMenuEntry()
    {
        if (hasMenuOption(CLEAR_DESTINATION))
        {
            return;
        }

        createRightClickOnlyMenuEntry(CLEAR_DESTINATION)
            .onClick(entry -> clearDestination());
    }

    private void addFocusOnDestinationMenuEntry()
    {
        if (hasMenuOption(FOCUS_ON_DESTINATION))
        {
            return;
        }

        createPriorityMenuEntry(FOCUS_ON_DESTINATION)
            .onClick(entry ->
            {
                if (destination != null)
                {
                    client.getWorldMap().setWorldMapPositionTarget(destination);
                }
            });
    }

    private MenuEntry createRightClickOnlyMenuEntry(String option)
    {
        /*
         * RuneLite treats the last/top menu entry as the left-click action.
         * Therefore, regular plugin actions are inserted at index 0 so they stay
         * available in the right-click menu without replacing the World Map's
         * default left-click behavior.
         */
        return client.createMenuEntry(0)
            .setOption(option)
            .setTarget(DESTINATION_TARGET)
            .setType(MenuAction.RUNELITE)
            .setDeprioritized(true)
            .setForceLeftClick(false);
    }

    private MenuEntry createPriorityMenuEntry(String option)
    {
        /*
         * Used only for Focus on Destination. When the mouse is over the custom
         * World Map indicator, this should become the primary action, matching
         * the behavior expected from map markers.
         */
        return client.createMenuEntry(client.getMenuEntries().length)
            .setOption(option)
            .setTarget(DESTINATION_TARGET)
            .setType(MenuAction.RUNELITE)
            .setDeprioritized(false)
            .setForceLeftClick(true);
    }

    private boolean hasMenuOption(String option)
    {
        MenuEntry[] menuEntries = client.getMenuEntries();

        if (menuEntries == null)
        {
            return false;
        }

        return Arrays.stream(menuEntries)
            .anyMatch(entry -> option.equals(entry.getOption()));
    }

    private boolean isMouseOverWorldMapIndicator(Point mousePosition, Rectangle worldMapBounds)
    {
        Point indicatorPoint = getWorldMapIndicatorPoint(worldMapBounds);

        if (indicatorPoint == null)
        {
            return false;
        }

        int dx = mousePosition.getX() - indicatorPoint.getX();
        int dy = mousePosition.getY() - indicatorPoint.getY();

        return dx * dx + dy * dy <= WORLD_MAP_INDICATOR_CLICK_RADIUS * WORLD_MAP_INDICATOR_CLICK_RADIUS;
    }

    private Point getWorldMapIndicatorPoint(Rectangle worldMapBounds)
    {
        if (destination == null)
        {
            return null;
        }

        Point destinationPoint = worldMapOverlay.mapWorldPointToGraphicsPoint(destination);

        if (destinationPoint == null)
        {
            return null;
        }

        if (isInsideWorldMapDotLimit(destinationPoint, worldMapBounds))
        {
            return destinationPoint;
        }

        return getWorldMapEdgeArrowPoint(destinationPoint, worldMapBounds);
    }

    private boolean isInsideWorldMapDotLimit(Point point, Rectangle bounds)
    {
        return point.getX() >= bounds.x + WORLD_MAP_EDGE_PADDING
            && point.getX() <= bounds.x + bounds.width - WORLD_MAP_EDGE_PADDING
            && point.getY() >= bounds.y + WORLD_MAP_EDGE_PADDING
            && point.getY() <= bounds.y + bounds.height - WORLD_MAP_EDGE_PADDING;
    }

    private Point getWorldMapEdgeArrowPoint(Point destinationPoint, Rectangle bounds)
    {
        int centerX = bounds.x + bounds.width / 2;
        int centerY = bounds.y + bounds.height / 2;

        double dx = destinationPoint.getX() - centerX;
        double dy = destinationPoint.getY() - centerY;

        if (dx == 0 && dy == 0)
        {
            return destinationPoint;
        }

        int minX = bounds.x + WORLD_MAP_EDGE_PADDING;
        int maxX = bounds.x + bounds.width - WORLD_MAP_EDGE_PADDING;
        int minY = bounds.y + WORLD_MAP_EDGE_PADDING;
        int maxY = bounds.y + bounds.height - WORLD_MAP_EDGE_PADDING;

        double tx = dx > 0
            ? (maxX - centerX) / dx
            : dx < 0
                ? (minX - centerX) / dx
                : Double.POSITIVE_INFINITY;

        double ty = dy > 0
            ? (maxY - centerY) / dy
            : dy < 0
                ? (minY - centerY) / dy
                : Double.POSITIVE_INFINITY;

        double t = Math.min(Math.abs(tx), Math.abs(ty));

        return new Point(
            centerX + (int) Math.round(dx * t),
            centerY + (int) Math.round(dy * t)
        );
    }

    private WorldPoint calculateWorldMapPoint(Point point)
    {
        RenderOverview renderOverview = client.getRenderOverview();
        float zoom = renderOverview.getWorldMapZoom();

        WorldPoint mapCenter = new WorldPoint(
            renderOverview.getWorldMapPosition().getX(),
            renderOverview.getWorldMapPosition().getY(),
            0
        );

        Point mapCenterOnCanvas = worldMapOverlay.mapWorldPointToGraphicsPoint(mapCenter);

        if (mapCenterOnCanvas == null)
        {
            return mapCenter;
        }

        int dx = (int) ((point.getX() - mapCenterOnCanvas.getX()) / zoom);
        int dy = (int) ((-(point.getY() - mapCenterOnCanvas.getY())) / zoom);

        return mapCenter.dx(dx).dy(dy);
    }

    @Provides
    SimpleDestinationConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(SimpleDestinationConfig.class);
    }
}
