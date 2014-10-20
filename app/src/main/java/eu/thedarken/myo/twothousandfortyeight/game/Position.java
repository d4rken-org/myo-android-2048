package eu.thedarken.myo.twothousandfortyeight.game;

public class Position {
    private int x;
    private int y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    void setX(int x) {
        this.x = x;
    }

    void setY(int y) {
        this.y = y;
    }

    public static boolean equal(Position first, Position second) {
        return first.getX() == second.getX() && first.getY() == second.getY();
    }

    public static Position getVector(int direction) {
        Position[] map = {
                new Position(0, -1), // up
                new Position(1, 0),  // right
                new Position(0, 1),  // down
                new Position(-1, 0)  // left
        };
        return map[direction];
    }
}
