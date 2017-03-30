package jp.deadend.noname.skk;

import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.btree.BTree;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;

public class SKKDictionary {
    String mDicFile;

    RecordManager mRecMan;
    long          mRecID;
    protected BTree mBTree;
    protected boolean isValid;

    SKKDictionary() {
    }

    SKKDictionary(String dicFile, String btreeName) {
        isValid = true;
        mDicFile = dicFile;

        try {
            mRecMan = RecordManagerFactory.createRecordManager(mDicFile);
            mRecID = mRecMan.getNamedObject(btreeName);

            if (mRecID == 0) {
                Log.e("SKK", "Dictionary not found: " + mDicFile);
                isValid = false;
            }

            mBTree = BTree.load(mRecMan, mRecID);
        } catch (Exception e) {
            Log.e("SKK", "Error in opening the dictionary: " + e.toString());
            isValid = false;
        }
    }

    boolean isValid() {
        return isValid;
    }

    public String[] getCandidates(String key) {
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

        return va_array;
    }

    public List<String> findKeys(String key) {
        List<String> list = new ArrayList<>();
        Tuple         tuple = new Tuple();
        TupleBrowser  browser;
        String str;

        try {
            browser = mBTree.browse(key);

            while (list.size() < 5) {
                if (!browser.getNext(tuple)) {break;}
                str = (String)tuple.getKey();
                if (!str.startsWith(key)) {break;}
                if (SKKUtils.isAlphabet(str.charAt(str.length()-1)) && !SKKUtils.isAlphabet(str.charAt(0))) {continue;} // 送りありエントリは飛ばす

                list.add(str);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    public void close() {
        try {
            mRecMan.commit();
            mRecMan.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        isValid = false;
    }

    private static void appendToEntry(String key, String value, BTree btree) throws IOException {
        String[] val_array;
        String[] oldval_array;
        String oldval = (String)btree.find(key);

        if (oldval != null) {
            val_array = value.substring(1).split("/");
            List<String> tmplist = new ArrayList<>();
            Collections.addAll(tmplist, val_array);
            oldval_array = oldval.substring(1).split("/");
            for (String str: oldval_array) {
                if (tmplist.indexOf(str) == -1) {
                    tmplist.add(str);
                }
            }

            StringBuilder newValue = new StringBuilder();
            newValue.append("/");
            for (String s: tmplist) {
                newValue.append(s);
                newValue.append("/");
            }
            btree.insert(key, newValue.toString(), true);
        } else {
            btree.insert(key, value, true);
        }
    }


    static void loadFromTextDic(String file, RecordManager recMan, BTree btree, boolean overwrite) throws IOException {
        CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPORT);
        decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
        FileInputStream fis = new FileInputStream(file);
        InputStreamReader fr = new InputStreamReader(fis, decoder);
        BufferedReader br = new BufferedReader(fr);

        String line, key, value;
        int idx, count=0;

        for (line = br.readLine(); line != null; line = br.readLine()) {
            if (line.startsWith(";;")) continue;

            idx = line.indexOf(' ');
            if (idx == -1) continue;
            key = line.substring(0, idx);
            value = line.substring(idx + 1, line.length());
            if (overwrite) {
                btree.insert(key, value, true);
            } else {
                appendToEntry(key, value, btree);
            }

            if (++count % 1000 == 0) {
                recMan.commit();
            }
        }

        recMan.commit();
    }
}