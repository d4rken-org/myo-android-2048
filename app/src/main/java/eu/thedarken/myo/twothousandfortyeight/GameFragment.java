package eu.thedarken.myo.twothousandfortyeight;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.thalmic.myo.Arm;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.Vector3;
import com.thalmic.myo.XDirection;

import eu.thedarken.myo.twothousandfortyeight.game.Game;
import eu.thedarken.myo.twothousandfortyeight.game.GameView;
import eu.thedarken.myo.twothousandfortyeight.game.Tile;
import eu.thedarken.myo.twothousandfortyeight.tools.InputListener;
import eu.thedarken.myo.twothousandfortyeight.tools.KeyListener;
import eu.thedarken.myo.twothousandfortyeight.tools.Logy;
import eu.thedarken.myo.twothousandfortyeight.tools.ScoreKeeper;

public class GameFragment extends Fragment implements KeyListener, DeviceListener, MainActivity.HideThemAdsCallback, Game.GameStateListener {
    private static final String TAG = "2048Myo:GameFragment";
    public static boolean sReversed = false;

    private GameView mGameView;
    private Game mGame;

    private static final String SCORE = "savegame.score";
    private static final String UNDO_SCORE = "savegame.undoscore";
    private static final String CAN_UNDO = "savegame.canundo";
    private static final String UNDO_GRID = "savegame.undo";
    private static final String GAME_STATE = "savegame.gamestate";
    private static final String UNDO_GAME_STATE = "savegame.undogamestate";

    private TextView mScoreText;
    private TextView mHighScoreText;
    private TextView mOverlay;
    private Hub mMyoHub;
    private ScoreKeeper mScoreKeeper;
    private ImageButton mResetButton, mUndoButton, mMyoButton, mInfoButton;
    private LinearLayout mAdContainer;
    private TextView mTitleText;
    private AdView mAdView;
    private MainActivity mMainActivity;

    @Override
    public void onAttach(Activity activity) {
        mMainActivity = (MainActivity) activity;
        mMyoHub = ((MainActivity) activity).getMyoHub();
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_game_layout, container, false);
        mGameView = (GameView) layout.findViewById(R.id.gameview);
        mScoreText = (TextView) layout.findViewById(R.id.tv_score);
        mHighScoreText = (TextView) layout.findViewById(R.id.tv_highscore);
        mTitleText = (TextView) layout.findViewById(R.id.tv_title);
        mResetButton = (ImageButton) layout.findViewById(R.id.ib_reset);
        mUndoButton = (ImageButton) layout.findViewById(R.id.ib_undo);
        mMyoButton = (ImageButton) layout.findViewById(R.id.ib_myo);
        mInfoButton = (ImageButton) layout.findViewById(R.id.ib_info);
        mAdView = (AdView) layout.findViewById(R.id.adv_banner);
        mAdContainer = (LinearLayout) layout.findViewById(R.id.ll_ad);
        mOverlay = (TextView) layout.findViewById(R.id.tv_endgame_overlay);
        return layout;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mScoreKeeper = new ScoreKeeper(getActivity());
        mScoreKeeper.setViews(mScoreText, mHighScoreText);
        mGame = new Game(getActivity());
        mGame.setup(mGameView);
        mGame.setScoreListener(mScoreKeeper);
        mGame.setGameStateListener(this);
        mGame.newGame();
        InputListener input = new InputListener();
        input.setView(mGameView);
        input.setGame(mGame);

