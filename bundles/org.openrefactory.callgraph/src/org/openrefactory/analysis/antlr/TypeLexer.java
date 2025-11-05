// Generated from Type.g4 by ANTLR 4.9.1
/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.analysis.antlr;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class TypeLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.9.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, SCALAR=15, NullLiteral=16, 
		LPAREN=17, RPAREN=18, LBRACE=19, RBRACE=20, LBRACK=21, RBRACK=22, SEMI=23, 
		COMMA=24, DOT=25, Identifier=26, UpperCase=27, AT=28, ELLIPSIS=29, WS=30, 
		COMMENT=31, LINE_COMMENT=32;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "T__6", "T__7", "T__8", 
			"T__9", "T__10", "T__11", "T__12", "T__13", "SCALAR", "NullLiteral", 
			"LPAREN", "RPAREN", "LBRACE", "RBRACE", "LBRACK", "RBRACK", "SEMI", "COMMA", 
			"DOT", "Identifier", "UpperCase", "IdentifierStart", "IdentifierPart", 
			"AT", "ELLIPSIS", "WS", "COMMENT", "LINE_COMMENT"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'boolean'", "'byte'", "'short'", "'int'", "'long'", "'char'", 
			"'float'", "'double'", "'extends'", "'&'", "'<'", "'>'", "'?'", "'super'", 
			null, "'null'", "'('", "')'", "'{'", "'}'", "'['", "']'", "';'", "','", 
			"'.'", null, null, "'@'", "'...'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, "SCALAR", "NullLiteral", "LPAREN", "RPAREN", "LBRACE", 
			"RBRACE", "LBRACK", "RBRACK", "SEMI", "COMMA", "DOT", "Identifier", "UpperCase", 
			"AT", "ELLIPSIS", "WS", "COMMENT", "LINE_COMMENT"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}


	public TypeLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "Type.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\"\u0112\b\1\4\2\t"+
		"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\3\3\3\3\3\3\3\3\3"+
		"\3\4\3\4\3\4\3\4\3\4\3\4\3\5\3\5\3\5\3\5\3\6\3\6\3\6\3\6\3\6\3\7\3\7\3"+
		"\7\3\7\3\7\3\b\3\b\3\b\3\b\3\b\3\b\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\n\3\n"+
		"\3\n\3\n\3\n\3\n\3\n\3\n\3\13\3\13\3\f\3\f\3\r\3\r\3\16\3\16\3\17\3\17"+
		"\3\17\3\17\3\17\3\17\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20"+
		"\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20"+
		"\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20"+
		"\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20"+
		"\3\20\3\20\3\20\3\20\3\20\5\20\u00c5\n\20\3\21\3\21\3\21\3\21\3\21\3\22"+
		"\3\22\3\23\3\23\3\24\3\24\3\25\3\25\3\26\3\26\3\27\3\27\3\30\3\30\3\31"+
		"\3\31\3\32\3\32\3\33\3\33\6\33\u00e0\n\33\r\33\16\33\u00e1\3\34\3\34\3"+
		"\35\5\35\u00e7\n\35\3\36\3\36\5\36\u00eb\n\36\3\37\3\37\3 \3 \3 \3 \3"+
		"!\6!\u00f4\n!\r!\16!\u00f5\3!\3!\3\"\3\"\3\"\3\"\7\"\u00fe\n\"\f\"\16"+
		"\"\u0101\13\"\3\"\3\"\3\"\3\"\3\"\3#\3#\3#\3#\7#\u010c\n#\f#\16#\u010f"+
		"\13#\3#\3#\3\u00ff\2$\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27"+
		"\r\31\16\33\17\35\20\37\21!\22#\23%\24\'\25)\26+\27-\30/\31\61\32\63\33"+
		"\65\34\67\359\2;\2=\36?\37A C!E\"\3\2\7\3\2C\\\u0194\2&&C\\aac|\u00a4"+
		"\u00a7\u00ac\u00ac\u00b7\u00b7\u00bc\u00bc\u00c2\u00d8\u00da\u00f8\u00fa"+
		"\u02c3\u02c8\u02d3\u02e2\u02e6\u02ee\u02ee\u02f0\u02f0\u0372\u0376\u0378"+
		"\u0379\u037c\u037f\u0381\u0381\u0388\u0388\u038a\u038c\u038e\u038e\u0390"+
		"\u03a3\u03a5\u03f7\u03f9\u0483\u048c\u0531\u0533\u0558\u055b\u055b\u0563"+
		"\u0589\u0591\u0591\u05d2\u05ec\u05f2\u05f4\u060d\u060d\u0622\u064c\u0670"+
		"\u0671\u0673\u06d5\u06d7\u06d7\u06e7\u06e8\u06f0\u06f1\u06fc\u06fe\u0701"+
		"\u0701\u0712\u0712\u0714\u0731\u074f\u07a7\u07b3\u07b3\u07cc\u07ec\u07f6"+
		"\u07f7\u07fc\u07fc\u0802\u0817\u081c\u081c\u0826\u0826\u082a\u082a\u0842"+
		"\u085a\u0862\u086c\u08a2\u08b6\u08b8\u08bf\u0906\u093b\u093f\u093f\u0952"+
		"\u0952\u095a\u0963\u0973\u0982\u0987\u098e\u0991\u0992\u0995\u09aa\u09ac"+
		"\u09b2\u09b4\u09b4\u09b8\u09bb\u09bf\u09bf\u09d0\u09d0\u09de\u09df\u09e1"+
		"\u09e3\u09f2\u09f5\u09fd\u09fe\u0a07\u0a0c\u0a11\u0a12\u0a15\u0a2a\u0a2c"+
		"\u0a32\u0a34\u0a35\u0a37\u0a38\u0a3a\u0a3b\u0a5b\u0a5e\u0a60\u0a60\u0a74"+
		"\u0a76\u0a87\u0a8f\u0a91\u0a93\u0a95\u0aaa\u0aac\u0ab2\u0ab4\u0ab5\u0ab7"+
		"\u0abb\u0abf\u0abf\u0ad2\u0ad2\u0ae2\u0ae3\u0af3\u0af3\u0afb\u0afb\u0b07"+
		"\u0b0e\u0b11\u0b12\u0b15\u0b2a\u0b2c\u0b32\u0b34\u0b35\u0b37\u0b3b\u0b3f"+
		"\u0b3f\u0b5e\u0b5f\u0b61\u0b63\u0b73\u0b73\u0b85\u0b85\u0b87\u0b8c\u0b90"+
		"\u0b92\u0b94\u0b97\u0b9b\u0b9c\u0b9e\u0b9e\u0ba0\u0ba1\u0ba5\u0ba6\u0baa"+
		"\u0bac\u0bb0\u0bbb\u0bd2\u0bd2\u0bfb\u0bfb\u0c07\u0c0e\u0c10\u0c12\u0c14"+
		"\u0c2a\u0c2c\u0c3b\u0c3f\u0c3f\u0c5a\u0c5c\u0c62\u0c63\u0c82\u0c82\u0c87"+
		"\u0c8e\u0c90\u0c92\u0c94\u0caa\u0cac\u0cb5\u0cb7\u0cbb\u0cbf\u0cbf\u0ce0"+
		"\u0ce0\u0ce2\u0ce3\u0cf3\u0cf4\u0d07\u0d0e\u0d10\u0d12\u0d14\u0d3c\u0d3f"+
		"\u0d3f\u0d50\u0d50\u0d56\u0d58\u0d61\u0d63\u0d7c\u0d81\u0d87\u0d98\u0d9c"+
		"\u0db3\u0db5\u0dbd\u0dbf\u0dbf\u0dc2\u0dc8\u0e03\u0e32\u0e34\u0e35\u0e41"+
		"\u0e48\u0e83\u0e84\u0e86\u0e86\u0e89\u0e8a\u0e8c\u0e8c\u0e8f\u0e8f\u0e96"+
		"\u0e99\u0e9b\u0ea1\u0ea3\u0ea5\u0ea7\u0ea7\u0ea9\u0ea9\u0eac\u0ead\u0eaf"+
		"\u0eb2\u0eb4\u0eb5\u0ebf\u0ebf\u0ec2\u0ec6\u0ec8\u0ec8\u0ede\u0ee1\u0f02"+
		"\u0f02\u0f42\u0f49\u0f4b\u0f6e\u0f8a\u0f8e\u1002\u102c\u1041\u1041\u1052"+
		"\u1057\u105c\u105f\u1063\u1063\u1067\u1068\u1070\u1072\u1077\u1083\u1090"+
		"\u1090\u10a2\u10c7\u10c9\u10c9\u10cf\u10cf\u10d2\u10fc\u10fe\u124a\u124c"+
		"\u124f\u1252\u1258\u125a\u125a\u125c\u125f\u1262\u128a\u128c\u128f\u1292"+
		"\u12b2\u12b4\u12b7\u12ba\u12c0\u12c2\u12c2\u12c4\u12c7\u12ca\u12d8\u12da"+
		"\u1312\u1314\u1317\u131a\u135c\u1382\u1391\u13a2\u13f7\u13fa\u13ff\u1403"+
		"\u166e\u1671\u1681\u1683\u169c\u16a2\u16ec\u16f0\u16fa\u1702\u170e\u1710"+
		"\u1713\u1722\u1733\u1742\u1753\u1762\u176e\u1770\u1772\u1782\u17b5\u17d9"+
		"\u17d9\u17dd\u17de\u1822\u1879\u1882\u1886\u1889\u18aa\u18ac\u18ac\u18b2"+
		"\u18f7\u1902\u1920\u1952\u196f\u1972\u1976\u1982\u19ad\u19b2\u19cb\u1a02"+
		"\u1a18\u1a22\u1a56\u1aa9\u1aa9\u1b07\u1b35\u1b47\u1b4d\u1b85\u1ba2\u1bb0"+
		"\u1bb1\u1bbc\u1be7\u1c02\u1c25\u1c4f\u1c51\u1c5c\u1c7f\u1c82\u1c8a\u1ceb"+
		"\u1cee\u1cf0\u1cf3\u1cf7\u1cf8\u1d02\u1dc1\u1e02\u1f17\u1f1a\u1f1f\u1f22"+
		"\u1f47\u1f4a\u1f4f\u1f52\u1f59\u1f5b\u1f5b\u1f5d\u1f5d\u1f5f\u1f5f\u1f61"+
		"\u1f7f\u1f82\u1fb6\u1fb8\u1fbe\u1fc0\u1fc0\u1fc4\u1fc6\u1fc8\u1fce\u1fd2"+
		"\u1fd5\u1fd8\u1fdd\u1fe2\u1fee\u1ff4\u1ff6\u1ff8\u1ffe\u2041\u2042\u2056"+
		"\u2056\u2073\u2073\u2081\u2081\u2092\u209e\u20a2\u20c1\u2104\u2104\u2109"+
		"\u2109\u210c\u2115\u2117\u2117\u211b\u211f\u2126\u2126\u2128\u2128\u212a"+
		"\u212a\u212c\u212f\u2131\u213b\u213e\u2141\u2147\u214b\u2150\u2150\u2162"+
		"\u218a\u2c02\u2c30\u2c32\u2c60\u2c62\u2ce6\u2ced\u2cf0\u2cf4\u2cf5\u2d02"+
		"\u2d27\u2d29\u2d29\u2d2f\u2d2f\u2d32\u2d69\u2d71\u2d71\u2d82\u2d98\u2da2"+
		"\u2da8\u2daa\u2db0\u2db2\u2db8\u2dba\u2dc0\u2dc2\u2dc8\u2dca\u2dd0\u2dd2"+
		"\u2dd8\u2dda\u2de0\u2e31\u2e31\u3007\u3009\u3023\u302b\u3033\u3037\u303a"+
		"\u303e\u3043\u3098\u309f\u30a1\u30a3\u30fc\u30fe\u3101\u3107\u3130\u3133"+
		"\u3190\u31a2\u31bc\u31f2\u3201\u3402\u4db7\u4e02\u9fec\ua002\ua48e\ua4d2"+
		"\ua4ff\ua502\ua60e\ua612\ua621\ua62c\ua62d\ua642\ua670\ua681\ua69f\ua6a2"+
		"\ua6f1\ua719\ua721\ua724\ua78a\ua78d\ua7b0\ua7b2\ua7b9\ua7f9\ua803\ua805"+
		"\ua807\ua809\ua80c\ua80e\ua824\ua83a\ua83a\ua842\ua875\ua884\ua8b5\ua8f4"+
		"\ua8f9\ua8fd\ua8fd\ua8ff\ua8ff\ua90c\ua927\ua932\ua948\ua962\ua97e\ua986"+
		"\ua9b4\ua9d1\ua9d1\ua9e2\ua9e6\ua9e8\ua9f1\ua9fc\uaa00\uaa02\uaa2a\uaa42"+
		"\uaa44\uaa46\uaa4d\uaa62\uaa78\uaa7c\uaa7c\uaa80\uaab1\uaab3\uaab3\uaab7"+
		"\uaab8\uaabb\uaabf\uaac2\uaac2\uaac4\uaac4\uaadd\uaadf\uaae2\uaaec\uaaf4"+
		"\uaaf6\uab03\uab08\uab0b\uab10\uab13\uab18\uab22\uab28\uab2a\uab30\uab32"+
		"\uab5c\uab5e\uab67\uab72\uabe4\uac02\ud7a5\ud7b2\ud7c8\ud7cd\ud7fd\uf902"+
		"\ufa6f\ufa72\ufadb\ufb02\ufb08\ufb15\ufb19\ufb1f\ufb1f\ufb21\ufb2a\ufb2c"+
		"\ufb38\ufb3a\ufb3e\ufb40\ufb40\ufb42\ufb43\ufb45\ufb46\ufb48\ufbb3\ufbd5"+
		"\ufd3f\ufd52\ufd91\ufd94\ufdc9\ufdf2\ufdfe\ufe35\ufe36\ufe4f\ufe51\ufe6b"+
		"\ufe6b\ufe72\ufe76\ufe78\ufefe\uff06\uff06\uff23\uff3c\uff41\uff41\uff43"+
		"\uff5c\uff68\uffc0\uffc4\uffc9\uffcc\uffd1\uffd4\uffd9\uffdc\uffde\uffe2"+
		"\uffe3\uffe7\uffe8\u00e6\2\62;\u0081\u00a1\u00af\u00af\u0302\u0371\u0485"+
		"\u0489\u0593\u05bf\u05c1\u05c1\u05c3\u05c4\u05c6\u05c7\u05c9\u05c9\u0602"+
		"\u0607\u0612\u061c\u061e\u061e\u064d\u066b\u0672\u0672\u06d8\u06df\u06e1"+
		"\u06e6\u06e9\u06ea\u06ec\u06ef\u06f2\u06fb\u0711\u0711\u0713\u0713\u0732"+
		"\u074c\u07a8\u07b2\u07c2\u07cb\u07ed\u07f5\u0818\u081b\u081d\u0825\u0827"+
		"\u0829\u082b\u082f\u085b\u085d\u08d6\u0905\u093c\u093e\u0940\u0951\u0953"+
		"\u0959\u0964\u0965\u0968\u0971\u0983\u0985\u09be\u09be\u09c0\u09c6\u09c9"+
		"\u09ca\u09cd\u09cf\u09d9\u09d9\u09e4\u09e5\u09e8\u09f1\u0a03\u0a05\u0a3e"+
		"\u0a3e\u0a40\u0a44\u0a49\u0a4a\u0a4d\u0a4f\u0a53\u0a53\u0a68\u0a73\u0a77"+
		"\u0a77\u0a83\u0a85\u0abe\u0abe\u0ac0\u0ac7\u0ac9\u0acb\u0acd\u0acf\u0ae4"+
		"\u0ae5\u0ae8\u0af1\u0afc\u0b01\u0b03\u0b05\u0b3e\u0b3e\u0b40\u0b46\u0b49"+
		"\u0b4a\u0b4d\u0b4f\u0b58\u0b59\u0b64\u0b65\u0b68\u0b71\u0b84\u0b84\u0bc0"+
		"\u0bc4\u0bc8\u0bca\u0bcc\u0bcf\u0bd9\u0bd9\u0be8\u0bf1\u0c02\u0c05\u0c40"+
		"\u0c46\u0c48\u0c4a\u0c4c\u0c4f\u0c57\u0c58\u0c64\u0c65\u0c68\u0c71\u0c83"+
		"\u0c85\u0cbe\u0cbe\u0cc0\u0cc6\u0cc8\u0cca\u0ccc\u0ccf\u0cd7\u0cd8\u0ce4"+
		"\u0ce5\u0ce8\u0cf1\u0d02\u0d05\u0d3d\u0d3e\u0d40\u0d46\u0d48\u0d4a\u0d4c"+
		"\u0d4f\u0d59\u0d59\u0d64\u0d65\u0d68\u0d71\u0d84\u0d85\u0dcc\u0dcc\u0dd1"+
		"\u0dd6\u0dd8\u0dd8\u0dda\u0de1\u0de8\u0df1\u0df4\u0df5\u0e33\u0e33\u0e36"+
		"\u0e3c\u0e49\u0e50\u0e52\u0e5b\u0eb3\u0eb3\u0eb6\u0ebb\u0ebd\u0ebe\u0eca"+
		"\u0ecf\u0ed2\u0edb\u0f1a\u0f1b\u0f22\u0f2b\u0f37\u0f37\u0f39\u0f39\u0f3b"+
		"\u0f3b\u0f40\u0f41\u0f73\u0f86\u0f88\u0f89\u0f8f\u0f99\u0f9b\u0fbe\u0fc8"+
		"\u0fc8\u102d\u1040\u1042\u104b\u1058\u105b\u1060\u1062\u1064\u1066\u1069"+
		"\u106f\u1073\u1076\u1084\u108f\u1091\u109f\u135f\u1361\u1714\u1716\u1734"+
		"\u1736\u1754\u1755\u1774\u1775\u17b6\u17d5\u17df\u17df\u17e2\u17eb\u180d"+
		"\u1810\u1812\u181b\u1887\u1888\u18ab\u18ab\u1922\u192d\u1932\u193d\u1948"+
		"\u1951\u19d2\u19db\u1a19\u1a1d\u1a57\u1a60\u1a62\u1a7e\u1a81\u1a8b\u1a92"+
		"\u1a9b\u1ab2\u1abf\u1b02\u1b06\u1b36\u1b46\u1b52\u1b5b\u1b6d\u1b75\u1b82"+
		"\u1b84\u1ba3\u1baf\u1bb2\u1bbb\u1be8\u1bf5\u1c26\u1c39\u1c42\u1c4b\u1c52"+
		"\u1c5b\u1cd2\u1cd4\u1cd6\u1cea\u1cef\u1cef\u1cf4\u1cf6\u1cf9\u1cfb\u1dc2"+
		"\u1dfb\u1dfd\u1e01\u200d\u2011\u202c\u2030\u2062\u2066\u2068\u2071\u20d2"+
		"\u20de\u20e3\u20e3\u20e7\u20f2\u2cf1\u2cf3\u2d81\u2d81\u2de2\u2e01\u302c"+
		"\u3031\u309b\u309c\ua622\ua62b\ua671\ua671\ua676\ua67f\ua6a0\ua6a1\ua6f2"+
		"\ua6f3\ua804\ua804\ua808\ua808\ua80d\ua80d\ua825\ua829\ua882\ua883\ua8b6"+
		"\ua8c7\ua8d2\ua8db\ua8e2\ua8f3\ua902\ua90b\ua928\ua92f\ua949\ua955\ua982"+
		"\ua985\ua9b5\ua9c2\ua9d2\ua9db\ua9e7\ua9e7\ua9f2\ua9fb\uaa2b\uaa38\uaa45"+
		"\uaa45\uaa4e\uaa4f\uaa52\uaa5b\uaa7d\uaa7f\uaab2\uaab2\uaab4\uaab6\uaab9"+
		"\uaaba\uaac0\uaac1\uaac3\uaac3\uaaed\uaaf1\uaaf7\uaaf8\uabe5\uabec\uabee"+
		"\uabef\uabf2\uabfb\ufb20\ufb20\ufe02\ufe11\ufe22\ufe31\uff01\uff01\uff12"+
		"\uff1b\ufffb\ufffd\5\2\13\f\16\17\"\"\4\2\f\f\17\17\2\u011d\2\3\3\2\2"+
		"\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3"+
		"\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2"+
		"\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2"+
		"\2\2\2\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2"+
		"\2\2\2\63\3\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2\2=\3\2\2\2\2?\3\2\2\2\2A\3"+
		"\2\2\2\2C\3\2\2\2\2E\3\2\2\2\3G\3\2\2\2\5O\3\2\2\2\7T\3\2\2\2\tZ\3\2\2"+
		"\2\13^\3\2\2\2\rc\3\2\2\2\17h\3\2\2\2\21n\3\2\2\2\23u\3\2\2\2\25}\3\2"+
		"\2\2\27\177\3\2\2\2\31\u0081\3\2\2\2\33\u0083\3\2\2\2\35\u0085\3\2\2\2"+
		"\37\u00c4\3\2\2\2!\u00c6\3\2\2\2#\u00cb\3\2\2\2%\u00cd\3\2\2\2\'\u00cf"+
		"\3\2\2\2)\u00d1\3\2\2\2+\u00d3\3\2\2\2-\u00d5\3\2\2\2/\u00d7\3\2\2\2\61"+
		"\u00d9\3\2\2\2\63\u00db\3\2\2\2\65\u00dd\3\2\2\2\67\u00e3\3\2\2\29\u00e6"+
		"\3\2\2\2;\u00ea\3\2\2\2=\u00ec\3\2\2\2?\u00ee\3\2\2\2A\u00f3\3\2\2\2C"+
		"\u00f9\3\2\2\2E\u0107\3\2\2\2GH\7d\2\2HI\7q\2\2IJ\7q\2\2JK\7n\2\2KL\7"+
		"g\2\2LM\7c\2\2MN\7p\2\2N\4\3\2\2\2OP\7d\2\2PQ\7{\2\2QR\7v\2\2RS\7g\2\2"+
		"S\6\3\2\2\2TU\7u\2\2UV\7j\2\2VW\7q\2\2WX\7t\2\2XY\7v\2\2Y\b\3\2\2\2Z["+
		"\7k\2\2[\\\7p\2\2\\]\7v\2\2]\n\3\2\2\2^_\7n\2\2_`\7q\2\2`a\7p\2\2ab\7"+
		"i\2\2b\f\3\2\2\2cd\7e\2\2de\7j\2\2ef\7c\2\2fg\7t\2\2g\16\3\2\2\2hi\7h"+
		"\2\2ij\7n\2\2jk\7q\2\2kl\7c\2\2lm\7v\2\2m\20\3\2\2\2no\7f\2\2op\7q\2\2"+
		"pq\7w\2\2qr\7d\2\2rs\7n\2\2st\7g\2\2t\22\3\2\2\2uv\7g\2\2vw\7z\2\2wx\7"+
		"v\2\2xy\7g\2\2yz\7p\2\2z{\7f\2\2{|\7u\2\2|\24\3\2\2\2}~\7(\2\2~\26\3\2"+
		"\2\2\177\u0080\7>\2\2\u0080\30\3\2\2\2\u0081\u0082\7@\2\2\u0082\32\3\2"+
		"\2\2\u0083\u0084\7A\2\2\u0084\34\3\2\2\2\u0085\u0086\7u\2\2\u0086\u0087"+
		"\7w\2\2\u0087\u0088\7r\2\2\u0088\u0089\7g\2\2\u0089\u008a\7t\2\2\u008a"+
		"\36\3\2\2\2\u008b\u008c\7E\2\2\u008c\u008d\7j\2\2\u008d\u008e\7c\2\2\u008e"+
		"\u008f\7t\2\2\u008f\u0090\7c\2\2\u0090\u0091\7e\2\2\u0091\u0092\7v\2\2"+
		"\u0092\u0093\7g\2\2\u0093\u00c5\7t\2\2\u0094\u0095\7K\2\2\u0095\u0096"+
		"\7p\2\2\u0096\u0097\7v\2\2\u0097\u0098\7g\2\2\u0098\u0099\7i\2\2\u0099"+
		"\u009a\7g\2\2\u009a\u00c5\7t\2\2\u009b\u009c\7F\2\2\u009c\u009d\7q\2\2"+
		"\u009d\u009e\7w\2\2\u009e\u009f\7d\2\2\u009f\u00a0\7n\2\2\u00a0\u00c5"+
		"\7g\2\2\u00a1\u00a2\7H\2\2\u00a2\u00a3\7n\2\2\u00a3\u00a4\7q\2\2\u00a4"+
		"\u00a5\7c\2\2\u00a5\u00c5\7v\2\2\u00a6\u00a7\7N\2\2\u00a7\u00a8\7q\2\2"+
		"\u00a8\u00a9\7p\2\2\u00a9\u00c5\7i\2\2\u00aa\u00ab\7U\2\2\u00ab\u00ac"+
		"\7j\2\2\u00ac\u00ad\7q\2\2\u00ad\u00ae\7t\2\2\u00ae\u00c5\7v\2\2\u00af"+
		"\u00b0\7D\2\2\u00b0\u00b1\7q\2\2\u00b1\u00b2\7q\2\2\u00b2\u00b3\7n\2\2"+
		"\u00b3\u00b4\7g\2\2\u00b4\u00b5\7c\2\2\u00b5\u00c5\7p\2\2\u00b6\u00b7"+
		"\7U\2\2\u00b7\u00b8\7v\2\2\u00b8\u00b9\7t\2\2\u00b9\u00ba\7k\2\2\u00ba"+
		"\u00bb\7p\2\2\u00bb\u00c5\7i\2\2\u00bc\u00bd\7X\2\2\u00bd\u00be\7q\2\2"+
		"\u00be\u00bf\7k\2\2\u00bf\u00c5\7f\2\2\u00c0\u00c1\7x\2\2\u00c1\u00c2"+
		"\7q\2\2\u00c2\u00c3\7k\2\2\u00c3\u00c5\7f\2\2\u00c4\u008b\3\2\2\2\u00c4"+
		"\u0094\3\2\2\2\u00c4\u009b\3\2\2\2\u00c4\u00a1\3\2\2\2\u00c4\u00a6\3\2"+
		"\2\2\u00c4\u00aa\3\2\2\2\u00c4\u00af\3\2\2\2\u00c4\u00b6\3\2\2\2\u00c4"+
		"\u00bc\3\2\2\2\u00c4\u00c0\3\2\2\2\u00c5 \3\2\2\2\u00c6\u00c7\7p\2\2\u00c7"+
		"\u00c8\7w\2\2\u00c8\u00c9\7n\2\2\u00c9\u00ca\7n\2\2\u00ca\"\3\2\2\2\u00cb"+
		"\u00cc\7*\2\2\u00cc$\3\2\2\2\u00cd\u00ce\7+\2\2\u00ce&\3\2\2\2\u00cf\u00d0"+
		"\7}\2\2\u00d0(\3\2\2\2\u00d1\u00d2\7\177\2\2\u00d2*\3\2\2\2\u00d3\u00d4"+
		"\7]\2\2\u00d4,\3\2\2\2\u00d5\u00d6\7_\2\2\u00d6.\3\2\2\2\u00d7\u00d8\7"+
		"=\2\2\u00d8\60\3\2\2\2\u00d9\u00da\7.\2\2\u00da\62\3\2\2\2\u00db\u00dc"+
		"\7\60\2\2\u00dc\64\3\2\2\2\u00dd\u00df\59\35\2\u00de\u00e0\5;\36\2\u00df"+
		"\u00de\3\2\2\2\u00e0\u00e1\3\2\2\2\u00e1\u00df\3\2\2\2\u00e1\u00e2\3\2"+
		"\2\2\u00e2\66\3\2\2\2\u00e3\u00e4\t\2\2\2\u00e48\3\2\2\2\u00e5\u00e7\t"+
		"\3\2\2\u00e6\u00e5\3\2\2\2\u00e7:\3\2\2\2\u00e8\u00eb\59\35\2\u00e9\u00eb"+
		"\t\4\2\2\u00ea\u00e8\3\2\2\2\u00ea\u00e9\3\2\2\2\u00eb<\3\2\2\2\u00ec"+
		"\u00ed\7B\2\2\u00ed>\3\2\2\2\u00ee\u00ef\7\60\2\2\u00ef\u00f0\7\60\2\2"+
		"\u00f0\u00f1\7\60\2\2\u00f1@\3\2\2\2\u00f2\u00f4\t\5\2\2\u00f3\u00f2\3"+
		"\2\2\2\u00f4\u00f5\3\2\2\2\u00f5\u00f3\3\2\2\2\u00f5\u00f6\3\2\2\2\u00f6"+
		"\u00f7\3\2\2\2\u00f7\u00f8\b!\2\2\u00f8B\3\2\2\2\u00f9\u00fa\7\61\2\2"+
		"\u00fa\u00fb\7,\2\2\u00fb\u00ff\3\2\2\2\u00fc\u00fe\13\2\2\2\u00fd\u00fc"+
		"\3\2\2\2\u00fe\u0101\3\2\2\2\u00ff\u0100\3\2\2\2\u00ff\u00fd\3\2\2\2\u0100"+
		"\u0102\3\2\2\2\u0101\u00ff\3\2\2\2\u0102\u0103\7,\2\2\u0103\u0104\7\61"+
		"\2\2\u0104\u0105\3\2\2\2\u0105\u0106\b\"\2\2\u0106D\3\2\2\2\u0107\u0108"+
		"\7\61\2\2\u0108\u0109\7\61\2\2\u0109\u010d\3\2\2\2\u010a\u010c\n\6\2\2"+
		"\u010b\u010a\3\2\2\2\u010c\u010f\3\2\2\2\u010d\u010b\3\2\2\2\u010d\u010e"+
		"\3\2\2\2\u010e\u0110\3\2\2\2\u010f\u010d\3\2\2\2\u0110\u0111\b#\2\2\u0111"+
		"F\3\2\2\2\n\2\u00c4\u00e1\u00e6\u00ea\u00f5\u00ff\u010d\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}