package jp.gr.java_conf.na2co3.skk.engine;

// 送り仮名入力中(▽モード，*つき)
public enum SKKOkuriganaState implements SKKState {
    INSTANCE;

    public boolean processKey(SKKEngine context, int pcode) {
        return false;
    }

    public boolean processRomajiExtension(SKKEngine context, String text, boolean isShifted) {
        return false;
    }

    public void processText(SKKEngine context, String text, boolean isShifted) {
        if (text != null) {
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
        context.commitTextSKK(context.convertText(context.getKanjiKey()), 1);
        if (context.getOkurigana() != null) {
            context.commitTextSKK(context.convertText(context.getOkurigana()), 1);
        }
        return true;
    }

    public void toggleKana(SKKEngine context) {
        StringBuilder text = new StringBuilder(context.getKanjiKey());
        if (context.getOkurigana() != null) {
            text.append(context.getOkurigana());
        }
        if (text.length() > 0) {
            context.commitTextSKK(context.getToggledKanaMode().convertText(text), 1);
        }
        context.changeState(SKKNormalState.INSTANCE);
    }

    public CharSequence getComposingText(SKKEngine context) {
        String okurigana = context.getOkurigana();
        StringBuilder sb = new StringBuilder();
        sb.append(context.convertText(context.getKanjiKey()));
        sb.append("*");
        if (okurigana != null) {
            sb.append(context.convertText(okurigana));
        }
        sb.append(context.getComposing());
        return sb;
    }

    public int getKeyboardType(SKKEngine context) { return -1; }

    public boolean isTransient() { return true; }

    public int getIcon() { return 0; }
}
