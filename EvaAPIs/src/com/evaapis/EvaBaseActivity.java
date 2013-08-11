package com.evaapis;


import java.util.Locale;

import roboguice.activity.RoboFragmentActivity;
import roboguice.event.EventManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;

import com.evaapis.events.NewSessionStarted;
import com.evature.util.ExternalIpAddressGetter;
import com.google.inject.Inject;
import com.google.inject.Injector;

abstract public class EvaBaseActivity extends RoboFragmentActivity implements EvaSearchReplyListener, OnInitListener{ 

	protected String mSessionId = "1";
	private final String TAG = "EvaBaseActivity";
	private String mPreferedLanguage = "en-US";	
	private String mLastLanguageUsed = "en-US";

	@Inject protected Injector injector;

	private boolean mTtsConfigured = false;
	private TextToSpeech mTts = null;
	protected SpeechRecognition mSpeechRecognition;

	@Inject private ExternalIpAddressGetter mExternalIpAddressGetter;
	@Inject private EvatureLocationUpdater mLocationUpdater;

	@Inject protected EventManager eventManager;

	protected void speak(String sayIt) {
		if (mTts != null) {
			mTts.speak(sayIt, TextToSpeech.QUEUE_FLUSH, null);
		}
	}
	
	private void setTtsLanguage(String destLanguage) {
		// Set preferred language to whatever the user used to speak to phone.
		// Note that a language may not be available, and the result will indicate this.
		Locale aLocale = Locale.US; // new Locale(destLanguage.substring(0, 2), destLanguage.substring(3, 5));
		mTts.setLanguage(aLocale);
		 
	}

	// Implements TextToSpeech.OnInitListener.
	public void onInit(int status) {
		// status can be either TextToSpeech.SUCCESS or TextToSpeech.ERROR.
		if (status == TextToSpeech.SUCCESS) {
			setTtsLanguage(mLastLanguageUsed);
			// Check the documentation for other possible result codes.
			// For example, the language may be available for the locale, but not for the specified country and variant.
			mTtsConfigured = true;
		} else {
			// Initialization failed.
			mTts = null;
		}
	}

	@Override
	public void onDestroy() {
		// Don't forget to shutdown!
		if (mTts != null) {
			mTts.stop();
			mTts.shutdown();
		}

		super.onDestroy();
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		mExternalIpAddressGetter.pause();
		mLocationUpdater.stopGPS();
	}
	
	
	// Request updates at startupResults
	@Override
	protected void onResume() {
		super.onResume();
		mExternalIpAddressGetter.start();
		mLocationUpdater.startGPS();
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		String oldLanguage = new String(mPreferedLanguage);
		mPreferedLanguage = prefs.getString("languages", "en-US");
		if (!oldLanguage.equals(mPreferedLanguage)) // User changed the settings and chose a new speech recognition
			// language
			if (mTtsConfigured)
				setTtsLanguage(mPreferedLanguage);
		mLastLanguageUsed = new String(mPreferedLanguage);
	}

	@Override
	public void onEvaReply(EvaApiReply reply, Object cookie) {
		if (reply.sessionId != null) {
			if (reply.sessionId.equals(mSessionId) == false) {
				// not same as previous session = new session
				if ("1".equals(mSessionId) == false) {
					eventManager.fire(new NewSessionStarted() );
				}
				mSessionId = reply.sessionId;
			}
		}
		else {
			// no session support - every reply starts a new session
			resetSession();
		}
	}
	
	
	// Handle the results from the speech recognition activity
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == SpeechRecognitionEva.VOICE_RECOGNITION_REQUEST_CODE_EVA && resultCode == RESULT_OK) {
			Log.i(TAG, "speech recognition activity result "+resultCode);
			Bundle bundle = data.getExtras();
			
			String result = bundle.getString("EVA_REPLY");
			
			EvaApiReply apiReply = new EvaApiReply(result);		
			
			onEvaReply(apiReply, "voice");		
		}

		super.onActivityResult(requestCode, resultCode, data);
	}
	

	@Override
	protected void onCreate(Bundle arg0) {
	
		if (mTts == null)
			mTtsConfigured = false;

		mTts = new TextToSpeech(this, this);
		mSpeechRecognition = new SpeechRecognitionEva(this);
		
		super.onCreate(arg0);
	}
	
	public void setPrefredLanguage(String preffredLanguage)
	{
		mPreferedLanguage = preffredLanguage;
	}
	
	public void searchWithVoice()
	{
		// stop the TTS speech - so that we don't record the generated speech
		if (mTts != null) {
			mTts.stop();
		}
		
		Log.i(TAG, "search with voice starting, lang="+mPreferedLanguage);
		mSpeechRecognition.startVoiceRecognitionActivity(mPreferedLanguage, mSessionId);
		mLastLanguageUsed = mPreferedLanguage;
	}

	public void searchWithText(String searchString) {
		Log.i(TAG, "search with text starting, lang="+mLastLanguageUsed);
		EvaCallerTask callerTask = injector.getInstance(EvaCallerTask.class);
		callerTask.initialize(this, mSessionId, mLastLanguageUsed, searchString, -1, null);
		callerTask.execute();
	}
	
	public void replyToDialog(int replyIndex) {
		Log.i(TAG, "replying to dialog: "+replyIndex);
		EvaCallerTask callerTask = injector.getInstance(EvaCallerTask.class);
		callerTask.initialize(this, mSessionId, mLastLanguageUsed, null, replyIndex, null);
		callerTask.execute();
	}
	
	public boolean isNewSession() {
		return "1".equals(mSessionId);
	}
	
	public void resetSession() {
		mSessionId = "1";
		eventManager.fire(new NewSessionStarted() );
	}

}
