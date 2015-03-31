package eu.thedarken.myo.twothousandfortyeight.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;

import eu.thedarken.myo.twothousandfortyeight.R;

public class GameView extends View {
    private static final int BASE_ANIMATION_TIME = 100000000;
    private static final float MERGING_ACCELERATION = (float) -0.5;
    private static final float INITIAL_VELOCITY = (1 - MERGING_ACCELERATION) / 4;
    private static final int SPAWN_ANIMATION = -1;
    private static final int MOVE_ANIMATION = 0;
    private static final int MERGE_ANIMATION = 1;
    private static final int FADE_GLOBAL_ANIMATION = 0;
    private static final long MOVE_ANIMATION_TIME = BASE_ANIMATION_TIME;
    private static final long SPAWN_ANIMATION_TIME = BASE_ANIMATION_TIME;
    private static final long NOTIFICATION_ANIMATION_TIME = BASE_ANIMATION_TIME * 5;
    private static final long NOTIFICATION_DELAY_TIME = MOVE_ANIMATION_TIME + SPAWN_ANIMATION_TIME;

    //Internal variables
    private final Paint mPaint = new Paint();


    //Layout variables
    private int mCellSize = 0;
    private float mTextSize = 0;
    private float mCellTextSize = 0;
    private int mGridWidth = 0;
    private int mStartingX;
    private int mStartingY;
    private int mEndingX;
    private int mEndingY;

    //Assets
    private Drawable mBackgroundRectangle;
    private Drawable[] mCellRectangle;
    private BitmapDrawable[] mBitmapCell;
    private Drawable mLightUpRectangle;
    private Drawable mFadeRectangle;

    private long mLastFPSTime = System.nanoTime();
    private long mCurrentTime = System.nanoTime();

    private float mGameOverTextSize;

    private boolean mRefreshLastTime = true;
    private int mNumberOfSquaresX;
    private int mNumberOfSquaresY;
    private Game.State mGameState;
    private AnimationGrid mAnimationGrid = new AnimationGrid(4, 4);
    private Bitmap mBackground;
    private GameGrid mGameGrid;

    public GameView(Context context) {
        super(context);
        init();
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GameView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        try {
            setSquareCount(Game.DEFAULT_HEIGHT_X, Game.DEFAULT_WIDTH_Y);
            mCellRectangle = new Drawable[Game.DEFAULT_TILE_TYPES];
            mBitmapCell = new BitmapDrawable[Game.DEFAULT_TILE_TYPES];

            updateGrid(new GameGrid(4, 4));
            //Getting assets
            mBackgroundRectangle = getResources().getDrawable(R.drawable.background_rectangle);
            mCellRectangle[0] = getResources().getDrawable(R.drawable.cell_rectangle);

            mCellRectangle[1] = getResources().getDrawable(R.drawable.cell_rectangle_2);
            mCellRectangle[2] = getResources().getDrawable(R.drawable.cell_rectangle_4);
            mCellRectangle[3] = getResources().getDrawable(R.drawable.cell_rectangle_8);
            mCellRectangle[4] = getResources().getDrawable(R.drawable.cell_rectangle_16);
            mCellRectangle[5] = getResources().getDrawable(R.drawable.cell_rectangle_32);
            mCellRectangle[6] = getResources().getDrawable(R.drawable.cell_rectangle_64);
            mCellRectangle[7] = getResources().getDrawable(R.drawable.cell_rectangle_128);
            mCellRectangle[8] = getResources().getDrawable(R.drawable.cell_rectangle_256);
            mCellRectangle[9] = getResources().getDrawable(R.drawable.cell_rectangle_512);
            mCellRectangle[10] = getResources().getDrawable(R.drawable.cell_rectangle_1024);
            mCellRectangle[11] = getResources().getDrawable(R.drawable.cell_rectangle_2048);
            mCellRectangle[12] = getResources().getDrawable(R.drawable.cell_rectangle_4096);
            mCellRectangle[13] = getResources().getDrawable(R.drawable.cell_rectangle_8192);
            mCellRectangle[14] = getResources().getDrawable(R.drawable.cell_rectangle_16384);
            mCellRectangle[15] = getResources().getDrawable(R.drawable.cell_rectangle_32768);
            mCellRectangle[16] = getResources().getDrawable(R.drawable.cell_rectangle_65536);
            mCellRectangle[17] = getResources().getDrawable(R.drawable.cell_rectangle_131072);
            mCellRectangle[18] = getResources().getDrawable(R.drawable.cell_rectangle_262144);
            mCellRectangle[19] = getResources().getDrawable(R.drawable.cell_rectangle_524288);

            for (int xx = 20; xx < mCellRectangle.length; xx++) {
                mCellRectangle[xx] = getResources().getDrawable(R.drawable.cell_rectangle_524288);
            }

            mLightUpRectangle = getResources().getDrawable(R.drawable.light_up_rectangle);
            mFadeRectangle = getResources().getDrawable(R.drawable.fade_rectangle);
            mPaint.setAntiAlias(true);
        } catch (Exception e) {
            System.out.println("Error getting assets?");
        }
    }

