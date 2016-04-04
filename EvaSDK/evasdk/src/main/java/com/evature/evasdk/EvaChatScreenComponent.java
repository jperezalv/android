package com.evature.evasdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.SpannableString;
import android.text.SpannedString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.evature.evasdk.appinterface.AppScope;
import com.evature.evasdk.appinterface.AppSetup;
import com.evature.evasdk.appinterface.AsyncCountResult;
import com.evature.evasdk.appinterface.CarCount;
import com.evature.evasdk.appinterface.CarSearch;
import com.evature.evasdk.appinterface.CruiseCount;
import com.evature.evasdk.appinterface.CruiseSearch;
import com.evature.evasdk.appinterface.EvaLifeCycleListener;
import com.evature.evasdk.appinterface.FlightCount;
import com.evature.evasdk.appinterface.FlightSearch;
import com.evature.evasdk.appinterface.HotelCount;
import com.evature.evasdk.appinterface.HotelSearch;
import com.evature.evasdk.evaapis.EvaComponent;
import com.evature.evasdk.evaapis.EvaSearchReplyListener;
import com.evature.evasdk.evaapis.EvaSpeechRecogComponent;
import com.evature.evasdk.evaapis.crossplatform.EvaApiReply;
import com.evature.evasdk.evaapis.crossplatform.EvaLocation;
import com.evature.evasdk.evaapis.crossplatform.EvaTime;
import com.evature.evasdk.evaapis.crossplatform.EvaWarning;
import com.evature.evasdk.evaapis.crossplatform.FlightAttributes;
import com.evature.evasdk.evaapis.crossplatform.HotelAttributes;
import com.evature.evasdk.evaapis.crossplatform.ParsedText;
import com.evature.evasdk.evaapis.crossplatform.RequestAttributes;
import com.evature.evasdk.evaapis.crossplatform.ServiceAttributes;
import com.evature.evasdk.evaapis.crossplatform.flow.FlowElement;
import com.evature.evasdk.evaapis.crossplatform.flow.QuestionElement;
import com.evature.evasdk.evaapis.crossplatform.flow.ReplyElement;
import com.evature.evasdk.evaapis.crossplatform.flow.StatementElement;
import com.evature.evasdk.model.ChatItem;
import com.evature.evasdk.model.appmodel.AppCruiseSearchModel;
import com.evature.evasdk.model.appmodel.AppFlightSearchModel;
import com.evature.evasdk.model.appmodel.AppHotelSearchModel;
import com.evature.evasdk.user_interface.ChatAdapter;
import com.evature.evasdk.user_interface.ErrorSpan;
import com.evature.evasdk.util.DLog;
import com.evature.evasdk.util.VolumeUtil;

import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;


public class EvaChatScreenComponent implements EvaSearchReplyListener, VolumeUtil.VolumeListener {

	@SuppressWarnings("nls")
	private static final String TAG = "EvaChatScreenComponent";

    public static final String INTENT_EVA_CONTEXT = "evature_context";


    private static class StoreResultData {
		ChatItem storeResultInItem;
		boolean editLastUtterance;
		SpannableString preEditChat;
	}
	
	private static class DeleteChatItemsData {
		int dismissFrom;
		int dismissTo;
	}

	// Different requests to Eva all come back to the same callback (onEvaReply)
	// (eg text vs voice, add vs delete or replace)
	// the "cookie" parameter that you use for the request is returned untouched to the
	// callback, so you can differentiate between the different type of activation calls
	private static final StoreResultData VOICE_COOKIE = new StoreResultData();
	private static final StoreResultData TEXT_TYPED_COOKIE = new StoreResultData();
	private static final DeleteChatItemsData DELETE_UTTERANCE_COOKIE = new DeleteChatItemsData();
	
	// key values to save/restore activity state
	@SuppressWarnings("nls")
	private static final String EVATURE_CHAT_LIST = "evature.chat_list";
	@SuppressWarnings("nls")
	private static final String EVATURE_SESSION_ID = "evature.session_id";

    private static final String EMPTY_FRAGMENT_TAG = "evature_empty_fragment_tag";

	private EvaComponent eva;
	private EvaSpeechRecogComponent speechSearch;
	private EvatureMainView mView;

	private ToneGenerator toneGenerator;  // TODO: replace with custom sounds

    private static String evaSessionId = "1";
    private static ArrayList<ChatItem> chatItems = new ArrayList<ChatItem>();

    private boolean mUseExtraFragmentForBackHack = false; // fragments can't capture "back" button, so instead can use special hack of starting an extra fragment and checking the fragment manager backstack


    private boolean isPaused;
	private boolean mShownWarningsTutorial;
	private static class PendingSayIt {
		public ChatItem chatItem;
		public String sayIt;
		public boolean cancel;
		public boolean isQuestion;
		public SpannableString chatText;
	}
	private PendingSayIt pendingReplySayit = new PendingSayIt();

    private View rootView;

    private Activity activity;
    public Activity getActivity() {
        return activity;
    }



    public EvaChatScreenComponent(Activity hostActivity,
                                  boolean useExtraFragmentForBackHack
                                  ) {
        this.activity = hostActivity;
        this.mUseExtraFragmentForBackHack = useExtraFragmentForBackHack;
    }
    public EvaChatScreenComponent(Activity hostActivity) {
        this(hostActivity, false);
    }

	@SuppressLint("NewApi")
	protected void onCreate(Bundle savedInstanceState) {
        DLog.d(TAG, "onCreate");
		//super.onCreate(savedInstanceState);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            Window window = this.getWindow();
//            window.addFlags(Window.FEATURE_ACTIVITY_TRANSITIONS);
//            window.setSharedElementEnterTransition(new ChangeImageTransform());
//            window.setSharedElementExitTransition(new ChangeImageTransform());
//        }


		Intent theIntent = activity.getIntent();
		Bundle bundle = theIntent.getExtras();
		DLog.i(TAG, "Bundle: " + bundle);

		toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);

		EvaComponent.EvaConfig config = new EvaComponent.EvaConfig(AppSetup.apiKey, AppSetup.siteCode);
		// Override Eva's deviceId with ICR deviceId
		/*TODO String deviceId =  ICruiseUtility.getDeviceID(this);
		if (deviceId != null && deviceId.equals("") == false) {
			config.deviceId = deviceId;
		}*/
        if (AppSetup.deviceId != null) {
            config.deviceId =  AppSetup.deviceId;
        }
        config.appVersion = AppSetup.appVersion;

        if (AppSetup.scopeStr == null) {
            config.scope = inferScopeFromHandler();
        }
        else {
            config.scope = AppSetup.scopeStr;
        }
        config.context = theIntent.getStringExtra(INTENT_EVA_CONTEXT);
        config.extraParams = AppSetup.extraParams;

        config.locationEnabled = AppSetup.locationTracking;

        config.semanticHighlightingTimes = AppSetup.semanticHighlightingTimes;
        config.semanticHighlightingLocations = AppSetup.semanticHighlightingLocations;
        config.autoOpenMicrophone = AppSetup.autoOpenMicrophone;

		eva = new EvaComponent(activity, this, config);
		eva.onCreate(savedInstanceState);

