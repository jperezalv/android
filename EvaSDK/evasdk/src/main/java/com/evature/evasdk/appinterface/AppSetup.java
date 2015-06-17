/* AUTO-GENERATED FILE.  DO NOT MODIFY.
 *
 * This class was automatically generated by the FMPP tool.
 * It should not be modified by hand.
 * Changes to this file will be overwritten next time the project is built.
 *
 * To make changes edit the file at src/main/codegen/templates/com/evature/evasdk/EvaAppSetup.java
 *
 * Source last modified on Jun 2, 2015 04:27 PM IDT
 * Generated on Jun 2, 2015 05:36 PM IDT
 */


package com.evature.evasdk.appinterface;


import android.content.Context;

import com.evature.evasdk.evaapis.EvaComponent;
import com.evature.evasdk.evaapis.EvaSpeak;
import com.evature.evasdk.util.DLog;

import java.util.HashMap;

public class AppSetup {
    private static final String TAG = "AppSetup";

    // mandatory parameters:
    public static String apiKey;
    public static String siteCode;

    // optional parameters:
    public static boolean semanticHighlightingTimes = true;
    public static boolean semanticHighlightingLocations = true;
    public static boolean autoOpenMicrophone = false;  // true for hands free usage
    public static boolean locationTracking = true;     // true to enable Eva tracking location - used for understanding "home" location

    public static String deviceId;   // if you have a unique identifier for the user/device (leave null and Eva will generate an internal ID)
    public static String appVersion; // recommended - will be passed to Eva for debugging and tracking

    public static String scopeStr = "";

    public static HashMap<String, String> extraParams = new HashMap<String,String>();

    public static void setScope(AppScope... args) {
        StringBuilder builder = new StringBuilder();
        for (AppScope s : args) {
            builder.append(s.toString());
        }
        scopeStr = builder.toString();
    }

//    private static EvaSpeak evaSpeak;


    /***
     * Setup Eva and wire the App callbacks
     * @param apiKey
     * @param siteCode
     * @param appHandler - inherits a set of interfaces from com.evature.evasdk.
     */
    public static void initEva(/*Context context, */String apiKey, String siteCode, Object appHandler) {
        AppSetup.apiKey = apiKey;
        AppSetup.siteCode = siteCode;
        EvaComponent.evaAppHandler = appHandler;
//        evaSpeak = new EvaSpeak(context);
    }

//    public static EvaSpeak

    public static void evaLogs(boolean enabled) {
        DLog.DebugMode = enabled;
    }
}