    public void updateGrid(GameGrid grid) {
        mGameGrid = grid;
    }

    public void setGameState(Game.State state) {
        mGameState = state;
    }

    private void setSquareCount(int x, int y) {
        mNumberOfSquaresX = x;
        mNumberOfSquaresY = y;
        mAnimationGrid = new AnimationGrid(mNumberOfSquaresX, mNumberOfSquaresY);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldw, int oldh) {
        super.onSizeChanged(width, height, oldw, oldh);
        getLayout(width, height);
        createBackgroundBitmap(width, height);
        createBitmapCells();
    }

    @Override
    public void onDraw(Canvas canvas) {
        //Reset the transparency of the screen
        canvas.drawBitmap(mBackground, 0, 0, mPaint);
        drawTiles(canvas);

        //Refresh the screen if there is still an animation running
        if (mAnimationGrid.isAnimationActive()) {
            invalidate(mStartingX, mStartingY, mEndingX, mEndingY);
            tick();
            //Refresh one last time on game end.
        } else if (!(mGameState != Game.State.WON && mGameState != Game.State.LOST) && mRefreshLastTime) {
            invalidate();
            mRefreshLastTime = false;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int size;
        if (widthMode == MeasureSpec.EXACTLY && widthSize > 0) {
            size = widthSize;
        } else if (heightMode == MeasureSpec.EXACTLY && heightSize > 0) {
            size = heightSize;
        } else {
            size = widthSize < heightSize ? widthSize : heightSize;
        }
        int finalMeasureSpec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
        super.onMeasure(finalMeasureSpec, finalMeasureSpec);
    }

    private void getLayout(int width, int height) {
        mCellSize = Math.min(width / (mNumberOfSquaresX + 1), height / (mNumberOfSquaresY + 1));
        mGridWidth = mCellSize / 5;
        int boardMiddleX = width / 2;
        int boardMiddleY = height / 2;

        mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setTextSize(mCellSize);
        mTextSize = mCellSize * mCellSize / Math.max(mCellSize, mPaint.measureText("0000"));
        mCellTextSize = mTextSize;

        mGameOverTextSize = mTextSize * 2;

        //Grid Dimensions
        double halfNumSquaresX = mNumberOfSquaresX / 2d;
        double halfNumSquaresY = mNumberOfSquaresY / 2d;

        mStartingX = (int) (boardMiddleX - (mCellSize + mGridWidth) * halfNumSquaresX - mGridWidth / 2);
        mEndingX = (int) (boardMiddleX + (mCellSize + mGridWidth) * halfNumSquaresX + mGridWidth / 2);
        mStartingY = (int) (boardMiddleY - (mCellSize + mGridWidth) * halfNumSquaresY - mGridWidth / 2);
        mEndingY = (int) (boardMiddleY + (mCellSize + mGridWidth) * halfNumSquaresY + mGridWidth / 2);
        resyncTime();
    }

    private void drawDrawable(Canvas canvas, Drawable draw, int startingX, int startingY, int endingX, int endingY) {
        draw.setBounds(startingX, startingY, endingX, endingY);
        draw.draw(canvas);
    }


    private void createBackgroundBitmap(int width, int height) {
        mBackground = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mBackground);
        drawTileBackground(canvas);
        drawEmptyTiles(canvas);
    }

