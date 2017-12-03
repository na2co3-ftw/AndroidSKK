package jp.gr.java_conf.na2co3.skk;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import android.util.Log;

public class SKKUtils {
//    private static final Pattern PAT_QUOTED = Pattern.compile("\"(.+?)\"");
    private static final Pattern PAT_ESCAPE_NUM = Pattern.compile("\\\\([0-9]+)");

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

    public static String removeAnnotation(String text) {
        int i = text.lastIndexOf(';'); // セミコロンで解説が始まる
        return ((i == -1) ? text : text.substring(0, i));
    }

    public static String processConcatAndEscape(String text) {
        int len = text.length();
        if (len < 12) {return text;}
        if (text.charAt(0) != '(' || !(text.substring(1, 9).equals("concat \"")) || !(text.substring(len-2, len).equals("\")"))) {return text;}

//        Matcher m = PAT_QUOTED.matcher(text.substring(8, len-1));
//        StringBuilder buf = new StringBuilder();
//        while (m.find()) {
//            buf.append(m.group(1));
//        }
//        m = PAT_ESCAPE_NUM.matcher(buf.toString());

        Matcher m = PAT_ESCAPE_NUM.matcher(text.substring(9, len-2));
        StringBuffer buf2 = new StringBuffer();
        while (m.find()) {
            int num = Integer.parseInt(m.group(1), 8); // emacs-lispのリテラルは8進数
            m.appendReplacement(buf2, Character.toString((char)num));
        }
        m.appendTail(buf2);

        return buf2.toString();
    }

    // debug log
    public static void dlog(String msg) {
        if (BuildConfig.DEBUG) {
            Log.d("SKK", msg);
        }
    }
}
