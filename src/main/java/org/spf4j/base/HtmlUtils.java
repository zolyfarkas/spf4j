
package org.spf4j.base;

/*
 * Copyright 2002-2004 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 


import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for HTML escaping. Escapes and unescapes
 * based on the W3C HTML 4.01 recommendation.
 *
 * <p>Reference:
 * <a href="http://www.w3.org/TR/html4/charset.html">
 * http://www.w3.org/TR/html4/charset.html
 * </a>
 *
 * @author Chris Wilson
 * @author Juergen Hoeller
 * @since 01.03.2003
 */
public abstract class HtmlUtils {

	private static final String EMPTY_REFERENCE = "&;";
	private static final String REFERENCE_START = "&#";
	private static final String MALFORMED_REFERENCE = "&#;";

	private static final Map ENTITIES = new HashMap();

	static {
		ENTITIES.put("nbsp", 160);
		ENTITIES.put("iexcl", 161);
		ENTITIES.put("cent", 162);
		ENTITIES.put("pound", 163);
		ENTITIES.put("curren", 164);
		ENTITIES.put("yen", 165);
		ENTITIES.put("brvbar", 166);
		ENTITIES.put("sect", 167);
		ENTITIES.put("uml", 168);
		ENTITIES.put("copy", 169);
		ENTITIES.put("ordf", 170);
		ENTITIES.put("laquo", 171);
		ENTITIES.put("not", 172);
		ENTITIES.put("shy", 173);
		ENTITIES.put("reg", 174);
		ENTITIES.put("macr", 175);
		ENTITIES.put("deg", 176);
		ENTITIES.put("plusmn", 177);
		ENTITIES.put("sup2", 178);
		ENTITIES.put("sup3", 179);
		ENTITIES.put("acute", 180);
		ENTITIES.put("micro", 181);
		ENTITIES.put("para", 182);
		ENTITIES.put("middot", 183);
		ENTITIES.put("cedil", 184);
		ENTITIES.put("sup1", 185);
		ENTITIES.put("ordm", 186);
		ENTITIES.put("raquo", 187);
		ENTITIES.put("frac14", 188);
		ENTITIES.put("frac12", 189);
		ENTITIES.put("frac34", 190);
		ENTITIES.put("iquest", 191);
		ENTITIES.put("Agrave", 192);
		ENTITIES.put("Aacute", 193);
		ENTITIES.put("Acirc", 194);
		ENTITIES.put("Atilde", 195);
		ENTITIES.put("Auml", 196);
		ENTITIES.put("Aring", 197);
		ENTITIES.put("AElig", 198);
		ENTITIES.put("Ccedil", 199);
		ENTITIES.put("Egrave", 200);
		ENTITIES.put("Eacute", 201);
		ENTITIES.put("Ecirc", 202);
		ENTITIES.put("Euml", 203);
		ENTITIES.put("Igrave", 204);
		ENTITIES.put("Iacute", 205);
		ENTITIES.put("Icirc", 206);
		ENTITIES.put("Iuml", 207);
		ENTITIES.put("ETH", 208);
		ENTITIES.put("Ntilde", 209);
		ENTITIES.put("Ograve", 210);
		ENTITIES.put("Oacute", 211);
		ENTITIES.put("Ocirc", 212);
		ENTITIES.put("Otilde", 213);
		ENTITIES.put("Ouml", 214);
		ENTITIES.put("times", 215);
		ENTITIES.put("Oslash", 216);
		ENTITIES.put("Ugrave", 217);
		ENTITIES.put("Uacute", 218);
		ENTITIES.put("Ucirc", 219);
		ENTITIES.put("Uuml", 220);
		ENTITIES.put("Yacute", 221);
		ENTITIES.put("THORN", 222);
		ENTITIES.put("szlig", 223);
		ENTITIES.put("agrave", 224);
		ENTITIES.put("aacute", 225);
		ENTITIES.put("acirc", 226);
		ENTITIES.put("atilde", 227);
		ENTITIES.put("auml", 228);
		ENTITIES.put("aring", 229);
		ENTITIES.put("aelig", 230);
		ENTITIES.put("ccedil", 231);
		ENTITIES.put("egrave", 232);
		ENTITIES.put("eacute", 233);
		ENTITIES.put("ecirc", 234);
		ENTITIES.put("euml", 235);
		ENTITIES.put("igrave", 236);
		ENTITIES.put("iacute", 237);
		ENTITIES.put("icirc", 238);
		ENTITIES.put("iuml", 239);
		ENTITIES.put("eth", 240);
		ENTITIES.put("ntilde", 241);
		ENTITIES.put("ograve", 242);
		ENTITIES.put("oacute", 243);
		ENTITIES.put("ocirc", 244);
		ENTITIES.put("otilde", 245);
		ENTITIES.put("ouml", 246);
		ENTITIES.put("divide", 247);
		ENTITIES.put("oslash", 248);
		ENTITIES.put("ugrave", 249);
		ENTITIES.put("uacute", 250);
		ENTITIES.put("ucirc", 251);
		ENTITIES.put("uuml", 252);
		ENTITIES.put("yacute", 253);
		ENTITIES.put("thorn", 254);
		ENTITIES.put("yuml", 255);
		ENTITIES.put("fnof", 402);
		ENTITIES.put("Alpha", 913);
		ENTITIES.put("Beta", 914);
		ENTITIES.put("Gamma", 915);
		ENTITIES.put("Delta", 916);
		ENTITIES.put("Epsilon", 917);
		ENTITIES.put("Zeta", 918);
		ENTITIES.put("Eta", 919);
		ENTITIES.put("Theta", 920);
		ENTITIES.put("Iota", 921);
		ENTITIES.put("Kappa", 922);
		ENTITIES.put("Lambda", 923);
		ENTITIES.put("Mu", 924);
		ENTITIES.put("Nu", 925);
		ENTITIES.put("Xi", 926);
		ENTITIES.put("Omicron", 927);
		ENTITIES.put("Pi", 928);
		ENTITIES.put("Rho", 929);
		ENTITIES.put("Sigma", 931);
		ENTITIES.put("Tau", 932);
		ENTITIES.put("Upsilon", 933);
		ENTITIES.put("Phi", 934);
		ENTITIES.put("Chi", 935);
		ENTITIES.put("Psi", 936);
		ENTITIES.put("Omega", 937);
		ENTITIES.put("alpha", 945);
		ENTITIES.put("beta", 946);
		ENTITIES.put("gamma", 947);
		ENTITIES.put("delta", 948);
		ENTITIES.put("epsilon", 949);
		ENTITIES.put("zeta", 950);
		ENTITIES.put("eta", 951);
		ENTITIES.put("theta", 952);
		ENTITIES.put("iota", 953);
		ENTITIES.put("kappa", 954);
		ENTITIES.put("lambda", 955);
		ENTITIES.put("mu", 956);
		ENTITIES.put("nu", 957);
		ENTITIES.put("xi", 958);
		ENTITIES.put("omicron", 959);
		ENTITIES.put("pi", 960);
		ENTITIES.put("rho", 961);
		ENTITIES.put("sigmaf", 962);
		ENTITIES.put("sigma", 963);
		ENTITIES.put("tau", 964);
		ENTITIES.put("upsilon", 965);
		ENTITIES.put("phi", 966);
		ENTITIES.put("chi", 967);
		ENTITIES.put("psi", 968);
		ENTITIES.put("omega", 969);
		ENTITIES.put("thetasym", 977);
		ENTITIES.put("upsih", 978);
		ENTITIES.put("piv", 982);
		ENTITIES.put("bull", 8226);
		ENTITIES.put("hellip", 8230);
		ENTITIES.put("prime", 8242);
		ENTITIES.put("Prime", 8243);
		ENTITIES.put("oline", 8254);
		ENTITIES.put("frasl", 8260);
		ENTITIES.put("weierp", 8472);
		ENTITIES.put("image", 8465);
		ENTITIES.put("real", 8476);
		ENTITIES.put("trade", 8482);
		ENTITIES.put("alefsym", 8501);
		ENTITIES.put("larr", 8592);
		ENTITIES.put("uarr", 8593);
		ENTITIES.put("rarr", 8594);
		ENTITIES.put("darr", 8595);
		ENTITIES.put("harr", 8596);
		ENTITIES.put("crarr", 8629);
		ENTITIES.put("lArr", 8656);
		ENTITIES.put("uArr", 8657);
		ENTITIES.put("rArr", 8658);
		ENTITIES.put("dArr", 8659);
		ENTITIES.put("hArr", 8660);
		ENTITIES.put("forall", 8704);
		ENTITIES.put("part", 8706);
		ENTITIES.put("exist", 8707);
		ENTITIES.put("empty", 8709);
		ENTITIES.put("nabla", 8711);
		ENTITIES.put("isin", 8712);
		ENTITIES.put("notin", 8713);
		ENTITIES.put("ni", 8715);
		ENTITIES.put("prod", 8719);
		ENTITIES.put("sum", 8721);
		ENTITIES.put("minus", 8722);
		ENTITIES.put("lowast", 8727);
		ENTITIES.put("radic", 8730);
		ENTITIES.put("prop", 8733);
		ENTITIES.put("infin", 8734);
		ENTITIES.put("ang", 8736);
		ENTITIES.put("and", 8743);
		ENTITIES.put("or", 8744);
		ENTITIES.put("cap", 8745);
		ENTITIES.put("cup", 8746);
		ENTITIES.put("int", 8747);
		ENTITIES.put("there4", 8756);
		ENTITIES.put("sim", 8764);
		ENTITIES.put("cong", 8773);
		ENTITIES.put("asymp", 8776);
		ENTITIES.put("ne", 8800);
		ENTITIES.put("equiv", 8801);
		ENTITIES.put("le", 8804);
		ENTITIES.put("ge", 8805);
		ENTITIES.put("sub", 8834);
		ENTITIES.put("sup", 8835);
		ENTITIES.put("nsub", 8836);
		ENTITIES.put("sube", 8838);
		ENTITIES.put("supe", 8839);
		ENTITIES.put("oplus", 8853);
		ENTITIES.put("otimes", 8855);
		ENTITIES.put("perp", 8869);
		ENTITIES.put("sdot", 8901);
		ENTITIES.put("lceil", 8968);
		ENTITIES.put("rceil", 8969);
		ENTITIES.put("lfloor", 8970);
		ENTITIES.put("rfloor", 8971);
		ENTITIES.put("lang", 9001);
		ENTITIES.put("rang", 9002);
		ENTITIES.put("loz", 9674);
		ENTITIES.put("spades", 9824);
		ENTITIES.put("clubs", 9827);
		ENTITIES.put("hearts", 9829);
		ENTITIES.put("diams", 9830);
		ENTITIES.put("quot", 34);
		ENTITIES.put("amp", 38);
		ENTITIES.put("lt", 60);
		ENTITIES.put("gt", 62);
		ENTITIES.put("OElig", 338);
		ENTITIES.put("oelig", 339);
		ENTITIES.put("Scaron", 352);
		ENTITIES.put("scaron", 353);
		ENTITIES.put("Yuml", 376);
		ENTITIES.put("circ", 710);
		ENTITIES.put("tilde", 732);
		ENTITIES.put("ensp", 8194);
		ENTITIES.put("emsp", 8195);
		ENTITIES.put("thinsp", 8201);
		ENTITIES.put("zwnj", 8204);
		ENTITIES.put("zwj", 8205);
		ENTITIES.put("lrm", 8206);
		ENTITIES.put("rlm", 8207);
		ENTITIES.put("ndash", 8211);
		ENTITIES.put("mdash", 8212);
		ENTITIES.put("lsquo", 8216);
		ENTITIES.put("rsquo", 8217);
		ENTITIES.put("sbquo", 8218);
		ENTITIES.put("ldquo", 8220);
		ENTITIES.put("rdquo", 8221);
		ENTITIES.put("bdquo", 8222);
		ENTITIES.put("dagger", 8224);
		ENTITIES.put("Dagger", 8225);
		ENTITIES.put("permil", 8240);
		ENTITIES.put("lsaquo", 8249);
		ENTITIES.put("rsaquo", 8250);
		ENTITIES.put("euro", 8364);
	}

