package eu.thedarken.myo.twothousandfortyeight.game;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import eu.thedarken.myo.twothousandfortyeight.BuildConfig;

public class Game {
    private static final int startingMaxValue = 2048;
    private int endingMaxValue;
    private GameGrid mGameGrid = null;
    static final int DEFAULT_HEIGHT_X = 4;
    static final int DEFAULT_WIDTH_Y = 4;
    static final int DEFAULT_TILE_TYPES = 24;
    private static final int DEFAULT_STARTING_TILES = 2;
    private int mPositionsX = DEFAULT_HEIGHT_X;
    private int mPositionsY = DEFAULT_WIDTH_Y;
    private int mTileTypes = DEFAULT_TILE_TYPES;
    private int mStartingTiles = DEFAULT_STARTING_TILES;
    private boolean mCanUndo;
    private State mLastGameState;
    private State mBufferGameState;
    private State mGameState = State.NORMAL;
    private Context mContext;
    private GameView mView;
    private ScoreListener mScoreListener;
    private long mScore = 0;
    private long mLastScore = 0;
    private long mBufferScore = 0;
    private GameStateListener mGameStateListener;

    public enum State {
        NORMAL, WON, LOST, ENDLESS, ENLESS_WON
    }

    public Game(Context context) {
        mContext = context;
    }


    public interface ScoreListener {
        public void onNewScore(long score);
    }

    public interface GameStateListener {
        public void onGameStateChanged(State state);
    }

    public void setGameStateListener(GameStateListener listener) {
        this.mGameStateListener = listener;
    }

    public GameGrid getGameGrid() {
        return mGameGrid;
    }

    public boolean isCanUndo() {
        return mCanUndo;
    }

    public void setCanUndo(boolean canUndo) {
        mCanUndo = canUndo;
    }

    public State getLastGameState() {
        return mLastGameState;
    }

    public void setLastGameState(State lastGameState) {
        mLastGameState = lastGameState;
    }

    public State getGameState() {
        return mGameState;
    }

    public long getScore() {
        return mScore;
    }

    public void setScore(long score) {
        mScore = score;
    }

    public long getLastScore() {
        return mLastScore;
    }

    public void setLastScore(long lastScore) {
        mLastScore = lastScore;
    }

    public void setScoreListener(ScoreListener listener) {
        mScoreListener = listener;
    }

    boolean isGameWon() {
        return mGameState == State.WON || mGameState == State.ENLESS_WON;
    }

    public boolean isGameOnGoing() {
        return mGameState != State.WON && mGameState != State.LOST && mGameState != State.ENLESS_WON;
    }

    public boolean isEndlessMode() {
        return mGameState == State.ENDLESS || mGameState == State.ENLESS_WON;
    }

    public void setup(GameView view) {
        mView = view;
    }

    private void updateScore(long score) {
        mScore = score;
        if (mScoreListener != null)
            mScoreListener.onNewScore(mScore);
    }

    public void newGame() {
        if (mGameGrid == null) {
            mGameGrid = new GameGrid(mPositionsX, mPositionsY);
        } else {
            prepareUndoState();
            saveUndoState();
            mGameGrid.clearGrid();
        }

        if (BuildConfig.DEBUG) {
            int value = 2;
            for (Position pos : mGameGrid.getAvailableCells().subList(0, mGameGrid.getAvailableCells().size())) {
                spawnTile(new Tile(pos, value));
                value *= 2;
            }
        }
        endingMaxValue = (int) Math.pow(2, mTileTypes - 1);
        mView.updateGrid(mGameGrid);

        updateScore(0);
        updateGameState(State.NORMAL);
        mView.setGameState(mGameState);
        addStartTiles();
        mView.setRefreshLastTime(true);
        mView.resyncTime();
        mView.invalidate();
    }

    private void addStartTiles() {
        for (int xx = 0; xx < mStartingTiles; xx++) {
            addRandomTile();
        }
    }

    private void addRandomTile() {
        if (mGameGrid.isCellsAvailable()) {
            int value = Math.random() < 0.9 ? 2 : 4;
            Tile tile = new Tile(mGameGrid.randomAvailableCell(), value);
            spawnTile(tile);
        }
    }

    private void spawnTile(Tile tile) {
        mGameGrid.insertTile(tile);
        mView.spawnTile(tile);
    }

    private void prepareTiles() {
        for (Tile[] array : mGameGrid.getGrid()) {
            for (Tile tile : array) {
                if (mGameGrid.isCellOccupied(tile)) {
                    tile.setMergedFrom(null);
                }
            }
        }
    }

    private void moveTile(Tile tile, Position cell) {
        mGameGrid.getGrid()[tile.getX()][tile.getY()] = null;
        mGameGrid.getGrid()[cell.getX()][cell.getY()] = tile;
        tile.updatePosition(cell);
    }

    private void saveUndoState() {
        mGameGrid.saveTiles();
        mCanUndo = true;
        mLastScore = mBufferScore;
        mLastGameState = mBufferGameState;
    }

    private void prepareUndoState() {
        mGameGrid.prepareSaveTiles();
        mBufferScore = mScore;
        mBufferGameState = mGameState;
    }

    public void revertUndoState() {
        if (mCanUndo) {
            mCanUndo = false;
            mView.cancelAnimations();
            mGameGrid.revertTiles();
            updateScore(mLastScore);
            updateGameState(mLastGameState);
            mView.setGameState(mGameState);
            mView.setRefreshLastTime(true);
            mView.invalidate();
        }
    }