    private void drawTileBackground(Canvas canvas) {
        drawDrawable(canvas, mBackgroundRectangle, mStartingX, mStartingY, mEndingX, mEndingY);
    }

    private void drawEmptyTiles(Canvas canvas) {
        // Outputting the game mGameGrid
        for (int xx = 0; xx < mNumberOfSquaresX; xx++) {
            for (int yy = 0; yy < mNumberOfSquaresY; yy++) {
                int sX = mStartingX + mGridWidth + (mCellSize + mGridWidth) * xx;
                int eX = sX + mCellSize;
                int sY = mStartingY + mGridWidth + (mCellSize + mGridWidth) * yy;
                int eY = sY + mCellSize;
                drawDrawable(canvas, mCellRectangle[0], sX, sY, eX, eY);
            }
        }
    }

    private void drawTiles(Canvas canvas) {
        mPaint.setTextSize(mTextSize);
        mPaint.setTextAlign(Paint.Align.CENTER);
        // Outputting the individual cells
        for (int xx = 0; xx < mNumberOfSquaresX; xx++) {
            for (int yy = 0; yy < mNumberOfSquaresY; yy++) {
                int sX = mStartingX + mGridWidth + (mCellSize + mGridWidth) * xx;
                int eX = sX + mCellSize;
                int sY = mStartingY + mGridWidth + (mCellSize + mGridWidth) * yy;
                int eY = sY + mCellSize;

                Tile currentTile = mGameGrid.getCellContent(xx, yy);
                if (currentTile != null) {
                    //Get and represent the value of the tile
                    int value = currentTile.getValue();
                    int index = log2(value);

                    //Check for any active animations
                    ArrayList<AnimationTile> aArray = mAnimationGrid.getAnimationCell(xx, yy);
                    boolean animated = false;
                    for (int i = aArray.size() - 1; i >= 0; i--) {
                        AnimationTile aCell = aArray.get(i);
                        //If this animation is not active, skip it
                        if (aCell.getAnimationType() == SPAWN_ANIMATION) {
                            animated = true;
                        }
                        if (!aCell.isActive()) {
                            continue;
                        }

                        if (aCell.getAnimationType() == SPAWN_ANIMATION) { // Spawning animation
                            double percentDone = aCell.getPercentageDone();
                            float textScaleSize = (float) (percentDone);
                            mPaint.setTextSize(mTextSize * textScaleSize);

                            float cellScaleSize = mCellSize / 2 * (1 - textScaleSize);
                            mBitmapCell[index].setBounds((int) (sX + cellScaleSize), (int) (sY + cellScaleSize), (int) (eX - cellScaleSize), (int) (eY - cellScaleSize));
                            mBitmapCell[index].draw(canvas);
                        } else if (aCell.getAnimationType() == MERGE_ANIMATION) { // Merging Animation
                            double percentDone = aCell.getPercentageDone();
                            float textScaleSize = (float) (1 + INITIAL_VELOCITY * percentDone
                                    + MERGING_ACCELERATION * percentDone * percentDone / 2);
                            mPaint.setTextSize(mTextSize * textScaleSize);

                            float cellScaleSize = mCellSize / 2 * (1 - textScaleSize);
                            mBitmapCell[index].setBounds((int) (sX + cellScaleSize), (int) (sY + cellScaleSize), (int) (eX - cellScaleSize), (int) (eY - cellScaleSize));
                            mBitmapCell[index].draw(canvas);
                        } else if (aCell.getAnimationType() == MOVE_ANIMATION) {  // Moving animation
                            double percentDone = aCell.getPercentageDone();
                            int tempIndex = index;
                            if (aArray.size() >= 2) {
                                tempIndex = tempIndex - 1;
                            }
                            int previousX = aCell.getExtras()[0];
                            int previousY = aCell.getExtras()[1];
                            int currentX = currentTile.getX();
                            int currentY = currentTile.getY();
                            int dX = (int) ((currentX - previousX) * (mCellSize + mGridWidth) * (percentDone - 1) * 1.0);
                            int dY = (int) ((currentY - previousY) * (mCellSize + mGridWidth) * (percentDone - 1) * 1.0);
                            mBitmapCell[tempIndex].setBounds(sX + dX, sY + dY, eX + dX, eY + dY);
                            mBitmapCell[tempIndex].draw(canvas);
                        }
                        animated = true;
                    }

                    //No active animations? Just draw the cell
                    if (!animated) {
                        mBitmapCell[index].setBounds(sX, sY, eX, eY);
                        mBitmapCell[index].draw(canvas);
                    }
                }
            }
        }
    }


