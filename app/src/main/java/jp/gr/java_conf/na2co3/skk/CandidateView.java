/*
 * Copyright (C) 2008-2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package jp.gr.java_conf.na2co3.skk;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class CandidateView extends View {

    private static final int OUT_OF_BOUNDS = -1;

    private CandidateViewContainer mContainer;
    private SKKService mService;
    private List<String> mSuggestions;
    private int mSelectedIndex;

    private int mChoosedIndex = 0;

    private int mTouchX = OUT_OF_BOUNDS;
    private Drawable mSelectionHighlight;

    private static final int MAX_SUGGESTIONS = 150;
    private int mScrollPixels;

    private int[] mWordWidth = new int[MAX_SUGGESTIONS];
    private int[] mWordX = new int[MAX_SUGGESTIONS];

    private static final int X_GAP = 5;

    private static final List<String> EMPTY_LIST = new ArrayList<>();

    private boolean mScrolled;

    private int mColorNormal;
    private int mColorRecommended;
    private int mColorOther;
    private Paint mPaint;

    private int mTargetScrollX;

    private int mTotalWidth;

    private GestureDetector mGestureDetector;

    private int mScrollX;

    /**
     * Construct a CandidateView for showing suggested words for completion.
     * @param context
     * @param attrs
     */
    public CandidateView(Context context, AttributeSet attrs) {
        super(context, attrs);

        Resources r = context.getResources();

        mSelectionHighlight = r.getDrawable(R.drawable.ic_suggest_scroll_background);
        mSelectionHighlight.setState(new int[] {
                android.R.attr.state_enabled,
                android.R.attr.state_focused,
                android.R.attr.state_window_focused,
                android.R.attr.state_pressed
        });

        setBackgroundColor(r.getColor(R.color.candidate_background));

        mColorNormal = r.getColor(R.color.candidate_normal);
        mColorRecommended = r.getColor(R.color.candidate_recommended);
        mColorOther = r.getColor(R.color.candidate_other);

        mScrollPixels = r.getDimensionPixelSize(R.dimen.candidates_scroll_size);

        mPaint = new Paint();
        mPaint.setColor(mColorNormal);
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(r.getDimensionPixelSize(R.dimen.candidate_font_height));
        mPaint.setStrokeWidth(0);

        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
          @Override
          public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                  float distanceX, float distanceY) {
            final int width = getWidth();
            mScrolled = true;
            mScrollX = getScrollX();
            mScrollX += (int) distanceX;
            if (mScrollX < 0) {
              mScrollX = 0;
            }
            if (distanceX > 0 && mScrollX + width > mTotalWidth) {
              mScrollX -= (int) distanceX;
            }
            mTargetScrollX = mScrollX;
            invalidate();
            return true;
          }
        });

        setHorizontalFadingEdgeEnabled(false);
        setWillNotDraw(false);
        setHorizontalScrollBarEnabled(false);
        setVerticalScrollBarEnabled(false);
    }

    /**
     * A connection back to the service to communicate with the text field
     * @param listener
     */
    public void setService(SKKService listener) {
        mService = listener;
    }

    public void setContainer(CandidateViewContainer c) {
        mContainer = c;
    }

    public void setTextSize(int px) {
        if (mPaint != null) {
            mPaint.setTextSize(px);
        }
    }

    @Override
    public int computeHorizontalScrollRange() {
        return mTotalWidth;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth = resolveSize(50, widthMeasureSpec);
        mScrollPixels = measuredWidth/12;

        // Get the desired height of the icon menu view (last row of items does
        // not have a divider below)
/*
        Rect padding = new Rect();
        mSelectionHighlight.getPadding(padding);
        final int desiredHeight = ((int)mPaint.getTextSize()) + mVerticalPadding
                + padding.top + padding.bottom;
*/
        int size = ((int)mPaint.getTextSize());
        final int desiredHeight = size + size/3;

        // Maximum possible width and desired height
        setMeasuredDimension(measuredWidth, resolveSize(desiredHeight, heightMeasureSpec));
    }

    private void calculateWidths() {
        mTotalWidth = 0;
        if (mSuggestions == null) return;

        int count = mSuggestions.size();
        if (count> MAX_SUGGESTIONS) count = MAX_SUGGESTIONS;

        int x = 0;
        for (int i = 0; i < count; i++) {
            String suggestion = mSuggestions.get(i);
            float textWidth = mPaint.measureText(suggestion);
            final int wordWidth = (int) textWidth + X_GAP * 2;

            mWordX[i] = x;
            mWordWidth[i] = wordWidth;

            x += wordWidth;
        }

        mTotalWidth = x;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (canvas != null) {
            super.onDraw(canvas);
        }

        if (mSuggestions == null) return;

        final int height = getHeight();
        final Paint paint = mPaint;
        final int touchX = mTouchX;
        final int scrollX = getScrollX();
        final boolean scrolled = mScrolled;
        final int y = (int) (((height - paint.getTextSize()) / 2) - paint.ascent());

        int count = mSuggestions.size();
        if (count> MAX_SUGGESTIONS) count = MAX_SUGGESTIONS;

        for (int i = 0; i < count; i++) {
            paint.setColor(mColorNormal);
            if (touchX + scrollX >= mWordX[i] && touchX + scrollX < mWordX[i] + mWordWidth[i] && !scrolled) {
                if (canvas != null) {
                    canvas.translate(mWordX[i], 0);
                    mSelectionHighlight.setBounds(0, 0, mWordWidth[i], height);
                    mSelectionHighlight.draw(canvas);
                    canvas.translate(-mWordX[i], 0);
                }
                mSelectedIndex = i;
            }

            if (canvas != null) {
                if (i == mChoosedIndex) {
                    paint.setFakeBoldText(true);
                    paint.setColor(mColorRecommended);
                } else {
                    paint.setColor(mColorOther);
                }
                canvas.drawText(mSuggestions.get(i), mWordX[i] + X_GAP, y, paint);
                paint.setColor(mColorOther);
                canvas.drawLine(mWordX[i] + mWordWidth[i] + 0.5f, 0,
                        mWordX[i] + mWordWidth[i] + 0.5f, height + 1, paint);
                paint.setFakeBoldText(false);
            }
        }

        if (scrolled && mTargetScrollX != getScrollX()) {
            scrollToTarget();
        }
    }

    private void scrollToTarget() {
        int sx = getScrollX();
        if (mTargetScrollX > sx) {
            sx += mScrollPixels;
            if (sx >= mTargetScrollX) {
                sx = mTargetScrollX;
                setScrollButtonsEnabled(sx);
            }
        } else {
            sx -= mScrollPixels;
            if (sx <= mTargetScrollX) {
                sx = mTargetScrollX;
                setScrollButtonsEnabled(sx);
            }
        }
        scrollTo(sx, getScrollY());
        invalidate();
    }

    private void setScrollButtonsEnabled(int targetX) {
        boolean left  = targetX > 0;
        boolean right = targetX + getWidth() < mTotalWidth;
        if (mContainer != null) {mContainer.setScrollButtonsEnabled(left, right);}
    }

    public void setContents(List<String> list) {
        if (list != null) {
            mSuggestions = list;
        } else {
            mSuggestions = EMPTY_LIST;
        }
        scrollTo(0, 0);
        mScrollX = 0;
        mTargetScrollX = 0;
        mTouchX = OUT_OF_BOUNDS;
        mSelectedIndex = -1;
        mChoosedIndex = 0;

        // Compute the total width
        calculateWidths();
        setScrollButtonsEnabled(0);
        invalidate();
    }

    public void scrollPrev() {
      mScrollX = getScrollX();
      int i = 0;
        final int count = mSuggestions.size();
        int firstItem = 0; // Actually just before the first item, if at the boundary
        while (i < count) {
            if (mWordX[i] < mScrollX
                    && mWordX[i] + mWordWidth[i] >= mScrollX - 1) {
                firstItem = i;
                break;
            }
            i++;
        }
        int leftEdge = mWordX[firstItem] + mWordWidth[firstItem] - getWidth();
        if (leftEdge < 0) leftEdge = 0;
        updateScrollPosition(leftEdge);
    }

    public void scrollNext() {
        int i = 0;
        mScrollX = getScrollX();
        int targetX = mScrollX;
        final int count = mSuggestions.size();
        int rightEdge = mScrollX + getWidth();
        while (i < count) {
            if (mWordX[i] <= rightEdge &&
                    mWordX[i] + mWordWidth[i] >= rightEdge) {
                targetX = Math.min(mWordX[i], mTotalWidth - getWidth());
                break;
            }
            i++;
        }
        updateScrollPosition(targetX);
    }

    private void updateScrollPosition(int targetX) {
      mScrollX = getScrollX();
        if (targetX != mScrollX) {
            // TODO: Animate
            mTargetScrollX = targetX;
            setScrollButtonsEnabled(targetX);
            invalidate();
            mScrolled = true;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent me) {

        // スクロールした時にはここで処理されて終わりのようだ。ソースの頭で定義している。
        if (mGestureDetector.onTouchEvent(me)) {
            return true;
        }

        int action = me.getAction();
        int x = (int) me.getX();
        int y = (int) me.getY();
        mTouchX = x;

        switch (action) {
        case MotionEvent.ACTION_DOWN:
            mScrolled = false;
            invalidate();
            break;
        case MotionEvent.ACTION_MOVE: // よってここのコードは生きていない。使用されない。
            if (y <= 0) {
                // Fling up!?
                if (mSelectedIndex >= 0) {
                    mService.pickCandidateViewManually(mSelectedIndex);
                    mSelectedIndex = -1;
                }
            }
            invalidate();
            break;
        case MotionEvent.ACTION_UP: // ここは生きている。
            if (!mScrolled) {
                if (mSelectedIndex >= 0) {
                    mService.pickCandidateViewManually(mSelectedIndex);
                }
            }
            mSelectedIndex = -1;
            mTouchX = OUT_OF_BOUNDS;
            invalidate();
            break;
        }
        return true;
    }

    public void choose(int choosedIndex) {
        if (mWordX[choosedIndex] != getScrollX()) {
            scrollTo(mWordX[choosedIndex], getScrollY());
            setScrollButtonsEnabled(mWordX[choosedIndex]);
            invalidate();
            mScrolled = false;
            mChoosedIndex = choosedIndex;
        }
    }
}
