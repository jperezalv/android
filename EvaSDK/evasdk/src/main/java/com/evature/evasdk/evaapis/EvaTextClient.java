package com.evature.evasdk.evaapis;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;
import android.os.AsyncTask;

import com.evature.evasdk.R;
import com.evature.evasdk.evaapis.crossplatform.EvaApiReply;
import com.evature.evasdk.util.DLog;
import com.evature.evasdk.util.DownloadUrl;


    //Uses AsyncTask to create a task away from the main UI thread. This task takes a
	// URL string and uses it to create an HttpUrlConnection. Once the connection
	// has been established, the AsyncTask downloads the contents of the webpage as
	// an InputStream. Finally, the InputStream is converted into a string,
	// which is displayed in the UI by the AsyncTask's onPostExecute method.
	public class EvaTextClient extends AsyncTask<Void, Integer, EvaApiReply> {


		private static final String TAG = "EvaCallerTask";
		
		private final EvaComponent mEva;
		private final Object mCookie;  
		private final String mInputText;
		private final ArrayList<String> mNBest;
		private int mResponseId;
		private long startOfTextSearch;
		private boolean mEditLastUtterance;

		private String recordingKey;

		private boolean mFromSpeech;
		
		/****
		 * 
		 * @param inputText - text to send to Eva,   ignored if null
		 * @param responseId - response to send to Eva, ignored if -1
		 * @param cookie   - will be returned to the listener on callback
		 */
		public EvaTextClient( final EvaComponent eva,
								final String inputText, 
								final int responseId, 
								final Object cookie, 
								final boolean editLastUtterance) {
			mResponseId = responseId;
			mEva = eva;
			mInputText = inputText;
			mNBest = null;
			mCookie = cookie;
			mFromSpeech = false;
			mEditLastUtterance = editLastUtterance;
		}
		
		/****
		 * 
		 * @param cookie   - will be returned to the listener on callback
		 */
		public EvaTextClient(final EvaComponent eva,
								final ArrayList<String> nBestText, 
								final Object cookie,
								final boolean editLastUtterance) {
			mResponseId = -1;
			mEva = eva;
			mNBest = nBestText;
			mFromSpeech = true; // hackish..
			mInputText = null;
			mCookie = cookie;
			mEditLastUtterance = editLastUtterance;
		}

		@Override
		protected EvaApiReply doInBackground(Void... non) {
			startOfTextSearch = System.nanoTime();
			String evatureUrl = mEva.mConfig.vproxyHost;
			

			evatureUrl += "/"+mEva.mConfig.apiVersion+"?";
			evatureUrl += ("site_code=" + mEva.getSiteCode());
			evatureUrl += ("&api_key=" + mEva.getApiKey());
			//evatureUrl += ("&language=" + mLanguage);
			evatureUrl += ("&session_id=" + mEva.getSessionId());
			evatureUrl += ("&sdk_version="+EvaComponent.SDK_VERSION);
			evatureUrl += "&ffi_chains=true&ffi_statement=true";
			evatureUrl += "&from_speech="+(mFromSpeech ? "true" : "false");
			String language = mEva.getPreferedLanguage();
			if (language != null && !"".equals(language)) {
				evatureUrl += "&language="+language.replaceAll("-.*$", "");
			}
			if (mEva.getLocale() != null) {
				evatureUrl += ("&locale="+ mEva.getLocale());
			}
			else {
				Locale currentLocale = Locale.getDefault();
				evatureUrl += "&locale="+ currentLocale.getCountry(); 
			}
			
			
			if (mEva.getDeviceId() != null) {
				evatureUrl += "&uid="+mEva.getDeviceId();
			}
			if (mEva.getContext() != null) {
				evatureUrl += "&context="+mEva.getContext();
			}
			if (mEva.getScope() != null) {
				evatureUrl += "&scope="+mEva.getScope();
			}
			if (mResponseId != -1) {
				evatureUrl += ("&dialog_response="+mResponseId);
			}
			if (mEva.mConfig.appVersion != null) {
				evatureUrl += "&app_version="+ mEva.mConfig.appVersion;
			}
            if (mEva.getAutoOpenMicrophone()) {
                evatureUrl += "&auto_open_mic=1";
            }
            if (mEva.semanticHighlightingEnabled()) {
                evatureUrl += "&add_text=1"; // ask Eva to reply with Semantic highlighting meta data
            }


            try {
				evatureUrl += "&time_zone="+URLEncoder.encode((""+TimeZone.getDefault().getRawOffset()/3600000.0).replaceFirst("\\.0+$",  ""), "UTF-8");
				evatureUrl += "&android_ver="+URLEncoder.encode(String.valueOf(android.os.Build.VERSION.RELEASE), "UTF-8");
				evatureUrl += "&device="+URLEncoder.encode(android.os.Build.MODEL, "UTF-8");
				
				HashMap<String, String> extraParams = mEva.getExtraParams();
				for (String key : extraParams.keySet()) {
					String val = extraParams.get(key);
					if (val != null)
						evatureUrl += "&"+key+"="+URLEncoder.encode(val, "UTF-8");
				}
			} catch (UnsupportedEncodingException e) {
				DLog.e(TAG, "UnsupportedEncodingException", e);
			}
			
			if (mNBest != null) {
				for (String input: mNBest) {
					try {
						evatureUrl += ("&input_text=" + URLEncoder.encode(input, "UTF-8"));
					} catch (UnsupportedEncodingException e) {
						DLog.e(TAG, "UnsupportedEncodingException", e);
					}
				}
			}
			if (mInputText != null) {
				try {
                    evatureUrl += ("&input_text=" + URLEncoder.encode(mInputText, "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					DLog.e(TAG, "UnsupportedEncodingException", e); 
				}
			}
//			String externalIpAddress = ExternalIpAddressGetter.getExternalIpAddr();
//			if (externalIpAddress != null) {
//				evatureUrl += ("&ip_addr=" + externalIpAddress);
//			}
            try {
                Location location = mEva.getLocation();
                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    if (latitude != EvaLocationUpdater.NO_LOCATION) {
                        evatureUrl += ("&longitude=" + longitude + "&latitude=" + latitude);
                    }
                }
            } catch (Exception e1) {
                DLog.e(TAG, "Exception setting location", e1);
            }

            if (mEditLastUtterance) {
				evatureUrl += "&edit_last_utterance=true";
			}
			if (recordingKey != null) {
				evatureUrl += "&recording_key="+recordingKey;
			}
			
            DLog.i(TAG, "<< Sending Eva URL = " + evatureUrl);
            try {
                String result = DownloadUrl.get(evatureUrl);
                EvaApiReply apiReply = new EvaApiReply(result);
                DLog.d(TAG, "Got reply: "+apiReply.toString());
                return apiReply;
            } catch (IOException e) {
                DLog.w(TAG, "IOException in request to Evature: "+e.getMessage());
            }
			// had 3 IOExceptions in a row - give up
			EvaApiReply errorReply = new EvaApiReply("{\"status\": false }");
			errorReply.errorMessage = mEva.activity.getResources().getString(R.string.evature_network_error);
			return errorReply;

		}


		@Override
		protected void onPostExecute(EvaApiReply apiReply) { // onPostExecute displays the results of the AsyncTask.
			
			// coming from text search
			if (mEva.isDebug()) {
				JSONObject debugData = new JSONObject();
				try {
					if (apiReply.JSONReply != null) {
						debugData.put("Time in HTTP Execute", ((System.nanoTime() - startOfTextSearch)/1000000)+"ms");
						apiReply.JSONReply.put("debug", debugData);
					}
				} catch (JSONException e) {
					DLog.e(TAG, "Failed setting debug data", e);
				}
			}
						
			mEva.onEvaReply(apiReply, mCookie);
		}

		public void setRecordingKey(String recordingKey) {
			this.recordingKey= recordingKey;
		}
		
	}

