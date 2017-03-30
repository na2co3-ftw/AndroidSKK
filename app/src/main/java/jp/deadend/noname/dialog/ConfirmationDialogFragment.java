package jp.deadend.noname.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import jp.deadend.noname.skk.R;

public class ConfirmationDialogFragment extends DialogFragment {
	public interface Listener {
		void onPositiveClick();
		void onNegativeClick();
	}

	private Listener mListener = null;

	public static ConfirmationDialogFragment newInstance(String message) {
		ConfirmationDialogFragment frag = new ConfirmationDialogFragment();
		Bundle args = new Bundle();
		args.putString("message", message);
		frag.setArguments(args);
		return frag;
	}

	public void setListener (Listener listener) {
		this.mListener = listener;
	}

	@Override
	public Dialog onCreateDialog(@NonNull Bundle savedInstanceState) {
		return new AlertDialog.Builder(getActivity())
				.setMessage(getArguments().getString("message"))
				.setCancelable(true)
				.setPositiveButton(R.string.label_OK,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int whichButton) {
								if (mListener != null) {
									mListener.onPositiveClick();
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
