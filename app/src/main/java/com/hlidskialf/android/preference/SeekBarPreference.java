/* The following code was written by Matthew Wiggins
 * and is released under the APACHE 2.0 license
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package com.hlidskialf.android.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.preference.DialogPreference;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.LinearLayout;


public class SeekBarPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener {
	private static final String androidns="http://schemas.android.com/apk/res/android";
	private static final String seekbarns="http://schemas.android.com/apk/res/jp.gr.java_conf.na2co3.skk";

	private SeekBar mSeekBar;
	private TextView mValueText;

	private String mDialogMessage, mSuffix;
	private int mDefault, mMax, mMin, mStep, mValue = 0;
	// mDefaultだけはPreferenceの値なので，外部に見える値

	public SeekBarPreference(Context context, AttributeSet attrs) {
		super(context,attrs);

		mDialogMessage = attrs.getAttributeValue(androidns,"dialogMessage");
		mSuffix = attrs.getAttributeValue(androidns,"text");
		mMin = attrs.getAttributeIntValue(seekbarns,"min", 0);
		mStep = attrs.getAttributeIntValue(seekbarns,"step", 1);
		mMax = (attrs.getAttributeIntValue(androidns,"max", 100) - mMin)/mStep;
		mDefault = attrs.getAttributeIntValue(androidns,"defaultValue", mMin);
	}

	@Override
	protected void onSetInitialValue(boolean restore, Object defaultValue) {
		super.onSetInitialValue(restore, defaultValue);

		if (restore) {
			mValue = (getPersistedInt(mDefault) - mMin) / mStep;
		} else {
			mValue = ((Integer) defaultValue - mMin) / mStep;
		}
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return a.getInteger(index, mDefault);
	}

	@Override
	protected View onCreateDialogView() {
		Context ctx = this.getContext();
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

		LinearLayout layout = new LinearLayout(ctx);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setPadding(6, 6, 6, 6);

		if (mDialogMessage != null) {
			TextView tv = new TextView(ctx);
			tv.setText(mDialogMessage);
			layout.addView(tv, params);
		}

		mValueText = new TextView(ctx);
		mValueText.setGravity(Gravity.CENTER_HORIZONTAL);
		mValueText.setTextSize(32);
		layout.addView(mValueText, params);

		mSeekBar = new SeekBar(ctx);
		mSeekBar.setOnSeekBarChangeListener(this);
		mSeekBar.setMax(mMax);
		layout.addView(mSeekBar, params);

		return layout;
	}

	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);

		mValue = (getPersistedInt(mDefault) - mMin) / mStep;
		mSeekBar.setProgress(mValue);
		String t = String.valueOf(mMin + mValue*mStep);
		mValueText.setText(mSuffix == null ? t : t.concat(mSuffix));
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);

		if (positiveResult) {
			if (shouldPersist()) {
				int value = mSeekBar.getProgress();
				persistInt(mMin + value*mStep);
			}
		}
	}

	public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {
		String t = String.valueOf(mMin + value*mStep);
		mValueText.setText(mSuffix == null ? t : t.concat(mSuffix));
		callChangeListener(Integer.valueOf(mMin + value*mStep));
	}

	public void onStartTrackingTouch(SeekBar seek) {}
	public void onStopTrackingTouch(SeekBar seek) {}
}
