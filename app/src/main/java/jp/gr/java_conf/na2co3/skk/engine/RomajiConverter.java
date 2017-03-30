package jp.gr.java_conf.na2co3.skk.engine;

import java.util.HashMap;
import java.util.Map;

import jp.gr.java_conf.na2co3.skk.SKKUtils;

enum RomajiConverter {
    INSTANCE;

    // ローマ字辞書
    private Map<String, String> mRomajiMap = new HashMap<>();
    {
        Map<String, String> m = mRomajiMap;
        m.put("a", "あ");m.put("i", "い");m.put("u", "う");m.put("e", "え");m.put("o", "お");
        m.put("ka", "か");m.put("ki", "き");m.put("ku", "く");m.put("ke", "け");m.put("ko", "こ");
        m.put("sa", "さ");m.put("si", "し");m.put("su", "す");m.put("se", "せ");m.put("so", "そ");
        m.put("ta", "た");m.put("ti", "ち");m.put("tu", "つ");m.put("te", "て");m.put("to", "と");
        m.put("na", "な");m.put("ni", "に");m.put("nu", "ぬ");m.put("ne", "ね");m.put("no", "の");
        m.put("ha", "は");m.put("hi", "ひ");m.put("hu", "ふ");m.put("he", "へ");m.put("ho", "ほ");
        m.put("ma", "ま");m.put("mi", "み");m.put("mu", "む");m.put("me", "め");m.put("mo", "も");
        m.put("ya", "や");m.put("yi", "い");m.put("yu", "ゆ");m.put("ye", "いぇ");m.put("yo", "よ");
        m.put("ra", "ら");m.put("ri", "り");m.put("ru", "る");m.put("re", "れ");m.put("ro", "ろ");
        m.put("wa", "わ");m.put("wi", "うぃ");m.put("we", "うぇ");m.put("wo", "を");m.put("nn", "ん");
        m.put("ga", "が");m.put("gi", "ぎ");m.put("gu", "ぐ");m.put("ge", "げ");m.put("go", "ご");
        m.put("za", "ざ");m.put("zi", "じ");m.put("zu", "ず");m.put("ze", "ぜ");m.put("zo", "ぞ");
        m.put("da", "だ");m.put("di", "ぢ");m.put("du", "づ");m.put("de", "で");m.put("do", "ど");
        m.put("ba", "ば");m.put("bi", "び");m.put("bu", "ぶ");m.put("be", "べ");m.put("bo", "ぼ");
        m.put("pa", "ぱ");m.put("pi", "ぴ");m.put("pu", "ぷ");m.put("pe", "ぺ");m.put("po", "ぽ");
        m.put("va", "う゛ぁ");m.put("vi", "う゛ぃ");m.put("vu", "う゛");m.put("ve", "う゛ぇ");m.put("vo", "う゛ぉ");

        m.put("xa", "ぁ");m.put("xi", "ぃ");m.put("xu", "ぅ");m.put("xe", "ぇ");m.put("xo", "ぉ");
        m.put("xtu", "っ");m.put("xke", "ヶ");
        m.put("cha", "ちゃ");m.put("chi", "ち");m.put("chu", "ちゅ");m.put("che", "ちぇ");m.put("cho", "ちょ");
        m.put("fa", "ふぁ");m.put("fi", "ふぃ");m.put("fu", "ふ");m.put("fe", "ふぇ");m.put("fo", "ふぉ");

        m.put("xya", "ゃ");                 m.put("xyu", "ゅ");                 m.put("xyo", "ょ");
        m.put("kya", "きゃ");               m.put("kyu", "きゅ");               m.put("kyo", "きょ");
        m.put("gya", "ぎゃ");               m.put("gyu", "ぎゅ");               m.put("gyo", "ぎょ");
        m.put("sya", "しゃ");               m.put("syu", "しゅ");               m.put("syo", "しょ");
        m.put("sha", "しゃ");m.put("shi", "し");m.put("shu", "しゅ");m.put("she", "しぇ");m.put("sho", "しょ");
        m.put("ja",  "じゃ");m.put("ji",  "じ");m.put("ju", "じゅ");m.put("je", "じぇ");m.put("jo", "じょ");
        m.put("cha", "ちゃ");m.put("chi", "ち");m.put("chu", "ちゅ");m.put("che", "ちぇ");m.put("cho", "ちょ");
        m.put("tya", "ちゃ");               m.put("tyu", "ちゅ");m.put("tye", "ちぇ");m.put("tyo", "ちょ");
        m.put("tha", "てゃ");m.put("thi", "てぃ");m.put("thu", "てゅ");m.put("the", "てぇ");m.put("tho", "てょ");
        m.put("dha", "でゃ");m.put("dhi", "でぃ");m.put("dhu", "でゅ");m.put("dhe", "でぇ");m.put("dho", "でょ");
        m.put("dya", "ぢゃ");m.put("dyi", "ぢぃ");m.put("dyu", "ぢゅ");m.put("dye", "ぢぇ");m.put("dyo", "ぢょ");
        m.put("nya", "にゃ");               m.put("nyu", "にゅ");               m.put("nyo", "にょ");
        m.put("hya", "ひゃ");               m.put("hyu", "ひゅ");               m.put("hyo", "ひょ");
        m.put("pya", "ぴゃ");               m.put("pyu", "ぴゅ");               m.put("pyo", "ぴょ");
        m.put("bya", "びゃ");               m.put("byu", "びゅ");               m.put("byo", "びょ");
        m.put("mya", "みゃ");               m.put("myu", "みゅ");               m.put("myo", "みょ");
        m.put("rya", "りゃ");               m.put("ryu", "りゅ");m.put("rye", "りぇ");m.put("ryo", "りょ");
        m.put("z,", "‥");m.put("z-", "〜");m.put("z.", "…");m.put("z/", "・");m.put("z[", "『");m.put("z]", "』");m.put("zh", "←");m.put("zj", "↓");m.put("zk", "↑");m.put("zl", "→");
    }

