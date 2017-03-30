package jp.gr.java_conf.na2co3.skk;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.io.IOException;

import jdbm.RecordManagerFactory;
import jdbm.btree.BTree;
import jdbm.helper.StringComparator;

public class SKKUserDictionary extends SKKDictionary {
    public class Entry {
        Entry(List<String> cd, List<List<String>> okr) {
            candidates = cd;
            okuri_blocks = okr;
        }

        public List<String> candidates = null;
        public List<List<String>> okuri_blocks = null;
    }

    private String mOldKey = null;
    private String mOldValue = null;

    SKKUserDictionary(String dicFile, String btreeName) {
        isValid = true;
        mDicFile = dicFile;

        try {
            mRecMan = RecordManagerFactory.createRecordManager(mDicFile);
            mRecID = mRecMan.getNamedObject(btreeName);

            if (mRecID == 0) {
                mBTree = BTree.createInstance(mRecMan, new StringComparator());
                mRecMan.setNamedObject(btreeName, mBTree.getRecid());
                mRecMan.commit();
                SKKUtils.dlog("New user dictionary created");
            } else {
                mBTree = BTree.load(mRecMan, mRecID);
            }
        } catch (Exception e) {
            Log.e("SKK", "Error in opening the user dic: " + e.toString());
            isValid = false;
        }
    }

    @Override
    public String[] getCandidates(String key) {
        Log.e("SKK", "Don't use SKKUserDictionary#getCandidates()");
        return null;
    }

    public Entry getEntry(String key) {
        List<String> cd = new ArrayList<>();
        List<List<String>> okr = new ArrayList<>();
        String[] va_array;

        String value;
        try {
            value = (String)mBTree.find(key);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (value == null) return null;

        va_array = value.substring(1).split("/"); // 先頭のスラッシュをとってから分割
        if (va_array.length <= 0) {
            Log.e("SKK", "Invalid value found: Key=" + key + " value=" + value);
            return null;
        }

        for (String str: va_array) {
            // 送りがなブロックが見つかったら中止
            if (str.startsWith("[")) { break; }
            cd.add(str);
        }

        if (value.contains("[") && value.contains("]")) {
            // 送りがなブロック
            va_array = value.split("[\\[\\]]");
            List<String> tmp_okr;
            String[] va_array2;
            // va_array[0]は変換候補なので1から始める
            for (int i=1; i<va_array.length; i++) {
                if (va_array[i].equals("/")) { continue; } // ブロックの境界
                va_array2 = va_array[i].split("/");
                tmp_okr = new ArrayList<>();
                Collections.addAll(tmp_okr, va_array2);
                okr.add(tmp_okr);
            }
        }

        return new Entry(cd, okr);
    }

    public void addEntry(String key, String val, String okuri) {
        mOldKey = key;
        StringBuilder new_val = new StringBuilder();
        Entry entry = getEntry(key);

        if (entry == null) {
            new_val.append("/");
            new_val.append(val);
            new_val.append("/");
            if (okuri != null) {
                new_val.append("[");
                new_val.append(okuri);
                new_val.append("/");
                new_val.append(val);
                new_val.append("/]/");
            }
            mOldValue = null;
        } else {
            List<String> cands = entry.candidates;
            cands.remove(val);
            cands.add(0, val);

            List<List<String>> okrs = entry.okuri_blocks;
            if (okuri != null) {
                boolean found = false;
                for (List<String> lst : okrs) {
                    if (lst.get(0).equals(okuri)) {
                        found = true;
                        if (!lst.contains(val)) {lst.add(val);}
                    }
                }
                if (!found) {
                    List<String> new_okr = new ArrayList<>();
                    new_okr.add(okuri);
                    new_okr.add(val);
                    okrs.add(new_okr);
                }
            }

            for (String str : cands) {
                new_val.append("/");
                new_val.append(str);
            }
            for (List<String> lst : okrs) {
                new_val.append("/[");
                for (String str : lst) {
                    new_val.append(str);
                    new_val.append("/");
                }
                new_val.append("]");
            }
            new_val.append("/");

            try {
                mOldValue = (String)mBTree.find(key);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            mBTree.insert(key, new_val.toString(), true);
            mRecMan.commit();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void rollBack() {
        if (mOldKey == null) { return; }

        try {
            if (mOldValue == null) {
                mBTree.remove(mOldKey);
            } else {
                mBTree.insert(mOldKey, mOldValue, true);
            }
            mRecMan.commit();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        mOldValue = null;
        mOldKey = null;
    }

    public void commitChanges() {
        try {
            mRecMan.commit();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}