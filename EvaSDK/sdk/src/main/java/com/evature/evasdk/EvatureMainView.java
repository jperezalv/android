package com.evature.evasdk;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.evature.evasdk.evaapis.EvaException;
import com.evature.evasdk.evaapis.android.EvaSpeechRecogComponent;
import com.evature.evasdk.evaapis.android.SoundLevelView;
import com.evature.evasdk.evaapis.android.SpeechAudioStreamer;
import com.evature.evasdk.model.ChatItem;
import com.evature.evasdk.util.DLog;


/****
 *  User interface parts of the Evature chat screen 
 */
public class EvatureMainView implements OnItemClickListener  {

	private static final String TAG = "EvatureMainView";
	private ImageButton mSearchButton;
	private SoundLevelView mSoundView;
	private ImageButton mUndoButton;
	private ImageButton mResetButton;
	private ImageButton mVolumeButton;
	private View mSearchButtonCont;
	private ProgressWheel mProgressBar;
	
	private ArrayList<ChatItem> mChatListModel;
	private ChatAdapter mChatAdapter;
	
	private int mEditedChatItemIndex = -1;
	private SearchByVoiceActivity mEvaActivity;
	private ListView mChatListView;
	
	private boolean mSideButtonsVisible;

	private WeakReference<Handler> mUpdateLevel;
	public static final long UPDATE_SOUND_VIEW_INTERVAL = 60; // update the soundView every such ms
	
	final static float INITIAL_SCALE_SIDE_BUTTON = 0.64f; // should be in sync with XML scaleX/Y of restart_button and undo_button

	
	enum SearchButtonIcon{
		MICROPHONE,
		FLAT,
		NONE
	};
	
	SearchButtonIcon mSearchButtonIcon = SearchButtonIcon.MICROPHONE;
	
	
	@SuppressLint("NewApi")
	public EvatureMainView(final SearchByVoiceActivity mainActivity, ArrayList<ChatItem> chatList) {
		mEvaActivity = mainActivity;
		mSearchButton = (ImageButton) mainActivity.findViewById(R.id.voice_search_button);
		mSoundView = (SoundLevelView)mainActivity.findViewById(R.id.surfaceView_sound_wave);
		mUndoButton = (ImageButton)mainActivity.findViewById(R.id.undo_button);
		mResetButton = (ImageButton)mainActivity.findViewById(R.id.restart_button);
		mSearchButtonCont = mainActivity.findViewById(R.id.voice_search_container);
		mProgressBar = (ProgressWheel)mainActivity.findViewById(R.id.progressBarEvaProcessing);
		
		//mVolumeButton = (ImageButton) mainActivity.findViewById(R.id.volume_button);
		
		mChatListView = (ListView) mainActivity.findViewById(R.id.chat_list);
		
		// Connect the data of the chat history to the view:
		if (chatList == null) {
			mChatListModel = new ArrayList<ChatItem>();
		}
		else {
			mChatListModel = chatList;
		}
		mChatAdapter = new ChatAdapter(mEvaActivity, this, R.layout.evature_row_eva_chat, R.id.label, mChatListModel);

		mChatListView.setAdapter(mChatAdapter);
		mChatListView.setOnItemClickListener(this);
		
		mSoundView.setColor(0xffffffff);
		mSoundView.setAlign(Gravity.RIGHT);
		
		//mProgressBar.getIndeterminateDrawable().setColorFilter(0xffffffff, android.graphics.PorterDuff.Mode.SRC_ATOP);
		mProgressBar.spin();
		
		setupSearchButtonDrag();
	}
	
	
	private static void scaleButton(final View button, int duration, float fromScale, float toScale) {
		button.setVisibility(View.VISIBLE);
		// no idea why the 1.5 factor is needed, but without it the size is smaller than when using setScale!
		ScaleAnimation anim = new ScaleAnimation(fromScale, 1.5f*toScale, fromScale, 1.5f*toScale, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f );
		anim.setDuration(duration);
		anim.setFillAfter(true);
		anim.setFillBefore(true);
		if (toScale > 0) {
			anim.setInterpolator(new OvershootInterpolator());
			button.setEnabled(true);
		}
		else {
			button.setEnabled(false);
			anim.setAnimationListener(new AnimationListener() {
				@Override public void onAnimationStart(Animation animation) {}
				@Override public void onAnimationRepeat(Animation animation) {}
				@Override
				public void onAnimationEnd(Animation animation) {
			        button.setVisibility(View.INVISIBLE);
				}
			});
		}
		button.startAnimation(anim);
	}
	
