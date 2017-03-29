package jp.gr.java_conf.na2co3.skk;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class DictionaryManager {
	static class Entry {
		List<String> candidates = null;
		List<List<String>> okuri_blocks = null;

		public Entry(List<String> cd, List<List<String>> okr) {
			candidates = cd;
			okuri_blocks = okr;
		}
	}

	static class Candidate {
		String dictionaryForm = null;
		String annotation = null;
		String value = null;

		public Candidate(String key, String candidate) {
			dictionaryForm = candidate;
			int i = candidate.indexOf(';');
			if (i != -1) {
				annotation = candidate.substring(i + 1);
				candidate = candidate.substring(0, i);
			}
			value = convCandidate(key, candidate);
		}
	}

	private SKKDictionary mMainDict;
	private SKKUserDictionary mUserDict;

	DictionaryManager(SKKDictionary mainDict, SKKUserDictionary userDict) {
		mMainDict = mainDict;
		mUserDict = userDict;
	}

	void setUserDict(SKKUserDictionary userDict) {
		mUserDict = userDict;
	}

	List<Candidate> findKanji(String key, String okuri) {
		SKKUtils.dlog("findKanji(): key = " + key + ", okuri = " + okuri);
		String convertedKey = convSearchKey(key);
		okuri = convSearchOkuri(okuri);

		Entry mainEntry = parseEntry(mMainDict.get(convertedKey));
		Entry userEntry = parseEntry(mUserDict.get(convertedKey));

		if (BuildConfig.DEBUG) {
			SKKUtils.dlog("*** Main dict entry ***");
			if (mainEntry != null) {
				SKKUtils.dlog("  " + mainEntry.candidates.toString());
			}
			SKKUtils.dlog("*** User dict entry ***");
			if (userEntry != null) {
				SKKUtils.dlog("  *** Candidates ***");
				SKKUtils.dlog("  " + userEntry.candidates.toString());
				SKKUtils.dlog("  *** Okuri blocks ***");
				for (List<String> lst : userEntry.okuri_blocks) {
					SKKUtils.dlog("  " + lst.toString());
				}
			}

			if (mainEntry == null && userEntry == null) {
				SKKUtils.dlog("Dictoinary: Can't find Kanji for " + convertedKey);
				return null;
			}
		}

		List<String> list;
		if (mainEntry != null){
			list = mainEntry.candidates;
		} else {
			list = new ArrayList<String>();
		}

		if (userEntry != null) {
			int idx = 0;
			for (String s : userEntry.candidates) {
				if (okuri != null) {
					boolean found = false;
					for (List<String> lst : userEntry.okuri_blocks) {
						if (lst.get(0).equals(okuri) && lst.contains(s)) {
							found = true;
							break;
						}
					}
					if (!found) {continue;} //送りがなブロックに見つからなければ，追加しない
				}
				//個人辞書の候補を先頭に追加
				list.remove(s);
				list.add(idx, s);
				idx++;
			}
		}

		if (list.size() == 0) {
			return null;
		}

		List<Candidate> candidates = new ArrayList<Candidate>();
		for (String cand : list) {
			candidates.add(new Candidate(key, cand));
		}
		return candidates;
	}

	void addEntry(String key, String val, String okuri) {
		key = convSearchKey(key);
		okuri = convSearchOkuri(okuri);

		StringBuilder new_val = new StringBuilder();
		Entry entry = parseEntry(mUserDict.get(key));

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
					List<String> new_okr = new ArrayList<String>();
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
		}

		mUserDict.put(key, new_val.toString());
	}

	static Entry parseEntry(String str) {
		if (str == null) {
			return null;
		}

		List<String> cd = new ArrayList<String>();
		List<List<String>> okr = new ArrayList<List<String>>();
		String[] va_array;

		va_array = str.split("/");
		SKKUtils.dlog("length = " + va_array.length);

		if (va_array.length <= 0) {
			Log.e("SKK", "Invalid value found: " + str);
			return null;
		}

		// va_array[0]は常に空文字列なので1から始める
		for (int i=1; i<va_array.length; i++) {
			// 送りがなブロックが見つかったら中止
			if (va_array[i].startsWith("[")) break;
			cd.add(va_array[i]);
		}

		if (str.contains("[") && str.contains("]")) {
			// 送りがなブロック
			va_array = str.split("[\\[\\]]");
			List<String> tmp_okr = null;
			String[] va_array2 = null;
			// va_array[0]は変換候補なので1から始める
			for (int i=1; i<va_array.length; i++) {
				if (va_array[i].equals("/")) {continue;}
				va_array2 = va_array[i].split("/");
				tmp_okr = new ArrayList<String>();
				for (int j=0; j<va_array2.length; j++) {
					tmp_okr.add(va_array2[j]);
				}
				okr.add(tmp_okr);
			}
		}

		return new Entry(cd, okr);
	}

	List<String> findKeys(String key) {
		key = convSearchKey(key);

		List<String> list = mMainDict.findKeys(key);
		List<String> list2 = mUserDict.findKeys(key);
		int idx = 0;
		for (String s : list2) {
			//個人辞書のキーを先頭に追加
			list.remove(s);
			list.add(idx, s);
			idx++;
		}

		return mUserDict.findKeys(key);
	}

	static String convSearchKey(String key) {
		key = SKKUtils.katakana2hirakana(key);
		if (key.contains("ゔ")) {
			key = key.replace("ゔ", "う゛");
		}

		key = key.replaceAll("\\d+", "#");

		return key;
	}

	static String convSearchOkuri(String okuri) {
		if (okuri == null) {
			return null;
		}
		okuri = SKKUtils.katakana2hirakana(okuri);
		if (okuri.contains("ゔ")) {
			okuri = okuri.replace("ゔ", "う゛");
		}
		return okuri;
	}

	private static final char[] NUM_KANJI = {'〇', '一', '二', '三', '四', '五', '六', '七', '八', '九'};
	private static final String[] NUM_KANJI_UNIT = {"", "十", "百", "千"};
	private static final String[] NUM_KANJI_UNIT2 = {"", "万", "億", "兆", "京", "垓", "𥝱", "穣", "溝", "澗", "正", "載", "極",
			"恒河沙", "阿僧祇", "那由他", "不可思議", "無量大数"};

	private static final char[] NUM_DAIJI = {'零', '壱', '弐', '参', '四', '伍', '六', '七', '八', '九'};
	private static final String[] NUM_DAIJI_UNIT = {"", "拾", "百", "阡"};
	private static final String[] NUM_DAIJI_UNIT2 = {"", "萬", "億", "兆", "京", "垓", "𥝱", "穣", "溝", "澗", "正", "載", "極",
			"恒河沙", "阿僧祇", "那由他", "不可思議", "無量大数"};

	static String convCandidate(String key, String candidate) {
		Matcher keyMatcher = Pattern.compile("\\d+").matcher(key);
		Matcher candMatcher = Pattern.compile("#(\\d)").matcher(candidate);

		StringBuffer ret = new StringBuffer ();
		while (keyMatcher.find() && candMatcher.find()) {
			String num = keyMatcher.group();
			String type = candMatcher.group(1);
			candMatcher.appendReplacement(ret, convNum(num, type));
		}
		candMatcher.appendTail(ret);

		return ret.toString();
	}

	static private String convNum(String num, String type) {
		StringBuilder ret;
		switch (type.charAt(0)) {
			case '0':
				return num;

			case '1':
				ret = new StringBuilder();
				for (int i = 0; i < num.length(); i ++) {
					ret.append((char)SKKUtils.hankaku2zenkaku(num.charAt(i)));
				}
				return ret.toString();

			case '2':
				ret = new StringBuilder();
				for (int i = 0; i < num.length(); i ++) {
					ret.append(NUM_KANJI[num.charAt(i) - '0']);
				}
				return ret.toString();

			case '3':
				return convKanjiNum(num, NUM_KANJI, NUM_KANJI_UNIT, NUM_KANJI_UNIT2);

			case '5':
				return convKanjiNum(num, NUM_DAIJI, NUM_DAIJI_UNIT, NUM_DAIJI_UNIT2);

			case '8': {
				int i = num.length() % 3;
				if (i == 0 && num.length() != 0) {
					i = 3;
				}
				ret = new StringBuilder();
				ret.append(num.substring(0, i));
				for (; i < num.length(); i += 3) {
					ret.append(",");
					ret.append(num.substring(i, i + 3));
				}
				return ret.toString();
			}

			case '9':
				if (num.length() != 2) {
					return num;
				}
				ret = new StringBuilder();
				ret.append((char)SKKUtils.hankaku2zenkaku(num.charAt(0)));
				ret.append(NUM_KANJI[num.charAt(1) - '0']);
				return ret.toString();

			default:
				return "#" + type;
		}
	}

	static private String convKanjiNum(String num, char[] kanji, String[] unit, String[] unit2) {
		StringBuilder ret = new StringBuilder();
		int len = num.length();
		int pos = len - 1;

		try {
			for (int i = 0; i < len; i++) {
				int digit = num.charAt(i) - '0';
				if (digit == 0) continue;
				if (pos % 4 == 0 || digit != 1) {
					ret.append(kanji[digit]);
				}
				ret.append(unit[pos % 4]);
				if (pos % 4 == 0) {
					ret.append(unit2[pos / 4]);
				}
				pos--;
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			return num;
		}

		return ret.toString();
	}

	void rollBack() {
		mUserDict.rollBack();
	}

	void commitChanges() {
		mUserDict.commitChanges();
	}
}
