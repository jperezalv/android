package com.evature.evasdk.appinterface;

import android.content.Context;

import com.evature.evasdk.evaapis.crossplatform.EvaLocation;
import com.evature.evasdk.evaapis.crossplatform.EvaTravelers;
import com.evature.evasdk.evaapis.crossplatform.HotelAttributes;
import com.evature.evasdk.evaapis.crossplatform.HotelAttributes.Amenities;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

/**
 * Created by iftah on 6/2/15.
 */
public interface HotelCount {

    void getHotelCount(Context context,
                           boolean isComplete, EvaLocation location,
                           Date arriveDateMin, Date arriveDateMax,
                           Integer durationMin, Integer durationMax,
                           EvaTravelers travelers,
                           ArrayList<HotelAttributes.HotelChain> chains,

                           // The hotel board:
                           Boolean selfCatering,
                           Boolean bedAndBreakfast,
                           Boolean halfBoard,
                           Boolean fullBoard,
                           Boolean allInclusive,
                           Boolean drinksInclusive,

                           // The quality of the hotel, measure in Stars
                           Integer minStars,
                           Integer maxStars,

                           HashSet<Amenities> amenities,
                       AsyncCountResult callback);
}