    // 濁音半濁音変換用
    private Map<String, String> mConsonantMap = new HashMap<>();
    {
        Map<String, String> m = mConsonantMap;
        m.put("が", "g");m.put("ぎ", "g");m.put("ぐ", "g");m.put("げ", "g");m.put("ご", "g");
        m.put("か", "k");m.put("き", "k");m.put("く", "k");m.put("け", "k");m.put("こ", "k");
        m.put("ざ", "z");m.put("じ", "z");m.put("ず", "z");m.put("ぜ", "z");m.put("ぞ", "z");
        m.put("さ", "s");m.put("し", "s");m.put("す", "s");m.put("せ", "s");m.put("そ", "s");
        m.put("だ", "d");m.put("ぢ", "d");m.put("づ", "d");m.put("で", "d");m.put("ど", "d");
        m.put("た", "t");m.put("ち", "t");m.put("つ", "t");m.put("て", "t");m.put("と", "t");
        m.put("ば", "b");m.put("び", "b");m.put("ぶ", "b");m.put("べ", "b");m.put("ぼ", "b");
        m.put("ぱ", "p");m.put("ぴ", "p");m.put("ぷ", "p");m.put("ぺ", "p");m.put("ぽ", "p");
        m.put("は", "h");m.put("ひ", "h");m.put("ふ", "h");m.put("へ", "h");m.put("ほ", "h");
    }

