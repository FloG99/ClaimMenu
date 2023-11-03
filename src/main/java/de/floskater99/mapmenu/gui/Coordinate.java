package de.floskater99.mapmenu.gui;

import java.util.Objects;

public class Coordinate {
    public int x;
    public int y;

    public Coordinate(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public static Coordinate from(int x, int y) {
        return new Coordinate(x, y);
    }

    public Coordinate addX(int x) {
        this.x += x;
        return this;
    }

    public Coordinate addY(int y) {
        this.y += y;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Coordinate that = (Coordinate) o;
        return x == that.x && y == that.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "Coordinate[x=" + x + ",y=" + y + "]";
    }
}
