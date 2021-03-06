package com.evature.evasdk.evaapis.crossplatform.flow;

import com.evature.evasdk.evaapis.crossplatform.EvaLocation;
import com.evature.evasdk.util.DLog;

import java.io.Serializable;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;


public class FlightFlowElement extends FlowElement  implements Serializable {

	private static final String TAG = "FlightFlowElement";
	public String RoundTripSayit;
	public int ActionIndex;
	
	public FlightFlowElement(JSONObject jFlowElement, List<String> parseErrors,
			EvaLocation[] locations) {
		super(jFlowElement, parseErrors, locations);
		
		if (jFlowElement.has("ReturnTrip")) {
			try {
				JSONObject jElement = jFlowElement.getJSONObject("ReturnTrip");
			
				RoundTripSayit = jElement.getString("SayIt");
				ActionIndex = jElement.getInt("ActionIndex");
			} catch (JSONException e) {
				DLog.e(TAG, "Bad EVA reply!  exception processing flight flow Element", e);
				parseErrors.add("Exception during parsing: "+e.getMessage());
			}
		}
	}

	public String getSayIt() {
		if (RoundTripSayit != null) {
			return RoundTripSayit;
		}
		return super.getSayIt();
	}
}
