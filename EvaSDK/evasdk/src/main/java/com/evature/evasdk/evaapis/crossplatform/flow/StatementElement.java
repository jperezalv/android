package com.evature.evasdk.evaapis.crossplatform.flow;

import java.io.Serializable;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.evature.evasdk.evaapis.crossplatform.EvaLocation;
import com.evature.evasdk.util.DLog;

public class StatementElement extends FlowElement  implements Serializable {
	private static final String TAG = "StatementElement";

	public enum StatementTypeEnum {
		Understanding, Chat, Unsupported, Unknown_Expression,
		Other
	}

	
	public StatementTypeEnum StatementType;

	public StatementElement(JSONObject jFlowElement, List<String> parseErrors, EvaLocation[] locations) {
		super(jFlowElement, parseErrors, locations);
		
		try {
			StatementType = StatementTypeEnum.valueOf(jFlowElement.getString("StatementType").replace(" ", "_"));
		}
		catch(IllegalArgumentException e) {
			DLog.w(TAG, "Unexpected StatementType in Flow element", e);
			StatementType = StatementTypeEnum.Other;
		}
		catch(JSONException e) {
			parseErrors.add("Exception during parsing Reply element: "+e.getMessage());	
		}
	}
}