package eu.thedarken.myo.twothousandfortyeight;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.thalmic.myo.Arm;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.Vector3;
import com.thalmic.myo.XDirection;
import com.thalmic.myo.scanner.ScanActivity;

import eu.thedarken.myo.twothousandfortyeight.tools.Logy;


public class WelcomeFragment extends Fragment implements DeviceListener {
    private static final String TAG = "2048Myo:WelcomeFragment";
    private TextView mConnectMyo, mSetupMyo, mWhenReady, mPlayNow;
    private ImageView mLeftIcon, mRightIcon, mDownIcon, mUpIcon;
    private LinearLayout mControlLayout, mUpBox, mDownBox, mLeftBox, mRightBox;
    private Hub mMyoHub;
    private static final long REFRESHINTERVAL = 250;
    private boolean mConnected = false;
    private boolean mSynced = false;
    private Animation mAnimWiggle;

    public static Fragment newInstance() {
        return new WelcomeFragment();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mMyoHub = ((MainActivity) activity).getMyoHub();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_welcome_layout, container, false);
        mConnectMyo = (TextView) layout.findViewById(R.id.tv_connect_myo);
        mSetupMyo = (TextView) layout.findViewById(R.id.tv_setup_myo);
        mWhenReady = (TextView) layout.findViewById(R.id.tv_when_ready);
        mPlayNow = (TextView) layout.findViewById(R.id.tv_play_now);
        mControlLayout = (LinearLayout) layout.findViewById(R.id.ll_controls);
        mLeftIcon = (ImageView) layout.findViewById(R.id.iv_gesture_left);
        mRightIcon = (ImageView) layout.findViewById(R.id.iv_gesture_right);
        mUpIcon = (ImageView) layout.findViewById(R.id.iv_gesture_up);
        mDownIcon = (ImageView) layout.findViewById(R.id.iv_gesture_down);

        mUpBox = (LinearLayout) layout.findViewById(R.id.ll_up_layout);
        mDownBox = (LinearLayout) layout.findViewById(R.id.ll_down_layout);
        mLeftBox = (LinearLayout) layout.findViewById(R.id.ll_left_layout);
        mRightBox = (LinearLayout) layout.findViewById(R.id.ll_right_layout);
        return layout;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        mConnectMyo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMyoHub != null) {
                    Intent intent = new Intent(getActivity(), ScanActivity.class);
                    startActivity(intent);
                }
            }
        });
        mSetupMyo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mMyoHub.getConnectedDevices().isEmpty()) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("https://support.getmyo.com/hc/en-us/articles/200755509-How-to-perform-the-sync-gesture"));
                    startActivity(intent);
                }
            }
        });
        mPlayNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager().popBackStack();
            }
        });
        refresh();
        if (getView() != null)
            getView().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (getView() != null && isAdded()) {
                        getView().postDelayed(this, REFRESHINTERVAL);
                        refresh();
                    }
                }
            }, REFRESHINTERVAL);

        if (mMyoHub != null) {
            mMyoHub.addListener(this);
        }

        mAnimWiggle = AnimationUtils.loadAnimation(getActivity(), R.anim.wiggle);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        if (mMyoHub != null)
            mMyoHub.removeListener(this);
        super.onDestroy();
    }

    private void setArm(boolean left) {
        if (left) {
            GameFragment.sReversed = true;
            mLeftIcon.setImageResource(R.drawable.ic_pose_waveleft_lh);
            mRightIcon.setImageResource(R.drawable.ic_pose_waveright_lh);
            mUpIcon.setImageResource(R.drawable.ic_pose_spread_lh);
            mDownIcon.setImageResource(R.drawable.ic_pose_fist_lh);
        } else {
            GameFragment.sReversed = false;
            mLeftIcon.setImageResource(R.drawable.ic_pose_waveleft);
            mRightIcon.setImageResource(R.drawable.ic_pose_waveright);
            mUpIcon.setImageResource(R.drawable.ic_pose_spread);
            mDownIcon.setImageResource(R.drawable.ic_pose_fist);
        }
    }

    private void refresh() {
        if (mMyoHub != null) {
            setArm(GameFragment.sReversed);
            mConnectMyo.setVisibility(mConnected ? View.GONE : View.VISIBLE);
            mSetupMyo.setVisibility(mConnected && !mSynced ? View.VISIBLE : View.GONE);
            mWhenReady.setVisibility(mConnected && mSynced ? View.VISIBLE : View.GONE);
            mPlayNow.setVisibility(mConnected && mSynced ? View.VISIBLE : View.GONE);
            mControlLayout.setVisibility(mConnected && mSynced ? View.VISIBLE : View.GONE);
        } else {
            mConnectMyo.setVisibility(View.GONE);
            mSetupMyo.setTextColor(View.GONE);
            mSetupMyo.setVisibility(View.GONE);
            mWhenReady.setVisibility(View.GONE);
            mPlayNow.setVisibility(View.GONE);
            mControlLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onAttach(Myo myo, long l) {
        Logy.d(TAG, "onAttach");
    }

    @Override
    public void onDetach(Myo myo, long l) {
        Logy.d(TAG, "onDetach");
    }

    @Override
    public void onConnect(Myo myo, long l) {
        Logy.d(TAG, "onConnect");
        mConnected = true;
        if (myo.isUnlocked())
            mSynced = true;
    }

    @Override
    public void onDisconnect(Myo myo, long l) {
        Logy.d(TAG, "onDisconnect");
        mConnected = false;
    }

    @Override
    public void onArmSync(Myo myo, long l, Arm arm, XDirection xDirection) {
        Logy.d(TAG, "onArmSync");
        mSynced = true;
        GameFragment.sReversed = arm == Arm.LEFT;
        myo.unlock(Myo.UnlockType.HOLD);
    }

    @Override
    public void onArmUnsync(Myo myo, long l) {
        Logy.d(TAG, "onArmUnsync");
        mSynced = false;
    }

    @Override
    public void onUnlock(Myo myo, long l) {
        Logy.d(TAG, "onUnlock");
    }

    @Override
    public void onLock(Myo myo, long l) {
        Logy.d(TAG, "onLock");
    }

    @Override
    public void onPose(Myo myo, long l, Pose pose) {
        Logy.i(TAG, "onPose:" + pose.name());
        if(!isResumed())
            return;

        if (pose == Pose.FIST) {
            mDownBox.startAnimation(mAnimWiggle);
        } else if (pose == Pose.WAVE_OUT) {
            if (GameFragment.sReversed) {
                mLeftBox.startAnimation(mAnimWiggle);
            } else {
                mRightBox.startAnimation(mAnimWiggle);
            }
        } else if (pose == Pose.WAVE_IN) {
            if (GameFragment.sReversed) {
                mRightBox.startAnimation(mAnimWiggle);
            } else {
                mLeftBox.startAnimation(mAnimWiggle);
            }
        } else if (pose == Pose.FINGERS_SPREAD) {
            mUpBox.startAnimation(mAnimWiggle);
        }
    }

    @Override
    public void onOrientationData(Myo myo, long l, Quaternion quaternion) {

    }

    @Override
    public void onAccelerometerData(Myo myo, long l, Vector3 vector3) {

    }

    @Override
    public void onGyroscopeData(Myo myo, long l, Vector3 vector3) {

    }

    @Override
    public void onRssi(Myo myo, long l, int i) {

    }
}
