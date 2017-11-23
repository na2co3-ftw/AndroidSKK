package jp.gr.java_conf.na2co3.skk;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.inputmethodservice.Keyboard;

import java.util.List;

class SKKKeyboard extends Keyboard {
    private int mNumRow;

    @Override
    public int getHeight() {
        return getKeyHeight() * mNumRow;
    }

    void changeKeyHeight(int px) {
        int y = 0;
        int rowNo = 0;
        for (Keyboard.Key key : getKeys()) {
            key.height = px;
            if (key.y != y) {
                y = key.y;
                rowNo++;
            }
            key.y = px*rowNo;
        }
        setKeyHeight(px);
        getNearestKeys(0, 0);
        //somehow adding this fixed a weird bug where bottom row keys could not be pressed if keyboard height is too tall.. from the Keyboard source code seems like calling this will recalculate some values used in keypress detection calculation
    }

    SKKKeyboard(Context context, int xmlLayoutResId, int numRow) {
        super(context, xmlLayoutResId);
        mNumRow = numRow;
    }

    @SuppressWarnings("unchecked")
    List<Key> getSKKKeys() {
        return (List<Key>)(List<? super Key>)super.getKeys();
    }

    @Override
    protected Key createKeyFromXml(Resources res, Row parent, int x, int y, XmlResourceParser parser) {
        return new Key(res, parent, x, y, parser);
    }

    static class Key extends Keyboard.Key {
        boolean skkRepeatable;

        Key(Resources res, Row parent, int x, int y, XmlResourceParser parser) {
            super(res, parent, x, y, parser);
            skkRepeatable = repeatable;
            repeatable = false;
        }
    }
}
