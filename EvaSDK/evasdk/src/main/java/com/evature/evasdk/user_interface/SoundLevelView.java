package com.evature.evasdk.user_interface;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Join;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AnimationUtils;


public class SoundLevelView extends View {

	private static final String TAG = "SoundLevelView";
	private float[] soundBuff;    // cyclic buffer of volume - (0 to index) or (index+1 to index+length (module buff size))
	private int soundBuffIndex;
	
	//private float[] velocities;
	private Paint paint;
	private float peakSound;
	private float minSound;
    private float lineWidth;

	private int gravity;
	private float xstep;
	private boolean autoMinMax = true;
	private boolean extendLine = true;
    private float springiness = 0.75f;
    private float damping = 0.92f;
    Path path = new Path();
    private boolean fishEye = true;

	public SoundLevelView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.soundBuff = null;
		this.paint = new Paint();
		this.peakSound = Integer.MIN_VALUE;
		this.minSound = Integer.MAX_VALUE;
		//paint.setColor(Color.GREEN);

        final float scale = context.getResources().getDisplayMetrics().density;
        // Convert the dps to pixels, based on density scale
		paint.setColor(0xff44aaff);
		paint.setAntiAlias(true);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeJoin(Join.ROUND);
        this.lineWidth = 2 * scale;
		paint.setStrokeWidth(lineWidth);
		gravity = Gravity.CENTER_HORIZONTAL;
		xstep = 0;
	}
	
	public void setColor(int color) {
		paint.setColor(color);
	}
	
	public void setAlign(int gravity) {
		this.gravity = gravity;
	}
	
	public void setXStep(float xstep) {
		this.xstep = xstep;
	}
	
	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (soundBuff == null) {
			return;
		}
		int height_2 = this.getHeight() / 2;
		float width = this.getWidth();
		float width_2 = width/2;

		int startOfBuff = 0;
		int numOfPoints = soundBuffIndex;
		if (soundBuffIndex > soundBuff.length) {
			startOfBuff = soundBuffIndex;
			numOfPoints = soundBuff.length;
		}
			
		float delta = Math.max(50f, peakSound - minSound);

		canvas.save();
		canvas.translate(0, height_2);

		float xStep = getXStep();
		if (xStep == 0) {
			xStep = width / (3*soundBuff.length);
			setXStep(xStep);
		}
		float xStep2 = 2*xStep;
		float centerStep = (1.5f*xStep);
		float xStep3 = 3*xStep;
		
		float curX;
		if ((gravity & Gravity.RIGHT) == Gravity.RIGHT){
			curX =  width - numOfPoints * xStep3;
		}
		else if ((gravity & Gravity.LEFT) == Gravity.LEFT) {
			curX = 0;
		}
		else {
			curX = (width - numOfPoints * xStep3) / 2;
		}

		path.reset();
		if (extendLine) {
			path.moveTo(0, 0);
			path.lineTo(curX, 0);
		}
		else {
			path.moveTo(curX, 0);
		}
		if (numOfPoints > 4) {
			float Rsqr = height_2 * height_2;
			for (int i = 0; i < numOfPoints; i++) {
				float soundLevel = Math.abs(soundBuff[(i + startOfBuff) % soundBuff.length]);
				if (soundLevel > 0) {
					soundLevel -= minSound;
				}
				float normLevel = soundLevel / delta; // normalize - from volume to 0..1

				// change sign of level based on odd/even
				float y = ((i+ startOfBuff) % 2 == 0 ? -1 : 1) * normLevel;
				
				
				// strech from 0..1 to height
				if (fishEye) {
					float centerX = curX+centerStep;
					float x = Math.abs(centerX - width_2);
					y *=  Math.sqrt(Rsqr - x*x);
				}
				else {
					y *= height_2;
				}
				
				
				path.cubicTo(curX+xStep, y, curX+xStep2, y, curX+xStep3, 0);
				curX += xStep3;
			}
		}
		
		if (extendLine) {
			path.lineTo(width, 0);
		}
		
        canvas.drawPath(path, paint);
		
		