	@SuppressLint("NewApi")
	private static void animateButton(final View button, String animProperty, int duration, float from, float to) {
		// TODO: use http://nineoldandroids.com/ (for compatibility with older devices)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			button.setVisibility(View.VISIBLE);
			Animator propAnim = ObjectAnimator.ofFloat(button, animProperty, from, to);
			propAnim.setInterpolator(new OvershootInterpolator());
			propAnim.setDuration(duration);
			propAnim.start();
			
		}
	}

	
	@SuppressLint("NewApi")
	private void toggleSideButtons(boolean show) {
		if (mSideButtonsVisible == show) {
			return;
		}
		mSideButtonsVisible = show;
		// use animation to show/hide buttons
		int animDuration = 400;
		if (show) {
			// turn search button to shadow
			showMicButton(SearchButtonIcon.FLAT);
			animateButton(mSearchButton, "alpha", animDuration/2, 1.0f, 0.7f);
			
			// show undo/reset
			scaleButton(mUndoButton, animDuration, 0f, INITIAL_SCALE_SIDE_BUTTON);
			scaleButton(mResetButton, animDuration, 0f, INITIAL_SCALE_SIDE_BUTTON);
		}
		else {
			// show search button
			showMicButton(SearchButtonIcon.MICROPHONE);
			animateButton(mSearchButton, "alpha", animDuration, 0.7f, 1.0f);
			animateButton(mSearchButtonCont, "translationX", animDuration, mSearchButtonCont.getTranslationX(), 0f);
			
			
			// hide undo/reset button
			scaleButton(mUndoButton, animDuration/2, INITIAL_SCALE_SIDE_BUTTON, 0f);
			scaleButton(mResetButton, animDuration/2, INITIAL_SCALE_SIDE_BUTTON, 0f);
		}
	}
	
	@SuppressLint("NewApi")
	private static void setTranslationX(View view, float translationX) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
	        view.setTranslationX(translationX);
	    } else {
	        TranslateAnimation anim = new TranslateAnimation(translationX, translationX, 0, 0);
	        anim.setFillAfter(true);
	        anim.setDuration(0);
	        view.startAnimation(anim);
	    }
	}
	
	private void setupSearchButtonDrag() {
		Resources r = mEvaActivity.getResources();
		final int margin = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, r.getDisplayMetrics());
		
		mSearchButton.setOnTouchListener(new View.OnTouchListener() {
			boolean hoveringReset = false;
			boolean hoveringUndo = false;
			
			@SuppressLint("NewApi")
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				
				switch (event.getAction()) {
				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_CANCEL:
					toggleSideButtons(false);
					if (event.getEventTime() - event.getDownTime() <= 500) {
						float x = event.getRawX() - mSearchButtonCont.getLeft();
				        float y = event.getRawY() - mSearchButtonCont.getTop();
				        if (y > mSearchButton.getTop()- margin && 
				        		x < mSearchButton.getRight() + margin && x > mSearchButton.getLeft() - margin ) {
				        	// mSearchButtonCont.performClick(); for some reason this doesn't work when the searchButton is embedded inside FrameLayout
				        	mEvaActivity.buttonClickHandler(mSearchButton);
				        }
					}
					if (hoveringUndo) {
						mUndoButton.performClick();
						mUndoButton.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
					}
					if (hoveringReset) {
						mResetButton.performClick();
						mResetButton.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
					}
					hoveringUndo = hoveringReset = false;
					break;
					
				case MotionEvent.ACTION_DOWN:
					hoveringUndo = false;
					hoveringReset = false;
					mSideButtonsVisible = false;
					break;
				
				case MotionEvent.ACTION_MOVE:
					float x = event.getRawX();
			        float y = event.getRawY();
			        // moved up, or recording started
			        if (y < mSearchButtonCont.getTop()+ mSearchButton.getTop()- margin
			        		||  isRecording()) {
			        	toggleSideButtons(false);
						hoveringUndo = false;
						hoveringReset = false;
			        	break;
			        }
			        
			        int searchRight = mSearchButtonCont.getLeft() + mSearchButton.getRight();
			        int searchLeft = mSearchButtonCont.getLeft() + mSearchButton.getLeft();
			        
					if (mSideButtonsVisible == false && isRecording() == false) {
						// show side buttons if long-press of beginning drag
						if (event.getEventTime() - event.getDownTime() > 500) {  
							mSearchButton.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
						}
						if ((event.getEventTime() - event.getDownTime() > 500) ||
								x > searchRight + margin || 
								x < searchLeft - margin ) {
							toggleSideButtons(true);
//							// show drag shadow - if honeycomb+
//							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//								if (mSearchButtonShadow == null) {
//									mSearchButtonShadow = new View.DragShadowBuilder();
//								}
//								mSearchButton.startDrag(ClipData.newPlainText("Nada",  "Nothing"), (View.DragShadowBuilder)mSearchButtonShadow, null, 0);
//							}
						}
					}
					if (mSideButtonsVisible) {
						int searchCenter = (searchRight+searchLeft)/2;
						float delta = x - searchCenter;
					
						if (x > searchCenter+margin) {
							int resetCenter = (mResetButton.getRight()+ mResetButton.getLeft())/2;
							delta = Math.min(delta, resetCenter - searchCenter);
							// linearly scale button up based on distance
							float fraction =  Math.min(1f, (x - searchRight) / Math.max(1f, (resetCenter - searchRight)));
							
							// TODO: use com.nineoldandroids.view.ViewHelper for older devices
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
								float scale = INITIAL_SCALE_SIDE_BUTTON + (1f-INITIAL_SCALE_SIDE_BUTTON)*fraction;
								mResetButton.clearAnimation();
								mResetButton.setScaleX(scale);
								mResetButton.setScaleY(scale);
								if (mUndoButton.getAnimation() == null || mUndoButton.getAnimation().hasEnded()) {
									mUndoButton.setScaleX(INITIAL_SCALE_SIDE_BUTTON);
									mUndoButton.setScaleY(INITIAL_SCALE_SIDE_BUTTON);
								}
							}
							hoveringReset = fraction > 0.7;
						}
						else if (x < searchCenter-margin) {
							int undoCenter = (mUndoButton.getRight()+mUndoButton.getLeft())/2;
							delta = Math.max(delta, undoCenter - searchCenter);
							// linearly scale button up based on distance
							float fraction =  Math.min(1f, (searchLeft - x) / Math.max(1f, (searchLeft - undoCenter)));
							
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
								float scale = INITIAL_SCALE_SIDE_BUTTON + (1f-INITIAL_SCALE_SIDE_BUTTON)*fraction;
								mUndoButton.clearAnimation();
								mUndoButton.setScaleX(scale);
								mUndoButton.setScaleY(scale);
								if (mResetButton.getAnimation() == null || mResetButton.getAnimation().hasEnded()) {
									mResetButton.setScaleX(INITIAL_SCALE_SIDE_BUTTON);
									mResetButton.setScaleY(INITIAL_SCALE_SIDE_BUTTON);
								}
							}
							hoveringUndo = fraction > 0.7;
						}

						setTranslationX(mSearchButtonCont, delta);
						
					}
					break;
				}
				
				return true;
			}
		});
		
	}
	
	public boolean isRecording() {
		if (mUpdateLevel != null && mUpdateLevel.get() != null) {
			SearchHandler  handler = (SearchHandler)mUpdateLevel.get();
			if (handler != null && handler.isRecording()) {
				return true;
			}
		}
		return false;
	}

	/**********  UI of Recording button state **************/
	
	@SuppressLint("NewApi")
	private void showMicButton(SearchButtonIcon what) {
		if (mSearchButtonIcon == what) {
			return;
		}
		if (what == SearchButtonIcon.FLAT) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				mSearchButton.clearAnimation();
				AnimatedVectorDrawable animatedDrawable = (AnimatedVectorDrawable) mSearchButton.getResources().getDrawable(R.drawable.animated_microphone);
				mSearchButton.setImageDrawable(animatedDrawable);
				animatedDrawable.start();

				// hide the flat microphone when the animation completes
				mSearchButton.postDelayed(new Runnable() {
					@Override
					public void run() {
						if (mSearchButtonIcon == SearchButtonIcon.FLAT) {
							mSearchButton.setImageDrawable(null);
						}
					}
				}, 450); // should be in sync with the animation duration defined in the xml
			}
			else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				mSearchButton.clearAnimation();
				ValueAnimator animator = ValueAnimator.ofInt(mSearchButton.getPaddingTop(), mSearchButton.getHeight()/2);
				animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
				    @Override
				    public void onAnimationUpdate(ValueAnimator valueAnimator){
				    	int val =  ((Integer) valueAnimator.getAnimatedValue()).intValue();
				        mSearchButton.setPadding(mSearchButton.getPaddingLeft(), 
				        		val, mSearchButton.getPaddingRight(), val);
				    }
				});
				animator.setDuration(450);
				animator.start();
			}
			else {
				mSearchButton.setImageDrawable(null);
			}
		}
		else if (what == SearchButtonIcon.MICROPHONE) {
			if (isRecording()) {
				DLog.d(TAG, "Not setting to microphone icon because in recording");
				return; // don't change back to icon if recording
			}
//			if (mProgressBar.getVisibility() == View.VISIBLE) {
//				// wait with icon switch until progressbar is hidden
//				pendingIconSwitch = true;
//				return;
//			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				mSearchButton.clearAnimation();
				AnimatedVectorDrawable animatedDrawable = (AnimatedVectorDrawable) mSearchButton.getResources().getDrawable(R.drawable.animated_microphone_reverse);
				mSearchButton.setImageDrawable(animatedDrawable);
				animatedDrawable.start();
			}
			else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				mSearchButton.clearAnimation();
				mSearchButton.setImageResource(R.drawable.evature_microphone_icon);
				ValueAnimator animator = ValueAnimator.ofInt(mSearchButton.getHeight()/2, 0);
				animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
				    @Override
				    public void onAnimationUpdate(ValueAnimator valueAnimator){
				    	int val =  ((Integer) valueAnimator.getAnimatedValue()).intValue();
				        mSearchButton.setPadding(mSearchButton.getPaddingLeft(), 
				        		val, mSearchButton.getPaddingRight(), val);
				    }
				});
				animator.setDuration(450);
				animator.start();
			}
			else {
				mSearchButton.setImageResource(R.drawable.evature_microphone_icon);
			}
		}
		else {
			mSearchButton.setImageDrawable(null);
		}
		mSearchButtonIcon = what;
	}

	
	public void activateSearchButton() {
		DLog.d(TAG, "activate search button");
		mSearchButton.post(new Runnable() {
		    @Override
		    public void run() {
		    	//to show the recording is active
		    	mSearchButton.setEnabled(true);
		    	mProgressBar.setVisibility(View.GONE);
				mSearchButton.setBackgroundResource(R.drawable.evature_transition_button_activate);
				TransitionDrawable drawable = (TransitionDrawable) mSearchButton.getBackground();
				drawable.startTransition(100);
				showMicButton(SearchButtonIcon.FLAT);
		    }
		});
	}
	
	@SuppressLint("NewApi")
	public void flashSearchButton(final int times) {
		// flash is used to draw user attention to button
		
		if (isRecording() || mSideButtonsVisible) {
			return; // do not flash button while recording, or while user is choosing side button
		}
		if (times == 0) {
			return;
		}
		
		if (times == -1) {
			DLog.d(TAG, "flash search button");
		}
		mSearchButton.post(new Runnable() {
		    @Override
		    public void run() {
				if (times > 0) {
					mProgressBar.setVisibility(View.GONE);
					showMicButton(SearchButtonIcon.MICROPHONE);
					mSearchButton.setEnabled(true);
					bounceView(times, mSearchButtonCont);
				}
				mSearchButton.setBackgroundResource(R.drawable.evature_transition_button_activate);
				TransitionDrawable drawable = (TransitionDrawable) mSearchButton.getBackground();
				final int duration = 150;

				drawable.startTransition(duration);
				mSearchButton.postDelayed(new Runnable() {
				    @Override
				    public void run() {
				      // reverse the transition after it completes
				    	mSearchButton.setBackgroundResource(R.drawable.evature_transition_button_activate);
				    	TransitionDrawable drawable = (TransitionDrawable) mSearchButton.getBackground();
				    	drawable.reverseTransition(duration);
				    	mSearchButton.postDelayed(new Runnable() {
						    @Override
						    public void run() {
						    	flashSearchButton(-(Math.abs(times)-1));
						    }
				    	}, duration);
				    }
				}, duration);
		    }
		});
	}

	public static void bounceView(final int height, View view) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			bounceView(height, view, "translationY", -30);
		}
