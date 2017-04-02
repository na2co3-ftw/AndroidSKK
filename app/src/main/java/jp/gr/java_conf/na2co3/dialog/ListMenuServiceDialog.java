package jp.gr.java_conf.na2co3.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.ContextThemeWrapper;

import jp.gr.java_conf.na2co3.skk.R;

public class ListMenuServiceDialog extends ServiceDialog {
    public interface Listener {
        void onClick(int whichButton);
    }

    private CharSequence[] mItems;
    private Listener mListener = null;

    public ListMenuServiceDialog(CharSequence[] items) {
        mItems = items;
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    @Override
    protected Dialog createDialog(Context context) {
        return new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.service_dialog_theme))
                .setTitle(R.string.ime_name)
                .setCancelable(true)
                .setItems(mItems,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                                if (mListener != null) {
                                    mListener.onClick(whichButton);
                                }
                            }
                        })
                .create();
    }
}
