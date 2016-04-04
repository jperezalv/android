package com.evature.evasdk.appinterface;

import java.util.ArrayList;

/**
 * Created by iftah on 6/2/15.
 */
public interface InitResult {
    /****
     *
     * @param err - null if there was no error, String description of error if there was
     * @param e - root cause exception
     */
    void initResult(String err, Exception e);


}
