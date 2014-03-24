package com.virtual_hotel_agent.search.controllers.web_services;

import org.json.JSONObject;

import android.content.Context;

import com.evaapis.crossplatform.EvaApiReply;
import com.evature.util.Log;
import com.google.inject.Inject;
import com.virtual_hotel_agent.search.MyApplication;
import com.virtual_hotel_agent.search.R;
import com.virtual_hotel_agent.search.controllers.activities.MainActivity;
import com.virtual_hotel_agent.search.controllers.web_services.DownloaderTaskListenerInterface.DownloaderStatus;
import com.virtual_hotel_agent.search.models.expedia.XpediaDatabase;
import com.virtual_hotel_agent.search.models.expedia.XpediaProtocol;

public class HotelListDownloaderTask extends DownloaderTask {

	private static final String TAG = HotelListDownloaderTask.class.getSimpleName();
	// String mSearchQuery;
	String mCurrencyCode;
	EvaApiReply apiReply;
	Context context;
	
	@Inject XpediaProtocol xpediaProtocol;

	public HotelListDownloaderTask() {
		super(R.string.HOTELS);
	}

	public void initialize(DownloaderTaskListenerInterface listener, Context context, EvaApiReply apiReply, String currencyCode) {
		Log.i(TAG, "CTOR");
		// mSearchQuery = searchQuery;
		this.apiReply = apiReply;
		attach(listener);
		this.context = context;
		mCurrencyCode = currencyCode;
	}

	void createHotelData(JSONObject hotelListResponseJSON) {

		if (hotelListResponseJSON == null) {
			return;
		}
		try {
//			MyApplication.getDb().EvaDatabaseUpdateExpedia(hotelListResponseJSON);
			XpediaDatabase db = new XpediaDatabase(hotelListResponseJSON);
			MyApplication.setDb(db);
			
			
//			if (MyApplication.getDb().mHotelData == null) {
//				return false;
//			}
//			else {
//				 MyApplication.setDb(db);
//			}
		} catch (NullPointerException e) {
			e.printStackTrace();
		}

	}

	@Override
	protected JSONObject doInBackground(Void... params) {

		Log.i(TAG, "doInBackground: start");
		// String searchQuery = EvaProtocol.getEvatureResponse(mQueryString);
		//mProgress = EvaDownloaderTaskInterface.PROGRESS_EXPEDIA_HOTEL_FETCH;
		publishProgress();
		Log.i(TAG, "doInBackground: Calling Expedia");
		JSONObject hotelListResponse = xpediaProtocol.getExpediaAnswer(context, apiReply, MyApplication.getExpediaAppState(), mCurrencyCode);
		if (hotelListResponse == null) {
			Log.w(TAG, "null hotelist response!");
		}
		//mProgress = EvaDownloaderTaskInterface.PROGRESS_CREATE_HOTEL_DATA;
		if (isCancelled()) {
			return null;
		}
		return hotelListResponse;
	}
	
	@Override
	protected void onPostExecute(JSONObject result) {
		if (result == null) {
			Log.i(TAG, "doInBackground: Error in Expedia response");
			mProgress = DownloaderStatus.FinishedWithError;
		}
		else {
			createHotelData(result);
			
			if (MyApplication.getDb().unrecoverableError) {
				mProgress = DownloaderStatus.FinishedWithError;
			}
			else {
				Log.i(TAG, "doInBackground: All OK");
				mProgress = DownloaderStatus.Finished;
				MyApplication.getExpediaAppState().setArrivalDate(apiReply.ean.get("arrivalDate"));
				MyApplication.getExpediaAppState().setDepartueDate(apiReply.ean.get("departureDate"));
				//MyApplication.getDb().setNumberOfAdults(1);
			}
			Log.i(TAG, "doInBackground: end");
		}	
		super.onPostExecute(result);
	}
}
