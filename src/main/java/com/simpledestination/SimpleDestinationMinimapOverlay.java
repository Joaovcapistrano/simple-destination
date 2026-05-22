package com.simpledestination;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

public class SimpleDestinationMinimapOverlay extends Overlay
{
    private static final int ARROW_EDGE_PADDING = 18;

    /*
     * Use a large projection distance and let the current minimap bounds decide
     * whether the marker should be a dot or an arrow. This avoids false arrows
     * around the World Map orb area, where the default localToMinimap distance
     * can reject a destination even though the projected point is still inside
     * the visible minimap area.
     */
    private static final int MINIMAP_PROJECTION_DISTANCE = 50_000;

    private final Client client;
    private final SimpleDestinationPlugin plugin;
    private final SimpleDestinationConfig config;

    @Inject
    public SimpleDestinationMinimapOverlay(
        Client client,
        SimpleDestinationPlugin plugin,
        SimpleDestinationConfig config
    )
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
        {
            return null;
        }

        WorldPoint destination = plugin.getDestination();

        if (destination == null)
        {
            return null;
        }

        if (drawDotWhenDestinationIsVisible(graphics, destination))
        {
            return null;
        }

        drawDirectionArrow(graphics, destination);
        return null;
    }

    private boolean drawDotWhenDestinationIsVisible(Graphics2D graphics, WorldPoint destination)
    {
        if (destination.getPlane() != client.getPlane())
        {
            return false;
        }

        LocalPoint destinationLocalPoint = LocalPoint.fromWorld(
            client,
            destination.getX(),
            destination.getY()
        );

        if (destinationLocalPoint == null)
        {
            return false;
        }

        /*
         * Do not compare this point against our own minimap widget bounds here.
         * Perspective.localToMinimap already uses RuneLite's active minimap draw
         * widget internally. Comparing it with a different widget rectangle was
         * the reason the arrow could remain active even when the destination was
         * close enough to be shown as a dot.
         *
         * The fixed edge padding is applied by reducing RuneLite's default
         * minimap projection distance instead of doing a second bounds check.
         */
        Point minimapPoint = Perspective.localToMinimap(
            client,
            destinationLocalPoint,
            getMinimapDotDistance()
        );

        if (minimapPoint == null)
        {
            return false;
        }

        drawDestinationDot(graphics, minimapPoint);
        return true;
    }

    private void drawDestinationDot(Graphics2D graphics, Point minimapPoint)
    {
        Color color = config.indicatorColor();

        int x = minimapPoint.getX();
        int y = minimapPoint.getY();

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        graphics.setColor(new Color(0, 0, 0, 150));
        graphics.fillOval(x - 5, y - 5, 10, 10);

        graphics.setColor(color);
        graphics.fillOval(x - 4, y - 4, 8, 8);

        graphics.setStroke(new BasicStroke(1.5f));
        graphics.setColor(config.borderColor());
        graphics.drawOval(x - 5, y - 5, 10, 10);
    }

    private void drawDirectionArrow(Graphics2D graphics, WorldPoint destination)
    {
        Rectangle bounds = getMinimapBounds();

        if (bounds == null)
        {
            return;
        }

        int centerX = bounds.x + bounds.width / 2;
        int centerY = bounds.y + bounds.height / 2;
        int radius = Math.max(8, Math.min(bounds.width, bounds.height) / 2 - ARROW_EDGE_PADDING);

        WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();

        int dx = destination.getX() - playerLocation.getX();
        int dy = destination.getY() - playerLocation.getY();

        if (dx == 0 && dy == 0)
        {
            return;
        }

        double mapAngle = client.getMapAngle() * Math.PI / 1024.0;
        double sin = Math.sin(mapAngle);
        double cos = Math.cos(mapAngle);

        double rotatedX = dx * cos + dy * sin;
        double rotatedY = dy * cos - dx * sin;

        double length = Math.hypot(rotatedX, rotatedY);

        int arrowX = centerX + (int) Math.round((rotatedX / length) * radius);
        int arrowY = centerY - (int) Math.round((rotatedY / length) * radius);

        double arrowAngle = Math.atan2(-rotatedY, rotatedX) + Math.PI / 2.0;

        drawArrow(graphics, arrowX, arrowY, arrowAngle);
    }

    private Rectangle getMinimapBounds()
    {
        Widget minimapWidget = client.getWidget(InterfaceID.Toplevel.MINIMAP);

        if (minimapWidget == null)
        {
            minimapWidget = client.getWidget(InterfaceID.ToplevelOsrsStretch.MINIMAP);
        }

        if (minimapWidget == null)
        {
            minimapWidget = client.getWidget(InterfaceID.ToplevelPreEoc.MINIMAP);
        }

        if (minimapWidget == null)
        {
            return null;
        }

        return minimapWidget.getBounds();
    }

    private int getMinimapDotDistance()
    {
        /*
         * This mirrors RuneLite's default localToMinimap distance and subtracts
         * the fixed arrow edge padding converted from pixels into local coords.
         * Keeping the value small also avoids integer overflow in RuneLite's
         * dx * dx + dy * dy distance check.
         */
        double minimapZoom = Math.max(1d, client.getMinimapZoom());

        int defaultDistance = (int) ((20 << Perspective.LOCAL_COORD_BITS) * (4d / minimapZoom));
        int paddingDistance = (int) Math.round(ARROW_EDGE_PADDING * Perspective.LOCAL_TILE_SIZE / minimapZoom);

        return Math.max(Perspective.LOCAL_TILE_SIZE, defaultDistance - paddingDistance);
    }

    private void drawArrow(Graphics2D graphics, int x, int y, double angle)
    {
        Polygon arrow = new Polygon();
        arrow.addPoint(0, -9);
        arrow.addPoint(-7, 7);
        arrow.addPoint(0, 3);
        arrow.addPoint(7, 7);

        AffineTransform oldTransform = graphics.getTransform();

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.translate(x, y);
        graphics.rotate(angle);

        graphics.setColor(new Color(0, 0, 0, 150));
        graphics.fillPolygon(arrow);

        graphics.translate(0, -1);
        graphics.setColor(config.indicatorColor());
        graphics.fillPolygon(arrow);

        graphics.setStroke(new BasicStroke(1.5f));
        graphics.setColor(config.borderColor());
        graphics.drawPolygon(arrow);

        graphics.setTransform(oldTransform);
    }
}
