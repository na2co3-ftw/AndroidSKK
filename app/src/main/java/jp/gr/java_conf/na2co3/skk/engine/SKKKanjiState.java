package jp.gr.java_conf.na2co3.skk.engine;

// 漢字変換のための入力中(▽モード)
public enum SKKKanjiState implements SKKState {
    INSTANCE;

    public boolean processKey(SKKEngine context, int pcode) {
        return false;
    }

    public boolean processRomajiExtension(SKKEngine context, String text, boolean isShifted) {
        StringBuilder kanjiKey = context.getKanjiKey();
        switch (text) {
            case ">":
                kanjiKey.append('>');
                context.conversionStart(kanjiKey);
                return true;
            case ".":
                context.pickCurrentSuggestion();
                return true;
        }
        return false;
    }

    public void processText(SKKEngine context, String text, char initial, boolean isShifted) {
        StringBuilder kanjiKey = context.getKanjiKey();
        if (text != null && text.equals(" ")) {
            context.conversionStart(kanjiKey);
            return;
        }

        if (isShifted) {
            // 送り仮名入力
            context.setOkuriConsonant(null);
            context.setOkurigana(null);
            context.changeState(SKKOkuriganaState.INSTANCE);
            SKKOkuriganaState.INSTANCE.processText(context, text, initial, false);
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

        if (kanjiKey.length() == 0 && !context.hasComposing()) {
            context.changeState(SKKNormalState.INSTANCE);
        } else {
            context.updateSuggestions(kanjiKey.toString());
        }
    }

    public boolean handleCancel(SKKEngine context) {
        context.changeState(SKKNormalState.INSTANCE);
        return true;
    }

    public boolean finish(SKKEngine context) {
        context.commitTextSKK(context.convertText(context.getKanjiKey()), 1);
        return true;
    }

    public void toggleKana(SKKEngine context) {
        StringBuilder kanjiKey = context.getKanjiKey();
        if (kanjiKey.length() > 0) {
            context.commitTextSKK(context.getToggledKanaMode().convertText(kanjiKey), 1);
        }
        context.changeState(SKKNormalState.INSTANCE);
    }


    public CharSequence getComposingText(SKKEngine context) {
        return context.convertText(context.getKanjiKey());
    }

    public int getKeyboardType(SKKEngine context) { return -1; }

    public boolean isTransient() { return true; }
    public boolean isConverting() { return false; }

    public int getIcon() { return 0; }
}
