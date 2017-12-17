package jp.gr.java_conf.na2co3.skk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DictionaryProcessor {
    private List<SKKDictionary> mDicts;
    private SKKUserDictionary mUserDict;
    //    private static final Pattern PAT_QUOTED = Pattern.compile("\"(.+?)\"");
    private static final Pattern PAT_ESCAPE_NUM = Pattern.compile("\\\\([0-9]+)");


    public static class Candidate {
        public String rawCandidate;
        public String candidate;
        public String annotation;
        Candidate(String rc, String c, String a) {
            rawCandidate = rc;
            candidate = c;
            annotation = a;
        }
    }

    DictionaryProcessor(List<SKKDictionary> dics, SKKUserDictionary userDic) {
        mDicts = dics;
        mUserDict = userDic;
    }

    void reopenDictionaries(List<SKKDictionary> dics) {
        for (SKKDictionary dic: mDicts) { dic.close(); }
        mDicts = dics;
    }

    public void addEntry(String key, String val, String okuri) {
        mUserDict.addEntry(convertKey(key), val, okuri);
    }

    public void commitChanges() {
        mUserDict.commitChanges();
    }

    public void rollback() {
        mUserDict.rollBack();
    }

    public List<Candidate> findCandidates(String key, String okurigana) {
        key = convertKey(key);
        List<String> list1 = new ArrayList<>();
        for (SKKDictionary dic: mDicts) {
            String[] cands = dic.getCandidates(key);
            if (cands != null) {
                Collections.addAll(list1, cands);
            }
        }

        List<String> list2 = null;

        SKKUserDictionary.Entry entry = mUserDict.getEntry(key);
        if (entry != null) {
            list2 = entry.candidates;
        }

        if (list1.isEmpty() && list2 == null) {
            SKKUtils.dlog("Dictoinary: Can't find Kanji for " + key);
            return null;
        }

        if (list2 != null) {
            int idx = 0;
            for (String s : list2) {
                if (okurigana != null) {
                    boolean found = false;
                    for (List<String> lst : entry.okuri_blocks) {
                        if (lst.get(0).equals(okurigana) && lst.contains(s)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {continue;} //送りがなブロックに見つからなければ，追加しない
                }
                //個人辞書の候補を先頭に追加
                list1.remove(s);
                list1.add(idx, s);
                idx++;
            }
        }
        if (list1.size() == 0) {
            return null;
        }

        List<Candidate> candidates = new ArrayList<>();
        for (String rawCand : list1) {
            String cand;
            String annotation;
            int i = rawCand.lastIndexOf(';'); // セミコロンで解説が始まる
            if (i != -1) {
                cand = unescape(rawCand.substring(0, i));
                annotation = unescape(rawCand.substring(i + 1, rawCand.length()));
            } else {
                cand = unescape(rawCand);
                annotation = null;
            }
            candidates.add(new Candidate(rawCand, cand, annotation));
        }

        return candidates;
    }

    public List<String> findSuggestions(String text) {
        String key = convertKey(text);
        List<String> list = new ArrayList<>();
        for (SKKDictionary dic: mDicts) {
            list.addAll(dic.findKeys(key));
        }
        List<String> list2 = mUserDict.findKeys(key);
        int idx = 0;
        for (String s : list2) {
            //個人辞書のキーを先頭に追加
            list.remove(s);
            list.add(idx, s);
            idx++;
        }
        for (int i = 0; i < list.size(); i++) {
            list.set(i, inverseConvertKey(list.get(i)));
        }
        return list;
    }

    private static String unescape(String text) {
        int len = text.length();
        if (len < 12) {return text;}
        if (text.charAt(0) != '(' || !(text.substring(1, 9).equals("concat \"")) || !(text.substring(len-2, len).equals("\")"))) {return text;}

//        Matcher m = PAT_QUOTED.matcher(text.substring(8, len-1));
//        StringBuilder buf = new StringBuilder();
//        while (m.find()) {
//            buf.append(m.group(1));
//        }
//        m = PAT_ESCAPE_NUM.matcher(buf.toString());

        Matcher m = PAT_ESCAPE_NUM.matcher(text.substring(9, len-2));
        StringBuffer buf2 = new StringBuffer();
        while (m.find()) {
            int num = Integer.parseInt(m.group(1), 8); // emacs-lispのリテラルは8進数
            m.appendReplacement(buf2, Character.toString((char)num));
        }
        m.appendTail(buf2);

        return buf2.toString();
    }

    private static String convertKey(String key) {
        if (key.contains("ゔ")) {
            return key.replace("ゔ", "う゛");
        }
        return key;
    }

    private static String inverseConvertKey(String key) {
        if (key.contains("う゛")) {
            return key.replace("う゛", "ゔ");
        }
        return key;
    }
}