    private void createBitmapCells() {
        mPaint.setTextAlign(Paint.Align.CENTER);
        for (int xx = 0; xx < mBitmapCell.length; xx++) {
            int value = (int) Math.pow(2, xx);
            mPaint.setTextSize(mCellTextSize);
            float tempTextSize = mCellTextSize * mCellSize * 0.9f / Math.max(mCellSize * 0.9f, mPaint.measureText(String.valueOf(value)));
            mPaint.setTextSize(tempTextSize);
            Bitmap bitmap = Bitmap.createBitmap(mCellSize, mCellSize, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawDrawable(canvas, mCellRectangle[xx], 0, 0, mCellSize, mCellSize);
            drawTileText(canvas, value, 0, 0);
            mBitmapCell[xx] = new BitmapDrawable(getResources(), bitmap);
        }
    }

    private void drawTileText(Canvas canvas, int value, int sX, int sY) {
        int textShiftY = centerText();
        if (value == 2) {
            mPaint.setColor(getResources().getColor(R.color.text_shadow));
            mPaint.setShadowLayer(2.0f, 0, 0, getResources().getColor(R.color.text_white));
        } else {
            mPaint.setColor(getResources().getColor(R.color.text_white));
            mPaint.setShadowLayer(2.0f, 0, 0, getResources().getColor(R.color.text_shadow));
        }
        canvas.drawText("" + value, sX + mCellSize / 2, sY + mCellSize / 2 - textShiftY, mPaint);
    }

    private void tick() {
        mCurrentTime = System.nanoTime();
        mAnimationGrid.tickAll(mCurrentTime - mLastFPSTime);
        mLastFPSTime = mCurrentTime;
    }

    public void resyncTime() {
        mLastFPSTime = System.nanoTime();
    }

    private static int log2(int n) {
        if (n <= 0) throw new IllegalArgumentException();
        return 31 - Integer.numberOfLeadingZeros(n);
    }


    private int centerText() {
        return (int) ((mPaint.descent() + mPaint.ascent()) / 2);
    }

    public void spawnTile(Tile tile) {
        mAnimationGrid.startAnimation(tile.getX(), tile.getY(), SPAWN_ANIMATION,
                SPAWN_ANIMATION_TIME, MOVE_ANIMATION_TIME, null); //Direction: -1 = EXPANDING
    }

    public void cancelAnimations() {
        mAnimationGrid.cancelAnimations();
    }

    public void moveTile(int x, int y, int[] extras) {
        mAnimationGrid.startAnimation(x, y, MOVE_ANIMATION, MOVE_ANIMATION_TIME, 0, extras);
    }

    public void mergeTile(int x, int y) {
        mAnimationGrid.startAnimation(x, y, MERGE_ANIMATION,
                SPAWN_ANIMATION_TIME, MOVE_ANIMATION_TIME, null);
    }

    public void endGame() {
        mAnimationGrid.startAnimation(-1, -1, FADE_GLOBAL_ANIMATION, NOTIFICATION_ANIMATION_TIME, NOTIFICATION_DELAY_TIME, null);
    }

    public void setRefreshLastTime(boolean refreshLastTime) {
        mRefreshLastTime = refreshLastTime;
    }
}