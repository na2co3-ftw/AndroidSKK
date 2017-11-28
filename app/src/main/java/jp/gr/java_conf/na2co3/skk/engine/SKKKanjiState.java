package jp.gr.java_conf.na2co3.skk.engine;

import jp.gr.java_conf.na2co3.skk.SKKUtils;

// 漢字変換のためのひらがな入力中(▽モード)
public enum SKKKanjiState implements SKKState {
    INSTANCE;

    public void handleKanaKey(SKKEngine context) {}

    public void processKey(SKKEngine context, int pcode) {
        context.processRomaji(pcode);
    }

    public void processText(SKKEngine context, String text, boolean isShifted) {
        StringBuilder kanjiKey = context.getKanjiKey();
        if (text != null) {
            switch (text) {
                case "q":
                    // カタカナ変換
                    if (kanjiKey.length() > 0) {
                        String str = SKKUtils.hirakana2katakana(kanjiKey.toString());
                        context.commitTextSKK(str, 1);
                    }
                    context.changeState(SKKHiraganaState.INSTANCE);
                    return;
                case ">":
                    if (text.equals(">")) {
                        kanjiKey.append('>');
                    }
                    // fallthrough
                case " ":
                    context.conversionStart(kanjiKey);
                    return;
                case ".":
                    context.pickCurrentSuggestion();
                    return;
            }
        }

        if (isShifted) {
            // 送り仮名入力，変換
            if (text != null) {
                String okuriConsonant = RomajiConverter.getConsonant(text.substring(0, 1));
                context.setOkuriConsonant(okuriConsonant);
                context.setOkurigana(text);
            } else {
                context.setOkuriConsonant(null);
                context.setOkurigana(null);
            }
            context.changeState(SKKOkuriganaState.INSTANCE);
        } else {
            // 未確定
            if (text != null) {
                kanjiKey.append(text);
                context.updateSuggestions(kanjiKey.toString());
            }
        }
    }

    public void onFinishRomaji(SKKEngine context) {}
    public void beforeBackspace(SKKEngine context) {}

    public void afterBackspace(SKKEngine context) {
        StringBuilder kanjiKey = context.getKanjiKey();
        StringBuilder composing = context.getComposing();

        if (kanjiKey.length() == 0 && composing.length() == 0) {
            context.changeState(SKKHiraganaState.INSTANCE);
        } else {
            context.updateSuggestions(kanjiKey.toString());
        }
    }

    public boolean handleCancel(SKKEngine context) {
        context.changeState(SKKHiraganaState.INSTANCE);
        return true;
    }

    public CharSequence getComposingText(SKKEngine context) {
        return new StringBuilder(context.getKanjiKey()).append(context.getComposing());
    }

    public boolean isTransient() { return true; }

    public int getIcon() { return 0; }
}
