package de.floskater99.mapmenu.gui;

import de.floskater99.mapmenu.Main;
import org.bukkit.Bukkit;

import java.awt.*;
import java.util.ArrayList;
import java.util.Objects;

public class Frame {
    protected final ArrayList<Frame> subFrames = new ArrayList<>();
    protected final ArrayList<Element> objects = new ArrayList<>();
    protected Frame parentFrame;
    public int x;
    public int y;
    double scrollY = 0;
    int maxScrollY = 0;
    int targetScrollY;
    double pendingScrollY = 0;
    public int width;
    public int height;
    int scrollTime = 10; // Game ticks it takes for the scroll to finish
    int scrollRefreshRate = 1; // Every <scrollRefreshRate> game tick, the screen is refreshed while scrolling
    double scrollDistancePerRefresh;
    Integer scrollBukkitTaskId;
    Element scrollbarSlider;

    public Frame(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void addElement(Element element) {
        objects.add(element);
        element.frame = this;
        this.maxScrollY = Math.max(this.maxScrollY, element.get(ElementProperty.Y) + element.get(ElementProperty.HEIGHT) - this.height);

        if (this.maxScrollY > 0) {
            // Add scrollbar

            Element scrollbar = new ElementBuilder(this.width - 6, 0, 7, this.height)
                .setBackgroundColor(new Color(0, 0, 0))
                .setBorder(1, new Color(200, 200, 200), true)
                .setFixed(true)
                .build();

            scrollbar.onClick((x, y) -> {
                double relativePostion = relativePosition(scrollbar.getActualCoordinates().y, scrollbar.getActualCoordinates().y + this.height, y);
                this.scrollbarSlider.setDefaultProperty(ElementProperty.Y, interpolate(1, this.height - 1 - 7, relativePostion));
                this.scrollY = interpolate(0, this.maxScrollY, relativePostion);
            });

            scrollbarSlider = new ElementBuilder(this.width - 5, 1, 5, 7)
                .setBackgroundColor(new Color(255, 255, 255))
                .setFixed(true)
                .build();

            objects.add(scrollbar);
            objects.add(scrollbarSlider);

            scrollbar.frame = this;
            scrollbarSlider.frame = this;
        }
    }

    public void addSubFrame(Frame frame) {
        this.subFrames.add(frame);
        frame.parentFrame = this;
    }

    public void setScrollY(int y, boolean animated) {
        this.pendingScrollY = animated ? (y - this.scrollY) : 0;
        this.scrollY = y;
    }

    public void scroll(int yDiff) {
        if (this.maxScrollY == 0) {
            return;
        }

            this.targetScrollY += yDiff;
        if (targetScrollY < 0) {
            this.pendingScrollY = -this.scrollY;
            this.targetScrollY = 0;
        } else if (targetScrollY > this.maxScrollY) {
            this.pendingScrollY = (this.maxScrollY - this.scrollY);
            this.targetScrollY = this.maxScrollY;
        } else {
            this.pendingScrollY += yDiff;
        }

        this.scrollDistancePerRefresh = this.pendingScrollY / ((double) scrollTime / scrollRefreshRate);
        this.startScrollAnimation();
    }

    private void startScrollAnimation() {
        if (scrollBukkitTaskId != null) {
            Main.instance.getLogger().info("returned cuz running");
            return;
        }

        scrollBukkitTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(Main.instance, () -> {
            boolean scrollDir = scrollDistancePerRefresh > 0;
            this.scrollY += scrollDistancePerRefresh;

            if ((scrollDir && scrollY >= targetScrollY) || (!scrollDir && scrollY <= targetScrollY)) { // Reached destination
                Main.instance.getLogger().info("reached destination");
                this.pendingScrollY = 0;
                this.scrollY = targetScrollY;
                Bukkit.getScheduler().cancelTask(scrollBukkitTaskId);
                scrollBukkitTaskId = null;
            }

            this.scrollbarSlider.setDefaultProperty(ElementProperty.Y, interpolate(1, this.height - 1 - 7, this.scrollY / this.maxScrollY));
        }, 0, scrollRefreshRate);

        Main.instance.getLogger().info("started, " + scrollBukkitTaskId);
    }

    private static int interpolate(int a, int b, double x) {
        return a + (int) Math.round(x * (b - a));
    }

    private static double relativePosition(int a, int b, int c) {
        if (a == b) {
            return 0.0;
        }

        return (double) (c - a) / (b - a);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subFrames, x, y, width, height, scrollY);
    }
}
