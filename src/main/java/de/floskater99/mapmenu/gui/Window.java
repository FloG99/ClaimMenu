package de.floskater99.mapmenu.gui;

import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;

public class Window {
    public Canvas canvas;
    private final ArrayList<Element> objects = new ArrayList<>();
    private final ArrayList<Element> hoverObjects = new ArrayList<>();
    private final ArrayList<Element> clickObjects = new ArrayList<>();
    public final ArrayList<Frame> frames = new ArrayList<>();

    public Window(Canvas canvas) {
        this.canvas = canvas;
    }

    public void addElement(Element element) {
        objects.add(element);

        if (element.onHoverStartFunc != null || element.onHoverStopFunc != null || !element.hoverProperties.isEmpty()) {
            hoverObjects.add(element);
        }

        if (element.onClickFunc != null) {
            clickObjects.add(element);
        }
    }

    private void removeElement(Element element) {
        element.triggerOnHoverStop();
        objects.remove(element);
        hoverObjects.remove(element);
        clickObjects.remove(element);
    }

    public void showFrame(Frame frame) {
        if (frame == null) {
            return;
        }

        frames.add(frame);

        for (Element element : frame.objects) {
            addElement(element);
        }

        for (Frame subFrame : frame.subFrames) {
            for (Element element : subFrame.objects) {
                addElement(element);
            }
        }
    }

    public void hideFrame(Frame frame) {
        if (frame == null) {
            return;
        }

        for (Element element : frame.objects) {
            removeElement(element);
        }

        for (Frame subFrame : frame.subFrames) {
            for (Element element : subFrame.objects) {
                removeElement(element);
            }
        }

        frames.remove(frame);
    }

    public void hoverUpdate(int x, int y) {
        boolean stopEventPropagation = false;
        // In reverse, so the newest (on top) is checked first
        for (int i = hoverObjects.size() - 1; i >= 0; i--) {
            Element object = hoverObjects.get(i);

            if (object.isAt(x, y)) {
                if (!stopEventPropagation) {
                    object.triggerOnHoverStart();
                }
                stopEventPropagation = true;
            } else {
                object.triggerOnHoverStop();
            }
        }
    }

    public boolean click(int x, int y) {
        boolean stopEventPropagation = false;
        // In reverse, so the newest (on top) is checked first
        for (int i = clickObjects.size() - 1; i >= 0; i--) {
            Element object = clickObjects.get(i);

            if (object.isAt(x, y)) {
                object.triggerOnClick(x, y);
                stopEventPropagation = true;
                break;
            }
        }

        return stopEventPropagation;
    }

    public boolean isObjectAt(int x, int y) {
        return objects.stream().anyMatch(object -> object.isAt(x, y));
    }

    public void draw() {
        for (Element object : objects) {
            object.draw(canvas);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(frames, objects, Arrays.hashCode(canvas.buffer));
    }
}
