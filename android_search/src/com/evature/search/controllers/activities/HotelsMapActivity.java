package com.evature.search.controllers.activities;

import roboguice.activity.RoboFragmentActivity;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;

import com.evature.search.MyApplication;
import com.evature.search.R;
import com.evature.search.models.expedia.EvaXpediaDatabase;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.LatLngBounds.Builder;
import com.google.android.gms.maps.model.MarkerOptions;

public class HotelsMapActivity extends RoboFragmentActivity  {
	private final String TAG = "HotelsMapActivity";

	GoogleMap mMap = null;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.hotel_map);
	}

	
	private void setUpMapIfNeeded() {
		if (mMap == null) {
			SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
			mMap = mapFragment.getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
            	mapFragment.getView().post(new Runnable(){
					@Override
					public void run() {
						addHotelsToMap();
					}
            	});
            }
        }
	}


	@Override
    protected void onResume() {
        super.onResume();
        int errCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (errCode != ConnectionResult.SUCCESS) {
			Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(errCode, this, 0);
			errorDialog.show();
		}
		else {
			setUpMapIfNeeded();
		}
    }
	
	
	public void onHotelsListUpdated() {
		Log.i(TAG, "Updating map because hotels list was updated");
		mMap = null;
		setUpMapIfNeeded();
	}
	
	
	private void addHotelsToMap()
	{	    
       
        EvaXpediaDatabase evaDb = MyApplication.getDb();
        
        int length= (evaDb != null && evaDb.mHotelData != null) ? evaDb.mHotelData.length : 0;
        if (length == 0) {
        	HotelsMapActivity.this.finishActivity(0);
        	return;
        }
        if(length>30) 
        	length=30;
        
        BitmapDescriptor hotelIcon = BitmapDescriptorFactory.fromResource(R.drawable.hotel_small);
        Builder boundsBuilder = new LatLngBounds.Builder();
        
        for(int i=0;i<length;i++)
        {
	        String name = evaDb.mHotelData[i].mSummary.mName;
	        LatLng point = new LatLng(evaDb.mHotelData[i].mSummary.mLatitude, evaDb.mHotelData[i].mSummary.mLongitude);
	        mMap.addMarker(new MarkerOptions()
				            .position(point)
				            .title(name)
				            .icon(hotelIcon));
            
	        boundsBuilder.include(point);
        }
        
        LatLngBounds bounds = boundsBuilder.build();
        
        try{
        	//This line will cause the exception first times when map is still not "inflated"
        	mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
        	Log.i(TAG, "Camera moved successfully");
        } catch(IllegalStateException e) {
            Log.w(TAG, "Camera move exceptoin", e);
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds,400,400,10));
            Log.i(TAG, "Camera moved to hardcoded width height");
        }
	}

	
}
