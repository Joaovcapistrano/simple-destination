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
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.worldmap.WorldMapOverlay;

public class SimpleDestinationWorldMapOverlay extends Overlay
{
    private static final int WORLD_MAP_EDGE_PADDING = 18;

    private final Client client;
    private final SimpleDestinationPlugin plugin;
    private final SimpleDestinationConfig config;
    private final WorldMapOverlay worldMapOverlay;

    @Inject
    public SimpleDestinationWorldMapOverlay(
        Client client,
        SimpleDestinationPlugin plugin,
        SimpleDestinationConfig config,
        WorldMapOverlay worldMapOverlay
    )
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.worldMapOverlay = worldMapOverlay;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        WorldPoint destination = plugin.getDestination();

        if (destination == null)
        {
            return null;
        }

        Widget worldMapView = client.getWidget(WidgetInfo.WORLD_MAP_VIEW);

        if (worldMapView == null)
        {
            return null;
        }

        Point destinationPoint = worldMapOverlay.mapWorldPointToGraphicsPoint(destination);

        if (destinationPoint == null)
        {
            return null;
        }

        Rectangle bounds = worldMapView.getBounds();

        if (isInsideWorldMapDotLimit(destinationPoint, bounds))
        {
            drawDestinationDot(graphics, destinationPoint);
            return null;
        }

        drawDirectionArrow(graphics, destinationPoint, bounds);
        return null;
    }

    private boolean isInsideWorldMapDotLimit(Point point, Rectangle bounds)
    {
        return point.getX() >= bounds.x + WORLD_MAP_EDGE_PADDING
            && point.getX() <= bounds.x + bounds.width - WORLD_MAP_EDGE_PADDING
            && point.getY() >= bounds.y + WORLD_MAP_EDGE_PADDING
            && point.getY() <= bounds.y + bounds.height - WORLD_MAP_EDGE_PADDING;
    }

    private void drawDestinationDot(Graphics2D graphics, Point point)
    {
        int x = point.getX();
        int y = point.getY();

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        graphics.setColor(new Color(0, 0, 0, 150));
        graphics.fillOval(x - 6, y - 6, 12, 12);

        graphics.setColor(config.indicatorColor());
        graphics.fillOval(x - 4, y - 4, 8, 8);

        graphics.setStroke(new BasicStroke(1.75f));
        graphics.setColor(config.borderColor());
        graphics.drawOval(x - 6, y - 6, 12, 12);
    }

    private void drawDirectionArrow(Graphics2D graphics, Point destinationPoint, Rectangle bounds)
    {
        int centerX = bounds.x + bounds.width / 2;
        int centerY = bounds.y + bounds.height / 2;

        double dx = destinationPoint.getX() - centerX;
        double dy = destinationPoint.getY() - centerY;

        if (dx == 0 && dy == 0)
        {
            return;
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

        int arrowX = centerX + (int) Math.round(dx * t);
        int arrowY = centerY + (int) Math.round(dy * t);

        double arrowAngle = Math.atan2(dy, dx) + Math.PI / 2.0;

        drawArrow(graphics, arrowX, arrowY, arrowAngle);
    }

    private void drawArrow(Graphics2D graphics, int x, int y, double angle)
    {
        Polygon arrow = new Polygon();
        arrow.addPoint(0, -10);
        arrow.addPoint(-8, 8);
        arrow.addPoint(0, 4);
        arrow.addPoint(8, 8);

        AffineTransform oldTransform = graphics.getTransform();

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.translate(x, y);
        graphics.rotate(angle);

        graphics.setColor(new Color(0, 0, 0, 150));
        graphics.fillPolygon(arrow);

        graphics.translate(0, -1);
        graphics.setColor(config.indicatorColor());
        graphics.fillPolygon(arrow);

        graphics.setStroke(new BasicStroke(1.75f));
        graphics.setColor(config.borderColor());
        graphics.drawPolygon(arrow);

        graphics.setTransform(oldTransform);
    }
}
