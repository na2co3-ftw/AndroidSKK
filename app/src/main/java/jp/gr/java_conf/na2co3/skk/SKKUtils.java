package jp.gr.java_conf.na2co3.skk;

import android.util.Log;

public class SKKUtils {
    // 半角から全角 (UNICODE)
    public static int hankaku2zenkaku(int pcode) {
        if (pcode == 0x20) { // スペース
            return 0x3000;
        }
        if (pcode == '¥') {
            return '￥';
        }
        if (pcode == '•') {
            return '・';
        }
        if (0x21 <= pcode && pcode <= 0x7E) {
            return pcode - 0x20 + 0xFF00;
        }
        return pcode;
    }

    public static CharSequence hankaku2zenkaku(CharSequence str) {
        if (str == null) {
            return null;
        }

        int len = str.length();
        StringBuilder buf = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            buf.append((char) SKKUtils.hankaku2zenkaku(str.charAt(i)));
        }
        return buf;
    }

    /**
    * 文字列・改
    *
    * @author 佐藤 雅俊さん <okome@siisise.net> http://siisise.net/java/lang/ のコードを改変
    */
    /**
    * ひらがなを全角カタカナにする
    */
    public static CharSequence hirakana2katakana(CharSequence str) {
        if (str == null) return null;

        StringBuilder str2 = new StringBuilder();

        for (int i=0; i<str.length(); i++) {
            char ch = str.charAt(i);

            if (ch >= 0x3040 && ch <= 0x309A) {
                ch += 0x60;
            }
            str2.append(ch);
        }

        int idx = str2.indexOf("ウ゛");
        if (idx != -1) str2.replace(idx, idx+2, "ヴ");

        return str2.toString();
    }

    public static boolean isAlphabet(int code) {
        return ((code >= 0x41 && code <= 0x5A) || (code >= 0x61 && code <= 0x7A));
    }

    // debug log
    public static void dlog(String msg) {
        if (BuildConfig.DEBUG) {
            Log.d("SKK", msg);
        }
    }
}
