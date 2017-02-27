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

package jp.deadend.noname.skk;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;

public class CandidateViewContainer extends LinearLayout implements OnTouchListener {

    private View mButtonLeft;
    private View mButtonRight;
    private CandidateView mCandidates;
    private int mFontSize = -1;
    private int mButtonWidth = -1;

    public CandidateViewContainer(Context screen, AttributeSet attrs) {
        super(screen, attrs);

		mButtonWidth = screen.getResources().getDimensionPixelSize(R.dimen.candidates_scrollbutton_width);
    }

    public void initViews() {
        if (mCandidates == null) {
            mButtonLeft = findViewById(R.id.candidate_left);
            if (mButtonLeft != null) {
                mButtonLeft.setOnTouchListener(this);
            }
            mButtonRight = findViewById(R.id.candidate_right);
            if (mButtonRight != null) {
                mButtonRight.setOnTouchListener(this);
            }
            mCandidates = (CandidateView) findViewById(R.id.candidates);
        }
    }

    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (v == mButtonRight) {
                mCandidates.scrollNext();
            } else if (v == mButtonLeft) {
                mCandidates.scrollPrev();
            }
        }
        return false;
    }

    public void setScrollButtonsEnabled(boolean left, boolean right) {
        if (mButtonLeft != null) {
            mButtonLeft.setEnabled(left);
        }
        if (mButtonRight != null) {
            mButtonRight.setEnabled(right);
        }
    }

	public void setSize(int px) {
		if (px == mFontSize) {return;}

		if (mCandidates != null)  {
			mCandidates.setTextSize(px);
			mCandidates.setLayoutParams(new LinearLayout.LayoutParams(0, px+px/3, 1));
		}
		if (mButtonLeft != null)  {
			mButtonLeft.setLayoutParams(new LinearLayout.LayoutParams(mButtonWidth, px+px/3));
		}
		if (mButtonRight != null) {
			mButtonRight.setLayoutParams(new LinearLayout.LayoutParams(mButtonWidth, px+px/3));
		}
        requestLayout();

        mFontSize = px;
	}
}
