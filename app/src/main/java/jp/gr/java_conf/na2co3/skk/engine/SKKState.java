package jp.gr.java_conf.na2co3.skk.engine;

public interface SKKState {
    boolean processKey(SKKEngine context, int pcode);
    boolean processRomajiExtension(SKKEngine context, String text, boolean isShifted);
    void processText(SKKEngine context, String text, boolean isShifted);
    void onFinishRomaji(SKKEngine context);
    void beforeBackspace(SKKEngine context);
    void afterBackspace(SKKEngine context);
    boolean handleCancel(SKKEngine context);
    boolean finish(SKKEngine context);
    void toggleKana(SKKEngine context);
    CharSequence getComposingText(SKKEngine context);
    int getKeyboardType(SKKEngine context);
    boolean isTransient();
    int getIcon();
}
