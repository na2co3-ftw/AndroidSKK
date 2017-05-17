package jp.gr.java_conf.na2co3.skk;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SetKeyPreferenceDialogFragmentCompat extends PreferenceDialogFragmentCompat implements OnKeyListener {
    private int mKeyCode;
    private TextView mTextView;
    private static final String FORMAT = "  Key: %s\n  Code: %d";
    private static final String SAVE_STATE_VALUE = "SetKeyPreferenceDialogFragmentCompat.value";

    public static SetKeyPreferenceDialogFragmentCompat newInstance(String key) {
        final SetKeyPreferenceDialogFragmentCompat fragment = new SetKeyPreferenceDialogFragmentCompat();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            mKeyCode = getSetKeyPreference().getValue();
        } else {
            mKeyCode = savedInstanceState.getInt(SAVE_STATE_VALUE);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVE_STATE_VALUE, mKeyCode);
    }

    @Override
    protected View onCreateDialogView(Context ctx) {
        int FP = ViewGroup.LayoutParams.FILL_PARENT;
        int WC = ViewGroup.LayoutParams.WRAP_CONTENT;

        LinearLayout linearLayout = new LinearLayout(ctx);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);

        mTextView = new TextView(ctx);
        mTextView.setTextSize(25);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, WC);
        lp.weight=1;
        linearLayout.addView(mTextView, lp);

        ImageButton button = new ImageButton(ctx);
        button.setImageResource(android.R.drawable.ic_delete);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mKeyCode = getSetKeyPreference().getDefaultValue();
                mTextView.setText(String.format(FORMAT, SetKeyPreference.getKeyName(mKeyCode), mKeyCode));
            }
        });
        button.setFocusable(false);
        linearLayout.addView(button, new LinearLayout.LayoutParams(WC, FP));

        return linearLayout;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        mTextView.setText(String.format(FORMAT, SetKeyPreference.getKeyName(mKeyCode), mKeyCode));
    }

    private SetKeyPreference getSetKeyPreference() {
        return (SetKeyPreference) getPreference();
    }

    @Override
    public void onStart() {
        super.onStart();
        getDialog().setOnKeyListener(this);
        getDialog().takeKeyEvents(true);
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            if (getSetKeyPreference().callChangeListener(mKeyCode)) {
                getSetKeyPreference().setValue(mKeyCode);
            }
        }
    }

    @Override
    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
        case KeyEvent.KEYCODE_ENTER:
            return false;
        case KeyEvent.KEYCODE_HOME:
            return true;
        default:
            mKeyCode = keyCode;
            mTextView.setText(String.format(FORMAT, SetKeyPreference.getKeyName(mKeyCode), mKeyCode));
            return true;
        }
    }
}
