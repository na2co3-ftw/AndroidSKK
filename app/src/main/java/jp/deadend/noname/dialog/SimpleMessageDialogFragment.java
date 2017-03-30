package jp.deadend.noname.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import jp.deadend.noname.skk.R;

public class SimpleMessageDialogFragment extends DialogFragment {
    public interface Listener {
        void onClick();
    }

    private Listener mListener = null;

    public static SimpleMessageDialogFragment newInstance(String message) {
        SimpleMessageDialogFragment frag = new SimpleMessageDialogFragment();
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
        Bundle args = getArguments();

        return new AlertDialog.Builder(getActivity())
                .setMessage(args.getString("message"))
                .setPositiveButton(R.string.label_OK,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                if (mListener != null) {
                                    mListener.onClick();
                                }
                                dismiss();
                            }
                        }
                )
                .create();
    }
}