//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//			bounceView(height, view, "translationZ", 5);
//		}
	}

	@SuppressLint("NewApi")
	private static void bounceView(final int height, View view, String property, int heightFactor) {
		ObjectAnimator anim1 = ObjectAnimator.ofFloat(view, property, 0, heightFactor*height);
		final ObjectAnimator anim2 = ObjectAnimator.ofFloat(view, property, heightFactor*height, 0);
		anim1.setDuration(70*height);
		anim2.setDuration(300*height);
		anim1.setInterpolator(new DecelerateInterpolator());
		anim2.setInterpolator(new BounceInterpolator());
		anim1.addListener(new AnimatorListenerAdapter() {
		    @Override
		    public void onAnimationEnd(Animator animation) {
		        super.onAnimationEnd(animation);
		        anim2.start();
		    }
		});
		anim1.start();
	}
	
	public void disableSearchButton() {
		DLog.d(TAG, "disable search button");
		mSearchButton.post(new Runnable() {
			@Override
			public void run() {
				mProgressBar.setVisibility(View.VISIBLE);
				mSearchButton.setEnabled(false);
				showMicButton(SearchButtonIcon.NONE);
				mSearchButton.setBackgroundResource(R.drawable.evature_transition_button_activate);
				TransitionDrawable drawable = (TransitionDrawable) mSearchButton.getBackground();
				drawable.reverseTransition(50);
				mSearchButton.postDelayed(new Runnable() {
					@Override
					public void run() {
						mSearchButton.setBackgroundResource(R.drawable.evature_transition_button_dectivate);
						TransitionDrawable drawable = (TransitionDrawable) mSearchButton.getBackground();
						drawable.startTransition(50);
					}
				}, 60);
			}
		});
	}
	
	// return to normal button view
	public void deactivateSearchButton() {
		DLog.d(TAG, "deactivate search button");
		mSearchButton.post(new Runnable() {
		    @Override
		    public void run() {
		    	mProgressBar.setVisibility(View.GONE);
		    	mSearchButton.setEnabled(true);
		    	showMicButton(SearchButtonIcon.MICROPHONE);
				mSearchButton.setBackgroundResource(R.drawable.evature_transition_button_activate);
				TransitionDrawable drawable = (TransitionDrawable) mSearchButton.getBackground();
				drawable.reverseTransition(100);
				mSearchButton.postDelayed(new Runnable() {
				    @Override
				    public void run() {
				    	mSearchButton.setBackgroundResource(R.drawable.evature_transition_button_activate);
				    	TransitionDrawable drawable = (TransitionDrawable) mSearchButton.getBackground();
				    	drawable.resetTransition();
				    }
				}, 110);
		    }
		});
	}
	
	
	@SuppressLint("NewApi")
	public void flashBadSearchButton(final int times) {
		if (isRecording() || mSideButtonsVisible) {
			return;
		}
		
		if (times == 0) {
			return;
		}
		
		if (times == -1) {
			DLog.d(TAG, "flash bad search button");
		}
				
		mSearchButton.post(new Runnable() {
		    @Override
		    public void run() {
		    	if (times > 0) {
		    		mProgressBar.setVisibility(View.GONE);
		    		showMicButton(SearchButtonIcon.MICROPHONE);
		    		mSearchButton.setEnabled(true);
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) { 
						ObjectAnimator anim = ObjectAnimator.ofFloat(mSearchButtonCont, "translationX", 50, -50f);
						anim.setDuration(110);
						anim.setRepeatCount(times);
						anim.setRepeatMode(ValueAnimator.REVERSE);
						anim.setInterpolator(new AccelerateDecelerateInterpolator());
						anim.addListener(new AnimatorListenerAdapter() {
						    @Override
						    public void onAnimationEnd(Animator animation) {
						        super.onAnimationEnd(animation);
						        mSearchButtonCont.setTranslationX(0);
						    }
						});
						anim.start();
					}
					else {
						TranslateAnimation anim = new TranslateAnimation(50f, -50f, 0, 0);
				        anim.setDuration(110);
				        anim.setRepeatCount(times);
				        anim.setInterpolator(new AccelerateDecelerateInterpolator());
				        anim.setAnimationListener(new AnimationListener() {
							
							@Override
							public void onAnimationStart(Animation animation) {
							}
							
							@Override
							public void onAnimationRepeat(Animation animation) {
							}
							
							@Override
							public void onAnimationEnd(Animation animation) {
								setTranslationX(mSearchButtonCont, 0);
							}
						});
				        mSearchButtonCont.startAnimation(anim);
					}
				}
		    	
				mSearchButton.setBackgroundResource(R.drawable.evature_transition_button_bad);
				TransitionDrawable drawable = (TransitionDrawable) mSearchButton.getBackground();
				drawable.startTransition(100);
				mSearchButton.postDelayed(new Runnable() {
				    @Override
				    public void run() {
						mSearchButton.setBackgroundResource(R.drawable.evature_transition_button_bad);
						TransitionDrawable drawable = (TransitionDrawable) mSearchButton.getBackground();
						drawable.reverseTransition(150);
						// repeat
						mSearchButton.postDelayed(new Runnable() { 
							public void run() {		flashBadSearchButton(-(Math.abs(times)-1)); }
						}, 110);
				    }
				}, 110);
		    }
		});
	}
	
	private boolean mainButtonsShown = true;
	
	public boolean areMainButtonsShown() {
		return mainButtonsShown && !mSideButtonsVisible;
	}

	// used to hide microphone when editing existing chatItem
	public void toggleMainButtons(boolean showMainButtons) {
		mainButtonsShown = showMainButtons;
		DLog.d(TAG, "Setting main button to "+showMainButtons);
		mSearchButtonCont.setVisibility(showMainButtons ? View.VISIBLE : View.GONE);
	}
	
	
	public void hideSpeechWave() {
		if (mUpdateLevel != null) {
			Handler handler = mUpdateLevel.get();
			if (handler != null)
				handler.removeMessages(0);
			mUpdateLevel = null;
		}
		mSoundView.setVisibility(View.GONE);
	}

	
	/***********  Update View based on Recording Volume **************/

	static class SearchHandler extends Handler {
		private boolean processing = false;
		private EvaSpeechRecogComponent speechSearch;
		private EvatureMainView view;
		
		public SearchHandler(EvaSpeechRecogComponent speechSearch, EvatureMainView view) {
			this.speechSearch = speechSearch;
			this.view = view;
		}
		
		public boolean isRecording() {
			return speechSearch.getSpeechAudioStreamer().getIsRecording();
		}
		
		@Override
		public void handleMessage(Message msg) {
			SpeechAudioStreamer speechAudioStreamer = speechSearch.getSpeechAudioStreamer();

			if (speechAudioStreamer.getIsRecording()) {
				view.mSoundView.setSoundData(
						speechAudioStreamer.getSoundLevelBuffer(), 
						speechAudioStreamer.getBufferIndex()
				);
				//view.mSoundView.stopSpringAnimation();
				if (view.mSoundView.getVisibility() != View.VISIBLE)
					view.mSoundView.setVisibility(View.VISIBLE);
				
			}
			else {
				// continue sending data to soundView - but fake it as a zero volume sound
				speechAudioStreamer.addVolumeSample(speechAudioStreamer.getMinSoundLevel());
				view.mSoundView.setSoundData(
						speechAudioStreamer.getSoundLevelBuffer(), 
						speechAudioStreamer.getBufferIndex()
				);

				
				if (speechAudioStreamer.wasNoise && !processing) {
					processing = true;
					view.disableSearchButton();		//view.showStatus("Processing...");
				}
			}
			
			sendEmptyMessageDelayed(0, UPDATE_SOUND_VIEW_INTERVAL);
			super.handleMessage(msg);
		}
	};

	
	public void startSpeechRecognition(final EvaSpeechRecogComponent.SpeechRecognitionResultListener listener, final EvaSpeechRecogComponent speechSearch, Object cookie, boolean editLastUtterance) {
		//showStatus("Listening...");
		
		mUpdateLevel = new WeakReference<Handler>(new SearchHandler(speechSearch, this));
		try {
			Handler handler = mUpdateLevel.get();
			if (handler != null) {
				speechSearch.startRecognizer(listener, cookie, editLastUtterance);
				handler.sendEmptyMessage(0);
			}
			else {
				throw new EvaException("updateVolume Level is null");
			}
		}
		catch (EvaException e) {
			DLog.e(TAG, "Exception starting recorder", e);
		}
		
		activateSearchButton();
	}
	

	/************  Chat item interaction **************/

	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		ChatItem item = (ChatItem) view.getTag();
		if (item == null) {
			if (mEditedChatItemIndex != -1) {
				closeEditChatItem(false);
			}
			return;
		}
		
		switch (item.getType()) {
//		case DialogAnswer:
//			clickedDialogAnswer(item);
//			break;
			
		case MultiChoiceQuestion:
		case Eva:
			if (item.getSearchModel() != null) {
                item.getSearchModel().triggerSearch(mEvaActivity);
				return;
			}
			editEvaChat(item, position);
			break;
			
		case EvaWelcome:
			if (mEditedChatItemIndex != -1) {
				closeEditChatItem(false);
			}
			else {
				showExamples();
			}
			break;
			
		case User:
			editMeChat(item, position);
			break;
		}		
	}
