package com.evature.evasdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Build;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

/**
 * Created by iftah on 5/26/15.
 */
public class EvaButton {

    private static float MARGIN_BOTTOM = 24;  // margin in DIP


    public static void addDefaultButton(final Activity activity) {
        final ImageButton searchButton = (ImageButton) LayoutInflater.from(activity).inflate(R.layout.voice_search_button, null);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(activity, searchButton, "microphone_button");
                    Intent intent = new Intent(activity, SearchByVoiceActivity.class);
                    activity.startActivity(intent, options.toBundle());
                }
                else {
                    Intent intent = new Intent(activity, SearchByVoiceActivity.class);
                    activity.startActivity(intent);
                }

            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            searchButton.setTransitionName("microphone_button");
        }
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        final float scale = activity.getResources().getDisplayMetrics().density;
        int pixels = (int) (MARGIN_BOTTOM * scale + 0.5f);
        params.bottomMargin = pixels;
        searchButton.setLayoutParams(params);
        RelativeLayout rl = new RelativeLayout(activity);
        rl.setGravity(Gravity.BOTTOM);
        rl.addView(searchButton);

        activity.getWindow().addContentView(rl, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
    }

}