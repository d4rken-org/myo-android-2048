package eu.thedarken.myo.twothousandfortyeight.game;

public class Tile extends Position {
    private final int value;
    private Tile[] mergedFrom = null;

    public Tile(int x, int y, int value) {
        super(x, y);
        this.value = value;
    }

    public Tile(Position cell, int value) {
        super(cell.getX(), cell.getY());
        this.value = value;
    }

    public void updatePosition(Position cell) {
        this.setX(cell.getX());
        this.setY(cell.getY());
    }

    public int getValue() {
        return this.value;
    }

    public Tile[] getMergedFrom() {
       return mergedFrom;
    }

    public void setMergedFrom(Tile[] tile) {
        mergedFrom = tile;
    }
}
