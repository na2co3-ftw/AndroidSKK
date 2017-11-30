package jp.gr.java_conf.na2co3.skk.engine;

import jp.gr.java_conf.na2co3.skk.SKKUtils;

// 送り仮名入力中(▽モード，*つき)
public enum SKKOkuriganaState implements SKKState {
    INSTANCE;

    public void handleKanaKey(SKKEngine context) {}

    public void processKey(SKKEngine context, int pcode) {
        context.processRomaji(pcode);
    }

    public void processText(SKKEngine context, String text, boolean isShifted) {
        if (text != null) {
            switch (text) {
                case "q":
                    toggleKana(context);
                    return;
                case "l":
                    if (isShifted) {
                        context.changeState(SKKZenkakuState.INSTANCE, true);
                    } else {
                        context.changeState(SKKASCIIState.INSTANCE, true);
                    }
                    return;
                case "/":
                    context.changeState(SKKAbbrevState.INSTANCE, true);
                    return;
            }
            String okurigana = context.getOkurigana();
            if (okurigana == null) {
                context.setOkurigana(text);
            } else {
                context.setOkurigana(okurigana + text);
            }
        }
    }

    public void onFinishRomaji(SKKEngine context) {
        String okurigana = context.getOkurigana();
        if (context.getOkuriConsonant() == null && okurigana != null) {
            String okuriConsonant = RomajiConverter.getConsonant(okurigana.substring(0, 1));
            if (okuriConsonant != null) {
                context.setOkuriConsonant(okuriConsonant);
            }
        }
        context.conversionStart(context.getKanjiKey());
    }

    public void beforeBackspace(SKKEngine context) {}

    public void afterBackspace(SKKEngine context) {
        if (context.getComposing().length() == 0 && context.getOkurigana() == null) {
            context.changeState(SKKKanjiState.INSTANCE);
        }
    }

    public boolean handleCancel(SKKEngine context) {
        StringBuilder kanjiKey = context.getKanjiKey();
        context.getComposing().setLength(0);
        context.setOkurigana(null);
        context.setOkuriConsonant(null);
        kanjiKey.deleteCharAt(kanjiKey.length()-1);
        context.changeState(SKKKanjiState.INSTANCE);

        return true;
    }

    public boolean finish(SKKEngine context) {
        context.commitTextSKK(context.getKanjiKey(), 1);
        if (context.getOkurigana() != null) {
            context.commitTextSKK(context.getOkurigana(), 1);
        }
        return true;
    }

    public boolean toggleKana(SKKEngine context) {
        StringBuilder text = new StringBuilder(context.getKanjiKey());
        if (context.getOkurigana() != null) {
            text.append(context.getOkurigana());
        }
        if (text.length() > 0) {
            String str = SKKUtils.hirakana2katakana(text.toString());
            context.commitTextSKK(str, 1);
        }
        context.changeState(SKKHiraganaState.INSTANCE);
        return true;
    }

    public CharSequence getComposingText(SKKEngine context) {
        String okurigana = context.getOkurigana();
        StringBuilder sb = new StringBuilder(context.getKanjiKey()).append("*");
        if (okurigana != null) {
            sb.append(okurigana);
        }
        sb.append(context.getComposing());
        return sb;
    }

    public boolean isTransient() { return true; }

    public int getIcon() { return 0; }
}
