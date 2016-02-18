// Relevant example: http://windrealm.org/tutorials/android/listview-with-checkboxes-without-listactivity.php
package com.evature.evasdk.user_interface;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.text.SpannableString;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.ScaleAnimation;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.evature.evasdk.EvatureMainView;
import com.evature.evasdk.R;
import com.evature.evasdk.appinterface.AppSetup;
import com.evature.evasdk.model.ChatItem;


public class ChatAdapter extends ArrayAdapter<ChatItem> {

	private static final String TAG = "ChatAdapter";
	private static final int VIEW_TYPE_COUNT = ChatItem.ChatType.values().length+1;
    public static WeakReference currentEditBox;

    private final ArrayList<ChatItem> mChatList;
	private final EvatureMainView chatView;
	private final LayoutInflater mInflater;

	
	public ChatAdapter(final Context context, final EvatureMainView chatView, int resource, int textViewResourceId, final ArrayList<ChatItem> chatList) {
		super(context, resource, chatList);
		mChatList = chatList;
		this.chatView = chatView;
		mInflater = LayoutInflater.from(context);
	}
	
	@Override
	public int getViewTypeCount(){
	  return VIEW_TYPE_COUNT;
	}
	
	@Override
	public long getItemId(final int position) {
		return position;
	}
	@Override
	public boolean hasStableIds() {
		return true;
	}

	
	@Override
	public ChatItem getItem(int position) {
		// todo: if some items are collapsed then count from start and skip them
		if (position >= mChatList.size()) {
			Log.e(TAG, "Accessing chat item "+position+" but size is "+mChatList.size());
			return null;
		}
		ChatItem chatItem = mChatList.get(position);
		return chatItem;
	};
	
	@Override public int getCount() {
		if (mChatList == null) {
			Log.e(TAG, "null chatList");
			return 0;
		}
		return mChatList.size()+1;
	};

	@Override
	public int getItemViewType(int position){
		if (position >= mChatList.size()) {
			return VIEW_TYPE_COUNT-1;
		}
		ChatItem chatItem = getItem(position);
		return chatItem.getType().ordinal(); 
	}
	

