package com.evaapis.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;

import com.evature.util.Log;

public class SoundLevelView extends View {

	private static final String TAG = "SoundLevelView";
	private int[] soundBuff;
	private float[] pointsBuff = null;
	private Paint paint;
	private int soundBuffIndex;
	private int peakSound;
	private int numOfPoints;
	private int minSound;
	private int gravity;

	public SoundLevelView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.soundBuff = null;
		this.paint = new Paint();
		//paint.setColor(Color.GREEN);
		paint.setColor(0xff44aaff);
		paint.setAntiAlias(true);
		paint.setStyle(Paint.Style.STROKE); 
		gravity = Gravity.CENTER_HORIZONTAL;
	}
	
	public void setColor(int color) {
		paint.setColor(color);
	}
	
	public void setAlign(int gravity) {
		this.gravity = gravity;
	}

	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (soundBuff != null && pointsBuff != null) {
			int startOfBuff = 0;
			numOfPoints = soundBuffIndex;
			if (soundBuffIndex > soundBuff.length) {
				startOfBuff = soundBuffIndex + 1;
				numOfPoints = soundBuff.length;
			}
			int max = peakSound;
			int min = minSound;
			int delta = max - min;
			// build points array
			int qi = 0;
			int height = (this.getHeight() - 2) / 2;
			float prevY = height;
			float width = this.getWidth() - 2;
			float xStep = width / soundBuff.length;
			float prevX;
			if ((gravity & Gravity.RIGHT) == Gravity.RIGHT){
				prevX = width - numOfPoints * xStep;
			}
			else if ((gravity & Gravity.LEFT) == Gravity.LEFT) {
				prevX = 0;
			}
			else {
				prevX = (width - numOfPoints * xStep) / 2;
			}

			for (int i = 0; i < numOfPoints; i++) {
				pointsBuff[qi] = prevX;
				pointsBuff[qi + 1] = prevY;
				pointsBuff[qi + 2] = prevX + xStep;
				float soundLevel = soundBuff[(i + startOfBuff)
						% soundBuff.length];
				if (soundLevel > 0) {
					soundLevel -= min;
				}
				float normLevel = soundLevel / delta;
				float y = height + (i % 4 < 2 ? -1 : 1) * height * normLevel;
				pointsBuff[qi + 3] = prevY = y;
				qi += 4;
				prevX += xStep;
			}
			this.soundBuff = null;
		}
		if (pointsBuff != null) {
			canvas.drawLines(pointsBuff, 0, numOfPoints * 4, paint);
		}
	}

	public void setSoundData(int[] buff, int index, int peakSound, int minSound) {
		this.soundBuff = buff;
		this.soundBuffIndex = index;
		this.peakSound = peakSound;
		this.minSound = minSound;
		if (pointsBuff == null || buff.length * 4 != pointsBuff.length) {
			Log.i(TAG, "Setting points buff for " + buff.length
					+ " sound samples");
			pointsBuff = new float[buff.length * 4];
		}
	}

}