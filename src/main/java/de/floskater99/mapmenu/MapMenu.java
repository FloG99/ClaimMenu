package de.floskater99.mapmenu;

import de.floskater99.mapmenu.gui.Canvas;
import de.floskater99.mapmenu.gui.Window;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.*;

public abstract class MapMenu {
    public int selectedSlotBefore;
    public ItemStack removedItem;
    public GameMode gameModeBefore;
    public byte cursorPositionX;
    public byte cursorPositionY;
    public ItemStack map;
    public MapCursor defaultCursor;
    public Player player;
    public Canvas canvas;
    public Window window;


    public MapMenu(Player player, ItemStack map) {
        this(player, map, player.getInventory().getItem(MapMenuAPI.defaultSlot));
    }

    public MapMenu(Player player, ItemStack map, ItemStack removedItem) {
        this.player = player;
        this.selectedSlotBefore = player.getInventory().getHeldItemSlot();
        this.removedItem = removedItem;
        this.gameModeBefore = player.getGameMode();
        this.cursorPositionX = (byte) 0;
        this.cursorPositionY = (byte) 0;
        this.defaultCursor = new MapCursor((byte) 0, (byte) 0, (byte) 8, MapCursor.Type.RED_POINTER, true);
        this.map = map;

        canvas = new Canvas();
        window = new Window(canvas);

        if (this.doRenderBase()) {
            canvas.refreshBase(player);
            canvas.renderBase();
        }
    }

    protected abstract void onLoad();

    public byte[] getCursorMapPosition() {
        byte x = (byte) ((this.cursorPositionX + 128) / 2);
        byte y = (byte) Math.max((this.cursorPositionY - 9 + 128) / 2, 0);
        return new byte[]{x, y};
    }

    public void addCursorX(int xChange) {
        this.cursorPositionX = (byte) Math.max(-128, Math.min(this.cursorPositionX + xChange, 127));
    }

    public void leftClick() {
        boolean stopEventPropagation = window.click(getCursorMapPosition()[0], getCursorMapPosition()[1]);
        if (!stopEventPropagation) {
            this.onLeftClick(getCursorMapPosition()[0], getCursorMapPosition()[1]);
        }
    }

    protected void onLeftClick(byte x, byte y) {
    }

    public void rightClick() {
        boolean stopEventPropagation = window.click(getCursorMapPosition()[0], getCursorMapPosition()[1]);
        if (!stopEventPropagation) {
            this.onRightClick(getCursorMapPosition()[0], getCursorMapPosition()[1]);
        }
    }

    protected void onRightClick(byte x, byte y) {
    }

    protected void onScroll(int direction) {
    }

    public void scroll(int direction) {
        this.onScroll(direction);
    }

    protected void mouseMove() {
        this.onMouseMove(getCursorMapPosition()[0], getCursorMapPosition()[1]);
    }

    protected abstract void onMouseMove(int x, int y);

    protected abstract void onRender();

    public void render() {
        // Clear previous drawings
        if (this.doRenderBase()) {
            canvas.renderBase();
        } else {
            canvas.drawRect(0, 0, 128, 128, canvas.backgroundColor, 0, null, 0, null);
        }

        this.onRender();
        window.draw();
    }

    public void liveUpdate(String key) {
    }

    public abstract boolean doRenderBase();

    public abstract String getMapDisplayName();

    public void close() {
        MapMenuAPI.closeMenu(player, true);
    }

}
