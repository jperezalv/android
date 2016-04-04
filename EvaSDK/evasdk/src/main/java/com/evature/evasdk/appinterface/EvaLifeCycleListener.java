package com.evature.evasdk.appinterface;

import com.evature.evasdk.EvaChatApi;

/**
 * Created by iftah on 22/02/2016.
 */
public interface EvaLifeCycleListener {

    void onPause();
    void onResume(EvaChatApi chatScreen);
}
