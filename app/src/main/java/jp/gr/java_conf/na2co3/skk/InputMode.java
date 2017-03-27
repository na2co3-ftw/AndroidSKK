package jp.gr.java_conf.na2co3.skk;

enum InputMode {
	HIRAKANA,  // 平仮名
	KATAKANA,  // 片仮名
	ZENKAKU,   // 全角英字モード
}

// isSKKOnがfalseの場合ASCIIモード．キー入力をそのまま通す
