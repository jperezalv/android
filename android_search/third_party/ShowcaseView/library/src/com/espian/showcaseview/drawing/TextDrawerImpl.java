package com.espian.showcaseview.drawing;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.text.DynamicLayout;
import android.text.Layout;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;

import com.espian.showcaseview.ShowcaseView;
import com.espian.showcaseview.utils.ShowcaseAreaCalculator;

/**
 * Draws the text as required by the ShowcaseView
 */
public class TextDrawerImpl implements TextDrawer {
	
	private static final int ACTIONBAR_PADDING = 66;

    private final TextPaint mPaintTitle;
    private final TextPaint mPaintDetail;

    private CharSequence mTitle, mDetails;
    private float mDensityScale;
    private ShowcaseAreaCalculator mCalculator;
    private float[] mBestTextPosition = new float[3];
    private DynamicLayout mDynamicTitleLayout;
    private DynamicLayout mDynamicDetailLayout;
    private TextAppearanceSpan mTitleSpan;
    private TextAppearanceSpan mDetailSpan;
    private final int mPadding;

    public TextDrawerImpl(float densityScale, ShowcaseAreaCalculator calculator, int padding) {
        mDensityScale = densityScale;
        mCalculator = calculator;
        this.mPadding = padding;

        mPaintTitle = new TextPaint();
        mPaintTitle.setAntiAlias(true);

        mPaintDetail = new TextPaint();
        mPaintDetail.setAntiAlias(true);
    }

    @Override
    public void draw(Canvas canvas, boolean hasPositionChanged) {
        if (shouldDrawText()) {
            float[] textPosition = getBestTextPosition();

            if (!TextUtils.isEmpty(mTitle)) {
                canvas.save();
                if (hasPositionChanged) {
                    mDynamicTitleLayout = new DynamicLayout(mTitle, mPaintTitle,
                            (int) textPosition[2], Layout.Alignment.ALIGN_NORMAL,
                            1.0f, 1.0f, true);
                }
                canvas.translate(textPosition[0], textPosition[1]);
                mDynamicTitleLayout.draw(canvas);
                canvas.restore();
            }

            if (!TextUtils.isEmpty(mDetails)) {
                canvas.save();
                if (hasPositionChanged) {
                    mDynamicDetailLayout = new DynamicLayout(mDetails, mPaintDetail,
                            (int) textPosition[2],
                            Layout.Alignment.ALIGN_NORMAL,
                            1.2f, 1.0f, true);
                }
                canvas.translate(textPosition[0], textPosition[1] + mDynamicTitleLayout.getHeight());
                mDynamicDetailLayout.draw(canvas);
                canvas.restore();

            }
        }
    }

    @Override
    public void setDetails(CharSequence details) {
        if (details != null) {
            SpannableString ssbDetail = new SpannableString(details);
            ssbDetail.setSpan(mDetailSpan, 0, ssbDetail.length(), 0);
            mDetails = ssbDetail;
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        if (title != null) {
            SpannableString ssbTitle = new SpannableString(title);
            ssbTitle.setSpan(mTitleSpan, 0, ssbTitle.length(), 0);
            mTitle = ssbTitle;
        }
    }

    /**
     * Calculates the best place to position text
     *
     * @param canvasW width of the screen
     * @param canvasH height of the screen
     */
    @Override
    public void calculateTextPosition(int canvasW, int canvasH, ShowcaseView showcaseView) {

    	Rect showcase = showcaseView.hasShowcaseView() ?
    			mCalculator.getShowcaseRect() :
    			new Rect();

    	float paddingScaled = mPadding * mDensityScale;
    	int twoPadding = (int)(paddingScaled * 2);

    	int[] areas = new int[4]; //left, top, right, bottom
    	areas[0] = (showcase.left - twoPadding) * (canvasH - twoPadding);
    	areas[1] = (showcase.top - twoPadding) * (canvasW - twoPadding);
    	areas[2] = (canvasW - showcase.right - twoPadding) * (canvasH - twoPadding);
    	areas[3] = (canvasH - showcase.bottom - twoPadding) * (canvasW - twoPadding);
    	
    	int largest = 0;
    	for(int i = 1; i < areas.length; i++) {
    		if(areas[i] > areas[largest])
    			largest = i;
    	}
    	
    	
    	// Position text in largest area
    	switch(largest) {
    	case 0:
    		mBestTextPosition[0] = paddingScaled;
    		mBestTextPosition[1] = paddingScaled;
    		mBestTextPosition[2] = showcase.left - 2 * paddingScaled;
    		break;
    	case 1:
    		mBestTextPosition[0] = paddingScaled;
    		mBestTextPosition[1] = (mPadding + ACTIONBAR_PADDING) * mDensityScale;
    		mBestTextPosition[2] = canvasW - 2 * paddingScaled;
    		break;
    	case 2:
    		mBestTextPosition[0] = showcase.right + paddingScaled;
    		mBestTextPosition[1] = paddingScaled;
    		mBestTextPosition[2] = (canvasW - showcase.right) - 2 * paddingScaled;
    		break;
    	case 3:
    		mBestTextPosition[0] = paddingScaled;
    		mBestTextPosition[1] = showcase.bottom + paddingScaled;
    		mBestTextPosition[2] = canvasW - 2 * paddingScaled;
    		break;
    	}
    	if(showcaseView.getConfigOptions().centerText) {
	    	// Center text vertically or horizontally
	    	switch(largest) {
	    	case 0:
	    	case 2:
	    		mBestTextPosition[1] += canvasH / 4;
	    		break;
	    	case 1:
	    	case 3:
	    		mBestTextPosition[2] /= 2;
	    		mBestTextPosition[0] += canvasW / 4;
	    		break;
	    	} 
    	} else {
    		// As text is not centered add actionbar padding if the text is left or right
	    	switch(largest) {
	    		case 0:
	    		case 2:
	    			mBestTextPosition[1] += ACTIONBAR_PADDING * mDensityScale;
	    			break;
	    	}
    	}
    }

    @Override
    public void setTitleStyling(Context context, int styleId) {
        mTitleSpan = new TextAppearanceSpan(context, styleId);
    }

    @Override
    public void setDetailStyling(Context context, int styleId) {
        mDetailSpan = new TextAppearanceSpan(context, styleId);
    }

    public float[] getBestTextPosition() {
        return mBestTextPosition;
    }

    public boolean shouldDrawText() {
        return !TextUtils.isEmpty(mTitle) || !TextUtils.isEmpty(mDetails);
    }

	@Override
	public void setCalculator(ShowcaseAreaCalculator calculator) {
		mCalculator = calculator;
	}
}