	private View fillerView(View view, ViewGroup parent) {
		if (view == null) {
			view = mInflater.inflate(R.layout.evature_row_filler, parent, false);
			view.setClickable(false);
			view.setEnabled(false);
            view.setVisibility(View.INVISIBLE);
		}
		return view;
	}
	
	
	@SuppressLint("NewApi")
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (position >= mChatList.size()) {
			return fillerView(convertView, parent);
		}
		ChatItem chatItem = getItem(position);
		ChatItem.ChatType viewType = chatItem.getType(); // should be same logic as getItemViewType
		View row = convertView;
		if (row == null) {
//			Ln.d("Creating view for row "+position+" type: "+viewType+"  chat: "+chatItem.getChat());
			switch (viewType) {
			case User:
				row = mInflater.inflate(R.layout.evature_row_user_chat, parent, false);
				UserRowHolder userHolder = new UserRowHolder();
				userHolder.editText = (EditText) row.findViewById(R.id.editText);
				userHolder.editText.setOnEditorActionListener(chatView.editorActionHandler);
				userHolder.label = (TextView) row.findViewById(R.id.label);
				userHolder.chatbubble = row.findViewById(R.id.chat_bubble);
				userHolder.inEdit = row.findViewById(R.id.edit_chat_item);
				row.setTag(R.id.evature_chat_row_holder, userHolder);
				break;
			case MultiChoiceQuestion:
			case Eva:
			case EvaWelcome:
				row = mInflater.inflate(R.layout.evature_row_eva_chat, parent, false);
				EvaRowHolder evaHolder = new EvaRowHolder();
				//evaHolder.cruisesFoundIcon = (ImageView) row.findViewById(R.id.cruises_found_icon); // TODO
				evaHolder.searchingProgress = (ProgressBar) row.findViewById(R.id.progressBar_search);
				evaHolder.subLabel = (TextView) row.findViewById(R.id.sub_label);
				evaHolder.label = (TextView) row.findViewById(R.id.label);
				evaHolder.chatbubble = row.findViewById(R.id.chat_bubble);
				row.setTag(R.id.evature_chat_row_holder, evaHolder);		
				break;
//			case MultiChoiceAnswer:
//				row = mInflater.inflate(R.layout.evature_row_dialog, parent, false);
//				break;
			}
		}
		else {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				// recycled view - undo animation done by dismiss
				row.setScaleY(1.0f);
			}
//			Ln.d("reusing view for row "+position+" type: "+viewType+"  chat: "+chatItem.getChat());
		}
		
		RowHolder holder = (RowHolder) row.getTag(R.id.evature_chat_row_holder);
		if (holder == null) {
			Log.w(TAG, "Null rowHolder?");
			return null;
		}
		
		TextView label = holder.label;
		if (label == null) {
			// rare bug that was hard to reproduce - I believe is solved now, but just in case, leaving the work-around fix 
			Log.e(TAG, "No label in Row?");
			if (convertView != null) {
				// try again without view recycling
				return getView(position, null, parent);
			}
			row.setTag(chatItem);
			return row;
		}
		else {
			SpannableString chatText = chatItem.getChat();
			if (chatText == null || "".equals(chatText.toString())) {
				label.setVisibility(View.GONE);
			}
			else {
				label.setText(chatText);
				label.setVisibility(View.VISIBLE);
			}
		}

		// animate the chat bubble appearing - if it wasn't done already for this chatItem
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB 
				&& chatItem.wasAlreadyAnimated() == false) {
			View chatbubble = holder.chatbubble;
			int delay = 0;
			int duration = 350;
			ObjectAnimator animator;
			if (viewType == ChatItem.ChatType.User) {
				animator  = ObjectAnimator.ofFloat(chatbubble, "translationX", 0 - parent.getWidth(), 0);
				chatbubble.setTranslationX(-parent.getWidth());
			}
			else {
				animator = ObjectAnimator.ofFloat(chatbubble, "translationX", parent.getWidth(), 0);
				chatbubble.setTranslationX(parent.getWidth());
				delay = 250;
			}
			
			chatItem.setAlreadyAnimated();
			animator.setStartDelay(delay);
			animator.setDuration(duration);
			animator.start();
			
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
		    	Animator heightAnim = ObjectAnimator.ofFloat(chatbubble, "translationZ", 12f, 0f);
		    	Animator yAnim = ObjectAnimator.ofFloat(chatbubble, "translationY", -12f, 0f);
		    	chatbubble.setTranslationZ(12f);
		    	chatbubble.setTranslationY(-12f);
				heightAnim.setStartDelay(duration+delay);
				heightAnim.setDuration(duration);
				yAnim.setStartDelay(duration+delay);
				yAnim.setDuration(duration);
				heightAnim.start();
				yAnim.start();
			}
		}
		
		// some row types require more than label setting...
		switch (viewType) {
			
		/*case DialogAnswer:
			DialogAnswerChatItem dialogItem = (DialogAnswerChatItem)chatItem;
			TextView respInd = (TextView)row.findViewById(R.id.response_index);
			respInd.setText( Integer.toString(dialogItem.getIndex() + 1) );
			if (dialogItem.isChosen()) {
				label.setTypeface(null, Typeface.BOLD);
			}
			else {
				label.setTypeface(null, Typeface.NORMAL);
			}
			break;
			*/
			
		case User:
//			
			UserRowHolder userHolder = (UserRowHolder) holder; 
			
			if (chatItem.getStatus() == ChatItem.Status.EDITING) {
				userHolder.editText.setText(chatItem.getChat().toString());
				label.setVisibility(View.GONE);
				userHolder.inEdit.setVisibility(View.VISIBLE);
                if (AppSetup.tapToEditChat) {
                    ChatAdapter.currentEditBox = new WeakReference(userHolder.editText);
                }
			}
			else {
				label.setVisibility(View.VISIBLE);
				userHolder.inEdit.setVisibility(View.GONE);
                if (AppSetup.tapToEditChat) {
                    if (ChatAdapter.currentEditBox != null &&
                            userHolder.editText == ChatAdapter.currentEditBox.get()) {
                        ChatAdapter.currentEditBox = null;
                    }
                }
			}
			break;
		
		case MultiChoiceQuestion:
		case Eva:
		case EvaWelcome:
			EvaRowHolder evaHolder = (EvaRowHolder) holder;
			if (chatItem.getSubLabel() == null) {
				evaHolder.subLabel.setVisibility(View.GONE);
				evaHolder.searchingProgress.setVisibility(View.GONE);
//				evaHolder.cruisesFoundIcon.setVisibility(View.GONE);
			}
			else {
				evaHolder.subLabel.setText(chatItem.getSubLabel());
				evaHolder.subLabel.setVisibility(View.VISIBLE);
				evaHolder.searchingProgress.setVisibility(chatItem.getStatus() == ChatItem.Status.SEARCHING ? View.VISIBLE : View.GONE);
//				evaHolder.cruisesFoundIcon.setVisibility(chatItem.getStatus() == ChatItem.Status.HAS_RESULTS ? View.VISIBLE :View.GONE);
			}
		}
		row.setTag(chatItem);
		return row;
	}
	
	private static class RowHolder {
		View chatbubble;
		TextView label;
	}
	
	private static class UserRowHolder extends RowHolder {
		public View inEdit;
		public EditText editText;
	}
	
	private static class EvaRowHolder extends RowHolder {
//		public ImageView cruisesFoundIcon;
		public ProgressBar searchingProgress;
		public TextView subLabel;
	}

	

	public void dismissItem(ListView listView, int itemIndex, DismissStep step) {
		dismissItems(listView, itemIndex, itemIndex+1, step);
	}
	
	public enum DismissStep {
		ANIMATE_DISMISS,    // only animate the dismiss - do not actually delete the items
		DO_DELETE, 		    // actually delete the items
		ANIMATE_RESTORE		// ooops, delete has failed or was canceled or undone - animate restore the items (can't be done if already deleted)
	}

	@SuppressLint("NewApi")
	public void dismissItems(ListView listview, int start, int end, DismissStep step) {
		if (start >= mChatList.size()) {
			return;
		}
		if (end > mChatList.size()) {
			end = mChatList.size();
		}
		final List<ChatItem> itemsToDismiss = mChatList.subList(start, end);
		
		if (step == DismissStep.DO_DELETE) {
			for (ChatItem item: itemsToDismiss) {
				View view = listview.findViewWithTag(item);
				if (view != null) {
					view.clearAnimation();
				}
			}
			itemsToDismiss.clear();
			ChatAdapter.this.notifyDataSetChanged();
		}
		else { 
			float from = 1f;
			float to = 0.01f;
			if (step == DismissStep.ANIMATE_RESTORE) {
				from = 0.01f;
				to = 1f;
			}
			for (ChatItem item: itemsToDismiss) {
				View view = listview.findViewWithTag(item);
				if (view != null) {
					ScaleAnimation animation = new ScaleAnimation(1f, 1f, from, to, ScaleAnimation.RELATIVE_TO_SELF, 0.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
					animation.setDuration(400);
					animation.setFillAfter(true);
					view.clearAnimation();
					view.startAnimation(animation);
				}
			}
		}
	}
	
}
