package jp.gr.java_conf.na2co3.skk;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Environment;

class MyUncaughtExceptionHandler implements UncaughtExceptionHandler {
    private static String mVersionName;
    private UncaughtExceptionHandler mDefaultUEH;
    MyUncaughtExceptionHandler(Context context) {
        mDefaultUEH = Thread.getDefaultUncaughtExceptionHandler();

        PackageInfo packInfo;
        try {
            packInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return;
        }
        mVersionName = packInfo.versionName;
    }

    public void uncaughtException(Thread th, Throwable t) {
        try {
            saveState(t);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        mDefaultUEH.uncaughtException(th, t);
    }

    private void saveState(Throwable e) throws FileNotFoundException {
        Date d = new Date();
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmm");
        String dateTimeStr = df.format(d);

        File file = new File(
            Environment.getExternalStorageDirectory().getPath()
            + File.separator
            + "SKK_strace_"
            + dateTimeStr
            + ".txt");
        PrintWriter pw = new PrintWriter(new FileOutputStream(file));

        pw.println("This is a crash report of SKK.");
        pw.println();
        pw.println("Device:  " + Build.DEVICE);
        pw.println("Model:   " + Build.MODEL);
        pw.println("SDK:     " + Build.VERSION.SDK_INT);
        pw.println("Version: " + mVersionName);
        pw.println();

        e.printStackTrace(pw);
        pw.close();
    }
}
