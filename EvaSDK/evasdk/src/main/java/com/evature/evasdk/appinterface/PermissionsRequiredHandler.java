package com.evature.evasdk.appinterface;

import java.util.ArrayList;

/**
 * Created by iftah on 21/02/2016.
 */
public interface PermissionsRequiredHandler {

    /****
     * Called when Eva Screen cannot start because there are missing runtime permissions
     * Your app is expected to request the user for the permissions and then call EvaTrigger.startSearchByVoice when permissions are granted
     * @param missingPermissions - a array of the missing permissions
     */
    void handleMissingPermissions(String[] missingPermissions);
}
