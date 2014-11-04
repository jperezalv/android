package com.virtual_hotel_agent.search.models.chat;

import java.util.ArrayList;
import java.util.HashMap;

import android.os.Bundle;

import com.evaapis.crossplatform.EvaApiReply;
import com.evature.util.Log;
import com.google.inject.Singleton;

@Singleton
public class ChatItemList extends ArrayList<ChatItem> {
	static final String TAG = "ChatItemList";

	public void saveInstanceState(Bundle instanceState) {
//		Bundle replyCache = new Bundle();
//		for (ChatItem ci: this) {
//			if (ci.getEvaReply() != null) {
//				if (replyCache.containsKey(ci.getEvaReply().transactionId) == false) {
//					replyCache.putString(ci.getEvaReply().transactionId, ci.getEvaReply().JSONReply.toString());
//				}
//			}
//		}
//		instanceState.putBundle("reply_cache", replyCache);
//		instanceState.putS("mChatListEva", this);
//		//instanceState.put
//		ArrayList<String> transactionIds = new ArrayList<String>();
//		for (ChatItem ci: this) {
//			transactionIds.add(ci.getEvaReply() == null ? "" : ci.getEvaReply().transactionId);
//		}
//		instanceState.putStringArrayList("chat_items_transactions", transactionIds);
		instanceState.putSerializable("chat-item-list", this);
	}
	
	public void loadInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
//			Log.d(TAG, "Loading ChatItems from savedInstanceState");
//			
//			HashMap<String, EvaApiReply> replyCache = new HashMap<String, EvaApiReply>();
//			Bundle savedReplies = savedInstanceState.getBundle("reply_cache");
//			for (String key : savedReplies.keySet()) {
//				String json = savedReplies.getString(key);
//				EvaApiReply reply = new EvaApiReply(json);
//				replyCache.put(reply.transactionId, reply);
//			}
//			
//			// Restore last state for checked position.
//			ArrayList<ChatItem> parcelableArrayList = savedInstanceState.getParcelableArrayList("mChatListEva");
//			ArrayList<String> transactionIds = savedInstanceState.getStringArrayList("chat_items_transactions");
//			int i=0;
			ArrayList<ChatItem> list = (ArrayList<ChatItem>) savedInstanceState.getSerializable("chat-item-list");
			clear();
			if (list != null) {
				for (ChatItem ci : list) {
					add(ci);
				}
			}
//				String transaction = transactionIds.get(i++);
//				if (transaction.equals("") == false) {
//					ci.setApiReply(replyCache.get(transaction));
//				}
//			}
		}
	}


}
