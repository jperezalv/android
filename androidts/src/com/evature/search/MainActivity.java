/*
 * Copyright (c) 2012 Evature.com
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to 
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:  
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.  
 *  
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 */

package com.evature.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.evaapis.EvaAPIs;
import com.evaapis.EvaApiReply;
import com.evaapis.EvaBaseActivity;
import com.evaapis.EvatureLocationUpdater;
import com.evaapis.SpeechRecognition;
import com.evature.util.ExternalIpAddressGetter;

public class MainActivity extends EvaBaseActivity implements TextToSpeech.OnInitListener, EvaDownloaderTaskInterface {

	private static final String TAG = MainActivity.class.getSimpleName();
	// private static String mExternalIpAddress = null;
	
	private static boolean mSpeechToTextWasConfigured = false;
	private List<String> mTabTitles;
	private SwipeyTabs mTabs; // The main swipey tabs element and the main view pager element:
	private ViewPager mViewPager; // see http://blog.peterkuterna.net/2011/09/viewpager-meets-swipey-tabs.html
	SearchVayantTask mSearchVayantTask;
	SearchTravelportTask mSearchTravelportTask;
	SwipeyTabsPagerAdapter mSwipeyAdapter;
	HotelListDownloaderTask mSearchExpediaTask;
		
	private ExternalIpAddressGetter mExternalIpAddressGetter;
	private boolean mIsNetworkingOk = false;

	static EvaHotelDownloaderTask mHotelDownloader = null;

	@Override
	public void onCreate(Bundle savedInstanceState) { // Called when the activity is first created.
		Log.d(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.new_main);
		EvatureLocationUpdater.initContext(this.getApplicationContext());
		mViewPager = (ViewPager) findViewById(R.id.viewpager);
		mTabs = (SwipeyTabs) findViewById(R.id.swipeytabs);
		if (savedInstanceState != null) { // Restore state
			// Same code as onRestoreInstanceState() ?
			mTabTitles = savedInstanceState.getStringArrayList("mTabTitles");
		} else {
			mTabTitles = new ArrayList<String>(Arrays.asList("CHAT"));
		}
		mSwipeyAdapter = new SwipeyTabsPagerAdapter(this, getSupportFragmentManager(), mViewPager, mTabs);
		mViewPager.setAdapter(mSwipeyAdapter);
		mTabs.setAdapter(mSwipeyAdapter);
		mViewPager.setOnPageChangeListener(mTabs); // To sync the tabs with the viewpager
		mViewPager.setCurrentItem(0);
		// Initialize text-to-speech. This is an asynchronous operation.
		// The OnInitListener (of the second argument) is called after initialization completes.
	
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		if (networkInfo != null && networkInfo.isConnected()) {
			Log.d(TAG, "Progress: We are connected to the network");
			mIsNetworkingOk = true;
			// fetch data
			// new GetExternalIpAddress().execute();
		}
		if (!mIsNetworkingOk) {
			fatal_error(R.string.network_error);
		}
		//mExternalIpAddressGetter = new ExternalIpAddressGetter();
		EvaAPIs.start();

		// patch for debug - bypass the speech recognition:
		// Intent data = new Intent();
		// Bundle a_bundle = new Bundle();
		// ArrayList<String> sentences = new ArrayList<String>();
		// sentences.add("3 star hotel in rome");
		// a_bundle.putStringArrayList(RecognizerIntent.EXTRA_RESULTS, sentences);
		// data.putExtras(a_bundle);
		// onActivityResult(VOICE_RECOGNITION_REQUEST_CODE, RESULT_OK, data);

	}

