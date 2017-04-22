/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * modified by na2co3
 * original: android.support.v7.preference.SeekBarPreference
 */

package jp.gr.java_conf.na2co3.skk;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

/**
 * Preference based on android.preference.SeekBarPreference but uses support v7 preference as base.
 * It contains a title and a seekbar and a seekbar value TextView. The actual preference layout is
 * customizable by setting {@code android:layout} on the preference widget layout or
 * {@code seekBarPreferenceStyle} attribute.
 * The preference will be responsive to DPAD left/right keys.
 * The seekbar value TextView can display its value with a unit of measurement. The unit can be
 * defined by setting {@code unit} attribute.
 * Other SeekBar specific attributes (e.g. {@code title, summary, defaultValue, min, max}) can be
 * set directly on the preference widget layout.
 */
public class SeekBarPreference extends Preference {

    private int mSeekBarValue;
    private String mUnit;
    private int mMin;
    private int mMax;
    private SeekBar mSeekBar;
    private TextView mSeekBarValueTextView;

    private static final String TAG = "SeekBarPreference";

    /**
     * Listener reacting to the SeekBar changing value by the user
     */
    private OnSeekBarChangeListener mSeekBarChangeListener = new OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                syncValueInternal(seekBar, false);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            syncValueInternal(seekBar, true);
        }
    };

    /**
     * Listener reacting to the user pressing DPAD left/right keys if {@code
     * adjustable} attribute is set to true; it transfers the key presses to the SeekBar
     * to be handled accordingly.
     */
    private View.OnKeyListener mSeekBarKeyListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (event.getAction() != KeyEvent.ACTION_DOWN) {
                return false;
            }

            // We don't want to propagate the click keys down to the seekbar view since it will
            // create the ripple effect for the thumb.
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                return false;
            }

            if (mSeekBar == null) {
                Log.e(TAG, "SeekBar view is null and hence cannot be adjusted.");
                return false;
            }
            return mSeekBar.onKeyDown(keyCode, event);
        }
    };

    public SeekBarPreference(
            Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.SeekBarPreference, defStyleAttr, defStyleRes);

        /**
         * The ordering of these two statements are important. If we want to set max first, we need
         * to perform the same steps by changing min/max to max/min as following:
         * mMax = a.getInt(...) and setMin(...).
         */
        mUnit = a.getString(R.styleable.SeekBarPreference_unit);
        mMin = a.getInt(R.styleable.SeekBarPreference_min, 0);
        setMax(a.getInt(R.styleable.SeekBarPreference_android_max, 100));
        a.recycle();
    }

    public SeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SeekBarPreference(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.seekBarPreferenceStyle);
    }

    public SeekBarPreference(Context context) {
        this(context, null);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        view.itemView.setOnKeyListener(mSeekBarKeyListener);
        mSeekBar = (SeekBar) view.findViewById(R.id.seekbar);
        mSeekBarValueTextView = (TextView) view.findViewById(R.id.seekbar_value);

        if (mSeekBar == null) {
            Log.e(TAG, "SeekBar view is null in onBindViewHolder.");
            return;
        }
        mSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
        mSeekBar.setMax(mMax - mMin);
        // This default increment value is set by AbsSeekBar
        // after calling setMax. That's why it's important to call setKeyProgressIncrement after
        // calling setMax() since setMax() can change the increment value.
        mSeekBar.setKeyProgressIncrement(1);

        mSeekBar.setProgress(mSeekBarValue - mMin);
        if (mSeekBarValueTextView != null) {
            mSeekBarValueTextView.setText(getValueText());
        }
        mSeekBar.setEnabled(isEnabled());
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValue(restoreValue ? getPersistedInt(mSeekBarValue)
                : (Integer) defaultValue);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }

    public void setMin(int min) {
        if (min > mMax) {
            min = mMax;
        }
        if (min != mMin) {
            mMin = min;
            notifyChanged();
        }
    }

    public int getMin() {
        return mMin;
    }

    public final void setMax(int max) {
        if (max < mMin) {
            max = mMin;
        }
        if (max != mMax) {
            mMax = max;
            notifyChanged();
        }
    }

    public int getMax() {
        return mMax;
    }

    public void setValue(int seekBarValue) {
        setValueInternal(seekBarValue, true, true);
    }

    private void setValueInternal(int seekBarValue, boolean persist, boolean notifyChanged) {
        if (seekBarValue < mMin) {
            seekBarValue = mMin;
        }
        if (seekBarValue > mMax) {
            seekBarValue = mMax;
        }

        if (seekBarValue != mSeekBarValue) {
            mSeekBarValue = seekBarValue;
            if (mSeekBarValueTextView != null) {
                mSeekBarValueTextView.setText(getValueText());
            }
        }
        if (persist) {
            persistInt(seekBarValue);
        }
        if (notifyChanged) {
            notifyChanged();
        }
    }

    public int getValue() {
        return mSeekBarValue;
    }

    private String getValueText() {
        StringBuilder text = new StringBuilder(String.valueOf(mSeekBarValue));
        if (mUnit != null) {
            text.append(" ");
            text.append(mUnit);
        }
        return text.toString();
    }

    /**
     * Persist the seekBar's seekbar value if callChangeListener
     * returns true, otherwise set the seekBar's value to the stored value
     */
    private void syncValueInternal(SeekBar seekBar, boolean persist) {
        int seekBarValue = mMin + seekBar.getProgress();
        if (seekBarValue != mSeekBarValue) {
            if (callChangeListener(seekBarValue)) {
                setValueInternal(seekBarValue, false, false);
            } else {
                seekBar.setProgress(mSeekBarValue - mMin);
            }
        }
        if (persist) {
            persistInt(seekBarValue);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }

        // Save the instance state
        final SavedState myState = new SavedState(superState);
        myState.seekBarValue = mSeekBarValue;
        myState.min = mMin;
        myState.max = mMax;
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        // Restore the instance state
        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        mSeekBarValue = myState.seekBarValue;
        mMin = myState.min;
        mMax = myState.max;
        notifyChanged();
    }

    /**
     * SavedState, a subclass of {@link BaseSavedState}, will store the state
     * of MyPreference, a subclass of Preference.
     * <p>
     * It is important to always call through to super methods.
     */
    private static class SavedState extends BaseSavedState {
        int seekBarValue;
        int min;
        int max;

        public SavedState(Parcel source) {
            super(source);

            // Restore the click counter
            seekBarValue = source.readInt();
            min = source.readInt();
            max = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);

            // Save the click counter
            dest.writeInt(seekBarValue);
            dest.writeInt(min);
            dest.writeInt(max);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @SuppressWarnings("unused")
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