    // かな小文字変換用
    private Map<String, String> mSmallKanaMap = new HashMap<>();
    {
        Map<String, String> m = mSmallKanaMap;
        m.put("あ", "ぁ");m.put("い", "ぃ");m.put("う", "ぅ");m.put("え", "ぇ");m.put("お", "ぉ");
        m.put("ぁ", "あ");m.put("ぃ", "い");m.put("ぅ", "う");m.put("ぇ", "え");m.put("ぉ", "お");
        m.put("や", "ゃ");m.put("ゆ", "ゅ");m.put("よ", "ょ");m.put("つ", "っ");
        m.put("ゃ", "や");m.put("ゅ", "ゆ");m.put("ょ", "よ");m.put("っ", "つ");
        m.put("ア", "ァ");m.put("イ", "ィ");m.put("ウ", "ゥ");m.put("エ", "ェ");m.put("オ", "ォ");
        m.put("ァ", "ア");m.put("ィ", "イ");m.put("ゥ", "ウ");m.put("ェ", "エ");m.put("ォ", "オ");
        m.put("ヤ", "ャ");m.put("ユ", "ュ");m.put("ヨ", "ョ");m.put("ツ", "ッ");
        m.put("ャ", "ヤ");m.put("ュ", "ユ");m.put("ョ", "ヨ");m.put("ッ", "ツ");
    }
    // 濁音変換用
    private Map<String, String> mDakutenMap = new HashMap<>();
    {
        Map<String, String> m = mDakutenMap;
        m.put("か", "が");m.put("き", "ぎ");m.put("く", "ぐ");m.put("け", "げ");m.put("こ", "ご");
        m.put("が", "か");m.put("ぎ", "き");m.put("ぐ", "く");m.put("げ", "け");m.put("ご", "こ");
        m.put("さ", "ざ");m.put("し", "じ");m.put("す", "ず");m.put("せ", "ぜ");m.put("そ", "ぞ");
        m.put("ざ", "さ");m.put("じ", "し");m.put("ず", "す");m.put("ぜ", "せ");m.put("ぞ", "そ");
        m.put("た", "だ");m.put("ち", "ぢ");m.put("つ", "づ");m.put("て", "で");m.put("と", "ど");
        m.put("だ", "た");m.put("ぢ", "ち");m.put("づ", "つ");m.put("で", "て");m.put("ど", "と");
        m.put("は", "ば");m.put("ひ", "び");m.put("ふ", "ぶ");m.put("へ", "べ");m.put("ほ", "ぼ");
        m.put("ば", "は");m.put("び", "ひ");m.put("ぶ", "ふ");m.put("べ", "へ");m.put("ぼ", "ほ");
        m.put("カ", "ガ");m.put("キ", "ギ");m.put("ク", "グ");m.put("ケ", "ゲ");m.put("コ", "ゴ");
        m.put("ガ", "カ");m.put("ギ", "キ");m.put("グ", "ク");m.put("ゲ", "ケ");m.put("ゴ", "コ");
        m.put("サ", "ザ");m.put("シ", "ジ");m.put("ス", "ズ");m.put("セ", "セ");m.put("ソ", "ゾ");
        m.put("ザ", "サ");m.put("ジ", "シ");m.put("ズ", "ス");m.put("ゼ", "ゼ");m.put("ゾ", "ソ");
        m.put("タ", "ダ");m.put("チ", "ヂ");m.put("ツ", "ヅ");m.put("テ", "デ");m.put("ト", "ド");
        m.put("ダ", "タ");m.put("ヂ", "チ");m.put("ヅ", "ツ");m.put("デ", "テ");m.put("ド", "ト");
        m.put("ハ", "バ");m.put("ヒ", "ビ");m.put("フ", "ブ");m.put("ヘ", "ベ");m.put("ホ", "ボ");
        m.put("バ", "ハ");m.put("ビ", "ヒ");m.put("ブ", "フ");m.put("ベ", "ヘ");m.put("ボ", "ホ");
        m.put("ウ", "ヴ");m.put("ヴ", "ウ");
    }
    // 濁音変換用
    private Map<String, String> mHanDakutenMap = new HashMap<>();
    {
        Map<String, String> m = mHanDakutenMap;
        m.put("は", "ぱ");m.put("ひ", "ぴ");m.put("ふ", "ぷ");m.put("へ", "ぺ");m.put("ほ", "ぽ");
        m.put("ぱ", "は");m.put("ぴ", "ひ");m.put("ぷ", "ふ");m.put("ぺ", "へ");m.put("ぽ", "ほ");
        m.put("ハ", "パ");m.put("ヒ", "ピ");m.put("フ", "プ");m.put("ヘ", "ペ");m.put("ホ", "ポ");
        m.put("パ", "ハ");m.put("ピ", "ヒ");m.put("プ", "フ");m.put("ペ", "ヘ");m.put("ポ", "ホ");
    }

    String convert(String romaji) {
        return mRomajiMap.get(romaji);
    }

    String getConsonantForVoiced(String kana) {
        return mConsonantMap.get(kana);
    }

    String convertLastChar(String kana, String type) {
        switch (type) {
            case SKKEngine.LAST_CONVERTION_SMALL:
                return mSmallKanaMap.get(kana);
            case SKKEngine.LAST_CONVERTION_DAKUTEN:
                return mDakutenMap.get(kana);
            case SKKEngine.LAST_CONVERTION_HANDAKUTEN:
                return mHanDakutenMap.get(kana);
        }
        return null;
    }

    // 1文字目と2文字目を合わせて"ん"・"っ"になるか判定
    // ならなかったらnull
    String checkSpecialConsonants(char first, int second) {
        if (first == 'n') {
            if (!SKKUtils.isVowel(second) && second != 'n' && second != 'y') {
                return "ん";
            }
        } else if (first == second) {
            return "っ";
        }

        return null;
    }
}
