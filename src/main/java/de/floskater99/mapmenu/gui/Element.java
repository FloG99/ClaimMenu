package de.floskater99.mapmenu.gui;

import de.floskater99.mapmenu.fontfix.MinecraftFontCopy;
import org.bukkit.Bukkit;
import org.bukkit.map.MinecraftFont;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import static de.floskater99.mapmenu.gui.ElementProperty.*;

public class Element {
    public Frame frame;
    protected Map<ElementProperty<?>, Object> defaultProperties = new HashMap<>();
    public Map<ElementProperty<?>, Object> hoverProperties = new HashMap<>();
    public Map<ElementProperty<?>, Object> styleProperties = new HashMap<>();
    boolean isHovered;
    Runnable onHoverStartFunc;
    Runnable onHoverStopFunc;
    Object onClickFunc;

    protected Element(Map<ElementProperty<?>, Object> properties) {
        for (Field field : ElementProperty.class.getDeclaredFields()) {
            try {
                setDefaultProperty((ElementProperty) field.get(null), properties.getOrDefault(field.get(null), null));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public void onClick(Runnable onClickFunc) {
        this.onClickFunc = onClickFunc;
    }

    public void onClick(BiConsumer<Integer, Integer> onClickFunc) {
        this.onClickFunc = onClickFunc;
    }

    public void onHoverStart(Runnable onHoverStartFunc) {
        this.onHoverStartFunc = onHoverStartFunc;
    }

    public void onHoverStop(Runnable onHoverStopFunc) {
        this.onHoverStopFunc = onHoverStopFunc;
    }

    protected void triggerOnClick(int x, int y) {
        if (this.onClickFunc != null) {
            if (this.onClickFunc instanceof Runnable) {
                ((Runnable) this.onClickFunc).run();
            } else if (this.onClickFunc instanceof BiConsumer<?, ?>) {
                ((BiConsumer<Integer, Integer>) this.onClickFunc).accept(x, y);
            }
        }
    }

    protected void triggerOnHoverStart() {
        if (isHovered) return;
        isHovered = true;

        if (this.onHoverStartFunc == null) return;
        this.onHoverStartFunc.run();
    }

    protected void triggerOnHoverStop() {
        if (!isHovered) return;
        isHovered = false;

        if (this.onHoverStopFunc == null) return;
        this.onHoverStopFunc.run();
    }

    public boolean isAt(int x, int y) {
        Coordinate topLeft = getActualCoordinates();

        return topLeft.x <= x && topLeft.x + this.get(WIDTH) >= x && topLeft.y <= y && topLeft.y + this.get(HEIGHT) >= y;
    }

    public Coordinate getActualCoordinates() {
        int elementX = this.get(X);
        int elementY = this.get(Y);

        Frame _frame = frame;
        while (_frame != null) {
            elementX += _frame.x;
            elementY += _frame.y - (this.get(FIXED) != null && this.get(FIXED) ? 0 : Math.round(_frame.scrollY));

            _frame = _frame.parentFrame;
        }

        return new Coordinate(elementX, elementY);
    }

    public <T> void setStyleProperty(ElementProperty<T> property, T value) {
        styleProperties.put(property, value);
    }

    public <T> void setHoverProperty(ElementProperty<T> property, T value) {
        hoverProperties.put(property, value);
    }

    public <T> void setDefaultProperty(ElementProperty<T> property, T value) {
        defaultProperties.put(property, value);
    }

    public <T> T get(ElementProperty<T> property) {
        T styleValue = (T) styleProperties.get(property);
        T hoverValue = (T) hoverProperties.get(property);
        T defaultValue = (T) defaultProperties.get(property);

        if (styleValue != null) {
            return styleValue;
        } else if (isHovered && hoverValue != null) {
            return hoverValue;
        } else {
            return defaultValue;
        }
    }

    public <T> T getOrDefault(ElementProperty<T> property, T def) {
        T value = get(property);
        if (value == null) {
            return def;
        }
        return value;
    }

    @Override
    public Element clone() {
        try {
            return (Element) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(defaultProperties, hoverProperties, styleProperties, isHovered);
    }

    public void draw(Canvas canvas) {
        if (get(HIDDEN) != null && get(HIDDEN)) {
            return;
        }

        Coordinate topLeft = getActualCoordinates();
        int x = topLeft.x;
        int y = topLeft.y;
        int width = get(WIDTH);
        int height = get(HEIGHT);
        String text = get(TEXT);
        Boolean textShadow = get(TEXT_SHADOW);
        Integer[] padding = getOrDefault(PADDING, new Integer[]{0, 0, 0, 0}); // Top, Right, Bottom, Left
        TextAlignment alignment = getOrDefault(TEXT_ALIGNMENT, TextAlignment.CENTER_CENTER);

        if (frame != null) {
            canvas.drawRect(
                x, y, width, height,
                frame.y, frame.y + frame.height,
                get(BACKGROUND_COLOR),
                getOrDefault(BORDER_SIZE, 0),
                get(BORDER_COLOR),
                getOrDefault(BORDER_RADIUS, 0),
                get(BACKGROUND_IMAGE));
        } else {
            canvas.drawRect(
                x, y, width, height,
                get(BACKGROUND_COLOR),
                getOrDefault(BORDER_SIZE, 0),
                get(BORDER_COLOR),
                getOrDefault(BORDER_RADIUS, 0),
                get(BACKGROUND_IMAGE));
        }

        if (text != null && !text.isEmpty()) {
            int heightWithoutBorder = height - 2 * getOrDefault(BORDER_SIZE, 0);
            int widthWithoutBorder = width - 2 * getOrDefault(BORDER_SIZE, 0);
            int horizontalAlignment = (alignment.val() & 0b00100000) > 0 ? 0 : ((alignment.val() & 0b00001000) > 0 ? 2 : 1); // 0 = Left, 1 = Center, 2 = Right
            int verticalAlignment = (alignment.val() & 0b00000001) > 0 ? 0 : ((alignment.val() & 0b00000100) > 0 ? 2 : 1);  // 0 = Top, 1 = Center, 2 = Bottom
            int lineSpacing = 2;

            ArrayList<String> lines = splitText(text, widthWithoutBorder - padding[1] - padding[3]);

            int totalTextHeight = (MinecraftFont.Font.getHeight() * lines.size() + lineSpacing * (lines.size() - 1));
            int distanceFromTop = verticalAlignment * (int) Math.ceil((heightWithoutBorder - totalTextHeight) / 2f);
            distanceFromTop = Math.max(distanceFromTop, padding[0]); // Padding Top
            distanceFromTop = Math.min(distanceFromTop, 2 * (int) Math.ceil((heightWithoutBorder - totalTextHeight) / 2f) - padding[2]); // Padding Bottom
            distanceFromTop += getOrDefault(BORDER_SIZE, 0);

            int lineIndex = 0;
            for (String line : lines) {
                int textWidth = MinecraftFontCopy.Font.getWidth(line);
                int distanceFromLeft = horizontalAlignment * (int) Math.ceil((widthWithoutBorder - textWidth) / 2);
                distanceFromLeft = Math.max(distanceFromLeft, padding[3]); // Padding left
                distanceFromLeft = Math.min(distanceFromLeft, 2 * (int) Math.ceil((widthWithoutBorder - textWidth) / 2) - padding[1]); // Padding right
                distanceFromLeft += getOrDefault(BORDER_SIZE, 0);
                int verticalOffset = lineIndex * (MinecraftFont.Font.getHeight() + lineSpacing);

                if (frame == null) {
                    if (textShadow) {
                        canvas.drawText(x + distanceFromLeft + 1, y + distanceFromTop + 1 + verticalOffset, MinecraftFont.Font, "§44;" + line.replaceAll("§-?..?;", ""));
                    }
                    canvas.drawText(x + distanceFromLeft, y + distanceFromTop + verticalOffset, MinecraftFont.Font, getColoredText(line));
                } else { // Limit the text to be inside the frame. No overflow allowed.
                    if (textShadow) {
                        canvas.drawText(x + distanceFromLeft + 1, y + distanceFromTop + 1 + verticalOffset, MinecraftFont.Font, "§44;" + line.replaceAll("§-?..?;", ""), frame.y, frame.y + frame.height);
                    }
                    canvas.drawText(x + distanceFromLeft, y + distanceFromTop + verticalOffset, MinecraftFont.Font, getColoredText(line), frame.y, frame.y + frame.height);
                }

                lineIndex++;
            }
        }
    }

    private ArrayList<String> splitText(String text, int maxWidth) {
        ArrayList<String> lines = new ArrayList<>();
        String[] words = text.split("\\s+"); // Split the text into words

        StringBuilder currentLine = new StringBuilder();
        int currentWidth = 0;
        final int spaceWidth = MinecraftFont.Font.getWidth(" ");

        for (String word : words) {
            int wordWidth = MinecraftFont.Font.getWidth(word);

            if (currentWidth + wordWidth + (currentLine.isEmpty() ? 0 : spaceWidth) <= maxWidth) {
                if (!currentLine.isEmpty()) {
                    currentLine.append(" "); // Add space if not the first word in the line
                    currentWidth += 8; // Simulated space width
                }
                currentLine.append(word);
                currentWidth += wordWidth;
            } else {
                lines.add(currentLine.toString()); // Line is wider than max, add it to the list
                currentLine = new StringBuilder(word); // Start a new line with the current word
                currentWidth = wordWidth;
            }
        }

        // Add the last line
        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    private String getColoredText(String text) {
        Byte textColor = get(TEXT_COLOR);

        if (textColor != 0) {
            return "§" + textColor + ";" + text;
        }
        return text;
    }
}
