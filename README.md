
# Eva Voice SDK - Android

Version 2.0



<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [Introduction](#introduction)
- [Step 1: Setup the SDK in your IDE](#step-1-setup-the-sdk-in-your-ide)
- [Step 2: Initialize and Configure Eva](#step-2-initialize-and-configure-eva)
- [Step 3: Add the microphone button!](#step-3-add-the-microphone-button)
  - [Mini Integration Test - Say Hi!](#mini-integration-test---say-hi)
  - [Context parameter](#context-parameter)
- [Step 4: Implement your applicative callbacks](#step-4-implement-your-applicative-callbacks)
  - [Applicative Search interfaces](#applicative-search-interfaces)
    - [Note:  null values of parameters](#note--null-values-of-parameters)
    - [The IsComplete argument](#the-iscomplete-argument)
  - [Applicative Count interfaces](#applicative-count-interfaces)
- [Advanced Integration](#advanced-integration)
  - [Look & Feel Customizations](#look-&-feel-customizations)
  - [Google Now Integration](#google-now-integration)
  - [Other App Setup settings](#other-app-setup-settings)
- [Building the SDK from Source Code](#building-the-sdk-from-source-code)
  - [Import to Android Studio (recommended by Google)](#import-to-android-studio-recommended-by-google)
  - [Alternatively, Import to an Eclipse Workspace (Slightly more complex)](#alternatively-import-to-an-eclipse-workspace-slightly-more-complex)
- [Support](#support)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Introduction

Voice enable your travel application in a couple of hours using the latest Eva Voice SDK.

With the new release you can add a cutting edge voice interface to your travel application with super simple integration.

Take advantage of the latest Deep Learning algorithms delivering near human precision.

The Eva Voice SDK comes batteries-included and has everything your application needs:

* Speech Recognition

* Natural Language Understanding

* Dialog Management

* Voice Chat user interface overlay

The beautiful user interface conforms to the latest Material Design guidelines and is fully customizable to match your application.

The SDK is open source. Fork us [on Github](https://github.com/evature/android)!

## Step 1: Include the SDK in your Android Studio Project

1. 
Download the compiled SDK as two AAR files, and place them in your project:
[evasdk-debug.aar](https://raw.githubusercontent.com/evature/android/master/EvaSDK/evasdk/build/outputs/aar/evasdk-debug.aar) and [evasdk-release.aar](https://raw.githubusercontent.com/evature/android/master/EvaSDK/evasdk/build/outputs/aar/evasdk-release.aar)

2. modify your project's build.grade:
``` gradle
repositories {
    flatDir {
        dirs '<the folder where you placed the aar files>'
    }
}
    // to your dependencies add:
    debugCompile(name:'evasdk-debug', ext:'aar')
    releaseCompile(name:'evasdk-release', ext:'aar')
```

3. 
If you wish to build the library yourself - see [Building the SDK from Source Code](#building-the-sdk-from-source-code) below.

**Verify Step 1:**

In your code you should be able to import EvaSDK classes. For example, try adding:
``` java 
import com.evature.evasdk.appinterface.AppSetup;
```
It should compile without an error. 
## Step 2: Initialize and Configure Eva

Do you have your Eva SITE_CODE and API_KEY?

Register for free at [http://www.evature.com/registration/form](http://www.evature.com/registration/form) 

The minimal required setup is to call AppSetup.initEva with these three parameters:

1. Your API_KEY - received when you register to Evature web service

2. Your SITE_CODE - received with your API_KEY

3. Your App handler class, see more details later at [step 4](#heading=h.yot6caolfjs9).

For example:

``` java
public class MyTravelApp extends Application {
   @Override
   public void onCreate() {
       super.onCreate();
       AppSetup.evaLogs(BuildConfig.DEBUG);  // enable Eva logs on Debug builds
       AppSetup.initEva(API_KEY, SITE_CODE, new MyTravelAppHandler());
   }
}
```

Optional settings can be accessed by public static fields/methods of the AppSetup class.

See more details below at the "Advanced integration" section.

**Verify Step 2**:
Make sure you've enabled evaLogs and run your application. If all goes well you should see in the LogCat a debug log that says: `Eva Initialized Successfully`

## Step 3: Add the microphone button!

For each activity you wish to voice-enable add a single line to onCreate:

``` java
public class SearchScreenActivity extends FragmentActivity {
  @Override
  public void onCreate ( Bundle savedInstanceState )
  {
     super.onCreate(savedInstanceState);
//   Your activity’s onCreate code eg.  
//   setContentView(R.layout.search_screen_layout);

     //   Add a default "Microphone" button
     EvaChatTrigger.addDefaultButton(this);
  }
```

This call adds a floating microphone action-button to the activity.

Alternatively, you can add your own button and trigger the chat screen when it is clicked:

``` java
public void onCreate ( Bundle savedInstanceState )
{
     super.onCreate(savedInstanceState);
//   Your activity’s onCreate code eg.  
//   setContentView(R.layout.search_screen_layout);

     Button btn = ((Button) findViewById(R.id.my_chat_btn));
     btn.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
           // Show the chat screen:  
           EvaChatTrigger.startSearchByVoice(FragmentActivity.this);
        }
    });
```

### Context parameter
If your application supports multiple scopes (eg. Flight search and Hotel search) and the current activity is relevant only to one scope (eg. viewing flight search results, or flights search only page) you can help Eva by passing a second parameter of *AppScope*. 

eg.

``` java
EvaChatTrigger.startSearchByVoice(FragmentActivity.this, AppScope.Flight);
// or
EvaChatTrigger.addDefaultButton(this, AppScope.Hotel);
```


**Note**: 
Eva assumes the host activity is of type *android.support.v4.app.FragmentActivity*. If this isn’t the case in your application, let us know and we can provide a workaround.

### Mini Integration Test - Say Hi!

At this point the floating microphone action button is visible and you can start communicating with Eva. Kick off your application and navigate to the activities which you voice-enabled.
Press the microphone and say "Hi" to Eva!

Assuming all went well you will hear the reply.

At this point Speech Recognition, Natural Language Understanding and Dialog Management are working, but Eva doesn’t know anything about your application yet!


## Step 4: Implement your applicative callbacks

Remember the "MyTravelAppHandler" which we passed to *initEva* in step two? This object should implement interfaces from the com.evature.evasdk.appinterface namespace.

These interfaces include:

* FlightSearch - trigger an applicative search for flights per the end-user’s request

* HotelSearch - trigger an applicative search for Hotels

* CarSearch - trigger an applicative search for Car Rentals

* CruiseSearch - trigger an applicative search for Cruises

* HotelCount, FlightCount, etc.. - count the number of relevant search results

There are several more interfaces for you to integrate with and we are adding more interfaces  all the time, so stay tuned!

Of course, you don’t have to implement all of the above interfaces, only the ones that are functionally supported by your application.

Eva will automatically infer which features should be enabled based on which interfaces your app handler implements. 

For example, if your app only supports searching for flights then you only need to implement one interface:  **_FlightSearch_**.

As you can tell from the above list, the current interfaces are pairs of **Search* and **Count*, see below their different meaning.  

Normally you should implement the \**Search* interface, while the \**Count* interface is optional but highly recommended.  

### Applicative Search interfaces

The methods in these interfaces receive the requested search criteria as function arguments.

All you have to do is translate the arguments to your native types and trigger your existing search function and you are done!

The different interfaces and their parameters are fully documented in the javadocs. They are straightforward and simply describe the criteria as entered by the user (e.g. airport codes, departure dates, etc…).

For example, this is a FlightSearch method:

``` java
    /***
     * handleOneWayFlightSearch - callback when Eva collects criteria to search for one way flights
     * @param context - Android context
     * @param isComplete - true if Eva considers the search flow "complete", ie. all the mandatory criteria have been requested by the user
     * @param origin - location of take-off
     * @param destination - location of landing
     * @param departDateMin - range of dates the user wishes to depart on
     * @param departDateMax   if only a single date is entered the Max date will be equal to the Min date
     * @param travelers - how many travelers, split into age categories
     * @param nonstop - True if the user requested nonstop, False if the user requested NOT nonstop, and null if the user did not mention this criteria
     * @param seatClass - array of seat classes (eg. economy, business, etc) requested by the user
     * @param airlines - array of airline codes requested by the user
     * @param redeye - True if the user requested Red Eye flight, False if the user requested NOT Red Eye flight, and null if the user did not mention this criteria
     * @param food - enum describing food in flight as requested by the user, null if not mentioned
     * @param seatType - window/aisle seats, or null if not mentioned
     * @param sortBy - how should the results be sorted (eg. price, date, etc..), or null if not mentioned
     * @param sortOrder - ascending or descending or null if not mentioned
     */
    void handleOneWayFlightSearch(Context context,
                                  boolean isComplete, 
                                  EvaLocation origin, EvaLocation destination,
                                  Date departDateMin, Date departDateMax,
                                  EvaTravelers travelers,
                                  Boolean nonstop,
                                  SeatClass[] seatClass,
                                  String[] airlines,
                                  Boolean redeye,
                                  FoodType food,
                                  SeatType seatType,
                                  SortEnum sortBy, SortOrderEnum sortOrder);
```

#### Note:  null values of parameters

If the user did not request a certain search criteria, it will be passed as "null", so please remember to check that each argument isn’t null before you use it.
For example the “nonStop” parameter will be *True* if the user requested non-stop flights, *False* if the user specifically requested NOT non-stop flights, and *null* if the user did not mention this criteria at all.

#### The IsComplete argument

One of the search methods’ parameters is called "*isComplete*". Unlike the other parameters, this is not a search criteria requested by the user but instead it is Eva’s dialog state. 
While the user has not entered all the **mandatory** search parameters Eva will continue asking the user for more information. After each utterance and question Eva will trigger the relevant search function with the parameters entered so far and *isComplete=False*.  After the user has entered **all **the mandatory search parameters Eva will trigger a search with *isComplete=True*, and Eva will end the dialog by saying “searching for <*text describing the search*>”.

The most common use case would be ignoring calls with *isComplete=False*, and triggering an actual search only when *isComplete=True*, and that search would open a new screen (ie. new activity or fragment) showing the search results.

*isComplete=False is mostly used to update the traditional form-search display with the partial information received thus far.*

### Applicative Count interfaces

If you app supports counting how many search results will be for a partial search (ie. isComplete=False), it is highly recommended that you implement the matching **Count* interface.

The **Count* interfaces (eg. HotelCount, FlightCount, etc..) receive a the same set of arguments as their matching **Search* interfaces, except that instead of the "*sortBy*" and “*sortOrder*” arguments they receive a callback of the type AsyncCountResult. 
Simply activate your counting logic (it can be easily an async function, eg. a request to server or database query) and call the *handleCountResult* method of the callback with your count results.

When implemented, Eva will use these count results to display the number of results below Eva’s text while the user chats, and will have special handling for one result (no more questions required) and zero results (urge the user to change some of the requested criteria).

## Advanced Integration

### Look & Feel Customizations

Simple customizations of the Eva chat screen and the default microphone button are very easy; all the strings, colors, layouts and bitmaps used by the SDK are located in standard resource files.

Simply copy the resources to your project and modify them to fit your needs. All the resource file names and IDs are prefixed with "evature_" so there should be no conflict with your own files/IDs.

Layout resources:

1. evature_chat_layout.xml - the screen containing the chat listview and the buttons at the bottom.

2. evature_row_eva_chat.xml - the layout of a Chat bubble response from Eva. Note there is another such file in the layout-v21 folder.

3. evature_row_user_chat.xml - the layout of a Chat bubble text from the User. Note there is another such in the layout-v21 folder.

4. evature_voice_search_button - the default triggering floating action button.

The chat screen fragment is contained in a view with ID *evature_root_view*. If no such view exists then it is created as an overlay above the current activity. If you wish to modify the layout parameters of the evature_root_view or default button you can call *EvaChatTrigger.setOverlayLayoutParams* or *setDefaultButtonLayoutParams*.

Let us know if you are encountering problems or if you wish to customize features which can’t be changed by modifying the resources. 

### Google Now Integration

The Eva Voice Interface SDK implemented Google Now integration.

End-users will be able to say to their phone:

> OK Google. Search for New York to Tokyo on *MyTravelApp*

And Google will automatically open your application and start a chat with Eva searching for "New York to Tokyo" !

Unfortunately, to complete this integration your voice-enabled application needs to be published to Google Play.
To preview and test the Google Now integration before uploading to the Play Store you can use ADB:

Run from your command line:

```
adb shell am start -a com.google.android.gms.actions.SEARCH_ACTION -e query "New York to Tokyo" your.app.package.here
```

### Other App Setup settings

Remember the app Setup at step two ?

This class has a few optional settings which can be accessed by static methods.

They include:

1. Enable/Disable Eva logs.

2. autoOpenMicrophone - set to True to enable full hands-free mode.
When inactive the end-user needs to press the microphone button to talk each time. When active Eva automatically opens the microphone for the end-user after each question.
We suggest NOT to automatically open the microphone.
**Pros**:   Hands free operation
**Cons**: 

    1. Slower - you have to wait until the entire Text to Speech is spoken by Eva before opening the microphone

    2. Not all users understand that the microphone was automatically activated resulting in many "empty" recordings.

    3. Many people get stage fright when the microphone opens automatically and begin to stutter.

3. appVersion - set this to a short string describing your application version. This may help us figure out if a bug is related to a certain version of the application.

4. locationTracking - set this to false to disable Eva from using a location provider (eg. GPS) to learn the user’s location. 
Location tracking is highly recommended - it allows the user to say things like "Hotel near me".

5. home - Nice to have: Do you know the end user’s current residence? If so please send it to Eva as "home" in free text (e.g. “London”).
The value can be either the Geoname ID for the home location (see [http://www.geonames.org/](http://www.geonames.org/) ) OR a string for the location name. 
Example (Geoname ID): "home=5128581" means home is New York City - [http://www.geonames.org/5128581](http://www.geonames.org/5128581). 
Example (name string): "home=paris" means the home is Paris, France, or "home=paris TX" means home is Paris Texas.

6. resetUserAdaptation - if set to True, erases the user acoustic adaptation data from the previous utterances.
For not using user adaptation from previous utterances, set resetUserAdaptation=True.
For a one-time reset of adaptation data, configure resetUserAdaptation=True, then start a new recording, and set resetUserAdaptation=False before starting any future recordings. 

## Building the SDK from Source Code

Download or clone the SDK -  the code repository is located at [https://github.com/evature/android](https://github.com/evature/android)

### Import to Android Studio (recommended by Google)

1. Import the SDK project to Android Studio:

    1. Choose from the menu:   File > New > Import Project

    2. Choose the build.gradle file in the *evasdk* folder

2. In your project’s *build.grade* add to the dependencies:

``` gradle
dependencies {
   /* your other dependencies here */
   releaseCompile project(path: ':evasdk', configuration: 'release')
   debugCompile project(path: ':evasdk', configuration: 'debug')
}
```


### Alternatively, Import to an Eclipse Workspace (Slightly more complex)

Integrating the SDK into an Eclipse project requires modifying the AndroidManifest.xml and copying the resource files:

1. Create a new Eclipse Android project, from existing sources and choose the SDK folder.

2. Mark the project as a library in the project settings "Android Tab".

3. Add a project dependency on Android Support library v4.

4. Copy the permissions and activities entries from the SDK’s Android Manifest file to your project’s Manifest file.

5. Copy all the resource files (files under the *res* folder) from the SDK to your *res *folder (keeping the sub-directories structure). 

Note that in the future when you wish to upgrade the SDK you may have to update the Android Manifest entries and/or the resource files that were copied from the SDK.


## Support

We would love to hear from you. Ask us anything at [info@evature.com](mailto:info@evature.com)

