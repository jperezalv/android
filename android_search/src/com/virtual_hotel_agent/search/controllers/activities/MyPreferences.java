/**
 * 
 */
package com.virtual_hotel_agent.search.controllers.activities;

import roboguice.activity.RoboPreferenceActivity;
import android.os.Bundle;

import com.virtual_hotel_agent.search.R;

public class MyPreferences extends RoboPreferenceActivity {

	// This approach was deprecated, but the new fragments approach is not backwards compatible!!!
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences); // Yeah, I know, I know...
	}

}

// Remove a preference: http://stackoverflow.com/questions/2240326/remove-hide-a-preference-from-the-screen