        mTitleText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mGame.isEndlessMode()) {
                    mGame.setEndlessMode();
                    mTitleText.setText(Html.fromHtml("&infin;"));
                }
            }
        });
        mResetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGame.newGame();
                mTitleText.setText(Html.fromHtml("2048"));
            }
        });
        mResetButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(getActivity(), mMainActivity.getString(R.string.start_new_game), Toast.LENGTH_SHORT).show();
                return true;
            }
        });
        mUndoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGame.revertUndoState();
                if (mGame.getGameState() == Game.State.ENDLESS || mGame.getGameState() == Game.State.ENLESS_WON) {
                    mTitleText.setText(Html.fromHtml("&infin;"));
                } else {
                    mTitleText.setText(Html.fromHtml("2048"));
                }
            }
        });
        mUndoButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(getActivity(), mMainActivity.getString(R.string.undo_last_move), Toast.LENGTH_SHORT).show();
                return true;
            }
        });
        mMyoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMyoHub != null) {
                    FragmentTransaction transaction = getFragmentManager().beginTransaction();
                    Fragment infoFrag = getFragmentManager().findFragmentByTag(InfoFragment.class.getName());
                    if (infoFrag == null)
                        infoFrag = WelcomeFragment.newInstance();
                    transaction.setCustomAnimations(R.anim.fragment_anim_normal_enter, R.anim.fragment_anim_normal_exit, R.anim.fragment_anim_normal_enter, R.anim.fragment_anim_normal_exit);
                    transaction.replace(R.id.container, infoFrag, WelcomeFragment.class.getName());
                    transaction.addToBackStack(GameFragment.class.getName());
                    transaction.commit();
                } else {
                    Toast.makeText(getActivity(), "Myo unavailable", Toast.LENGTH_SHORT).show();
                }
            }
        });
        mMyoButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(getActivity(), mMainActivity.getString(R.string.myo_options), Toast.LENGTH_SHORT).show();
                return true;
            }
        });
        mInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                Fragment infoFrag = getFragmentManager().findFragmentByTag(InfoFragment.class.getName());
                if (infoFrag == null)
                    infoFrag = InfoFragment.newInstance();
                transaction.setCustomAnimations(R.anim.fragment_anim_normal_enter, R.anim.fragment_anim_normal_exit, R.anim.fragment_anim_normal_enter, R.anim.fragment_anim_normal_exit);
                transaction.replace(R.id.container, infoFrag, InfoFragment.class.getName());
                transaction.addToBackStack(GameFragment.class.getName());
                transaction.commit();
            }
        });
        mInfoButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(getActivity(), mMainActivity.getString(R.string.about_this_app), Toast.LENGTH_SHORT).show();
                return true;
            }
        });
        Logy.d(TAG, "Checking if we should show ads");
        mMainActivity.tellMeAboutTheAds(this);
    }

    @Override
    public void onAdStateDetermined(boolean hideAds) {
        Logy.d(TAG, "We got a result, Hide the ads? " + hideAds);
        hideAds(hideAds);
    }

    private void hideAds(boolean hideAds) {
        if (!hideAds) {
            mAdContainer.setVisibility(View.VISIBLE);
            mAdView.setVisibility(View.VISIBLE);
            AdRequest adRequest = new AdRequest.Builder()
                    .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                    .addTestDevice("C34CF836138E576F0C3CC8BBF6B19388")
                    .build();
            mAdView.loadAd(adRequest);
        } else {
            mAdView.setVisibility(View.GONE);
            mAdContainer.setVisibility(View.GONE);
            mAdContainer.removeView(mAdView);
        }
    }


    @Override
    public void onResume() {
        mAdView.resume();
        if (mMyoHub != null)
            mMyoHub.addListener(this);
        load();
        if (mGame.getGameState() == Game.State.ENDLESS || mGame.getGameState() == Game.State.ENLESS_WON) {
            mTitleText.setText(Html.fromHtml("&infin;"));
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        mAdView.pause();
        if (mMyoHub != null)
            mMyoHub.removeListener(this);
        if (mGame != null)
            save();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (mMainActivity != null)
            mMainActivity.dontTellMeAboutTheAdds(this);
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            mGame.move(Game.DIRECTION_DOWN);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            mGame.move(Game.DIRECTION_UP);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            mGame.move(Game.DIRECTION_LEFT);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            mGame.move(Game.DIRECTION_RIGHT);
            return true;
        }
        return false;
    }

    private void save() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = settings.edit();
        Tile[][] field = mGame.getGameGrid().getGrid();
        Tile[][] undoField = mGame.getGameGrid().getUndoGrid();
        for (int xx = 0; xx < field.length; xx++) {
            for (int yy = 0; yy < field[0].length; yy++) {
                if (field[xx][yy] != null) {
                    editor.putInt(xx + " " + yy, field[xx][yy].getValue());
                } else {
                    editor.putInt(xx + " " + yy, 0);
                }

                if (undoField[xx][yy] != null) {
                    editor.putInt(UNDO_GRID + xx + " " + yy, undoField[xx][yy].getValue());
                } else {
                    editor.putInt(UNDO_GRID + xx + " " + yy, 0);
                }
            }
        }
        editor.putLong(SCORE, mGame.getScore());
        editor.putLong(UNDO_SCORE, mGame.getLastScore());
        editor.putBoolean(CAN_UNDO, mGame.isCanUndo());
        editor.putString(GAME_STATE, mGame.getGameState().name());
        editor.putString(UNDO_GAME_STATE, mGame.getLastGameState().name());
        editor.commit();
    }

    private void load() {
        //Stopping all animations
        mGameView.cancelAnimations();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
        for (int xx = 0; xx < mGame.getGameGrid().getGrid().length; xx++) {
            for (int yy = 0; yy < mGame.getGameGrid().getGrid()[0].length; yy++) {
                int value = settings.getInt(xx + " " + yy, -1);
                if (value > 0) {
                    mGame.getGameGrid().getGrid()[xx][yy] = new Tile(xx, yy, value);
                } else if (value == 0) {
                    mGame.getGameGrid().getGrid()[xx][yy] = null;
                }

                int undoValue = settings.getInt(UNDO_GRID + xx + " " + yy, -1);
                if (undoValue > 0) {
                    mGame.getGameGrid().getUndoGrid()[xx][yy] = new Tile(xx, yy, undoValue);
                } else if (value == 0) {
                    mGame.getGameGrid().getUndoGrid()[xx][yy] = null;
                }
            }
        }

        mGame.setScore(settings.getLong(SCORE, 0));
        mGame.setLastScore(settings.getLong(UNDO_SCORE, 0));
        mGame.setCanUndo(settings.getBoolean(CAN_UNDO, mGame.isCanUndo()));
        try {
            mGame.updateGameState(Game.State.valueOf(settings.getString(GAME_STATE, Game.State.NORMAL.name())));
        } catch (IllegalArgumentException e) {
            mGame.updateGameState(Game.State.NORMAL);
        }
        try {
            mGame.setLastGameState(Game.State.valueOf(settings.getString(UNDO_GAME_STATE, Game.State.NORMAL.name())));
        } catch (IllegalArgumentException e) {
            mGame.setLastGameState(Game.State.NORMAL);
        }
        mGame.updateUI();
    }

    @Override
    public void onAttach(Myo myo, long l) {

    }

    @Override
    public void onDetach(Myo myo, long l) {

    }

    @Override
    public void onConnect(Myo myo, long l) {
        Logy.d(TAG, "onPair:" + myo.getMacAddress());
    }

    @Override
    public void onDisconnect(Myo myo, long l) {
        Logy.d(TAG, "onPair:" + myo.getMacAddress());
    }

    @Override
    public void onArmSync(Myo myo, long l, Arm arm, XDirection xDirection) {
        Logy.i(TAG, "onArmSync:" + xDirection.name());
        sReversed = arm == Arm.LEFT;
        Toast.makeText(getActivity(), getString(R.string.arm_recognized), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onArmUnsync(Myo myo, long l) {
        Logy.i(TAG, "onArmUnsync");
        Toast.makeText(getActivity(), getString(R.string.arm_lost), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onUnlock(Myo myo, long l) {

    }

    @Override
    public void onLock(Myo myo, long l) {

    }

    @Override
    public void onPose(Myo myo, long l, Pose pose) {
        Logy.i(TAG, "onPose:" + pose.name());
        if (pose == Pose.FIST) {
            mGame.move(Game.DIRECTION_DOWN);
        } else if (pose == Pose.WAVE_OUT) {
            if (GameFragment.sReversed) {
                mGame.move(Game.DIRECTION_LEFT);
            } else {
                mGame.move(Game.DIRECTION_RIGHT);
            }
        } else if (pose == Pose.WAVE_IN) {
            if (GameFragment.sReversed) {
                mGame.move(Game.DIRECTION_RIGHT);
            } else {
                mGame.move(Game.DIRECTION_LEFT);
            }
        } else if (pose == Pose.FINGERS_SPREAD) {
            mGame.move(Game.DIRECTION_UP);
        }
    }

    @Override
    public void onOrientationData(Myo myo, long l, Quaternion quaternion) {
        //Logy.v(TAG, l + " onOrientationData:" + quaternion.toString());

    }

    @Override
    public void onAccelerometerData(Myo myo, long timestamp, Vector3 v3) {
        //Logy.v(TAG, timestamp + " onAccelerometerData:" + v3.toString());

    }

    @Override
    public void onGyroscopeData(Myo myo, long l, Vector3 vector3) {
        //Logy.v(TAG, l + " onGyoscopeData:" + vector3.toString());
    }

    @Override
    public void onRssi(Myo myo, long l, int i) {
        Logy.d(TAG, myo.toString());
    }

    @Override
    public void onGameStateChanged(Game.State state) {
        if (state == Game.State.WON || state == Game.State.ENLESS_WON) {
            mOverlay.setVisibility(View.VISIBLE);
            mOverlay.setText(R.string.you_win);
        } else if (state == Game.State.LOST) {
            mOverlay.setVisibility(View.VISIBLE);
            mOverlay.setText(R.string.game_over);
        } else {
            mOverlay.setVisibility(View.GONE);
        }
    }
}
