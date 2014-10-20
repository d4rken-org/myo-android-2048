package eu.thedarken.myo.twothousandfortyeight;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.thalmic.myo.Hub;
import com.thalmic.myo.scanner.ScanActivity;


public class WelcomeFragment extends Fragment {
    private TextView mConnectMyo, mSetupMyo, mPlayNow;
    private TextView mLeftMapping, mRightMapping;
    private LinearLayout mControlLayout;
    private Switch mReverseSwitch;
    private Hub mMyoHub;
    private static final long REFRESHINTERVAL = 250;
    private boolean mIsMyoSetup = false;
    public static final String KEY_LEFT_RIGHT_REVERSED = "input.mapping.reverse.leftright";

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
        mLeftMapping = (TextView) layout.findViewById(R.id.tv_left);
        mRightMapping = (TextView) layout.findViewById(R.id.tv_right);
        mConnectMyo = (TextView) layout.findViewById(R.id.tv_connect_myo);
        mSetupMyo = (TextView) layout.findViewById(R.id.tv_setup_myo);
        mPlayNow = (TextView) layout.findViewById(R.id.tv_play_now);
        mControlLayout = (LinearLayout) layout.findViewById(R.id.ll_controls);
        mReverseSwitch = (Switch) layout.findViewById(R.id.swt_reverse);
        return layout;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean isReversed = preferences.getBoolean(KEY_LEFT_RIGHT_REVERSED, false);
        mReverseSwitch.setChecked(isReversed);
        mReverseSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                preferences.edit().putBoolean(KEY_LEFT_RIGHT_REVERSED, isChecked).commit();
                reverseLeftRight();
            }
        });
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
                    intent.setData(Uri.parse("https://support.getmyo.com/hc/en-us/articles/200755509-How-to-perform-the-setup-gesture"));
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
        if (isReversed)
            reverseLeftRight();
        refresh();
        if (getView() != null)
            getView().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (getView() != null && isAdded()) {
                        mIsMyoSetup = mMyoHub.getConnectedDevices().size() > 0;
                        getView().postDelayed(this, REFRESHINTERVAL);
                        refresh();
                    }
                }
            }, REFRESHINTERVAL);
        super.onActivityCreated(savedInstanceState);
    }

    private void reverseLeftRight() {
        String leftText = mLeftMapping.getText().toString();
        String rightText = mRightMapping.getText().toString();
        mLeftMapping.setText(rightText);
        mRightMapping.setText(leftText);
    }

    private void refresh() {
        if (mMyoHub != null) {
            if (mMyoHub.getConnectedDevices().isEmpty()) {
                mConnectMyo.setTextColor(getResources().getColor(R.color.myoteal));
                mSetupMyo.setTextColor(Color.WHITE);
                mPlayNow.setTextColor(Color.WHITE);
                mSetupMyo.setVisibility(View.GONE);
                mPlayNow.setVisibility(View.GONE);
                mControlLayout.setVisibility(View.GONE);
                mReverseSwitch.setVisibility(View.GONE);
            } else {
                mConnectMyo.setTextColor(Color.WHITE);
                if (!mIsMyoSetup && mMyoHub != null && mMyoHub.getConnectedDevices().isEmpty()) {
                    mSetupMyo.setTextColor(getResources().getColor(R.color.myoteal));
                } else {
                    mSetupMyo.setTextColor(Color.WHITE);
                    mPlayNow.setTextColor(getResources().getColor(R.color.myoteal));
                }
                mReverseSwitch.setVisibility(View.VISIBLE);
                mSetupMyo.setVisibility(View.VISIBLE);
                mPlayNow.setVisibility(View.VISIBLE);
                mControlLayout.setVisibility(View.VISIBLE);
            }
        }
    }
}
