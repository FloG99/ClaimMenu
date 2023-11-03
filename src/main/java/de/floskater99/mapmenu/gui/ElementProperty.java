package de.floskater99.mapmenu.gui;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ElementProperty<T> {
    public static final ElementProperty<Integer> X = new ElementProperty<>() {};
    public static final ElementProperty<Integer> Y = new ElementProperty<>() {};
    public static final ElementProperty<Integer> WIDTH = new ElementProperty<>() {};
    public static final ElementProperty<Integer> HEIGHT = new ElementProperty<>() {};
    public static final ElementProperty<String> TEXT = new ElementProperty<>() {};
    public static final ElementProperty<Byte> TEXT_COLOR = new ElementProperty<>() {};
    public static final ElementProperty<Integer[]> PADDING = new ElementProperty<>() {};
    public static final ElementProperty<TextAlignment> TEXT_ALIGNMENT = new ElementProperty<>() {};
    public static final ElementProperty<Boolean> TEXT_SHADOW = new ElementProperty<>() {};
    public static final ElementProperty<Color> BACKGROUND_COLOR = new ElementProperty<>() {};
    public static final ElementProperty<BufferedImage> BACKGROUND_IMAGE = new ElementProperty<>() {};
    public static final ElementProperty<Color> BORDER_COLOR = new ElementProperty<>() {};
    public static final ElementProperty<Integer> BORDER_RADIUS = new ElementProperty<>() {};
    public static final ElementProperty<Integer> BORDER_SIZE = new ElementProperty<>() {};
    public static final ElementProperty<Boolean> HIDDEN = new ElementProperty<>() {};
    public static final ElementProperty<Boolean> FIXED = new ElementProperty<>() {};
}
