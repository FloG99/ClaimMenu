package de.floskater99.mapmenu.gui;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.map.MapFont;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapView;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Canvas {
    public Color backgroundColor = new Color(50, 50, 50);
    public final byte[] buffer = new byte[16384];
    public byte[] base = new byte[16384];
    public ImmutablePair<Integer, Integer> baseCoord; // World coordinate of the top left base pixel
    public boolean needsRerender = false; // Flag to be set by the user
    public Map<String, MapDecoration> mapDecorations = new LinkedHashMap<>();

    public static Coordinate[] getAllRectPoints(int x, int y, int width, int height) {
        Coordinate[] points = new Coordinate[width * height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                points[j * width + i] = new Coordinate(x + i, j + y);
            }
        }
        return points;
    }

    public void renderBase() {
        for (int x = 0; x < 128; x++) {
            for (int y = 0; y < 128; y++) {
                setPixel(x, y, this.base[y * 128 + x]);
            }
        }
    }

    public static LinkedHashSet<Coordinate> getEllipsePoints(Coordinate center, int radiusX, int radiusY) {
        LinkedHashSet<Coordinate> points = new LinkedHashSet<>();
        for (double rotation = 0; rotation < 2 * Math.PI; rotation += Math.PI / 200) {
            double denominator = Math.sqrt(radiusY * radiusY + radiusX * radiusX * Math.pow(Math.tan(rotation), 2));
            int x = (int) ((radiusX * radiusY) / denominator);
            int y = (int) ((radiusX * radiusY * Math.tan(rotation)) / denominator);
            if (rotation < 0.5 * Math.PI || rotation > 1.5 * Math.PI)
                points.add(Coordinate.from(x + center.x, y + center.y));
            else if (rotation > 0.5 * Math.PI && rotation < 1.5 * Math.PI)
                points.add(Coordinate.from(-x + center.x, -y + center.y));
        }
        return points;
    }

    private static Set<Coordinate> getLinePoints(Coordinate p1, Coordinate p2) {
        Set<Coordinate> set = new HashSet<>();
        int x1 = p1.x, y1 = p1.y, x2 = p2.x, y2 = p2.y;

        int dx = Math.abs(x2 - x1), sx = x1 < x2 ? 1 : -1;
        int dy = Math.abs(y2 - y1), sy = y1 < y2 ? 1 : -1;
        int err = (dx > dy ? dx : -dy) / 2, e2;
        for (; ; ) {
            set.add(new Coordinate(x1, y1));
            if (x1 == x2 && y1 == y2) break;
            e2 = err;
            if (e2 > -dx) {
                err -= dy;
                x1 += sx;
            }
            if (e2 < dy) {
                err += dx;
                y1 += sy;
            }
        }
        return set;
    }

    private static Set<Coordinate> getPolygonPoints(Coordinate... vertices) {
        if (vertices.length < 2) return null;

        Set<Coordinate> set = new HashSet<>();

        Coordinate last = vertices[vertices.length - 1];
        for (Coordinate next : vertices) {
            set.addAll(getLinePoints(last, next));
            last = next;
        }
        return set;
    }
    
    public void drawRect(int x, int y, int width, int height, Color color, Integer borderWidth, Color borderColor, int borderRadius, BufferedImage backgroundImage) {
        this.drawRect(x, y, width, height, Integer.MIN_VALUE, Integer.MAX_VALUE, color, borderWidth, borderColor, borderRadius, backgroundImage);
    }
    public void drawRect(int x, int y, int width, int height, int minY, int maxY, Color color, Integer borderWidth, Color borderColor, int borderRadius, BufferedImage backgroundImage) {
        if (borderRadius > 0 && borderRadius >= Math.min(width, height) / 2) {
            borderRadius = Math.min(width, height) / 2;
        }

        for (int j = 0; j < height; j++) {
            if (y + j > maxY || y + j < minY) {
                continue;
            }

            for (int i = 0; i < width; i++) {
                boolean isBorder = borderWidth > 0 && (i < borderWidth || i >= width - borderWidth || j < borderWidth || j >= height - borderWidth);
                if (borderRadius > 0 && borderColor != null) {
                    int xDist = 0, yDist = 0;
                    if (i <= borderRadius) xDist = borderRadius - i;
                    else if (i >= width - borderRadius) xDist = width - i - borderRadius - 1;
                    if (j <= borderRadius) yDist = borderRadius - j;
                    else if (j >= height - borderRadius) yDist = height - borderRadius - j - 1;

                    if (xDist != 0 && yDist != 0 && xDist * xDist + yDist * yDist > borderRadius * borderRadius)
                        continue;  //Don't draw outside the rounded corner

                    if (xDist * xDist + yDist * yDist > (borderRadius - borderWidth) * (borderRadius - borderWidth)) {
                        setPixel(x + i, y + j, borderColor);  //Corner
                        continue;
                    }
                }
                if (isBorder) {
                    setPixel(i + x, j + y, borderColor);
                    continue;
                }

                Color pixelColor = null;
                if (backgroundImage != null) {
                    int pixel = backgroundImage.getRGB(i - borderWidth, j - borderWidth);
                    pixelColor = new Color(pixel, true);
                } else if (color != null) {
                    pixelColor = color;
                }

                setPixel(i + x, j + y, pixelColor);
            }
        }
    }

    private Color interpolateColors(Color color2, Color color1, float opacity) {
        double inverse_percent = 1.0 - opacity;
        int redPart = (int) (color1.getRed() * opacity + color2.getRed() * inverse_percent);
        int greenPart = (int) (color1.getGreen() * opacity + color2.getGreen() * inverse_percent);
        int bluePart = (int) (color1.getBlue() * opacity + color2.getBlue() * inverse_percent);
        return new Color(redPart, greenPart, bluePart);
    }

    public void drawLine(Coordinate startPoint, Coordinate endPoint, Byte color) {
        for (Coordinate point : getLinePoints(startPoint, endPoint)) {
            this.setPixel(point.x, point.y, color);
        }
    }

    public void drawEllipse(Coordinate center, int radiusX, int radiusY, Byte color) {
        for (Coordinate point : getEllipsePoints(center, radiusX, radiusY)) {
            this.setPixel(point.x, point.y, color);
        }
    }

    public void drawPolygon(Byte color, Coordinate... vertices) {
        for (Coordinate point : Objects.requireNonNull(getPolygonPoints(vertices))) {
            this.setPixel(point.x, point.y, color);
        }
    }

    public void shiftBasePixelColor(int x, int y, int redOffset, int greenOffset, int blueOffset) {
        byte pixel = this.getBasePixel(x, y);

        Color pixelColor = MapPalette.getColor(pixel);
        byte overlayColor = MapPalette.matchColor(
            Math.max(0, Math.min(255, pixelColor.getRed() + redOffset)),
            Math.max(0, Math.min(255, pixelColor.getGreen() + greenOffset)),
            Math.max(0, Math.min(255, pixelColor.getBlue() + blueOffset))
        );
        this.setPixel(x, y, overlayColor);
    }

    public void setPixel(int x, int y, Byte color) {
        if (color == null) return;

        if (x >= 0 && y >= 0 && x < 128 && y < 128) {
            if (this.buffer[y * 128 + x] != color) {
                this.buffer[y * 128 + x] = color;
            }
        }
    }

    public void setPixel(int x, int y, Color color) {
        if (color == null) return;

        int opacity = color.getAlpha();

        if (opacity == 255) {
            setPixel(x, y, MapPalette.matchColor(color));
        } else if (opacity > 0) {
            color = interpolateColors(MapPalette.getColor(getPixel(x, y)), color, opacity / 255f);
            setPixel(x, y, MapPalette.matchColor(color));
        }
    }

    public byte getPixel(int x, int y) {
        return x >= 0 && y >= 0 && x < 128 && y < 128 ? this.buffer[y * 128 + x] : 0;
    }

    public byte getBasePixel(int x, int y) {
        return x >= 0 && y >= 0 && x < 128 && y < 128 ? this.base[y * 128 + x] : 0;
    }


    protected void setBase(byte[] base) {
        this.base = base;
    }

    public void drawImage(int x, int y, Image image) {
        byte[] bytes = MapPalette.imageToBytes(image);

        for (int x2 = 0; x2 < image.getWidth(null); x2++) {
            for (int y2 = 0; y2 < image.getHeight(null); y2++) {
                this.setPixel(x + x2, y + y2, bytes[y2 * image.getWidth(null) + x2]);
            }
        }
    }
    
    public void drawText(int x, int y, MapFont font, String text) {
        this.drawText(x, y, font, text, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public void drawText(int x, int y, MapFont font, String text, int minY, int maxY) {
        int xStart = x;
        byte color = 44;
        boolean underline = false;

        if (!font.isValid(text)) {
            throw new IllegalArgumentException("text contains invalid characters");
        } else {
            int i = 0;

            while (true) {
                if (i >= text.length()) {
                    return;
                }

                char ch = text.charAt(i);
                if (ch == '\n') {
                    x = xStart;
                    y += font.getHeight() + 1;
                } else if (ch == '§') {
                    int j = text.indexOf(';', i);
                    if (j < 0) {
                        break;
                    }

                    try {
                        String substring = text.substring(i + 1, j);
                        if (substring.equals("n")) {
                            underline = true;
                        } else {
                            color = Byte.parseByte(substring);
                        }
                        i = j;
                    } catch (NumberFormatException var12) {
                        break;
                    }
                } else {
                    MapFont.CharacterSprite sprite = font.getChar(text.charAt(i));

                    for (int r = 0; r < font.getHeight(); r++) {
                        if (y + r > maxY || y + r < minY) {
                            continue;
                        }

                        for (int c = 0; c < sprite.getWidth(); c++) {
                            if (sprite.get(r, c)) {
                                this.setPixel(x + c, y + r, color);
                            }
                        }
                    }

                    if (underline) {
                        for (int c = 0; c < sprite.getWidth() + 1; c++) {
                            if (!sprite.get(font.getHeight() - 1, c)) {
                                this.setPixel(x + c, y + font.getHeight() - 1, (byte) -49 /* black */);
                            }
                        }
                    }

                    x += sprite.getWidth() + 1;
                }

                ++i;
            }

            throw new IllegalArgumentException("Text contains unterminated color string");
        }
    }

    public void refreshBase(Player player) {
        MapView mapView = Bukkit.getMap(0);
        CraftPlayer craftPlayer = (CraftPlayer) player;
        ((CraftEntity) player).getHandle();

        CraftWorld craftWorld = (CraftWorld) craftPlayer.getWorld();
        ServerLevel nmsLevel = craftWorld.getHandle();
        MapItemSavedData worldMap = MapItemSavedData.createFresh(player.getLocation().getX(), player.getLocation().getZ(), mapView.getScale().getValue(), false, false, nmsLevel.dimension());
        fillBase(nmsLevel, ((CraftPlayer) player).getHandle(), worldMap);
        this.base = worldMap.colors;
        this.baseCoord = new ImmutablePair<>(player.getLocation().getBlockX() - 64 * (worldMap.scale + 1), player.getLocation().getBlockZ() - 64 * (worldMap.scale + 1));
    }

    private void fillBase(Level world, Entity entity, MapItemSavedData worldMap) {
        int scale = 1 << worldMap.scale; // 1 for closest, 16 for furthest
        int mapX = entity.getBlockX();
        int mapZ = entity.getBlockZ();
        int playerTileX = 64; // In the center
        int playerTileZ = 64; // In the center
        int tileRadius = 128 / scale; // A "tile" is always 8x8 chunks. The tileRadius specifies how many pixels cover such an 8x8 chunk area. The higher the scale, the less size those 8x8 chunks will have on the map.
        if (world.dimensionType().hasCeiling()) {
            tileRadius /= 2;
        }

        for (int tileX = playerTileX - tileRadius + 1; tileX < playerTileX + tileRadius; tileX++) {
            double playerToTileDistance = 0.0D;

            for (int tileZ = playerTileZ - tileRadius - 1; tileZ < playerTileZ + tileRadius; tileZ++) {
                if (tileX >= 0 && tileZ >= -1 && tileX < 128 && tileZ < 128) {
                    int blockX = (mapX / scale + tileX - 64) * scale;
                    int blockZ = (mapZ / scale + tileZ - 64) * scale;
                    Multiset<MapColor> colorMap = LinkedHashMultiset.create();
                    LevelChunk chunk = world.getChunkAt(new BlockPos(blockX, 0, blockZ));
                    if (!chunk.isEmpty()) {
                        ChunkPos chunkPos = chunk.getPos();
                        int localX = blockX & 15;
                        int localZ = blockZ & 15;
                        int blockCount = 0;
                        double blockHeightSum = 0.0D;
                        if (world.dimensionType().hasCeiling()) {
                            int noiseValue = blockX + blockZ * 231871;
                            noiseValue = noiseValue * noiseValue * 31287121 + noiseValue * 11;
                            if ((noiseValue >> 20 & 1) == 0) {
                                colorMap.add(Blocks.DIRT.defaultBlockState().getMapColor(world, BlockPos.ZERO), 10);
                            } else {
                                colorMap.add(Blocks.STONE.defaultBlockState().getMapColor(world, BlockPos.ZERO), 100);
                            }

                            blockHeightSum = 100.0D;
                        } else {
                            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
                            BlockPos.MutableBlockPos mutablePos1 = new BlockPos.MutableBlockPos();

                            for (int localTileX = 0; localTileX < scale; ++localTileX) {
                                for (int localTileZ = 0; localTileZ < scale; ++localTileZ) {
                                    int blockY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, localTileX + localX, localTileZ + localZ) + 1;
                                    BlockState blockState;
                                    if (blockY <= world.getMinBuildHeight() + 1) {
                                        blockState = Blocks.BEDROCK.defaultBlockState();
                                    } else {
                                        do {
                                            --blockY;
                                            mutablePos.set(chunkPos.getMinBlockX() + localTileX + localX, blockY, chunkPos.getMinBlockZ() + localTileZ + localZ);
                                            blockState = chunk.getBlockState(mutablePos);
                                        } while (blockState.getMapColor(world, mutablePos) == MapColor.NONE && blockY > world.getMinBuildHeight());

                                        if (blockY > world.getMinBuildHeight() && !blockState.getFluidState().isEmpty()) {
                                            int fluidHeight = blockY - 1;
                                            mutablePos1.set(mutablePos);

                                            BlockState fluidBlockState;
                                            do {
                                                mutablePos1.setY(fluidHeight--);
                                                fluidBlockState = chunk.getBlockState(mutablePos1);
                                                blockCount++;
                                            } while (fluidHeight > world.getMinBuildHeight() && !fluidBlockState.getFluidState().isEmpty());

                                            blockState = this.getCorrectStateForFluidBlock(world, blockState, mutablePos);
                                        }
                                    }

                                    worldMap.checkBanners(world, chunkPos.getMinBlockX() + localTileX + localX, chunkPos.getMinBlockZ() + localTileZ + localZ);
                                    blockHeightSum += (double) blockY / (double) (scale * scale);
                                    colorMap.add(blockState.getMapColor(world, mutablePos));
                                }
                            }
                        }

                        blockCount /= scale * scale;
                        MapColor dominantColor = Iterables.getFirst(Multisets.copyHighestCountFirst(colorMap), MapColor.NONE);
                        double colorIntensity;
                        MapColor.Brightness brightness;
                        if (dominantColor == MapColor.WATER) {
                            colorIntensity = (double) blockCount * 0.1D + (double) (tileX + tileZ & 1) * 0.2D;
                            if (colorIntensity < 0.5D) {
                                brightness = MapColor.Brightness.HIGH;
                            } else if (colorIntensity > 0.9D) {
                                brightness = MapColor.Brightness.LOW;
                            } else {
                                brightness = MapColor.Brightness.NORMAL;
                            }
                        } else {
                            colorIntensity = (blockHeightSum - playerToTileDistance) * 4.0D / (double) (scale + 4) + ((double) (tileX + tileZ & 1) - 0.5D) * 0.4D;
                            if (colorIntensity > 0.6D) {
                                brightness = MapColor.Brightness.HIGH;
                            } else if (colorIntensity < -0.6D) {
                                brightness = MapColor.Brightness.LOW;
                            } else {
                                brightness = MapColor.Brightness.NORMAL;
                            }
                        }

                        playerToTileDistance = blockHeightSum;
                        if (tileZ >= 0) {
                            worldMap.updateColor(tileX, tileZ, dominantColor.getPackedId(brightness));
                        }
                    }
                }
            }
        }
    }

    private BlockState getCorrectStateForFluidBlock(Level world, BlockState iblockdata, BlockPos blockposition) {
        FluidState fluid = iblockdata.getFluidState();
        return !fluid.isEmpty() && !iblockdata.isFaceSturdy(world, blockposition, Direction.UP) ? fluid.createLegacyBlock() : iblockdata;
    }
}
