package com.evature.evasdk;

import com.evature.evasdk.model.ChatItem;

import java.util.ArrayList;

/**
 */
public interface EvaChatApi {
    void addChatItem(final ChatItem chatItem);
    ArrayList<ChatItem> getChatListModel();
}