	/**
	 * Turn special characters into HTML character references.
	 * Handles complete character set defined in HTML 4.01 recommendation.
	 * <p>Escapes all special characters to their corresponding numerial reference
	 * in the decimal format: &#<i>Decimal</i>;
	 * <p>Reference:
	 * <a href="http://www.w3.org/TR/html4/sgml/entities.html">
	 * http://www.w3.org/TR/html4/sgml/entities.html
	 * </a>
	 */
	public static String htmlEscape(String s) {
		if (s == null) {
			return null;
		}

		StringBuffer escaped = new StringBuffer(s.length());
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			// handle non special ASCII chars first since they will be most common
			if ((c >= 0 && c <= 33)
			    || (c >= 35 && c <= 37)
			    || (c >= 39 && c <= 59)
			    || (c == 61)
			    || (c >= 63 && c <= 159)) {
				escaped.append(c);
				continue;
			}

			// handle special chars
			if (c == 34) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c == 38) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c == 60) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c == 62) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c >= 160 && c <= 255) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c >= 338 && c <= 339) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c >= 352 && c <= 353) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c == 376) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c == 402) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c == 710) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c == 732) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c >= 913 && c <= 929) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c >= 931 && c <= 937) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c >= 945 && c <= 969) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c >= 977 && c <= 978) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c == 982) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c >= 8194 && c <= 8195) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c == 8201) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c >= 8204 && c <= 8207) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c >= 8211 && c <= 8212) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c >= 8216 && c <= 8218) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c >= 8220 && c <= 8222) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c >= 8224 && c <= 8226) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c == 8230) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c == 8240) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c >= 8242 && c <= 8243) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c >= 8249 && c <= 8250) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c == 8254) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c == 8260) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c == 8364) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c == 8465) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c == 8472) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c == 8476) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c == 8482) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c == 8501) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c >= 8592 && c <= 8596) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c == 8629) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c >= 8656 && c <= 8660) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c == 8704) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c >= 8706 && c <= 8707) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c == 8709) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c >= 8711 && c <= 8713) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c == 8715) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c == 8719) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c >= 8721 && c <= 8722) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c == 8727) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c == 8730) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c >= 8733 && c <= 8734) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c == 8736) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c >= 8743 && c <= 8747) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c == 8756) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c == 8764) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c == 8773) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c == 8776) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c >= 8800 && c <= 8801) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c >= 8804 && c <= 8805) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c >= 8834 && c <= 8836) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c >= 8838 && c <= 8839) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c == 8853) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c == 8855) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c == 8869) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c == 8901) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c >= 8968 && c <= 8971) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c >= 9001 && c <= 9002) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c == 9674) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c == 9824) {
				writeDecimalReference(c, escaped);
				continue;
			}
			if (c == 9827) {
				writeDecimalReference(c, escaped);
				continue;
			}

			// all other chars
			escaped.append(c);
		}
		return escaped.toString();
	}

	/**
	 * Turn HTML character references into their plain text UNICODE equivalent.
	 * <p>Handles complete character set defined in HTML 4.01 recommendation
	 * and all reference types (decimal, hex, and entity).
	 * <p>Correctly converts the following formats:
	 * <blockquote>
	 * &amp;#<i>Decimal</i>; - <i>(Example: &amp;#68;)</i><br>
	 * &amp;#x<i>Hex</i>; - <i>(Example: &amp;#xE5;) case insensitive</i><br>
	 * &amp;#<i>Entity</i>; - <i>(Example: &amp;amp;) case sensitive</i>
	 * </blockquote>
	 * Gracefully handles malformed character references by copying original
	 * characters as is when encountered.<p>
	 * <p>Reference:
	 * <a href="http://www.w3.org/TR/html4/sgml/entities.html">
	 * http://www.w3.org/TR/html4/sgml/entities.html
	 * </a>
	 */
	public static String htmlUnescape(String s) {
		if (s == null) {
			return null;
		}

		StringBuffer unescaped = new StringBuffer(s.length());
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '&') {
				// don't look more than 12 chars ahead as reference like strings
				// should not be longer than 12 chars in length (including ';')
				// prevents the entire string from being searched when an '&'
				// with no following ';' is an encountered
				int start = Math.min(i + 1, s.length() - 1);
				int end = Math.min(s.length(), start + 12);

				String reference = s.substring(start, end);
				int semi = reference.indexOf(';');

				if (semi == -1) {
					unescaped.append(c);
					continue;
				}

				reference = reference.substring(0, semi);
				i = start + semi;

				// try entity reference first
				Integer iso = (Integer) ENTITIES.get(reference);
				if (iso != null) {
					unescaped.append((char) iso.intValue());
					continue;
				}

				if (reference.length() == 0) {
					unescaped.append(EMPTY_REFERENCE);
					continue;
				}

				if (reference.charAt(0) == '#') {
					if (reference.length() > 2) {
						int index = 1;
						if (reference.charAt(1) == 'x' || reference.charAt(1) == 'X') {
							index = 2;
						}
						try {
							unescaped.append(
							    (char) Integer.parseInt(
							        reference.substring(index),
							        (index == 1) ? 10 : 16));
							continue;
						}
						catch (NumberFormatException e) {
							// wasn't hex or decimal, copy original chars
							unescaped.append('&' + reference + ';');
							continue;
						}
					}
					unescaped.append(MALFORMED_REFERENCE);
					continue;
				}

				// may not be valid reference, forget it
				i = start - 1;
			}
			unescaped.append(c);
		}
		return unescaped.toString();
	}

	/**
	 * Write the given character as decimal reference.
	 * @param c the character to write
	 * @param buf the buffer to write into
	 */
	private static void writeDecimalReference(char c, StringBuffer buf) {
		buf.append(REFERENCE_START);
		buf.append((int) c);
		buf.append(';');
	}

}