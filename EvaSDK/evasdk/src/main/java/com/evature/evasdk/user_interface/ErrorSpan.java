package com.evature.evasdk.user_interface;


import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.style.DynamicDrawableSpan;

import com.evature.evasdk.R;

/***
 * Draw a Red Wavy line below text (actually a horizontal-repeating bitmap)
 * @author iftah
 *
 */
public class ErrorSpan extends DynamicDrawableSpan {
	
	private int width;
    int lineWidth;
    int waveSize;
    int color;


    public ErrorSpan(Resources resources) {
        this(resources, Color.RED, 1, 3);
    }

	public ErrorSpan(Resources resources, int color, int lineWidth, int waveSize) {
		super(DynamicDrawableSpan.ALIGN_BASELINE);
        // Get the screen's density scale
        final float scale = resources.getDisplayMetrics().density;
        // Convert the dps to pixels, based on density scale
        this.lineWidth = (int) (lineWidth * scale + 0.5f);
        this.waveSize = (int) (waveSize * scale + 0.5f);

        this.color = color;
	}

	@Override
    public Drawable getDrawable() {
		return null;
	}
	
	@Override
    public int getSize(Paint paint, CharSequence text,
                         int start, int end,
                         Paint.FontMetricsInt fm) {
        width = (int) paint.measureText(text, start, end);
	    return width;
	}
                         

	@Override
    public void draw(Canvas canvas, CharSequence text,
                     int start, int end, float x, 
                     int top, int y, int bottom, Paint paint) {
		
        Paint p = new Paint(paint);
        p.setColor(color);
        p.setStrokeWidth(lineWidth);
        p.setAntiAlias(true);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeJoin(Paint.Join.ROUND);

        int doubleWaveSize = waveSize * 2;
        for (int i = (int)x; i < x + width; i += doubleWaveSize) {
            canvas.drawLine(i, bottom, i + waveSize, bottom - waveSize, p);
            canvas.drawLine(i + waveSize, bottom - waveSize, i + doubleWaveSize, bottom, p);
        }
		canvas.drawText(text.subSequence(start, end).toString(), x, y, paint);
	}
}