package eu.thedarken.myo.twothousandfortyeight;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.thalmic.myo.Hub;

import java.util.ArrayList;
import java.util.List;

import eu.thedarken.myo.twothousandfortyeight.tools.KeyListener;
import eu.thedarken.myo.twothousandfortyeight.tools.Logy;
import eu.thedarken.myo.twothousandfortyeight.tools.iap.IabHelper;
import eu.thedarken.myo.twothousandfortyeight.tools.iap.IabResult;
import eu.thedarken.myo.twothousandfortyeight.tools.iap.Inventory;
import eu.thedarken.myo.twothousandfortyeight.tools.iap.Purchase;
import io.fabric.sdk.android.Fabric;


public class MainActivity extends Activity {
    private static final String TAG = "2048Myo:MainActivity";
    private static final int RC_REQUEST = 2048;
    private Fragment mGameFragment;
    private Hub mMyoHub;
    private IabHelper mHelper;
    private final Object mSyncObject = new Object();
    private Purchase mNoAdsPurchase;
    private Boolean mIsAdFree;
    private static final String SKU_NOADS = "purchase.removeads";
    private final List<HideThemAdsCallback> mAdStateCallbacks = new ArrayList<HideThemAdsCallback>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (BuildConfig.BUILD_TYPE.equals("release"))
            Fabric.with(this, new Crashlytics());
        setContentView(eu.thedarken.myo.twothousandfortyeight.R.layout.activity_main);
        Window w = getWindow();
        w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(eu.thedarken.myo.twothousandfortyeight.R.id.container, new GameFragment())
                    .commit();
        }
        if (mIsAdFree == null) {
            initIAP();
        } else {

        }
    }

    public Hub getMyoHub() {
        if (mMyoHub == null) {
            mMyoHub = Hub.getInstance();
            if (!mMyoHub.init(this)) {
                Logy.e(TAG, "Could not initialize the Hub.");
                mMyoHub = null;
            }
        }
        return mMyoHub;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            Fragment current = getFragmentManager().findFragmentById(eu.thedarken.myo.twothousandfortyeight.R.id.container);
            if (current != null && current instanceof KeyListener) {
                return ((KeyListener) current).onKeyDown(keyCode, event);
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    // We're being destroyed. It's important to dispose of the helper here!
    @Override
    public void onDestroy() {
        // very important:
        Logy.d(TAG, "Destroying helper.");
        if (mHelper != null) {
            mHelper.dispose();
            mHelper = null;
        }
        super.onDestroy();
    }

    public interface HideThemAdsCallback {
        /**
         * Callback when the IAP state has been determined
         *
         * @param hideAds true if ads should be hidden
         */
        public void onAdStateDetermined(boolean hideAds);
    }

    public void tellMeAboutTheAds(HideThemAdsCallback callback) {
        synchronized (mSyncObject) {
            if (!mAdStateCallbacks.contains(callback))
                mAdStateCallbacks.add(callback);
            if (mIsAdFree != null) {
                callback.onAdStateDetermined(mIsAdFree);
            }
        }
    }

    public void dontTellMeAboutTheAdds(HideThemAdsCallback callback) {
        mAdStateCallbacks.remove(callback);
    }

    // User clicked the "Upgrade to Premium" button.
    public void purchaseUpgrade() {
        if (mHelper != null && mNoAdsPurchase == null) {
            // No payload because we don't have to secure an open source app against piracy
            String payload = "";
            Logy.d(TAG, "Upgrade button clicked; launching purchase flow for upgrade.");
            mHelper.launchPurchaseFlow(this, SKU_NOADS, RC_REQUEST, mPurchaseFinishedListener, payload);
        }
    }

//    public void consumePurchase() {
//        if (mHelper != null && mNoAdsPurchase != null) {
//            mHelper.consumeAsync(mNoAdsPurchase, mConsumeFinishedListener);
//        }
//    }

    private void initIAP() {
        final String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAo2EJHuxh5xWoD/FPe2a4tEbCr7oUmu2RZvAUx/dIM5ER4RXjZxb4LEZRcf8Vh0z0AXjnY0SdtB58cyZ8Bw6GIb8TgGTs00WQMbJ9OGG0u/S9F/7L77Na7/iFpMJ8SAYye3uPNI3AzJ1AhuCkYb7fMdkDo7YkWp9iUJB+/6VV32XXtgz7JIqxuupDdYddONzNAsu+zj/oq3mvd/QGVxK61UdpjnMPDkKAK6+jPMH+c/5NsEuuoRGOZYnerlQg/MxRjNo0GdvFYItKYsxl8xP7FZqL241TpGRGf9DAxc/H4bXAcU1GcA/WLyOMwzUMgslDjtvDDNg1SJDlR1+imJsYOwIDAQAB";
        // Create the helper, passing it our context and the public key to verify signatures with
        Logy.d(TAG, "Creating IAB helper.");
        mHelper = new IabHelper(this, base64EncodedPublicKey);

        // enable debug logging (for a production application, you should set this to false).
        mHelper.enableDebugLogging(BuildConfig.DEBUG);

        // Start setup. This is asynchronous and the specified listener
        // will be called once setup completes.
        Logy.d(TAG, "Starting setup.");
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                Logy.d(TAG, "Setup finished.");

                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                    Logy.e(TAG, "No Google Play? No Problem. We don't care.");
                    return;
                }

                // Have we been disposed of in the meantime? If so, quit.
                if (mHelper == null) return;

                // IAB is fully set up. Now, let's get an inventory of stuff we own.
                Logy.d(TAG, "Setup successful. Querying inventory.");
                mHelper.queryInventoryAsync(mGotInventoryListener);
            }
        });
    }

    // Listener that's called when we finish querying the items and subscriptions we own
    private final IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Logy.d(TAG, "Query inventory finished.");

            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) return;

            if (result.isFailure()) {
                Logy.e(TAG, "Failed to retrieve inventory.");
                return;
            }

            Logy.d(TAG, "Query inventory was successful.");
            /*
             * Check for items we own. Notice that for each purchase, we check
             * the developer payload to see if it's correct! See
             * verifyDeveloperPayload().
             */

            // Do we have the premium upgrade?
            synchronized (mSyncObject) {
                mNoAdsPurchase = inventory.getPurchase(SKU_NOADS);
                mIsAdFree = (mNoAdsPurchase != null && verifyDeveloperPayload(mNoAdsPurchase));
                for (HideThemAdsCallback callback : mAdStateCallbacks) {
                    callback.onAdStateDetermined(mIsAdFree);
                }
            }

            Logy.d(TAG, "AD FREE? " + (mIsAdFree ? "YES" : "NO"));
            Logy.d(TAG, "Initial inventory query finished; enabling main UI.");
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Logy.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);
        if (mHelper == null) return;

        // Pass on the activity result to the helper for handling
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        } else {
            Logy.d(TAG, "onActivityResult handled by IABUtil.");
        }
    }

    private boolean verifyDeveloperPayload(Purchase p) {
        /**
         * Yeah, well, we could make it SUPER PIRACY PROOF, or not.
         * This app will be open source, so put your hands up in the air,
         * BECAUSE WE DON'T CARE \o/!
         */
        return true;
    }

    // Callback for when a purchase is finished
    private final IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Logy.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            if (result.isFailure()) {
                Logy.e(TAG, "result was FAILURE");
                Toast.makeText(MainActivity.this, getString(R.string.purchase_unsuccessfull), Toast.LENGTH_LONG).show();
                return;
            }
            if (!verifyDeveloperPayload(purchase)) {
                Logy.e(TAG, "verifyDeveloperPayload failed, but we don't care");
                return;
            }

            Logy.d(TAG, "Purchase successful.");
            if (purchase.getSku().equals(SKU_NOADS)) {
                // bought the premium upgrade!
                Logy.d(TAG, "Purchase is premium upgrade. Congratulating user.");
                Toast.makeText(MainActivity.this, eu.thedarken.myo.twothousandfortyeight.R.string.thank_you, Toast.LENGTH_LONG).show();
                mIsAdFree = true;
                synchronized (mSyncObject) {
                    mNoAdsPurchase = purchase;
                    mIsAdFree = (purchase != null && verifyDeveloperPayload(purchase));
                    for (HideThemAdsCallback callback : mAdStateCallbacks) {
                        callback.onAdStateDetermined(mIsAdFree);
                    }
                }
            }
        }
    };

    // Called when consumption is complete
//    private final IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
//        public void onConsumeFinished(Purchase purchase, IabResult result) {
//            Logy.d(TAG, "Consumption finished. Purchase: " + purchase + ", result: " + result);
//
//            // if we were disposed of in the meantime, quit.
//            if (mHelper == null) return;
//
//            // We know this is the "gas" sku because it's the only one we consume,
//            // so we don't check which sku was consumed. If you have more than one
//            // sku, you probably should check...
//            if (result.isSuccess()) {
//                // successfully consumed, so we apply the effects of the item in our
//                // game world's logic, which in our case means filling the gas tank a bit
//                Logy.d(TAG, "Consumption successful.");
//                synchronized (mSyncObject) {
//                    mIsAdFree = false;
//                    mNoAdsPurchase = null;
//                    for (HideThemAdsCallback callback : mAdStateCallbacks) {
//                        callback.onAdStateDetermined(mIsAdFree);
//                    }
//                }
//            } else {
//                // We fail silently
//            }
//            Logy.d(TAG, "End consumption flow.");
//        }
//    };


}