		speechSearch = new EvaSpeechRecogComponent(eva);
		isPaused = false;
	}

    public List<String> getExamplesStrings() {

        String evaContext = eva.getContext() == null ? eva.getScope() : eva.getContext();

        List<String> examples = new ArrayList<String>();
        Resources resources = activity.getResources();
        if (evaContext.contains(AppScope.Flight.toString())) {
            String[] examplesArr = resources.getStringArray(R.array.evature_examples_flights);
            examples.addAll(Arrays.asList(examplesArr));
        }
        if (evaContext.contains(AppScope.Cruise.toString())) {
            String[] examplesArr = resources.getStringArray(R.array.evature_examples_cruises);
            examples.addAll(Arrays.asList(examplesArr));
        }
        if (evaContext.contains(AppScope.Car.toString())) {
            String[] examplesArr = resources.getStringArray(R.array.evature_examples_cars);
            examples.addAll(Arrays.asList(examplesArr));
        }
        if (evaContext.contains(AppScope.Hotel.toString())) {
            String[] examplesArr = resources.getStringArray(R.array.evature_examples_hotels);
            examples.addAll(Arrays.asList(examplesArr));
        }
        return examples;
    }

    private String getGreeting() {
        Resources resources = activity.getResources();
        String evaContext = eva.getContext() == null ? eva.getScope() : eva.getContext();

        if (evaContext.equals(AppScope.Flight.toString())) {
            return resources.getString(R.string.evature_greeting_flight);
        }
        if (evaContext.equals(AppScope.Cruise.toString())) {
            return resources.getString(R.string.evature_greeting_cruise);
        }
        if (evaContext.equals(AppScope.Car.toString())) {
            return resources.getString(R.string.evature_greeting_car);
        }
        if (evaContext.equals(AppScope.Hotel.toString())) {
            return resources.getString(R.string.evature_greeting_hotel);
        }

        return resources.getString(R.string.evature_greeting);
    }

    public View createMainView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        DLog.d(TAG, "createMainView");
        rootView = inflater.inflate(R.layout.evature_chat_layout, container, false);
        if (savedInstanceState != null) {
            chatItems = (ArrayList<ChatItem>) savedInstanceState.getSerializable(EVATURE_CHAT_LIST);
            evaSessionId = savedInstanceState.getString(EVATURE_SESSION_ID);
            eva.setSessionId(evaSessionId);
        }

        if (evaSessionId.equals("1")) {
            chatItems.clear();
            mView = new EvatureMainView(this, chatItems);
            Resources resources = activity.getResources();
            String greeting = getGreeting();
            int pos = greeting.length();
            String seeExamples = resources.getString(R.string.evature_tap_for_examples);
            SpannableString sgreet = new SpannableString(greeting + new SpannedString(seeExamples));
            int col = resources.getColor(R.color.evature_chat_secondary_text);
            sgreet.setSpan(new ForegroundColorSpan(col), pos, pos+seeExamples.length(), 0);
            sgreet.setSpan( new StyleSpan(Typeface.ITALIC), pos, pos+seeExamples.length(), 0);
            ChatItem chat = new ChatItem(sgreet,null, ChatItem.ChatType.EvaWelcome);
            mView.addChatItem(chat);
            speak(greeting, true, new Runnable() {

                @Override
                public void run() {
                    mView.flashSearchButton(5);
                    if (eva.getAutoOpenMicrophone()) {
                        voiceRecognitionSearch(null, false);
                    }
                }
            });
        }
        else {
            mView = new EvatureMainView(this, chatItems);
            eva.setSessionId(evaSessionId);
            mView.flashSearchButton(5);
            if (eva.getAutoOpenMicrophone()) {
                voiceRecognitionSearch(null, false);
            }
        }

        mView.setVolumeIcon();

        return rootView;
    }



    public View getRootView() {
        return rootView;
    }


    private final SimpleDateFormat evaDateFormat = new SimpleDateFormat("yyyy-M-dd", Locale.US);



    private void findSearchResults(final EvaApiReply reply, final FlowElement flow, final ChatItem chatItem) {

        EvaLocation from = null;
        EvaLocation to = null;
        FlowElement.TypeEnum  searchType = null;
        boolean isComplete = false;
        switch (flow.Type) {
            case Cruise:
            case Flight:
            case Car:
                if (flow.RelatedLocations.length < 2) {
                    DLog.w(TAG, flow.Type.toString()+" search without two locations?");
                    chatItem.setSearchModel(null);
                    return;
                }
                searchType = flow.Type;
                from = flow.RelatedLocations[0];
                to = flow.RelatedLocations[1];
                isComplete = true;
                break;

            case Hotel:
                if (flow.RelatedLocations.length < 1) {
                    DLog.w(TAG, flow.Type.toString()+" search without a location");
                    chatItem.setSearchModel(null);
                    return;
                }
                searchType = flow.Type;
                from = flow.RelatedLocations[0];
                isComplete = true;
                break;

            case Question:
                QuestionElement qe = (QuestionElement)flow;
                searchType = qe.actionType;
                // cruises have (for now) only origin and destination
                if (reply.locations.length > 0) {
                    from = reply.locations[0];
                }
                if (reply.locations.length > 1) {
                    to = reply.locations[1];
                }
                isComplete = false;
                break;
        }

        if (searchType == null) {
            return;
        }

        switch (searchType) {

            case Cruise:
                findCruiseResults(isComplete, from, to, reply, chatItem);
                break;

            case Flight:
                findFlightResults(isComplete, from, to, reply, chatItem);
                break;

            case Hotel:
                findHotelResults(isComplete, from, reply, chatItem);
                break;
        }
    }

    class ResultsCountHandler implements AsyncCountResult {
        private final String oneResultFound;
        private final String manyResultsFound;
        private final String noResultsFound;
        private final ChatItem chatItem;
        private final boolean isComplete;
        private final Context context;

        ResultsCountHandler(Context context, String oneResultFound, String manyResultsFound, String noResultsFound, boolean isComplete, ChatItem chatItem) {
            this.oneResultFound = oneResultFound;
            this.manyResultsFound = manyResultsFound;
            this.noResultsFound = noResultsFound;
            this.chatItem = chatItem;
            this.isComplete = isComplete;
            this.context = context;
        }

        @Override
        public void handleCountResult(final int count) {
            if (count < 0) {
                DLog.w(TAG, "No count response");
                chatItem.setSearchModel(null); // don't allow tapping to see empty search results
                chatItem.setSubLabel(null);
                chatItem.setStatus(ChatItem.Status.NONE);
                mView.notifyDataChanged();
            }
            else {
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        DLog.d(TAG, "Count result: " + count);
                        if (count == 0) {
                            boolean alreadySpoken = pendingReplySayit.cancel == true;

                            // attempt to cancel the pending sayit - no need to ask question or say the cruise if there are no results
                            pendingReplySayit.cancel = true;
                            synchronized (pendingReplySayit) {
                                pendingReplySayit.notifyAll();
                            }

                            // hide the searching sublabel
                            chatItem.setSubLabel(null);
                            chatItem.setStatus(ChatItem.Status.NONE);
                            if (alreadySpoken) {
                                ChatItem noResults = new ChatItem(noResultsFound, chatItem.getEvaReplyId(), ChatItem.ChatType.Eva);
                                mView.addChatItem(noResults);
                            } else {
                                chatItem.setChat(noResultsFound);
                                chatItem.setSearchModel(null); // don't allow tapping to see empty search results
                                mView.notifyDataChanged();
                            }
                            speak(noResultsFound, false);
                        } else {
                            // there are results - can go ahead and say the pending sayIt
                            synchronized (pendingReplySayit) {
                                pendingReplySayit.notifyAll();
                            }
                            if (count == 1) {
                                chatItem.setSubLabel(oneResultFound);
                            } else {
                                chatItem.setSubLabel(count + manyResultsFound);
                            }
                            chatItem.setStatus(ChatItem.Status.HAS_RESULTS);
                            if (isComplete || count == 1) {
                                // this is a final flow element, not a question, so trigger search
                                // alternatively, there is only one left - no need to ask more questions
                                chatItem.getSearchModel().setIsComplete(true);
                                chatItem.getSearchModel().triggerSearch(context);
                            }
                        }
                        mView.notifyDataChanged();
                    }
                });
                t.start();
            }
        }
    }

    private void findFlightResults(final boolean isComplete, final EvaLocation from, final EvaLocation to, final EvaApiReply reply, final ChatItem chatItem) {

        // TODO: we default to round trip - maybe should use AppSetup to decide if default to round trip or one way?
        boolean oneWay = reply.flightAttributes != null && reply.flightAttributes.oneWay != null && reply.flightAttributes.oneWay.booleanValue() == true;

        Date departDateMin = null;
        Date departDateMax = null;
        String departureStr = (from != null && from.Departure != null) ? from.Departure.Date : null;
        if (departureStr != null) {
            try {
                departDateMin = evaDateFormat.parse(departureStr);

                Integer days = from.Departure.daysDelta();
                if (days != null) {
                    Calendar c = Calendar.getInstance();
                    c.setTime(departDateMin); // Now use today date.
                    c.add(Calendar.DATE, days.intValue()); // Adding duration days
                    departDateMax = c.getTime();
                }
            } catch (ParseException e) {
                DLog.e(TAG, "Failed to parse eva departure date: " + departureStr);
                e.printStackTrace();
            }
        }

        Date returnDateMin = null;
        Date returnDateMax = null;

        if (!oneWay) {
            String returnStr = (to != null && to.Departure != null) ? to.Departure.Date : null;
            if (returnStr == null) {
                oneWay = true;
            }
            else {
                try {
                    returnDateMin = evaDateFormat.parse(returnStr);

                    Integer days = to.Departure.daysDelta();
                    if (days != null) {
                        Calendar c = Calendar.getInstance();
                        c.setTime(departDateMin);
                        c.add(Calendar.DATE, days.intValue()); // Adding duration days
                        returnDateMax = c.getTime();
                    }
                } catch (ParseException e) {
                    DLog.e(TAG, "Failed to parse eva departure date: " + departureStr);
                    e.printStackTrace();
                }
            }
        }


        final Context context = activity;
        RequestAttributes.SortEnum sortBy = null;
        RequestAttributes.SortOrderEnum sortOrder = null;
        if (reply.requestAttributes != null) {
            sortBy = reply.requestAttributes.sortBy;
            sortOrder = reply.requestAttributes.sortOrder;
        }

        final Boolean nonstop;
        final Boolean redeye;
        final String[] airlines;
        final FlightAttributes.FoodType food;
        final FlightAttributes.SeatType seatType;
        final FlightAttributes.SeatClass[] seatClass;

        if (reply.flightAttributes == null) {
            nonstop = null;
            redeye = null;
            airlines = null;
            food = null;
            seatType = null;
            seatClass = null;
        }
        else {
            FlightAttributes fa = reply.flightAttributes;
            nonstop = fa.nonstop;
            redeye = fa.redeye;
            airlines = fa.airlines;
            food = fa.food;
            seatType = fa.seatType;
            seatClass = fa.seatClass;
        }

        chatItem.setSearchModel(new AppFlightSearchModel(isComplete, from, to, departDateMin, departDateMax, returnDateMin, returnDateMax, reply.travelers,
                oneWay, nonstop, seatClass, airlines, redeye, food, seatType,
                sortBy, sortOrder));



        if (EvaComponent.evaAppHandler instanceof FlightCount) {
            chatItem.setStatus(ChatItem.Status.SEARCHING);
            chatItem.setSubLabel("Searching...");
            mView.notifyDataChanged();

            AsyncCountResult flightCountHandler = new ResultsCountHandler(context, "One flight found.\nTap here to see it.",
                    " flights found.\nTap here to see them.",
                    activity.getString(R.string.evature_zero_count),
                    isComplete,
                    chatItem
                    );

            if (oneWay) {
                ((FlightCount) EvaComponent.evaAppHandler).getOneWayFlightCount(context, isComplete, from, to,
                        departDateMin, departDateMax, returnDateMin, returnDateMax, reply.travelers,
                        nonstop, seatClass, airlines, redeye, food, seatType,
                        flightCountHandler);
            }
            else {
                ((FlightCount) EvaComponent.evaAppHandler).getRoundTripFlightCount(context, isComplete, from, to,
                        departDateMin, departDateMax, returnDateMin, returnDateMax, reply.travelers,
                        nonstop, seatClass, airlines, redeye, food, seatType,
                        flightCountHandler);
            }
        }
        else {
            // count is not supported - trigger search
            if (EvaComponent.evaAppHandler instanceof FlightSearch) {
                chatItem.getSearchModel().triggerSearch(context);
            } else {
                // TODO: insert new chat item saying the app doesn't support flight search?
                Log.e(TAG, "App reached flight search, but has no matching handler");
            }
        }
    }


    private void findHotelResults(boolean isComplete, EvaLocation location, EvaApiReply reply, ChatItem chatItem) {

        Date arriveDateMin = null;
        Date arriveDateMax = null;
        String arrivalStr = (location != null && location.Arrival != null) ? location.Arrival.Date : null;
        if (arrivalStr != null) {
            try {
                arriveDateMin = evaDateFormat.parse(arrivalStr);

                Integer days = location.Arrival.daysDelta();
                if (days != null) {
                    Calendar c = Calendar.getInstance();
                    c.setTime(arriveDateMin); // Now use today date.
                    c.add(Calendar.DATE, days.intValue()); // Adding duration days
                    arriveDateMax = c.getTime();
                }
            } catch (ParseException e) {
                DLog.e(TAG, "Failed to parse eva arrival date: " + arrivalStr);
                e.printStackTrace();
            }
        }

        Integer durationMin = null, durationMax = null;
        if (location != null && location.Stay != null) {
            if (location.Stay.MinDelta != null && location.Stay.MaxDelta != null) {
                durationMin = EvaTime.daysDelta(location.Stay.MinDelta);
                durationMax = EvaTime.daysDelta(location.Stay.MaxDelta);
            } else {
                durationMin = location.Stay.daysDelta();
                durationMax = durationMin;
            }
        }


        final Context context = activity;
        RequestAttributes.SortEnum sortBy = null;
        RequestAttributes.SortOrderEnum sortOrder = null;
        if (reply.requestAttributes != null) {
            sortBy = reply.requestAttributes.sortBy;
            sortOrder = reply.requestAttributes.sortOrder;
        }

        ArrayList<HotelAttributes.HotelChain> chains = new ArrayList<>();
        // The hotel board:
        Boolean selfCatering = null;
        Boolean bedAndBreakfast = null;
        Boolean halfBoard = null;
        Boolean fullBoard = null;
        Boolean allInclusive = null;
        Boolean drinksInclusive = null;

        // The quality of the hotel, measure in Stars
        Integer minStars = null;
        Integer maxStars = null;

        HashSet<HotelAttributes.Amenities> amenities = new HashSet<>();

        if (reply.hotelAttributes != null) {
            HotelAttributes ha = reply.hotelAttributes;
            selfCatering = ha.selfCatering;
            bedAndBreakfast = ha.bedAndBreakfast;
            halfBoard = ha.halfBoard;
            fullBoard = ha.fullBoard;
            allInclusive = ha.allInclusive;
            drinksInclusive = ha.drinksInclusive;

            chains = ha.chains;
            minStars = ha.minStars;
            maxStars = ha.maxStars;
            amenities = ha.amenities;
        }

        if (location.hotelAttributes != null) {
            HotelAttributes ha = location.hotelAttributes;
            if (ha.selfCatering != null) {
                selfCatering = ha.selfCatering;
            }
            if (ha.bedAndBreakfast != null) {
                bedAndBreakfast = ha.bedAndBreakfast;
            }
            if (ha.halfBoard != null) {
                halfBoard = ha.halfBoard;
            }
            if (ha.fullBoard != null) {
                fullBoard = ha.fullBoard;
            }
            if (ha.allInclusive != null) {
                allInclusive = ha.allInclusive;
            }
            if (ha.drinksInclusive != null) {
                drinksInclusive = ha.drinksInclusive;
            }
            if (ha.chains != null) {
                chains = ha.chains;
            }
            if (ha.minStars != null) {
                minStars = ha.minStars;
            }
            if (ha.maxStars != null) {
                maxStars = ha.maxStars;
            }
            if (ha.amenities != null) {
                amenities = ha.amenities;
            }

        }

        chatItem.setSearchModel(new AppHotelSearchModel(isComplete, location,
                arriveDateMin, arriveDateMax,
                durationMin, durationMax,
                reply.travelers,
                chains,

                // The hotel board:
                selfCatering, bedAndBreakfast, halfBoard, fullBoard, allInclusive, drinksInclusive,

                // The quality of the hotel, measure in Stars
                minStars, maxStars,

                amenities,
                sortBy, sortOrder));



        if (EvaComponent.evaAppHandler instanceof HotelCount) {
            chatItem.setStatus(ChatItem.Status.SEARCHING);
            chatItem.setSubLabel("Searching...");
            mView.notifyDataChanged();

            AsyncCountResult hotelCountHandler = new ResultsCountHandler(context, "One hotel found.\nTap here to see it.",
                    " hotels found.\nTap here to see them.",
                    activity.getString(R.string.evature_zero_count),
                    isComplete,
                    chatItem
            );


            ((HotelCount) EvaComponent.evaAppHandler).getHotelCount(context, isComplete, location,
                    arriveDateMin, arriveDateMax,
                    durationMin, durationMax,
                    reply.travelers,
                    chains,

                    // The hotel board:
                    selfCatering, bedAndBreakfast, halfBoard, fullBoard, allInclusive, drinksInclusive,

                    // The quality of the hotel, measure in Stars
                    minStars, maxStars,

                    amenities,
                    hotelCountHandler);

        }
        else {
            // count is not supported - trigger search
            if (EvaComponent.evaAppHandler instanceof HotelSearch) {
                chatItem.getSearchModel().triggerSearch(context);
            }
            else {
                // TODO: insert new chat item saying the app doesn't support search?
                Log.e(TAG, "App reached hotel search, but has no matching handler");
            }
        }
    }

    private void findCruiseResults(boolean isComplete, EvaLocation from, EvaLocation to, final EvaApiReply reply, final ChatItem chatItem) {
        Date dateFrom = null, dateTo = null;
        Integer durationFrom = null, durationTo = null;

        String departure = (from != null && from.Departure != null) ? from.Departure.Date : null;
        if (departure != null) {
            try {
                dateFrom = evaDateFormat.parse(departure);

                Integer days = from.Departure.daysDelta();
                if (days != null) {
                    Calendar c = Calendar.getInstance();
                    c.setTime(dateFrom); // Now use today date.
                    c.add(Calendar.DATE, days.intValue()); // Adding duration days
                    dateTo = c.getTime();
                }
            } catch (ParseException e) {
                DLog.e(TAG, "Failed to parse eva departure date: " + departure);
                e.printStackTrace();
            }
        }

        if (to != null && to.Stay != null) {
            if (to.Stay.MinDelta != null && to.Stay.MaxDelta != null) {
                durationFrom = EvaTime.daysDelta(to.Stay.MinDelta);
                durationTo = EvaTime.daysDelta(to.Stay.MaxDelta);
            } else {
                durationFrom = to.Stay.daysDelta();
                durationTo = durationFrom;
            }
        }

        if (from != null && from.nearestCustomerLocation != null) {
            from = from.nearestCustomerLocation;
        }
        if (to != null && to.nearestCustomerLocation != null) {
            to = to.nearestCustomerLocation;
        }

        RequestAttributes.SortEnum sortBy = null;
        RequestAttributes.SortOrderEnum sortOrder = null;

        if (reply.requestAttributes != null) {
            sortBy = reply.requestAttributes.sortBy;
            sortOrder = reply.requestAttributes.sortOrder;
        }

        chatItem.setSearchModel(new AppCruiseSearchModel(isComplete, from, to, dateFrom, dateTo, durationFrom, durationTo, reply.cruiseAttributes, sortBy, sortOrder));
        chatItem.setStatus(ChatItem.Status.SEARCHING);
        chatItem.setSubLabel("Searching...");
        mView.notifyDataChanged();


        if (EvaComponent.evaAppHandler instanceof CruiseCount) {

            AsyncCountResult cruiseCountHandler = new ResultsCountHandler(activity, "One cruise found.\nTap here to see it.",
                    " cruises found.\nTap here to see them.",
                    activity.getString(R.string.evature_zero_count),
                    isComplete,
                    chatItem
            );

            // count the results and update the chat item,  if there is only one result then activate search right away
            ((CruiseCount) EvaComponent.evaAppHandler).getCruiseCount(activity, from, to, dateFrom, dateTo, durationFrom, durationTo, reply.cruiseAttributes,
                    cruiseCountHandler);
        }
        else {
            // count is not supported - trigger search
            if (EvaComponent.evaAppHandler instanceof CruiseSearch) {
                chatItem.getSearchModel().triggerSearch(activity);
            }
            else {
                // TODO: insert new chat item saying the app doesn't support search?
                Log.e(TAG, "App reached hotel search, but has no matching handler");
            }
        }

    }


    public void onSaveInstanceState(Bundle savedInstanceState) {
		DLog.d(TAG, "onSaveInstanceState");
		mView.handleBackPressed(); // cancel edit chat item if one is being edited
		savedInstanceState.putSerializable(EVATURE_CHAT_LIST, mView.getChatListModel());
		savedInstanceState.putString(EVATURE_SESSION_ID, eva.getSessionId());
	}

