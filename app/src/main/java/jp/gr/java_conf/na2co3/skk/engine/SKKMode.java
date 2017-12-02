package jp.gr.java_conf.na2co3.skk.engine;

public interface SKKMode {
    void processKey(SKKEngine context, int pcode);
    CharSequence convertText(CharSequence text);
    SKKMode getToggledKanaMode();
    int getKeyboardType();
    int getIcon();
}