	// Using FragmentStatePagerAdapter to overcome bug: http://code.google.com/p/android/issues/detail?id=19001
	// This is an uglier approach I did not use: http://stackoverflow.com/a/7287121/78234
	private class SwipeyTabsPagerAdapter extends FragmentStatePagerAdapter implements SwipeyTabsAdapter,
			ViewPager.OnPageChangeListener {
		// Nicer example: http://developer.android.com/reference/android/support/v4/view/ViewPager.html
		private final Context mContext;
		private final ViewPager mViewPager;
		private final String TAG = SwipeyTabsPagerAdapter.class.getSimpleName();

		public SwipeyTabsPagerAdapter(Context context, FragmentManager fm, ViewPager pager, SwipeyTabs tabs) {
			super(fm);
			Log.i(TAG, "CTOR");
			mViewPager = pager;
			mViewPager.setAdapter(this);
			mViewPager.setOnPageChangeListener(this);
			this.mContext = context;
		}

		@Override
		public Fragment getItem(int position) {// Asks for the main fragment
			Log.i(TAG, "getItem " + String.valueOf(position));
			int size = mTabTitles.size();
			if (position < size && mTabTitles.get(position).equals(getString(R.string.CHAT))) { // Main Chat window
				return ChatFragment.newInstance();
			}
			if (position < size && mTabTitles.get(position).equals(getString(R.string.HOTELS))) { // Hotel list window
				return HotelsFragment.newInstance();
			}
			
			if (position < size && mTabTitles.get(position).equals(getString(R.string.HOTELS_MAP))) { // Hotel list window
				Fragment fragment=HotelsMapFragment.newInstance();
				return fragment;
			}
			
			if (position < size && mTabTitles.get(position).equals(getString(R.string.FLIGHTS))) { // flights list
				return FlightsFragment.newInstance();
			}
			if (position < size && mTabTitles.get(position).equals(getString(R.string.HOTEL))) { // Single hotel
				int hotelIndex = MyApplication.getDb().getHotelId();
				Log.i(TAG, "starting hotel fragment for hotel # " + hotelIndex);
				return HotelFragment.newInstance(hotelIndex);
			} else { // (position == TRAINS_POSITION) trains list window
				return TrainsFragment.newInstance();
			}
		}

		@Override
		public int getCount() {
			int count = mTabTitles.size();
			// Log.i(TAG, "getCount() " + String.valueOf(count));
			return count;
		}

		public TextView getTab(final int position, SwipeyTabs root) { // asks for just the tab part
			Log.i(TAG, "getTab() " + String.valueOf(position));
			TextView view = (TextView) LayoutInflater.from(mContext)
					.inflate(R.layout.swipey_tab_indicator, root, false);
			view.setText(mTabTitles.get(position));
			view.setOnClickListener(new OnClickListener() { // You can swipe AND click on a specific tab
				public void onClick(View v) {
					mViewPager.setCurrentItem(position);
				}
			});
			return view;
		}

		@Override
		public void onPageScrollStateChanged(int arg0) {
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
		}

		@Override
		public void onPageSelected(int arg0) {
		}

		@Override
		public void notifyDataSetChanged() {
			Log.i(TAG, "notifyDataSetChanged()");
			mTabs.setAdapter(this);
			super.notifyDataSetChanged();
		}

		// Internal helper function
		public void stuffChanged(int position) {
			mTabs.setAdapter(mSwipeyAdapter);
			mViewPager.setAdapter(mSwipeyAdapter); // I crashed here once ?! java.lang.IllegalStateException: Fragment
													// ChatFragment{41ac6dd0} is not currently in the FragmentManager
			this.notifyDataSetChanged();
			mTabs.onPageSelected(position);
			mViewPager.setCurrentItem(position);
		}

		public void addTab(String name) { // Dynamic tabs add to end
			int position = mViewPager.getCurrentItem();
			mTabs.setAdapter(null);
			mTabTitles.add(name);
			stuffChanged(position);
		}

		public void removeTab() { // Dynamic tabs remove from end
			int position = mViewPager.getCurrentItem();
			int size = mTabTitles.size();
			if (size > 0) { // fast clicking on remove gets us here...
				if (position == size - 1) { // We are at the last tab
					position = position - 1; // Move to the NEW last tab
				}
				mTabs.setAdapter(null);
				mTabTitles.remove(size - 1);
				stuffChanged(position);
			}
		}
		
		public void removeTab(int tabIndex)
		{
				mTabTitles.remove(tabIndex);
				
				if(tabIndex!=0)
				{
					stuffChanged(tabIndex-1);
				}
				else
				{
					stuffChanged(tabIndex);
				}
			}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mainmenu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) { // user pressed the menu button
		switch (item.getItemId()) {
		case R.id.settings: // Did the user select "settings"?
			Intent intent = new Intent();
			// Then set the activity class that needs to be launched/started.
			intent.setClass(this, MyPreferences.class);
			Bundle a_bundle = new Bundle(); // Lets send some data to the preferences activity
		//	a_bundle.putStringArrayList("mLanguages", (ArrayList<String>) mSpeechRecognition.getmGoogleLanguages());
			intent.putExtras(a_bundle);
			startActivity(intent); // start the activity by calling
			return true;
		case R.id.about: // Did the user select "About us"?
			// Links in alertDialog:
			// http://stackoverflow.com/questions/1997328/android-clickable-hyperlinks-in-alertdialog
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(getString(R.string.app_name));
			final TextView message = new TextView(this);
			final SpannableString s = new SpannableString(this.getText(R.string.lots_of_text));
			Linkify.addLinks(s, Linkify.WEB_URLS);
			message.setText(s);
			message.setMovementMethod(LinkMovementMethod.getInstance());
			message.setPadding(10, 10, 10, 10);
			builder.setView(message);
			builder.setPositiveButton(getString(R.string.ok_button), null);
			builder.setCancelable(false); // Can you just press back and dismiss it?
			builder.create().show();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void onSaveInstanceState(Bundle savedInstanceState) {
		// Save UI state changes to the savedInstanceState.
		// This bundle will be passed to onCreate if the process is killed and restarted.
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putBoolean("mTtsWasConfigured", mSpeechToTextWasConfigured);
		// savedInstanceState.putString("mExternalIpAddress", mExternalIpAddress);
		savedInstanceState.putStringArrayList("mTabTitles", (ArrayList<String>) mTabTitles);
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		// Restore UI state from the savedInstanceState.
		// This bundle has also been passed to onCreate.
		// restore state:
		// mExternalIpAddress = savedInstanceState.getString("mExternalIpAddress");
		mSpeechToTextWasConfigured = savedInstanceState.getBoolean("mTtsWasConfigured");
	}

	@Override
	public void onSpeechRecognitionResults(Bundle bundle) {		
		super.onSpeechRecognitionResults(bundle);
		
		ArrayList<String> matches =bundle.getStringArrayList(RecognizerIntent.EXTRA_RESULTS);
				
		String chatTabName = getString(R.string.CHAT);
		int index = mTabTitles.indexOf(chatTabName);
		if (index == -1) {
			mSwipeyAdapter.addTab(chatTabName);
			index = mTabTitles.size() - 1;
		}
		SwipeyTabsPagerAdapter adapter = (SwipeyTabsPagerAdapter) mViewPager.getAdapter();
		// http://stackoverflow.com/a/8886019/78234
		ChatFragment fragment = (ChatFragment) adapter.instantiateItem(mViewPager, index);
		if (fragment != null) // could be null if not instantiated yet
		{
			if (fragment.getView() != null) {
				ListView chatListView = fragment.mChatListView;
				ChatAdapter chatAdapter = (ChatAdapter) chatListView.getAdapter();
				chatAdapter.add(new ChatItem(matches.get(0), false));
			} else {
				Log.e(TAG, "chat fragment.getView() == null!?! [from voice]");
			}
		} else {
			Log.e(TAG, "chat fragment == null!?! [from voice]");
		}

	}
	

	protected void handleSayIt(EvaApiReply apiReply) {
		handleChat(apiReply);
		if (apiReply.sayIt != null) {
			String say_it = apiReply.sayIt;
			if (say_it != null && !say_it.isEmpty() && !say_it.trim().isEmpty()) {
				// say_it = "Searching for a " + say_it; Need to create an international version of this...
				ChatItem chatItem = new ChatItem(say_it, true);
				String chatTabName = getString(R.string.CHAT);
				int index = mTabTitles.indexOf(chatTabName);
				if (index == -1) {
					mSwipeyAdapter.addTab(chatTabName);
					index = mTabTitles.size() - 1;
				}
				// http://stackoverflow.com/a/8886019/78234
				SwipeyTabsPagerAdapter adapter = (SwipeyTabsPagerAdapter) mViewPager.getAdapter();
				ChatFragment fragment = (ChatFragment) adapter.instantiateItem(mViewPager, index);
				if (fragment != null) // could be null if not instantiated yet
				{
					if (fragment.getView() != null) {
						ListView chatListView = fragment.mChatListView;
						ChatAdapter chatAdapter = (ChatAdapter) chatListView.getAdapter();
						chatAdapter.add(chatItem);
					} else {
						Log.e(TAG, "chat fragment.getView() == null!?! [for sayit]");
					}
				} else {
					Log.e(TAG, "chat fragment == null!?! [for sayit]");
				}
				speak(say_it);
			}
		}
	}

	private void handleChat(EvaApiReply apiReply) {
		if (!apiReply.isFlightSearch() && !apiReply.isHotelSearch() && (apiReply.chat != null)) {
			if (apiReply.chat.hello != null && apiReply.chat.hello) {
				apiReply.sayIt = "Why, Hello there!";
			}
			if (apiReply.chat.who != null && apiReply.chat.who) {
				apiReply.sayIt = "I'm Eva, your travel search assistant";
			}
			if (apiReply.chat.meaningOfLife != null && apiReply.chat.meaningOfLife) {
				apiReply.sayIt = "Disrupting travel search, of course!";
			}
		}
	}


	public void setVayantReply() {
		String tabName = getString(R.string.FLIGHTS);
		int index = mTabTitles.indexOf(tabName);
		if (index == -1) {
			mSwipeyAdapter.addTab(tabName);
			index = mTabTitles.size() - 1;
		}
		// get the fragment: http://stackoverflow.com/a/7393477/78234
		String tag = "android:switcher:" + R.id.viewpager + ":" + index; // wtf...
		FlightsFragment fragment = (FlightsFragment) getSupportFragmentManager().findFragmentByTag(tag);
		if (fragment != null) // could be null if not instantiated yet
		{
			fragment.mAdapter.notifyDataSetChanged();
		} else {
			Log.e(TAG, "Flights fragment == null!?!");
		}
		
		mViewPager.setCurrentItem(index);
	}

	public void setTravelportReply(boolean train) {
		// get the fragment: http://stackoverflow.com/a/7393477/78234
		int string_id = train ? R.string.TRAINS : R.string.FLIGHTS;
		String tabName = getString(string_id);
		int index = mTabTitles.indexOf(tabName);
		if (index == -1) {
			mSwipeyAdapter.addTab(tabName);
			index = mTabTitles.size() - 1;
		}
		mViewPager.setCurrentItem(index);
		String tag = "android:switcher:" + R.id.viewpager + ":" + index; // wtf...
		if (train) {
			TrainsFragment fragment = (TrainsFragment) getSupportFragmentManager().findFragmentByTag(tag);
			if (fragment != null) // could be null if not instantiated yet
			{
				TrainListAdapter adapter = fragment.mAdapter;
				if (adapter != null) {
					adapter.notifyDataSetChanged();
					Log.d(TAG, "TrainListAdapter notifyDataSetChanged()");
				}
				// fragment.updateDisplay();
			} else {
				Log.e(TAG, "Trains fragment == null!?!");
			}
		} else {
			FlightsFragment fragment = (FlightsFragment) getSupportFragmentManager().findFragmentByTag(tag);
			if (fragment != null) // could be null if not instantiated yet
			{
				// fragment.updateDisplay();
				FlightListAdapterTP adapter = fragment.mAdapter;
				if (adapter != null) {
					adapter.notifyDataSetChanged();
					Log.d(TAG, "Flights fragment adapter notifyDataSetChanged()");
				} else {
					Log.e(TAG, "Flights fragment adapter == null!?!");
				}
			} else {
				Log.e(TAG, "Flights fragment == null!?!");
			}
		}
	}

	
	private void fatal_error(final int string_id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.fatal_error));
		builder.setMessage(string_id);
		builder.setPositiveButton(getString(R.string.ok_button), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				// if this button is clicked, close current activity (exit the app).
				// Test1Activity.this.finish();
			}
		});
		builder.setCancelable(false); // Can you just press back and dismiss it?
		builder.create().show();
	}

	int mMapTabIndex=-1;
	int mHotelTabIndex=-1;
	
	@Override
	public void endProgressDialog(int id) { // we got the hotel search reply successfully
		Log.d(TAG, "endProgressDialog() for id " + id);

		if (id == R.string.HOTEL && mHotelDownloader != null) {
			int hotelIndex = mHotelDownloader.getHotelIndex();
			Log.d(TAG, "endProgressDialog() Hotel # " + hotelIndex);
			MyApplication.getDb().setHotelId(hotelIndex);
		}

		String tabName = getString(id); // Yeah, I'm using the string ID for distinguishing between downloader tasks
		
		int index = mTabTitles.indexOf(tabName);
		if (index == -1) {
			mSwipeyAdapter.addTab(tabName);
			index = mTabTitles.size() - 1;
			
			if(tabName.equals("HOTELS"))
			{
				mSwipeyAdapter.addTab("MAP");
				mMapTabIndex = mTabTitles.size() - 1;
			}
		} else if (id == R.string.HOTEL) {
			mSwipeyAdapter.removeTab();
			mSwipeyAdapter.addTab(tabName);
			// HotelFragment fragment = (HotelFragment) adapter.instantiateItem(mViewPager, index);
			// fragment.mAdapter.notifyDataSetChanged();
			// I need to invalidate the entire view somehow!!!
			mSwipeyAdapter.stuffChanged(index);
			mHotelTabIndex = mTabTitles.size() - 1;
		}
		SwipeyTabsPagerAdapter adapter = (SwipeyTabsPagerAdapter) mViewPager.getAdapter();
		if (id == R.string.HOTELS) {
			HotelsFragment fragment = (HotelsFragment) adapter.instantiateItem(mViewPager, index);
			fragment.mAdapter.notifyDataSetChanged();
			HotelsMapFragment mapFragment = (HotelsMapFragment) adapter.instantiateItem(mViewPager, index+1);
			
			if(mHotelTabIndex!=-1)
			{
				mSwipeyAdapter.removeTab(mHotelTabIndex);
				mHotelTabIndex=-1;
			}
		}

		mViewPager.setCurrentItem(index);

		// mAdapter = new HotelListAdapter(mHotelsFragment, MyApplication.getDb());

		// if (mEnabledPaging && mFooterView != null)
		// mHotelListView.removeFooterView(mFooterView);
		//
		// mEnabledPaging = false;
		// mPaging = false;
		//
		// if (EvaSearchApplication.getDb().mMoreResultsAvailable) {
		// LayoutInflater li = getActivity().getLayoutInflater();
		// hideKeyboard();
		// mFooterView = (LinearLayout) li.inflate(R.layout.listfoot, null);
		// mHotelListView.addFooterView(mFooterView);
		// mHotelListView.setOnScrollListener(mListScroll);
		// mEnabledPaging = true;
		// }

		// mHotelListView.setAdapter(mAdapter);

	}

	@Override
	public void startProgressDialog(int id) {
		// TODO Auto-generated method stub

	}

	@Override
	public void endProgressDialogWithError(int id) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateProgress(int id, int mProgress) {
		// TODO Auto-generated method stub

	}

	// search button click handler ("On Click property" of the button in the xml)
	// http://stackoverflow.com/questions/6091194/how-to-handle-button-clicks-using-the-xml-onclick-within-fragments
	public void myClickHandler(View view) {
		switch (view.getId()) {
		case R.id.search_button:
		    MainActivity.this.searchWithVoice(getCurrentSpeechMethod());
			break;
		}
	}



	@Override
	protected void onPause() {
		EvatureLocationUpdater location;
		try {
			location = EvatureLocationUpdater.getInstance();
			location.stopGPS();
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.onPause();
	}

	public void showHotelDetails(int hotelIndex) {
		Log.d(TAG, "showHotelDetails()");
		if (MyApplication.getDb() == null) {
			Log.w(TAG, "MyApplication.getDb() == null");
			return;
		}

		if (mHotelDownloader != null) {
			if (false == mHotelDownloader.cancel(true)) {
				Log.d(TAG, "false == mHotelDownloader.cancel(true)");
				// return;
			}
		}

		mHotelDownloader = new EvaHotelDownloaderTask(this, hotelIndex);

		mHotelDownloader.execute();

	}

	

	@Override
	public void onEvaReply(EvaApiReply reply) {
		handleSayIt(reply); // Say (using TTS) the eva reply
		if (reply.isHotelSearch()) {
			Log.d(TAG, "Running Hotel Search!");
			mSearchExpediaTask = new HotelListDownloaderTask(this, reply, "$");
			mSearchExpediaTask.execute();
		} else {
			// if (apiReply.isFlightSearch()) {
			// Log.d(TAG, "Running Flight Search!");
			// mSearchVayantTask = new SearchVayantTask(mContext, apiReply);
			// mSearchVayantTask.execute();
			// }
			if (reply.isTrainSearch() || reply.isFlightSearch()) {
				Log.d(TAG, "Running Travelport Search!");
				mSearchTravelportTask = new SearchTravelportTask(this, reply);
				mSearchTravelportTask.execute();
			}
		}
	}

	public int getCurrentSpeechMethod() {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		String currentMethod = sp.getString("engine", "Eva");
		int returnValue = SpeechRecognition.SPEECH_RECOGNITION_EVA;
		
		if(currentMethod.toLowerCase().equals("eva"))
		{
			returnValue = SpeechRecognition.SPEECH_RECOGNITION_EVA;
		}
		
		if(currentMethod.toLowerCase().equals("nuance"))
		{
			returnValue = SpeechRecognition.SPEECH_RECOGNITION_NUANCE;
		}
		
		if(currentMethod.toLowerCase().equals("google"))
		{
			returnValue = SpeechRecognition.SPEECH_RECOGNITION_GOOGLE;
		}
		
		return returnValue;
	}

	
}
// TODO: I took the microphone icon from: http://www.iconarchive.com/show/atrous-icons-by-iconleak/microphone-icon.html
// Need to add attribution in the about text.
// TODO: refactor classes out of this mess.
// TODO: progress bar or spinning wheel when contacting Eva?
// TODO: very cool: http://code.google.com/p/google-gson/
// Gson is a Java library that can be used to convert Java Objects into their JSON representation. It can also be used
// to convert a JSON string to an equivalent Java object. Gson can work with arbitrary Java objects including
// pre-existing objects that you do not have source-code of.
/*
 * 
 * Example of google speech recognition:
 * http://developer.android.com/resources/samples/ApiDemos/src/com/example/android/apis/app/VoiceRecognition.html
 * Connecting to the network: http://developer.android.com/training/basics/network-ops/connecting.html
 */
// TODO: chat list should be from top of the screen

// Translate: Bing primary key = "uMLqpa+YkdRvJHukpbt06yQNa+ozPiGwrKSwnvjBYh4="

// I think a crash is caused if I start a speech recognition activity and I rotate the screen.
// The result is delivered to a dead activity and I get 