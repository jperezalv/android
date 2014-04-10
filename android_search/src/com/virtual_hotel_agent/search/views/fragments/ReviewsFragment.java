package com.virtual_hotel_agent.search.views.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.evature.util.Log;
import com.virtual_hotel_agent.search.R;

import roboguice.fragment.RoboFragment;

public class ReviewsFragment extends RoboFragment {

	private static final String TAG = "ReviewsFragment";
	private View mView;
	private WebView mWebView;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		if (mView != null) {
			((ViewGroup) mView.getParent()).removeView(mView);
			Log.w(TAG, "Fragment create view twice");
			return mView;
		}

		mView = inflater.inflate(R.layout.fragment_reviews, container, false);
		
		mWebView = (WebView) mView.findViewById(R.id.reviews_webview); 
				
		return mView;
	}

	public void hotelChanged(long hotelId) {
		mWebView.loadUrl("http://www.tripadvisor.com/WidgetEmbed-cdspropertydetail?locationId="+
						hotelId+
						"&partnerId=AF104870A8534DDAA5C4875420B5AE5F&display=true");
	}

}