//    handled in onCreate
//    public void onRestoreInstanceState(Bundle savedInstanceState) {
//        DLog.i(TAG, "onRestoreInstanceState");
//        if (savedInstanceState != null) {
//            ArrayList<ChatItem> chatItems = (ArrayList<ChatItem>) savedInstanceState.getSerializable(EVATURE_CHAT_LIST);
//
//            mView = new EvatureMainView(this, chatItems );
//            eva.setSessionId(savedInstanceState.getString(EVATURE_SESSION_ID));
//            mView.flashSearchButton(5);
//        }
//    }
//
	
	public void onResume() {
        DLog.d(TAG, "onResume");
        for (Iterator<WeakReference<ImageButton>> iterator = EvaChatTrigger.evaButtons.iterator(); iterator.hasNext();) {
            WeakReference<ImageButton> weakRef = iterator.next();
            ImageButton imgButton = weakRef.get();
            if (imgButton == null) {
                iterator.remove();
            }
            else {
                //EvatureMainView.scaleButton(imgButton, 400, 1.0f, 0.0f);
                //EvatureMainView.animateButton(imgButton, "alpha", 400, 1.0f, 0.0f);
                imgButton.setVisibility(View.GONE);
            }

        }

//        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
		isPaused = false;
		Intent intent = activity.getIntent();
		if ("com.google.android.gms.actions.SEARCH_ACTION".equals(intent.getAction())) {
            eva.stopSpeak();
			eva.resetSession();
			eva.cancelSearch();
			if (speechSearch != null) {
				speechSearch.cancel();
			}
			mView.clearChatHistory();
			
			String searchString = intent.getStringExtra(SearchManager.QUERY);
			ChatItem chat = new ChatItem(searchString);
			mView.addChatItem(chat);
			VOICE_COOKIE.storeResultInItem = chat;
			eva.searchWithText(searchString, VOICE_COOKIE, false);
			mView.addChatItem(new ChatItem(getActivity().getString(R.string.evature_thinking), null, ChatItem.ChatType.Eva));
			
			// clear the intent - it shouldn't run again resuming!
			//activity.onNewIntent(new Intent());
		}

		eva.onResume();
		VolumeUtil.register(activity, this);

        if (EvaComponent.evaAppHandler != null && EvaComponent.evaAppHandler instanceof EvaLifeCycleListener) {
            ((EvaLifeCycleListener) EvaComponent.evaAppHandler).onResume(mView);
        }
		
	}

