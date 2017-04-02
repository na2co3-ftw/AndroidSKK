package jp.gr.java_conf.na2co3.dialog;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class ServiceDialog {
    public ServiceDialog() {
    }

    protected Dialog createDialog(Context context) {
        return new Dialog(context);
    }

    public void show(View view) {
        Dialog dialog = this.createDialog(view.getContext());
        Window window = dialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.token = view.getWindowToken();
        lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
        window.setAttributes(lp);
        window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        dialog.show();
    }
}
