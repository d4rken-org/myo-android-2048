package eu.thedarken.myo.twothousandfortyeight.tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.TextView;
import eu.thedarken.myo.twothousandfortyeight.game.Game;

public class ScoreKeeper implements Game.ScoreListener {

    private TextView mScoreDisplay;
    private TextView mHighScoreDisplay;
    private static final String HIGH_SCORE = "score.highscore";
    private static final String PREFERENCES = "score";
    private final SharedPreferences mPreferences;
    private long mScore;
    private long mHighScore;

    public ScoreKeeper(Context context) {
        mPreferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    public void setViews(TextView score, TextView highscore) {
        mScoreDisplay = score;
        mHighScoreDisplay = highscore;
        reset();
    }

    private void reset() {
        mHighScore = loadHighScore();
        if (mHighScoreDisplay != null)
            mHighScoreDisplay.setText("" + mHighScore);
        mScore = 0;
        if (mScoreDisplay != null)
            mScoreDisplay.setText("" + mScore);
    }

    private long loadHighScore() {
        if (mPreferences == null)
            return -1;
        return mPreferences.getLong(HIGH_SCORE, 0);
    }

    private void saveHighScore(long highScore) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putLong(HIGH_SCORE, highScore);
        editor.commit();
    }

    void setScore(long score) {
        mScore = score;
        if (mScoreDisplay != null)
            mScoreDisplay.setText("" + mScore);
        if (mScore > mHighScore) {
            mHighScore = mScore;
            if (mHighScoreDisplay != null)
                mHighScoreDisplay.setText("" + mHighScore);
            saveHighScore(mHighScore);
        }
    }

    @Override
    public void onNewScore(long score) {
        setScore(score);
    }
}
