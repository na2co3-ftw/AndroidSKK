package jp.gr.java_conf.na2co3.skk;

enum InputState {
	NORMAL,    // 通常状態
	KANJI,     // 漢字変換候補(ひらかな)入力中
	CHOOSE,    // 漢字選択中
	ENG2JP,    // 英日変換用英単語入力モード
	OKURIGANA, // 送り仮名入力中
}

// ちょっとわかりづらいが、漢字モード(mInputMode == KANJI)とは漢字変換
// するためのひらがなを入力するモード

// 漢字にするひらがなが決定したときにモードは漢字選択モードのCHOOSEになる
