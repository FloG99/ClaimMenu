package de.floskater99.mapmenu.gui;

public enum TextAlignment {
    TOP_LEFT(0b00100001),
    TOP_CENTER(0b00010001),
    TOP_RIGHT(0b00001001),
    CENTER_LEFT(0b00100010),
    CENTER_CENTER(0b00010010),
    CENTER_RIGHT(0b00001010),
    BOTTOM_LEFT(0b00100100),
    BOTTOM_CENTER(0b00010100),
    BOTTOM_RIGHT(0b00001100);

    private final int numVal;

    TextAlignment(int numVal) {
        this.numVal = numVal;
    }

    public int val() {
        return numVal;
    }
}