/*
	private void clickedDialogAnswer(ChatItem item) {
		DialogAnswerChatItem dialogItem = (DialogAnswerChatItem) item;
		DialogQuestionChatItem question = dialogItem.getQuestion();
		if (question.isAnswered()) {
			return; // already answered
		}
		dialogItem.setChosen();
		// remove un-chosen answers
		ArrayList<Integer> indexesToRemove = new ArrayList<Integer>();
		for (DialogAnswerChatItem answer : question.getAnswers()) {
			if (answer.isChosen() == false) {
				indexesToRemove.add(mChatListModel.indexOf(answer));
				//mChatAdapter.remove(answer);
			}
		}
		mAnimAdapter.animateDismiss(indexesToRemove);
		
		addChatItem(new ChatItem(dialogItem.getChat()));
		
		// move this to mainActivity
		//eva.replyToDialog(dialogItem.getIndex());
	}*/
	
	public void addChatItem(final ChatItem chatItem) {
		mEvaActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (mEditedChatItemIndex != -1 && chatItem.getType() == ChatItem.ChatType.User) {
					// adding a "me" chat - close the editted me-chat
					closeEditChatItem(false);
				}
				if (mChatAdapter != null) {
					mChatAdapter.add(chatItem);
				}
				else if (mChatListModel != null) {
					mChatListModel.add(chatItem);
				}
				scrollToPosition(mChatListModel.size()-1);
			}
		});
	}

	private void addUtterance() {
		if (mEditedChatItemIndex != -1) {
			closeEditChatItem(false);
		}
		
		ChatItem editChat = new ChatItem("");
		editChat.setStatus(ChatItem.Status.EDITING);
		toggleMainButtons(false);
		addChatItem(editChat);
		mEditedChatItemIndex = mChatListModel.size()-1;
	}

	
	private void editEvaChat(ChatItem item, int position) {

		if (mEditedChatItemIndex != -1) {
			closeEditChatItem(false);
		}
		else {
			if (item.getType() == ChatItem.ChatType.MultiChoiceQuestion) {
				// search for "Me" chat after
				for (int i=position+1; i<mChatListModel.size(); i++) {
					ChatItem itemAfter = mChatListModel.get(i);
					if (itemAfter.getType() == ChatItem.ChatType.User) {
						editMeChat(itemAfter, i);
						return;
					}
				}
				// no me chat after question - add one
				addUtterance();
			}
			else {
				// search for last "Me" chat before
				for (int i=mChatListModel.size()-1; i>0; i--) {
					ChatItem itemBefore = mChatListModel.get(i);
					if (itemBefore.getType() == ChatItem.ChatType.User) {
						editMeChat(itemBefore, i);
						return;
					}
				}
				// no me-chat - add one
				addUtterance();
			}
		}

	}

	private void editMeChat(ChatItem current, int position) {
		if (mEditedChatItemIndex != -1) {
			closeEditChatItem(false);
		}
		else {
			// you can only edit the last utterance - make sure there isn't another one of type User later on
			for (int i=position+1; i< mChatListModel.size(); i++ ) {
				ChatItem itemAfter = mChatListModel.get(i);
				if (itemAfter.getType() == ChatItem.ChatType.User) {
					Toast.makeText(mEvaActivity, "You can only modify your last utterance", Toast.LENGTH_SHORT).show();
					return;
				}
			}
			
			if (current.getStatus() != ChatItem.Status.EDITING) {
				current.setStatus(ChatItem.Status.EDITING);
				toggleMainButtons(false);
				mEditedChatItemIndex = position;
				mChatAdapter.notifyDataSetChanged();
			}
		}
	}
	
	private ChatItem exampleChatItem;
	private void showExamples() {
		if (exampleChatItem != null && mChatListModel.contains(exampleChatItem)) {
			return;
		}
		
		if (exampleChatItem == null) {
			
			String[] examples = {
					"  Cruise to Alaska in August", 
					"  10 days cruise departing from Miami", 
					"  Princess Cruises",
					"  Cruises in July, sorted by price",
					"  Norwegian Jewel",
				};
			String greeting = mEvaActivity.getResources().getString(R.string.evature_examples_greetings);
			String examplesString = "";
			for (String example : examples) {
				examplesString += "\n"+example;
			}
			SpannableString chatFormatted = new SpannableString(greeting+examplesString);
			int col = mEvaActivity.getResources().getColor(R.color.eva_chat_secondary_text);
			chatFormatted.setSpan(new ForegroundColorSpan(col), greeting.length(), chatFormatted.length(), 0);
			chatFormatted.setSpan( new StyleSpan(Typeface.ITALIC), greeting.length(), chatFormatted.length(), 0);
			exampleChatItem = new ChatItem(chatFormatted, null, ChatItem.ChatType.Eva);
		}
		exampleChatItem.clearAlreadyAnimated();
		addChatItem(exampleChatItem);
		flashSearchButton(5);
	}

	
	public View getViewForChatItem(ChatItem chatItem) {
		return mChatListView.findViewWithTag(chatItem);
	}
	
	/***
	 * Close the chat-utterance that is being edited right now
	 * 
	 * @param isSubmitted - true if submit modification, false if revert to pre-modified text (or remove new utterance)
	 */
	private void closeEditChatItem(boolean isSubmitted) {
		if (mEditedChatItemIndex == -1) {
			DLog.e(TAG, "Unexpected closed edit chat item");
			return;
		}
		if (mEditedChatItemIndex >= mChatListModel.size()) {
			DLog.e(TAG, "Edited item "+mEditedChatItemIndex+" but chatList size is "+mChatListModel.size());
			return;
		}

		InputMethodManager imm = (InputMethodManager)mEvaActivity.getSystemService( Context.INPUT_METHOD_SERVICE);
		if (imm == null) {
			DLog.e(TAG, "no input method manager");
		}
		else {
			imm.hideSoftInputFromWindow(mChatListView.getWindowToken(), 0);
		}
		
		ChatItem editedChatItem = mChatListModel.get(mEditedChatItemIndex);
		editedChatItem.setStatus(ChatItem.Status.NONE);
		toggleMainButtons(true);
		String preModifiedString = editedChatItem.getChat().toString();
		if (isSubmitted) {
			View rowView = getViewForChatItem(editedChatItem);
			if (rowView == null) {
				DLog.e(TAG, "Unexpected edited row not found");
				return;
			}
			EditText editText = (EditText)rowView.findViewById(R.id.editText);
			if (editText == null) {
				DLog.e(TAG, "Unexpected editText not found");
				return;
			}
			// if the pre-edit text is empty - this is a new chat to be added - not existing chat to edit
			boolean editLastUtterance = false == preModifiedString.equals("");
			SpannableString preEditChat = editedChatItem.getChat();
			String newText = editText.getText().toString();
			editedChatItem.setChat(newText);
	
			mEvaActivity.onEventChatItemModified(editedChatItem, preEditChat, false, editLastUtterance);
		}
		else {
			// not submitting - just canceling edit
			// if this chat was empty text (new chat) - cancel adding it
			if (preModifiedString.equals("")) {
				mChatAdapter.dismissItem(mChatListView, mEditedChatItemIndex, ChatAdapter.DismissStep.ANIMATE_DISMISS);
				mChatAdapter.dismissItem(mChatListView, mEditedChatItemIndex, ChatAdapter.DismissStep.DO_DELETE);
			}
		}
		
		mEditedChatItemIndex = -1;
		mChatAdapter.notifyDataSetChanged();
	}
	
	
	
	public void dismissItems(int start,  int end, ChatAdapter.DismissStep step) {
		if (start < 0) {
			return;
		}
		mChatAdapter.dismissItems(mChatListView, start, end, step);
		if (step == ChatAdapter.DismissStep.ANIMATE_RESTORE) {
			scrollToPosition(end-1);
		}
		else if (step == ChatAdapter.DismissStep.ANIMATE_DISMISS)
		{
			scrollToPosition(start-1);
		}
	}
	
	@SuppressLint("NewApi")
	public void scrollToPosition(final int scrollTo) {
		mChatListView.post(new Runnable() {
			@Override
			public void run() {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
					mChatListView.smoothScrollToPositionFromTop(scrollTo, 110);
				}
				else {
					mChatListView.smoothScrollToPosition(scrollTo);
				}
			}
		});

	}
	
	
	
	public final OnEditorActionListener editorActionHandler = new OnEditorActionListener() {
		
		@Override
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			DLog.d(TAG, "editor Action "+actionId);
			if (mEditedChatItemIndex == -1) {
				DLog.e(TAG, "Unexpected execute no edit chat item");
				return false;
			}
			closeEditChatItem(true);
			return false;
		}
	};


	/**
	 * If a chat item is edited  - back will close the edit
	 * @return  true if back button was useful
	 */
	public boolean handleBackPressed() {
		if (mEditedChatItemIndex == -1) {
			return false;
		}
		closeEditChatItem(false);
		return true;
	}

	public void clearChatHistory() {
		if (mEditedChatItemIndex != -1) {
			closeEditChatItem(false);
		}
		mChatAdapter.clear();
	}

	public ArrayList<ChatItem> getChatListModel() {
		return mChatListModel;
	}
	
	public void notifyDataChanged() {
		mEvaActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mChatAdapter.notifyDataSetChanged();
			}
		});
	}

	public void voiceResponseToChatItem(ChatItem storeResultInItem, SpannableString chat) {
		if (mEditedChatItemIndex != -1) {
			closeEditChatItem(false);
		}
		storeResultInItem.setChat(chat);
		dismissItems(mChatListModel.indexOf(storeResultInItem) + 1, mChatListModel.size(), ChatAdapter.DismissStep.DO_DELETE);
	}

	public void setVolumeIcon() {
		if (mVolumeButton == null) {
			return;
		}
		VolumeUtil.checkVolume(this.mEvaActivity);
		if (VolumeUtil.isLowVolume()) {
			mVolumeButton.setVisibility(View.VISIBLE);
			mVolumeButton.setImageResource(VolumeUtil.getVolumeIcon());
		}
		else {
			mVolumeButton.setVisibility(View.GONE);
		}
	}

	public void restoreFromSeralizeable(Serializable chatList) {
		mChatListModel = (ArrayList<ChatItem>) chatList;
		
	}

	
}