package com.evature.evasdk.evaapis.crossplatform;

import com.evature.evasdk.util.DLog;

import java.io.Serializable;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;


public class EvaTime  implements Serializable {

	public String Date; // Represent a specific date and time if given.
	public String Time; // Example: ???????fly to ny 3/4/2010 at 10am???????? results: ???????date????????: ???????2010-04-03????????, ???????time????????: ???????10:00:00????????.
	public String Delta; // May represent:
	// A range starting from Date/Time. Example: ???????next week???????? results: ???????date????????: ???????2010-10-25????????, ???????delta????????: ???????days=+6????????
	// A duration without an anchor date. Example: ???????hotel for a week???????? results: ???????delta????????: ???????days=+7????????
	//
	public String MaxDelta;
	public String MinDelta;
	public String Restriction;
	// A restriction on the date/time requirement. Values can be: ???????no_earlier????????, ???????no_later????????, ???????no_more????????, ???????no_less????????,
	// ???????latest????????, ???????earliest????????
	//
	// Example: ???????depart NY no later than 10am???????? results: ???????restriction????????: ???????no_later????????, ???????time????????: ???????10:00:00????????

	// A boolean flag representing that a particular time has been calculated from other times, and not directly derived
	// from the input text. In most cases if an arrival time to a location is specified, the departure time from the
	// previous location is calculated.
	public Boolean Calculated;

	public EvaTime(JSONObject evaTime, List<String> parseErrors) {
		try {
			if (evaTime.has("Date")) {
				Date = evaTime.getString("Date");
			}
			if (evaTime.has("Time")) {
				Time = evaTime.getString("Time");
			}
			if (evaTime.has("Restriction")) {
				Restriction = evaTime.getString("Restriction");
			}
			if (evaTime.has("Delta")) {
				Delta = evaTime.getString("Delta");
			}
			MaxDelta = evaTime.optString("MaxDelta", null);
			MinDelta = evaTime.optString("MinDelta", null);
			if (evaTime.has("Calculated")) {
				Calculated = evaTime.getBoolean("Calculated");
			}

		} catch (JSONException e) {
			DLog.e("EvaTime", "Error parsing JSON", e);
			parseErrors.add("Error during parsing time: "+e.getMessage());
		}
	}
	
	public Integer daysDelta() {
		return daysDelta(Delta);
	}
	
	public static Integer daysDelta(String delta) {
		Integer result = null;
		if (delta != null) {
			if (delta.startsWith("days=+")) {
				result = Integer.parseInt(delta.substring(6));
			}
		}
		return result;
	}
}
