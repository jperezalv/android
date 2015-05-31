package com.evaapis.crossplatform.flow;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.evaapis.crossplatform.EvaLocation;
import com.evature.util.DLog;

public class StatementElement extends FlowElement {
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