package de.floskater99.mapmenu.gui;

import org.bukkit.map.MapPalette;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static de.floskater99.mapmenu.gui.ElementProperty.*;

public class ElementBuilder {
    private final Map<ElementProperty<?>, Object> properties = new HashMap<>();
    private Runnable onHoverStartFunc;
    private Runnable onHoverStopFunc;
    private Object onClickFunc;
    private boolean resizeImage;

    public ElementBuilder(int x, int y, int width, int height) {
        setProperty(X, x);
        setProperty(Y, y);
        setProperty(WIDTH, width);
        setProperty(HEIGHT, height);
    }
    
    public ElementBuilder onClick(Runnable onClickFunc) {
        this.onClickFunc = onClickFunc;
        return this;
    }
    public ElementBuilder onClick(BiConsumer<Integer, Integer> onClickFunc) {
        this.onClickFunc = onClickFunc;
        return this;
    }

    public ElementBuilder onHoverStart(Runnable onHoverStartFunc) {
        this.onHoverStartFunc = onHoverStartFunc;
        return this;
    }

    public ElementBuilder onHoverStop(Runnable onHoverStopFunc) {
        this.onHoverStopFunc = onHoverStopFunc;
        return this;
    }

    public ElementBuilder setBackgroundColor(Color backgroundColor) {
        setProperty(BACKGROUND_COLOR, backgroundColor);
        return this;
    }
    
    public ElementBuilder setFixed(boolean fixed) {
        setProperty(FIXED, fixed);
        return this;
    }

    public ElementBuilder setBorder(int borderSize, Color borderColor, boolean includeBorderInSize) {
        setProperty(BORDER_SIZE, borderSize);
        setProperty(BORDER_COLOR, borderColor);
        if (!includeBorderInSize) {
            setProperty(X, getProperty(X) - borderSize);
            setProperty(Y, getProperty(Y) - borderSize);
            setProperty(WIDTH, getProperty(WIDTH) + 2 * borderSize);
            setProperty(HEIGHT, getProperty(HEIGHT) + 2 * borderSize);
        }
        return this;
    }

    public ElementBuilder setBorderRadius(int borderRadius) {
        setProperty(BORDER_RADIUS, borderRadius);
        return this;
    }

    public ElementBuilder setBackgroundImage(BufferedImage image, boolean resizeImage) {
        setProperty(BACKGROUND_IMAGE, image);
        this.resizeImage = resizeImage;
        return this;
    }

    public ElementBuilder setText(String text, Byte textColor, TextAlignment textAlignment, Boolean textShadow) {
        setProperty(TEXT, text);
        setProperty(TEXT_ALIGNMENT, textAlignment);
        setProperty(TEXT_COLOR, textColor);
        setProperty(TEXT_SHADOW, textShadow);
        return this;
    }

    public ElementBuilder setText(String text, Byte textColor, TextAlignment textAlignment) {
        return setText(text, textColor, textAlignment, false);
    }
    
    public ElementBuilder setText(String text, Color textColor, TextAlignment textAlignment) {
        return setText(text, MapPalette.matchColor(textColor), textAlignment, false);
    }

    public ElementBuilder setHidden(boolean isHidden) {
        setProperty(HIDDEN, isHidden);
        return this;
    }

    public ElementBuilder setPadding(Integer paddingTop, Integer paddingRight, Integer paddingBottom, Integer paddingLeft) {
        setProperty(PADDING, new Integer[] {paddingTop, paddingRight, paddingBottom, paddingLeft});
        return this;
    }

    public Element build() {
        if (getProperty(BACKGROUND_IMAGE) != null && resizeImage) {
            setProperty(BACKGROUND_IMAGE, resizeImage(getProperty(BACKGROUND_IMAGE), getProperty(WIDTH) - getProperty(BORDER_SIZE) * 2, getProperty(HEIGHT) - getProperty(BORDER_SIZE) * 2));
        }
        
        Element element = new Element(properties);
        element.onClickFunc = onClickFunc;
        element.onHoverStartFunc = onHoverStartFunc;
        element.onHoverStopFunc = onHoverStopFunc;

        return element;
    }


    private BufferedImage resizeImage(BufferedImage src, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int col = src.getRGB(Math.min(src.getWidth() - 1, x * src.getWidth() / (w - 1)), y * src.getHeight() / h);
                img.setRGB(x, y, col);
            }
        }
        return img;
    }

    private <T> void setProperty(ElementProperty<T> property, T value) {
        properties.put(property, value);
    }

    private <T> T getProperty(ElementProperty<T> property) {
        return (T) properties.get(property);
    }
}