//			if (extendLine) {
//				canvas.drawLine(pointsBuff[4*numOfPoints-2], height, width, height, paint);
//			}
		
		canvas.restore();
	}

	private float getXStep() {
		return xstep;
	}

	public void setSoundData(float[] buff, int index) {
		boolean wasNull = false;
		if (soundBuff == null) {
			wasNull = true;
			this.soundBuff = new float[buff.length];
			//velocities = new float[buff.length]; // need velocities for y values
		}
		// append new data to end of soundBuff
		int numOfPoints = (index-soundBuffIndex);
		int startOfBuff = index-numOfPoints;
		if (autoMinMax) {
			for (int i=0; i<numOfPoints; i++) {
				float volume = buff[(startOfBuff+i)% buff.length];
				soundBuff[(soundBuffIndex+i)%soundBuff.length] = volume;
				peakSound = Math.max(peakSound, volume);
				minSound = Math.min(minSound, volume);
			}
		}
		else {
			for (int i=0; i<numOfPoints; i++) {
				soundBuff[(soundBuffIndex+i)%soundBuff.length] = buff[(startOfBuff+i)% buff.length];
			}
		}
		soundBuffIndex = index;
//		DLog.i(TAG, "@@@ added "+numOfPoints+"  to buffer, peak="+peakSound+"  minSound="+minSound);
		
//		if (springiness != 0) {
//			postDelayed(animator, 15);
//        }
		
		invalidate();
	}
	
	@Override 
	public void setVisibility(int visibility) {
		super.setVisibility(visibility);
		if (visibility == View.GONE || visibility == View.INVISIBLE) {
			// continue next time with same peak values - better than starting from start
			//peakSound = Integer.MIN_VALUE;
			//minSound = Integer.MAX_VALUE;
			soundBuff = null;
			//velocities = null;
			soundBuffIndex = 0;
		}
	};
	/*
	public void stopSpringAnimation() {
		lastTime = -1;
		removeCallbacks(animator);
	}
	
	public void startSpringAnimation() {
		lastTime = AnimationUtils.currentAnimationTimeMillis();
		postDelayed(animator, 1);
	}*/
	
	/*private long lastTime = 0;
	private Runnable animator = new Runnable() {
	    @Override
	    public void run() {
	        if (soundBuff == null || lastTime == -1) {
	        	return;
	        }
	        
	        float THRESHOLD_TO_CONTINUE = 0.1f;
	        int FREQUENCY_TS = 15; // how often to animate - in ms 
	        
	        float maxVelocity = 0;
	        long now = AnimationUtils.currentAnimationTimeMillis();
	        float dt = Math.min(now - lastTime, 50) / 1000f;
	        float targetPosition = minSound;
	        
	        int startOfBuff = 0;
			int numOfPoints = soundBuffIndex;
			if (soundBuffIndex > soundBuff.length) {
				startOfBuff = soundBuffIndex;
				numOfPoints = soundBuff.length;
			}
			
			for (int i = 0; i < numOfPoints; i++) {
				if (soundBuff == null) {
		        	return;
		        }
				int index = (startOfBuff+i) % soundBuff.length;
				float volume = soundBuff[index];
				float velocity = velocities[index];
				velocity += (targetPosition - volume) * springiness;
				velocity *= damping;
				volume += velocity * dt;
				if (maxVelocity < THRESHOLD_TO_CONTINUE)
					maxVelocity = Math.max(maxVelocity, Math.abs(velocity));
				velocities[index] = velocity;
				soundBuff[index] = volume;
			}
	        lastTime = now;
	        
	        if (maxVelocity > THRESHOLD_TO_CONTINUE) {
	        	postDelayed(animator, FREQUENCY_TS);
	        }
	        
	        invalidate();
	    }
	};
*/
}