//	@Override
//	protected void onNewIntent(Intent intent) {
//	    super.onNewIntent(intent);
//	    // getIntent() should always return the most recent
//	    setIntent(intent);
//	}

	public void onPause() {
		DLog.i(TAG, "onPause");
        evaSessionId = eva.getSessionId();
        for (Iterator<WeakReference<ImageButton>> iterator = EvaChatTrigger.evaButtons.iterator(); iterator.hasNext();) {
            WeakReference<ImageButton> weakRef = iterator.next();
            ImageButton imgButton = weakRef.get();
            if (imgButton == null) {
                iterator.remove();
            }
            else {
                //EvatureMainView.scaleButton(imgButton, 400, 0f, 0.666f);
                //EvatureMainView.animateButton(imgButton, "alpha", 400, 0.0f, 1.0f);
                imgButton.setVisibility(View.VISIBLE);
            }
        }

        eva.onPause();
		eva.cancelSearch();
		isPaused = true; // don't allow speech callbacks to start followup recording
		// cancel recording if during recording
	    if (speechSearch != null && speechSearch.isInSpeechRecognition()) {
		   DLog.i(TAG, "Canceling recording");
		   speechSearch.cancel();
		   mView.hideSpeechWave();
		   mView.deactivateSearchButton();
	    }
		VolumeUtil.unregister(activity);

        if (EvaComponent.evaAppHandler != null && EvaComponent.evaAppHandler instanceof EvaLifeCycleListener) {
            ((EvaLifeCycleListener) EvaComponent.evaAppHandler).onPause();
        }

    }

    public void onDestroy() {
		eva.onDestroy();
		if (speechSearch != null) {
			speechSearch.onDestroy();
		}
	}
	
	public void speak(String sayIt) {
		speak(sayIt, true);
	}
	
	public void speak(String sayIt, boolean flush) {
		speak(sayIt, flush, null);
	}

	public void speak(String sayIt, boolean flush, final Runnable onComplete) {
		// Do not speak if a recording is taking place!
		if (mView.isRecording() == false) {
			if (onComplete == null) {
				eva.speak(sayIt, flush, null);
			}
			else {
				eva.speak(sayIt, flush, new Runnable() {
					@Override
					public void run() {
						if (isPaused == false)  {
							onComplete.run();
						}
					}
				});
			}
		}
	}
	
	
	private EvaSpeechRecogComponent.SpeechRecognitionResultListener mSpeechSearchListener = new EvaSpeechRecogComponent.SpeechRecognitionResultListener() {
		
		@Override
		public void speechResultError(final String message, final Object cookie) {
			DLog.d(TAG, "Speech recognition error: "+message);
            String networkError = getActivity().getResources().getString(R.string.evature_network_error);
            if (networkError.equals(message)) {
                ChatItem myChat = new ChatItem(networkError, null, ChatItem.ChatType.Eva);
                mView.addChatItem(myChat);
            }
			mView.hideSpeechWave();
			Thread t = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 25);
						Thread.sleep(120);
						toneGenerator.startTone(ToneGenerator.TONE_PROP_NACK, 25);
					} catch (InterruptedException e) {
					}
					finally {
						activity.runOnUiThread(new Runnable() {
                            public void run() {
                                eva.speechResultError(message, cookie);
                            }
                        });
					}
				}
			});
			t.start();
		}

		@Override
		public void speechResultOK(final String evaJson, final Bundle debugData, final Object cookie) {
			DLog.d(TAG, "Speech recognition ok");
			mView.hideSpeechWave();
			Thread t = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 25);
						Thread.sleep(120); // small delay
						toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 25);
					} catch (InterruptedException e) {
					}
					finally {
						activity.runOnUiThread(new Runnable() {
                            public void run() {
                                eva.speechResultOK(evaJson, debugData, cookie);
                            }

                            ;
                        });
					}
				}
			});
			t.start();
			
		}
	};
	
	
	
	/*** Start a voice recognition - and place the results in the chatItem (or add
	 * a new one if null) */
	// - adding chatItem to the cookie that will be
	// returned to the onEvaReply callback
	// will be using it in the callback
	// the cookie is just something that will be returned untouched to the
	// "onEvaReply" callback - so in the callback you can tell which
	// code triggered it (ie. there could be multiple entry point all leading to
	// the same callback)
	private void voiceRecognitionSearch(ChatItem chatItem, final boolean editLastUtterance) {

		VOICE_COOKIE.storeResultInItem = chatItem;
		VOICE_COOKIE.editLastUtterance = editLastUtterance;
		
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				if (mView.areMainButtonsShown() == false) {
					DLog.d(TAG, "Not starting voice recognition because main button isn't shown");
					return;
				}
				if (mView.isRecording()) {
					DLog.d(TAG, "Not starting voice recognition because isRecording already!");
					return;
				}
				eva.stopSpeak();// stop the speech if there is one going
				
		        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 25);
		        try {
					Thread.sleep(120); // small delay
				} catch (InterruptedException e) {
				}
		        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 25);
		        try {
					Thread.sleep(25); // wait for beep to end - do NOT record the beep
				} catch (InterruptedException e) {
				}
                activity.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        DLog.i(TAG, "Starting recognizer");
                        eva.stopSpeak();
                        mView.startSpeechRecognition(mSpeechSearchListener, speechSearch, VOICE_COOKIE, editLastUtterance);
                        addEmptyFragment();
                    }
                });
			}
		});
		t.start();
	}

	
	

	@SuppressWarnings("nls")
	public void buttonClickHandler(View view) {
		int viewId = view.getId();
		if (viewId == R.id.restart_button) {   // can't use Switch-Case because R.id  isn't final for library code
			startNewSession();
		} else if (viewId == R.id.voice_search_button) {
			if (speechSearch.isInSpeechRecognition() == true) {
				speechSearch.stop();
			} else {
				voiceRecognitionSearch(null, false);
			}
		} else if (viewId == R.id.undo_button) {
			undoLastUserChat();
		}
//		else if (viewId == R.id.volume_button) {
//			Intent intent = new Intent(this, EvaVolumeSettingsDialog.class);
//			startActivity(intent);
//		}
	}// End of evatureClickHandler


	private void undoLastUserChat() {
		ArrayList<ChatItem> chatList= mView.getChatListModel();
		if (chatList.size() > 0) {
			int dismissFrom = -1;
			// search for last user chat - dismiss from the point forward
			for (int i=chatList.size()-1; i>=0; i--) {
				if (chatList.get(i).getType() == ChatItem.ChatType.User) {
					dismissFrom = i;
					break;
				}
			}
			if (dismissFrom > 0) {
				// dismiss also the eva reply that came before it - it will be added again
				String lastId = chatList.get(dismissFrom-1).getEvaReplyId();
				if (lastId != null) {
					for (int i=dismissFrom-1; i>=0; i--) {
						if (lastId.equals(chatList.get(i).getEvaReplyId()) == false) {
							dismissFrom = i+1;
							break;
						}
					}
				}
			}
			
			if (dismissFrom > 0) {
				DELETE_UTTERANCE_COOKIE.dismissFrom = dismissFrom;
				int dismissTo = chatList.size();
				DELETE_UTTERANCE_COOKIE.dismissTo = dismissTo;
				mView.dismissItems(dismissFrom, dismissTo, ChatAdapter.DismissStep.ANIMATE_DISMISS);
			}
		}
		mView.disableSearchButton();
		eva.searchWithText("", DELETE_UTTERANCE_COOKIE, true); // undo last utterance - edit it to an empty string
	}


    /***
     * returns true if handled internally
     * @return
     */
	public boolean onBackPressed() {
	   DLog.d(TAG, "onBackPressed Called");

	   // cancel recording if during recording
	   if (speechSearch.isInSpeechRecognition()) {
		   DLog.i(TAG, "Canceling recording");
		   speechSearch.cancel();
		   mView.deactivateSearchButton();
		   mView.hideSpeechWave();
		   return true;
	   }

		return mView.handleBackPressed();
	}
	
	
	/**** Start new session from menu item */
	@SuppressWarnings("nls")
	private void startNewSession() {
		mView.clearChatHistory();
		eva.stopSpeak();
		eva.cancelSearch();
		if (speechSearch != null) {
			speechSearch.cancel();
 		    mView.hideSpeechWave();
		    mView.deactivateSearchButton();
		}
		
		eva.resetSession();
		// triggered by button - create a "start new session" fake chat
		VOICE_COOKIE.storeResultInItem = null;
        Resources resources = activity.getResources();
		ChatItem myChat = new ChatItem(resources.getString(R.string.evature_user_start_new));
		mView.addChatItem(myChat);
		String greeting = resources.getString(R.string.evature_start_new_session_speak);
		int pos = greeting.length();
		String seeExamples = resources.getString(R.string.evature_tap_for_examples);
		SpannableString sgreet = new SpannableString(greeting + new SpannedString(seeExamples));
		int col = resources.getColor(R.color.evature_chat_secondary_text);
		sgreet.setSpan(new ForegroundColorSpan(col), pos, pos+seeExamples.length(), 0);
		sgreet.setSpan( new StyleSpan(Typeface.ITALIC), pos, pos+seeExamples.length(), 0);
		ChatItem chat = new ChatItem(sgreet,null, ChatItem.ChatType.EvaWelcome);
		mView.addChatItem(chat);
		speak(greeting, true, new Runnable() {
			
			@Override
			public void run() {
				mView.flashSearchButton(3);
				if (eva.getAutoOpenMicrophone()) {
					voiceRecognitionSearch(null, false);
				}
			}
		});
	

	}

	
	/****
	 * newSessionStarted - callback from EvaComponent - activated when a response arrives with different SessionId than was requested 
     *
	 * selfTriggered:
	 * 	  true - user requested new session (eg. said  "clear all")
	 *   false - server decided to change session (eg. session timeout) 
	 */
	@Override
	@SuppressWarnings("nls")
	public void newSessionStarted(boolean selfTriggered) {
		// self triggered are already handled
		if (!selfTriggered) {
			mView.clearChatHistory();
			// triggered from server or by chat - the last chat utterance should be included in this session
			if (VOICE_COOKIE.storeResultInItem != null) {
				mView.addChatItem(VOICE_COOKIE.storeResultInItem);
			}
		}
	}


	

	@SuppressWarnings("nls")
	public void onEventChatItemModified(ChatItem chatItem, SpannableString preEditChat, boolean startRecord, boolean editLastUtterance) {
		if (startRecord) {
			if (chatItem == null) {
				DLog.e(TAG, "Unexpected chatItem=null startRecord");
				return;
			}
			voiceRecognitionSearch(chatItem, editLastUtterance);
		} else {
			if (chatItem == null) {
				// removed last item
				undoLastUserChat();
			} else {
				if (editLastUtterance) {
					int index = mView.getChatListModel().indexOf(chatItem);
					mView.dismissItems(index+1, mView.getChatListModel().size(), ChatAdapter.DismissStep.ANIMATE_DISMISS);
				}
				String searchText = chatItem.getChat().toString();
				TEXT_TYPED_COOKIE.storeResultInItem = chatItem;
				TEXT_TYPED_COOKIE.editLastUtterance = editLastUtterance;
				TEXT_TYPED_COOKIE.preEditChat = preEditChat;
				mView.disableSearchButton();
				eva.searchWithText(searchText, TEXT_TYPED_COOKIE, editLastUtterance);
			}
		}
	}

	public void onEvaReply(EvaApiReply reply, Object cookie) {
//		if (reply.chat != null && reply.chat.newSession && cookie == DELETE_UTTERANCE_COOKIE) {
//			mView.clearChatHistory();
//			//mView.getChatListModel().clear();
//			handleFlow(reply);
//			mView.notifyDataChanged();
//			return;
//		}

		SpannableString chat = null;
		boolean hasWarnings = false;
		if (reply.processedText != null) {
			// reply of voice -  add a "Me" chat item for the input text
			chat = new SpannableString(reply.processedText);
			if (reply.evaWarnings.size() > 0) {
				for (EvaWarning warning: reply.evaWarnings) {
					if (warning.position == -1) {
						continue;
					}
					hasWarnings = true;
					if (warning.text.equals(reply.processedText)) {
						// for some odd reason the error span doesn't show when its the entire string, so until a fix is available skip it
						break;
					}
					//chat.setSpan( new ForegroundColorSpan(col), warning.position, warning.position+warning.text.length(), 0);
					chat.setSpan( new ErrorSpan(activity.getResources()), warning.position, warning.position+warning.text.length(), 0);
				}
			}
			if (eva.semanticHighlightingEnabled() && reply.parsedText != null) {
				try {
					if (eva.getSemanticHighlightTimes() && reply.parsedText.times != null) {
						int col = activity.getResources().getColor(R.color.evature_times_markup);
						
						for (ParsedText.TimesMarkup time : reply.parsedText.times) {
							chat.setSpan( new ForegroundColorSpan(col), time.position, time.position+time.text.length(), 0);
						}
					}
					
					if (eva.getSemanticHighlightLocations() && reply.parsedText.locations != null) {
						int col = activity.getResources().getColor(R.color.evature_locations_markup);
						
						for (ParsedText.LocationMarkup location: reply.parsedText.locations) {
							chat.setSpan( new ForegroundColorSpan(col), location.position, location.position+location.text.length(), 0);
						}
					}
				}
				catch (IndexOutOfBoundsException e) {
					DLog.e(TAG, "Index out of bounds setting spans of chat ["+chat+"]", e);
				}
			}
		}
		
		mView.deactivateSearchButton();
        removeEmptyFragment();


		if (VOICE_COOKIE == cookie) {
			if (chat != null) {
				if (VOICE_COOKIE.storeResultInItem != null) {
					// this voice recognition replaces the last utterance
					mView.voiceResponseToChatItem(VOICE_COOKIE.storeResultInItem, chat);
				}
				else {
					mView.addChatItem(new ChatItem(chat));
				}
			}
		}

		if (cookie == TEXT_TYPED_COOKIE) {
			if (TEXT_TYPED_COOKIE.storeResultInItem != null) {
				// replaces the last utterance
				mView.voiceResponseToChatItem(TEXT_TYPED_COOKIE.storeResultInItem, chat);

				if (TEXT_TYPED_COOKIE.editLastUtterance) {
					// we animated the dismiss of the follow up chat items - now edit is success do the actual delete
					int index = mView.getChatListModel().indexOf(TEXT_TYPED_COOKIE.storeResultInItem);
					mView.dismissItems(index+1, mView.getChatListModel().size(), ChatAdapter.DismissStep.DO_DELETE);
				}
			}
		}
		
		if (cookie == DELETE_UTTERANCE_COOKIE) {
			// deleted successfully last utterance 
			// make sure not to play pending SayIt if it was undone before sayIt was spoken 
			pendingReplySayit.cancel = true;
   	    	synchronized (pendingReplySayit) {
   	    		pendingReplySayit.notifyAll();
   	    	}
   	    	// remove the chat items
   	    	mView.dismissItems(DELETE_UTTERANCE_COOKIE.dismissFrom, DELETE_UTTERANCE_COOKIE.dismissTo, ChatAdapter.DismissStep.DO_DELETE);
		}
		
		if (reply.flow != null) {
			handleFlow(reply);
		}

		if (hasWarnings && !mShownWarningsTutorial) {
			mShownWarningsTutorial = true;
			ChatItem warnExplanation = new ChatItem("", reply.transactionId, ChatItem.ChatType.Eva);
			warnExplanation.setSubLabel(activity.getString(R.string.evature_undo_tutorial));
			mView.addChatItem(warnExplanation);
		}
	}


    /***
     * The only purpose of this fragment is to be on top of the chat screen while the recording is running,
     * so pressing "back" will destroy this invisible fragment and trigger the "onBackPressed" of the chatScreen
     * ie. it will cancel the recording
     */
    public static class EmptyFragment extends Fragment {
        boolean necessary = true;

        @Override
        public void onResume() {
            super.onResume();
            if (ChatAdapter.currentEditBox != null) {
                Handler handler = new Handler(Looper.getMainLooper());
                final Runnable r = new Runnable() {
                    public void run() {
                        if (ChatAdapter.currentEditBox != null) {
                            EditText editTextToFocus = (EditText) ChatAdapter.currentEditBox.get();
                            if (editTextToFocus != null) {
                                editTextToFocus.requestFocus();
                                getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                            }
                        }
                    }
                };
                handler.postDelayed(r, 1500);
            }
        }

        @Override
        public void onDestroy() {
            DLog.d(TAG, "Destroying empty fragment, triggerback necessary: " + necessary);
            if (necessary) {
                FragmentManager manager = getFragmentManager();
                Fragment fragment = manager.findFragmentByTag(EvaChatScreenFragment.TAG);
                if (fragment != null) {
                    EvaChatScreenFragment evaChatScreenFragment = (EvaChatScreenFragment) fragment;
                    boolean handledInternally = evaChatScreenFragment.chatScreen.onBackPressed();
                }
            }
            super.onDestroy();
        }

        public void notNecessaryAnymore() {
            DLog.d(TAG, "Setting necessary to false");
            necessary = false;
        }
    }


    void addEmptyFragment() {
        if (mUseExtraFragmentForBackHack) {
            DLog.d(TAG, "Adding empty fragment");
            FragmentActivity fa = (FragmentActivity) activity;
            FragmentManager manager = fa.getSupportFragmentManager();
            Fragment fragment = manager.findFragmentByTag(EMPTY_FRAGMENT_TAG);
            if (fragment != null) {
                DLog.e(TAG, "Unexpected fragment stack state - already has empty fragment");
                return;
            }
            EmptyFragment empty = new EmptyFragment();
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.add(R.id.evature_root_view, empty, EMPTY_FRAGMENT_TAG);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }

    void removeEmptyFragment() {
        if (mUseExtraFragmentForBackHack) {
            DLog.d(TAG, "Removing empty fragment");
            FragmentActivity fa = (FragmentActivity)activity;
            FragmentManager manager = fa.getSupportFragmentManager();
            Fragment fragment = manager.findFragmentByTag(EMPTY_FRAGMENT_TAG);
            if (fragment == null) {
                DLog.e(TAG, "Unexpected fragment stack state - missing empty fragment");
                return;
            }
            EmptyFragment emptyFragment = (EmptyFragment) fragment;
            emptyFragment.notNecessaryAnymore();
            manager.popBackStack();  // assume empty fragment is on top!
        }

    }


    public void closeChatScreen() {
        FragmentActivity fa = (FragmentActivity)activity;
        FragmentManager manager = fa.getSupportFragmentManager();
        Fragment fragment = manager.findFragmentByTag(EMPTY_FRAGMENT_TAG);
//        FragmentTransaction transaction = manager.beginTransaction();
        if (fragment != null) {
            DLog.d(TAG, "Removing empty fragment");
//            EmptyFragment emptyFragment = (EmptyFragment) fragment;
//            emptyFragment.notNecessaryAnymore();
//            transaction.remove(emptyFragment);
            manager.popBackStack();  // assume empty fragment is on top!
        }
        else {
            DLog.d(TAG, "No empty fragment found");
        }
        fragment = manager.findFragmentByTag(EvaChatScreenFragment.TAG);
        if (fragment != null) {
//            transaction.remove(fragment);
            DLog.d(TAG, "Removing chat fragment");
            manager.popBackStack();  // assume chat fragment is on top!
        }
        else {
            DLog.e(TAG, "No chat fragment found!");
        }
//        transaction.commit();

    }


    /**** Display chat items for each flow element - execute the first question
	 * element or, if no question element, execute the first flow element
	 * 
	 * @param reply */
	private void handleFlow(EvaApiReply reply) {
		boolean hasQuestion = false;
		for (FlowElement flow : reply.flow.Elements) {
			if (flow.Type == FlowElement.TypeEnum.Question) {
				hasQuestion = true;
				break;
			}
		}

		boolean first = true;

		// if there is a question - show and activate only statements and questions
		// otherwise - show all items and activate the first
		for (FlowElement flow : reply.flow.Elements) {
			ChatItem chatItem = null;
			if (flow.Type == FlowElement.TypeEnum.Question) {
				// This question element for multiple choice answers... not integrated yet :(
//				QuestionElement question = (QuestionElement) flow;
				// DialogQuestionChatItem questionChatItem = new
				// DialogQuestionChatItem(flow.getSayIt(), reply, flow);
				ChatItem questionChatItem = new ChatItem(flow.getSayIt(), reply.transactionId, ChatItem.ChatType.Eva);
				chatItem = questionChatItem;
				mView.addChatItem(questionChatItem);

				// if (question.choices != null && question.choices.length > 0)
				// {
				// for (int index=0; index < question.choices.length; index++) {
				// addChatItem(new DialogAnswerChatItem(questionChatItem, index,
				// question.choices[index]));
				// }
				// }
				executeFlowElement(reply, flow, chatItem);
			} else {
				if (!hasQuestion || flow.Type == FlowElement.TypeEnum.Statement) {
					chatItem = new ChatItem(flow.getSayIt(), reply.transactionId, ChatItem.ChatType.Eva);
					mView.addChatItem(chatItem);
					if (!hasQuestion && flow.Type !=  FlowElement.TypeEnum.Statement && first) {
						first = false;
						// activate only the first non-statement
						executeFlowElement(reply, flow, chatItem);
					}
				}
				if (flow.Type ==  FlowElement.TypeEnum.Statement) {
					executeFlowElement(reply, flow, chatItem);
				}
			}
		}
	}

	
	@SuppressWarnings("nls")
	private void executeFlowElement(EvaApiReply reply, FlowElement flow, ChatItem chatItem) {
		// chatItem.setActivated(true);
		final String sayIt = flow.getSayIt();
		if (sayIt != null && !"".equals(sayIt)) {
			// non-statement flow types are delayed until a search-count is returned or timeout
			if (flow.Type == FlowElement.TypeEnum.Cruise || flow.Type ==  FlowElement.TypeEnum.Question ) {
				pendingReplySayit.cancel = false;
				pendingReplySayit.chatItem = chatItem;
				pendingReplySayit.sayIt = sayIt;
				pendingReplySayit.chatText = chatItem.getChat();
				chatItem.setChat("");
				mView.notifyDataChanged();
				pendingReplySayit.isQuestion = flow.Type ==  FlowElement.TypeEnum.Question;
				Thread t = new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							synchronized (pendingReplySayit) {
							    pendingReplySayit.wait(1200); // wait some - maybe no results will show and this will be canceled
							}
							if (pendingReplySayit.cancel == false) {
								pendingReplySayit.cancel = true;
								pendingReplySayit.chatItem.setChat(pendingReplySayit.chatText);
								mView.notifyDataChanged();
								if (pendingReplySayit.isQuestion) {
									speak(sayIt, false, new Runnable() {
										public void run() {
											DLog.d(TAG, "Question asked");
											mView.flashSearchButton(3);
											if (eva.getAutoOpenMicrophone()) {
												voiceRecognitionSearch(null, false);
											}
										}
									});
								}
								else {
									speak(sayIt, false);
								}
							}
						} catch (InterruptedException e) {
						}
					}
				});
				t.start();
			}
			else if (flow.Type ==  FlowElement.TypeEnum.Statement && ((StatementElement) flow).StatementType == StatementElement.StatementTypeEnum.Chat &&
					reply.chat != null && reply.chat.newSession) {
				speak(sayIt, false, new Runnable() {
					public void run() {
						DLog.d(TAG, "New session started");
						mView.flashSearchButton(3);
						if (eva.getAutoOpenMicrophone()) {
							voiceRecognitionSearch(null, false);
						}
					}
				});
			}
			else {
				speak(sayIt, false);
			}
		}

		switch (flow.Type) {
			case Reply:
				ReplyElement replyElement = (ReplyElement) flow;
				if (ServiceAttributes.CALL_SUPPORT.equals(replyElement.AttributeKey)) {
					// TODO: trigger call support
					Toast.makeText(activity, "Phoning Call Support", Toast.LENGTH_LONG).show();
				}
				break;
			case Flight:
			case Car:
			case Hotel:
			case Explore:
			case Train:
			case Cruise:
            case Question:
                findSearchResults(reply, flow, chatItem);
				break;

			case Statement:
				StatementElement se = (StatementElement) flow;
				switch (se.StatementType) {
					case Understanding:
					case Unknown_Expression:
					case Unsupported:
						mShownWarningsTutorial = true;
						chatItem.setSubLabel(activity.getString(R.string.evature_undo_tutorial));
						break;
                    case Chat:
                        if (reply.originalInputText != null && reply.originalInputText.toLowerCase().equals("bye bye")) {
                            EvaChatTrigger.closeChatScreen(getActivity());
                        }
                        break;
				}
				break;

            default:
                DLog.w(TAG, "Unexpected flow type "+flow.Type);
				break;

		}
	}

	public void onEvaError(String message, EvaApiReply reply, boolean isServerError, Object cookie) {
		mView.flashBadSearchButton(2);
        removeEmptyFragment();

		// You can show the message with a Toast, or ChatItem
		// Toast.makeText(this, message, Toast.LENGTH_LONG).show();
		// or
		// mView.addChatItem(new ChatItem(message, null, ChatItem.ChatType.Eva));

		// if this is a response to the delete-last-utterance - restore the chat items
		if (cookie == DELETE_UTTERANCE_COOKIE) {
			// we animated-dismissed the items - need to restore them
			mView.dismissItems(DELETE_UTTERANCE_COOKIE.dismissFrom, DELETE_UTTERANCE_COOKIE.dismissTo, ChatAdapter.DismissStep.ANIMATE_RESTORE);
		}
		else {
			// failed edit chat item - restore the chat item text before the edit
			StoreResultData data =  (StoreResultData)cookie;
			if (data.storeResultInItem != null) {
				if (data.preEditChat != null) {
					data.storeResultInItem.setChat(data.preEditChat);
					mView.notifyDataChanged();
				}
			
				// we animated-dismissed the items following the chatitem - need to restore them
				int index = mView.getChatListModel().indexOf(data.storeResultInItem);
				mView.dismissItems(index+1, mView.getChatListModel().size(), ChatAdapter.DismissStep.ANIMATE_RESTORE);
			}
		}
	}

	@Override
	public void onVolumeChange() {
		mView.setVolumeIcon();
	}

    private String inferScopeFromHandler() {
        StringBuilder builder = new StringBuilder();
        if ((EvaComponent.evaAppHandler instanceof CarSearch) || (EvaComponent.evaAppHandler instanceof CarCount)) {
            builder.append(AppScope.Car.toString());
        }
        if ((EvaComponent.evaAppHandler instanceof CruiseSearch) || (EvaComponent.evaAppHandler instanceof CruiseCount)) {
            builder.append(AppScope.Cruise.toString());
        }
        if ((EvaComponent.evaAppHandler instanceof FlightSearch) || (EvaComponent.evaAppHandler instanceof FlightCount)) {
            builder.append(AppScope.Flight.toString());
        }
        if ((EvaComponent.evaAppHandler instanceof HotelSearch) || (EvaComponent.evaAppHandler instanceof HotelCount)) {
            builder.append(AppScope.Hotel.toString());
        }
        return builder.toString();
    }
}

