package jp.gr.java_conf.na2co3.skk.engine;

// 送り仮名入力中(▽モード，*つき)
enum SKKOkuriganaState implements SKKState {
    INSTANCE;

    public boolean processKey(SKKEngine context, int pcode) {
        return false;
    }

    public boolean processRomajiExtension(SKKEngine context, String text, boolean isShifted) {
        return false;
    }

    public void processText(SKKEngine context, String text, char initial, boolean isShifted) {
        if (text != null) {
            String okurigana = context.getOkurigana();
            if (okurigana == null) {
                context.setOkurigana(text);
                if (initial != '\0') {
                    context.setOkuriConsonant(String.valueOf(initial));
                } else {
                    context.setOkuriConsonant(RomajiConverter.getConsonant(text.substring(0, 1)));
                }
            } else {
                context.setOkurigana(okurigana + text);
            }
        }
    }

    public void onFinishRomaji(SKKEngine context) {
        context.conversionStart();
    }

    public void beforeBackspace(SKKEngine context) {}

    public void afterBackspace(SKKEngine context) {
        if (!context.hasComposing() && context.getOkurigana() == null) {
            context.changeState(SKKKanjiState.INSTANCE);
        }
    }

    public boolean handleCancel(SKKEngine context) {
        return SKKKanjiState.INSTANCE.handleCancel(context);
    }

    public boolean finish(SKKEngine context) {
        context.commitTextSKK(context.convertText(context.getConvKey()), 1);
        if (context.getOkurigana() != null) {
            context.commitTextSKK(context.convertText(context.getOkurigana()), 1);
        }
        return true;
    }

    public void toggleKana(SKKEngine context) {
        StringBuilder text = new StringBuilder(context.getConvKey());
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
        sb.append(context.convertText(context.getConvKey()));
        sb.append("*");
        if (okurigana != null) {
            sb.append(context.convertText(okurigana));
        }
        return sb;
    }

    public int getKeyboardType() { return SKKEngine.KEYBOARD_NONE; }

    public boolean isTransient() { return true; }
    public boolean isConverting() { return false; }

    public int getIcon() { return 0; }
}
