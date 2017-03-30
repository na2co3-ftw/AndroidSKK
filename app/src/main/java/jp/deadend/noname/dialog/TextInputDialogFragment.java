package jp.deadend.noname.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.widget.EditText;

import jp.deadend.noname.skk.R;

public class TextInputDialogFragment extends DialogFragment {
	public interface Listener {
		void onPositiveClick(String result);
		void onNegativeClick();
	}

	private Listener mListener = null;
	private EditText mEditText = null;
	private boolean mSingleLine = false;

	public static TextInputDialogFragment newInstance(String message) {
		TextInputDialogFragment frag = new TextInputDialogFragment();
		Bundle args = new Bundle();
		args.putString("message", message);
		frag.setArguments(args);
		return frag;
	}

	public void setListener (Listener listener) {
		this.mListener = listener;
	}

	public void setSingleLine(boolean val) {
		mSingleLine = val;
	}

	@Override
	public Dialog onCreateDialog(@NonNull Bundle savedInstanceState) {
		mEditText = new EditText(getActivity());
		if (mSingleLine) {mEditText.setSingleLine();}

		return new AlertDialog.Builder(getActivity())
				.setMessage(getArguments().getString("message"))
				.setView(mEditText)
				.setCancelable(true)
				.setPositiveButton(R.string.label_OK,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int whichButton) {
								if (mListener != null) {
									mListener.onPositiveClick(mEditText.getText().toString());
								}
								dismiss();
							}
						}
				)
				.setNegativeButton(R.string.label_CANCEL,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int whichButton) {
								if (mListener != null) {
									mListener.onNegativeClick();
								}
								dismiss();
							}
						}
				)
				.create();
	}
}