    public void updateUI() {
        updateScore(mScore);
        mView.setGameState(mGameState);
        mView.setRefreshLastTime(true);
        mView.invalidate();
    }

    public static final int DIRECTION_UP = 0;
    public static final int DIRECTION_RIGHT = 1;
    public static final int DIRECTION_DOWN = 2;
    public static final int DIRECTION_LEFT = 3;

    public void move(int direction) {
        mView.cancelAnimations();
        if (!isGameOnGoing()) {
            return;
        }
        prepareUndoState();
        Position vector = Position.getVector(direction);
        List<Integer> traversalsX = buildTraversalsX(vector);
        List<Integer> traversalsY = buildTraversalsY(vector);
        boolean moved = false;

        prepareTiles();

        for (int xx : traversalsX) {
            for (int yy : traversalsY) {
                Position cell = new Position(xx, yy);
                Tile tile = mGameGrid.getTile(cell);

                if (tile != null) {
                    Position[] positions = findFarthestPosition(cell, vector);
                    Tile next = mGameGrid.getTile(positions[1]);

                    if (next != null && next.getValue() == tile.getValue() && next.getMergedFrom() == null) {
                        Tile merged = new Tile(positions[1], tile.getValue() * 2);
                        Tile[] temp = {tile, next};
                        merged.setMergedFrom(temp);

                        mGameGrid.insertTile(merged);
                        mGameGrid.removeTile(tile);

                        // Converge the two tiles' positions
                        tile.updatePosition(positions[1]);

                        int[] extras = {xx, yy};
                        //Direction: 0 = MOVING MERGED
                        mView.moveTile(merged.getX(), merged.getY(), extras);
                        mView.mergeTile(merged.getX(), merged.getY());

                        updateScore(mScore + merged.getValue());

                        // The mighty 2048 tile
                        if (merged.getValue() >= winValue() && !isGameWon()) {
                            if (mGameState == State.ENDLESS) {
                                updateGameState(State.ENLESS_WON);
                            } else if (mGameState == State.NORMAL) {
                                updateGameState(State.WON);
                            } else {
                                throw new RuntimeException("Can't move into win state");
                            }
                            mView.setGameState(mGameState);
                            endGame();
                        }
                    } else {
                        moveTile(tile, positions[0]);
                        int[] extras = {xx, yy, 0};
                        //Direction: 1 = MOVING NO MERGE
                        mView.moveTile(positions[0].getX(), positions[0].getY(), extras);
                    }

                    if (!Position.equal(cell, tile)) {
                        moved = true;
                    }
                }
            }
        }
        mView.updateGrid(mGameGrid);
        if (moved) {
            saveUndoState();
            addRandomTile();
            checkLose();
        }
        mView.resyncTime();
        mView.invalidate();
    }

    private Position[] findFarthestPosition(Position cell, Position vector) {
        Position previous;
        Position nextCell = new Position(cell.getX(), cell.getY());
        do {
            previous = nextCell;
            nextCell = new Position(previous.getX() + vector.getX(),
                    previous.getY() + vector.getY());
        } while (mGameGrid.isCellWithinBounds(nextCell) && mGameGrid.isCellAvailable(nextCell));
        Position[] answer = {previous, nextCell};
        return answer;
    }

    public void updateGameState(State state) {
        mGameState = state;
        if (mGameStateListener != null)
            mGameStateListener.onGameStateChanged(mGameState);
    }

    private void checkLose() {
        if (!isMovePossible() && !isGameWon()) {
            updateGameState(State.LOST);
            mView.setGameState(mGameState);
            endGame();
        }
    }

    private void endGame() {
        mView.endGame();
        updateScore(mScore);
    }

    private List<Integer> buildTraversalsX(Position vector) {
        List<Integer> traversals = new ArrayList<Integer>();
        for (int xx = 0; xx < mPositionsX; xx++) {
            traversals.add(xx);
        }
        if (vector.getX() == 1) {
            Collections.reverse(traversals);
        }
        return traversals;
    }

    private List<Integer> buildTraversalsY(Position vector) {
        List<Integer> traversals = new ArrayList<Integer>();
        for (int xx = 0; xx < mPositionsY; xx++) {
            traversals.add(xx);
        }
        if (vector.getY() == 1) {
            Collections.reverse(traversals);
        }
        return traversals;
    }

    private boolean isMovePossible() {
        return mGameGrid.isCellsAvailable() || tileMatchesAvailable();
    }

    private boolean tileMatchesAvailable() {
        Tile tile;
        for (int xx = 0; xx < mPositionsX; xx++) {
            for (int yy = 0; yy < mPositionsY; yy++) {
                tile = mGameGrid.getTile(new Position(xx, yy));
                if (tile != null) {
                    for (int direction = 0; direction < 4; direction++) {
                        Position vector = Position.getVector(direction);
                        Position cell = new Position(xx + vector.getX(), yy + vector.getY());
                        Tile other = mGameGrid.getTile(cell);
                        if (other != null && other.getValue() == tile.getValue()) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private int winValue() {
        if (isEndlessMode()) {
            return endingMaxValue;
        } else {
            return startingMaxValue;
        }
    }

    public void setEndlessMode() {
        updateGameState(State.ENDLESS);
        mView.setGameState(mGameState);
        mView.invalidate();
        mView.setRefreshLastTime(true);
    }

}
