package com.evature.evasdk;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.PermissionChecker;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.evature.evasdk.appinterface.AppScope;
import com.evature.evasdk.appinterface.AppSetup;
import com.evature.evasdk.appinterface.PermissionsRequiredHandler;
import com.evature.evasdk.evaapis.EvaComponent;
import com.evature.evasdk.evaapis.EvaSpeak;
import com.evature.evasdk.util.DLog;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by iftah on 5/26/15.
 */
public class EvaChatTrigger {

    private static final String TAG = "EvaChatTrigger";

    // these buttons will be hidden/displayed when the Eva chat screen is opened/closed
    public static ArrayList<WeakReference<ImageButton>> evaButtons = new ArrayList<WeakReference<ImageButton>>();

    private static float MARGIN_BOTTOM = 24;  // margin in DIP

    public static void addDefaultButton(FragmentActivity activity) {
        addDefaultButton(activity, null);
    }

    private static ViewGroup.LayoutParams buttonLayoutParams = null;
    public static ViewGroup.LayoutParams getDefaultButtonLayoutParams(Context context) {
        if (buttonLayoutParams == null) {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.CENTER_IN_PARENT);

            final float scale = context.getResources().getDisplayMetrics().density;
            int pixels = (int) (MARGIN_BOTTOM * scale + 0.5f);
            params.bottomMargin = pixels;
            params.width = (int)(76f*scale+0.5f);
            params.height = (int)(76f*scale+0.5f);
            buttonLayoutParams = params;
        }
        return buttonLayoutParams;
    }

    public static void setDefaultButtonLayoutParams(ViewGroup.LayoutParams params) {
        buttonLayoutParams = params;
    }


    public static void addDefaultButton(FragmentActivity activity, final AppScope evaContext) {
        // initialize TTS
        EvaSpeak.getOrCreateInstance(activity.getApplicationContext());

        ImageButton searchButton = (ImageButton) LayoutInflater.from(activity).inflate(R.layout.evature_voice_search_button, null);
        WeakReference<ImageButton> weakRef = new WeakReference<ImageButton>(searchButton);
        evaButtons.add(weakRef);
        searchButton.setImageResource(R.drawable.evature_microphone_icon);
        final FragmentActivity fActivity = activity;
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AppSetup.startEvaAsActivity) {
                    startSearchByVoiceActivity(fActivity, evaContext);
                }
                else {
                    startSearchByVoice(fActivity, evaContext);
                }
            }
        });
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            searchButton.setTransitionName("eva_microphone_button");
//        }
        ViewGroup.LayoutParams params = getDefaultButtonLayoutParams(activity);
        searchButton.setLayoutParams(params);

        ViewGroup rootView = getOrCreateRootView(activity);

        rootView.addView(searchButton);
    }

    private static ViewGroup.LayoutParams overlayLayoutParams = null;
    public static ViewGroup.LayoutParams getOverlayLayoutParams() {
        if (overlayLayoutParams == null ) {
            overlayLayoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
        }
        return overlayLayoutParams;
    }

    public static void setOverlayLayoutParams(ViewGroup.LayoutParams params) {
        overlayLayoutParams = params;
    }

    public static void setOverlayBelowActionBar(Context context) {
        final TypedArray styledAttributes = context.getTheme().obtainStyledAttributes(
                new int[]{android.R.attr.actionBarSize});
        int actionBarSize = (int) styledAttributes.getDimension(0, 0);
        styledAttributes.recycle();
        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        params.topMargin = actionBarSize;
        setOverlayLayoutParams(params);
    }

    private static ViewGroup getOrCreateRootView(FragmentActivity activity) {
        View rootView = activity.findViewById(R.id.evature_root_view);
        if (rootView == null) {
            EvaSpeak.getOrCreateInstance(activity.getApplicationContext());

            View rl = LayoutInflater.from(activity).inflate(R.layout.evature_overlay_container, null);
            activity.getWindow().addContentView(rl, getOverlayLayoutParams());
            rootView = rl;
        }
        return (ViewGroup) rootView;
    }

    private static boolean checkPermissions(Context context) {
        String[] permissionsToCheck;
        if (AppSetup.locationTracking) {
            permissionsToCheck = new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.RECORD_AUDIO
            };
        }
        else {
            permissionsToCheck = new String[] {
                    Manifest.permission.RECORD_AUDIO
            };
        }
        ArrayList<String> missingPermissions = new ArrayList<String>();
        for (String perm : permissionsToCheck) {
            if (PackageManager.PERMISSION_GRANTED != PermissionChecker.checkSelfPermission(context, perm)) {
                missingPermissions.add(perm);
            }
        }

        if (missingPermissions.size() > 0) {
            Log.i(TAG, "Eva cannot start due to missing permissions: " + missingPermissions.toString());
            if (EvaComponent.evaAppHandler instanceof PermissionsRequiredHandler) {
                String[] missingPermissionsArray = missingPermissions.toArray(new String[missingPermissions.size()]);
                ((PermissionsRequiredHandler)EvaComponent.evaAppHandler).handleMissingPermissions(missingPermissionsArray);
            }
            return false;
        }

        return true;
    }

    public static void startSearchByVoice(FragmentActivity activity) {
        startSearchByVoice(activity, null);
    }
    public static void startSearchByVoice(FragmentActivity activity, AppScope evaContext) {
        final FragmentManager manager = activity.getSupportFragmentManager();

        getOrCreateRootView(activity);

        Intent theIntent = activity.getIntent();
        if (evaContext != null) {
            theIntent.putExtra(EvaChatScreenComponent.INTENT_EVA_CONTEXT, evaContext.toString());
        }

        boolean hasPermissions = checkPermissions(activity);
        if (!hasPermissions) {
            return;
        }


        Fragment newFragment = new EvaChatScreenFragment();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out);
        transaction.add(R.id.evature_root_view, newFragment, EvaChatScreenFragment.TAG);
        transaction.addToBackStack(null);
        transaction.commitAllowingStateLoss();
    }


    private static EvaChatScreenFragment getEvaChatFragment(Context activity) {
        if (activity instanceof FragmentActivity) {
            FragmentActivity fa = (FragmentActivity) activity;
            FragmentManager manager = fa.getSupportFragmentManager();
            Fragment fragment = manager.findFragmentByTag(EvaChatScreenFragment.TAG);
            EvaChatScreenFragment evaChatScreenFragment = (EvaChatScreenFragment) fragment;
            return evaChatScreenFragment;
        }
        return null;
    }

    public static void closeChatScreen(Context activity) {
        EvaChatScreenFragment evaChatScreenFragment = getEvaChatFragment(activity);
        if (evaChatScreenFragment != null) {
            evaChatScreenFragment.closeChatFragment();
        }
        else {
            DLog.w(TAG, "No EvaChatScreenFragment to close");
        }
    }

    public static void startSearchByVoiceActivity( Context activity, AppScope evaContext) {
        boolean hasPermissions = checkPermissions(activity);
        if (!hasPermissions) {
            return;
        }

        Intent intent = new Intent(activity, EvaChatScreenActivity.class);
        if (evaContext != null) {
            intent.putExtra(EvaChatScreenComponent.INTENT_EVA_CONTEXT, evaContext.toString());
        }
        activity.startActivity(intent);
    }
}
