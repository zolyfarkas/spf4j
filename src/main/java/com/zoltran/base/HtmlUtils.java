
package com.zoltran.base;

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
		ENTITIES.put("nbsp", new Integer(160));
		ENTITIES.put("iexcl", new Integer(161));
		ENTITIES.put("cent", new Integer(162));
		ENTITIES.put("pound", new Integer(163));
		ENTITIES.put("curren", new Integer(164));
		ENTITIES.put("yen", new Integer(165));
		ENTITIES.put("brvbar", new Integer(166));
		ENTITIES.put("sect", new Integer(167));
		ENTITIES.put("uml", new Integer(168));
		ENTITIES.put("copy", new Integer(169));
		ENTITIES.put("ordf", new Integer(170));
		ENTITIES.put("laquo", new Integer(171));
		ENTITIES.put("not", new Integer(172));
		ENTITIES.put("shy", new Integer(173));
		ENTITIES.put("reg", new Integer(174));
		ENTITIES.put("macr", new Integer(175));
		ENTITIES.put("deg", new Integer(176));
		ENTITIES.put("plusmn", new Integer(177));
		ENTITIES.put("sup2", new Integer(178));
		ENTITIES.put("sup3", new Integer(179));
		ENTITIES.put("acute", new Integer(180));
		ENTITIES.put("micro", new Integer(181));
		ENTITIES.put("para", new Integer(182));
		ENTITIES.put("middot", new Integer(183));
		ENTITIES.put("cedil", new Integer(184));
		ENTITIES.put("sup1", new Integer(185));
		ENTITIES.put("ordm", new Integer(186));
		ENTITIES.put("raquo", new Integer(187));
		ENTITIES.put("frac14", new Integer(188));
		ENTITIES.put("frac12", new Integer(189));
		ENTITIES.put("frac34", new Integer(190));
		ENTITIES.put("iquest", new Integer(191));
		ENTITIES.put("Agrave", new Integer(192));
		ENTITIES.put("Aacute", new Integer(193));
		ENTITIES.put("Acirc", new Integer(194));
		ENTITIES.put("Atilde", new Integer(195));
		ENTITIES.put("Auml", new Integer(196));
		ENTITIES.put("Aring", new Integer(197));
		ENTITIES.put("AElig", new Integer(198));
		ENTITIES.put("Ccedil", new Integer(199));
		ENTITIES.put("Egrave", new Integer(200));
		ENTITIES.put("Eacute", new Integer(201));
		ENTITIES.put("Ecirc", new Integer(202));
		ENTITIES.put("Euml", new Integer(203));
		ENTITIES.put("Igrave", new Integer(204));
		ENTITIES.put("Iacute", new Integer(205));
		ENTITIES.put("Icirc", new Integer(206));
		ENTITIES.put("Iuml", new Integer(207));
		ENTITIES.put("ETH", new Integer(208));
		ENTITIES.put("Ntilde", new Integer(209));
		ENTITIES.put("Ograve", new Integer(210));
		ENTITIES.put("Oacute", new Integer(211));
		ENTITIES.put("Ocirc", new Integer(212));
		ENTITIES.put("Otilde", new Integer(213));
		ENTITIES.put("Ouml", new Integer(214));
		ENTITIES.put("times", new Integer(215));
		ENTITIES.put("Oslash", new Integer(216));
		ENTITIES.put("Ugrave", new Integer(217));
		ENTITIES.put("Uacute", new Integer(218));
		ENTITIES.put("Ucirc", new Integer(219));
		ENTITIES.put("Uuml", new Integer(220));
		ENTITIES.put("Yacute", new Integer(221));
		ENTITIES.put("THORN", new Integer(222));
		ENTITIES.put("szlig", new Integer(223));
		ENTITIES.put("agrave", new Integer(224));
		ENTITIES.put("aacute", new Integer(225));
		ENTITIES.put("acirc", new Integer(226));
		ENTITIES.put("atilde", new Integer(227));
		ENTITIES.put("auml", new Integer(228));
		ENTITIES.put("aring", new Integer(229));
		ENTITIES.put("aelig", new Integer(230));
		ENTITIES.put("ccedil", new Integer(231));
		ENTITIES.put("egrave", new Integer(232));
		ENTITIES.put("eacute", new Integer(233));
		ENTITIES.put("ecirc", new Integer(234));
		ENTITIES.put("euml", new Integer(235));
		ENTITIES.put("igrave", new Integer(236));
		ENTITIES.put("iacute", new Integer(237));
		ENTITIES.put("icirc", new Integer(238));
		ENTITIES.put("iuml", new Integer(239));
		ENTITIES.put("eth", new Integer(240));
		ENTITIES.put("ntilde", new Integer(241));
		ENTITIES.put("ograve", new Integer(242));
		ENTITIES.put("oacute", new Integer(243));
		ENTITIES.put("ocirc", new Integer(244));
		ENTITIES.put("otilde", new Integer(245));
		ENTITIES.put("ouml", new Integer(246));
		ENTITIES.put("divide", new Integer(247));
		ENTITIES.put("oslash", new Integer(248));
		ENTITIES.put("ugrave", new Integer(249));
		ENTITIES.put("uacute", new Integer(250));
		ENTITIES.put("ucirc", new Integer(251));
		ENTITIES.put("uuml", new Integer(252));
		ENTITIES.put("yacute", new Integer(253));
		ENTITIES.put("thorn", new Integer(254));
		ENTITIES.put("yuml", new Integer(255));
		ENTITIES.put("fnof", new Integer(402));
		ENTITIES.put("Alpha", new Integer(913));
		ENTITIES.put("Beta", new Integer(914));
		ENTITIES.put("Gamma", new Integer(915));
		ENTITIES.put("Delta", new Integer(916));
		ENTITIES.put("Epsilon", new Integer(917));
		ENTITIES.put("Zeta", new Integer(918));
		ENTITIES.put("Eta", new Integer(919));
		ENTITIES.put("Theta", new Integer(920));
		ENTITIES.put("Iota", new Integer(921));
		ENTITIES.put("Kappa", new Integer(922));
		ENTITIES.put("Lambda", new Integer(923));
		ENTITIES.put("Mu", new Integer(924));
		ENTITIES.put("Nu", new Integer(925));
		ENTITIES.put("Xi", new Integer(926));
		ENTITIES.put("Omicron", new Integer(927));
		ENTITIES.put("Pi", new Integer(928));
		ENTITIES.put("Rho", new Integer(929));
		ENTITIES.put("Sigma", new Integer(931));
		ENTITIES.put("Tau", new Integer(932));
		ENTITIES.put("Upsilon", new Integer(933));
		ENTITIES.put("Phi", new Integer(934));
		ENTITIES.put("Chi", new Integer(935));
		ENTITIES.put("Psi", new Integer(936));
		ENTITIES.put("Omega", new Integer(937));
		ENTITIES.put("alpha", new Integer(945));
		ENTITIES.put("beta", new Integer(946));
		ENTITIES.put("gamma", new Integer(947));
		ENTITIES.put("delta", new Integer(948));
		ENTITIES.put("epsilon", new Integer(949));
		ENTITIES.put("zeta", new Integer(950));
		ENTITIES.put("eta", new Integer(951));
		ENTITIES.put("theta", new Integer(952));
		ENTITIES.put("iota", new Integer(953));
		ENTITIES.put("kappa", new Integer(954));
		ENTITIES.put("lambda", new Integer(955));
		ENTITIES.put("mu", new Integer(956));
		ENTITIES.put("nu", new Integer(957));
		ENTITIES.put("xi", new Integer(958));
		ENTITIES.put("omicron", new Integer(959));
		ENTITIES.put("pi", new Integer(960));
		ENTITIES.put("rho", new Integer(961));
		ENTITIES.put("sigmaf", new Integer(962));
		ENTITIES.put("sigma", new Integer(963));
		ENTITIES.put("tau", new Integer(964));
		ENTITIES.put("upsilon", new Integer(965));
		ENTITIES.put("phi", new Integer(966));
		ENTITIES.put("chi", new Integer(967));
		ENTITIES.put("psi", new Integer(968));
		ENTITIES.put("omega", new Integer(969));
		ENTITIES.put("thetasym", new Integer(977));
		ENTITIES.put("upsih", new Integer(978));
		ENTITIES.put("piv", new Integer(982));
		ENTITIES.put("bull", new Integer(8226));
		ENTITIES.put("hellip", new Integer(8230));
		ENTITIES.put("prime", new Integer(8242));
		ENTITIES.put("Prime", new Integer(8243));
		ENTITIES.put("oline", new Integer(8254));
		ENTITIES.put("frasl", new Integer(8260));
		ENTITIES.put("weierp", new Integer(8472));
		ENTITIES.put("image", new Integer(8465));
		ENTITIES.put("real", new Integer(8476));
		ENTITIES.put("trade", new Integer(8482));
		ENTITIES.put("alefsym", new Integer(8501));
		ENTITIES.put("larr", new Integer(8592));
		ENTITIES.put("uarr", new Integer(8593));
		ENTITIES.put("rarr", new Integer(8594));
		ENTITIES.put("darr", new Integer(8595));
		ENTITIES.put("harr", new Integer(8596));
		ENTITIES.put("crarr", new Integer(8629));
		ENTITIES.put("lArr", new Integer(8656));
		ENTITIES.put("uArr", new Integer(8657));
		ENTITIES.put("rArr", new Integer(8658));
		ENTITIES.put("dArr", new Integer(8659));
		ENTITIES.put("hArr", new Integer(8660));
		ENTITIES.put("forall", new Integer(8704));
		ENTITIES.put("part", new Integer(8706));
		ENTITIES.put("exist", new Integer(8707));
		ENTITIES.put("empty", new Integer(8709));
		ENTITIES.put("nabla", new Integer(8711));
		ENTITIES.put("isin", new Integer(8712));
		ENTITIES.put("notin", new Integer(8713));
		ENTITIES.put("ni", new Integer(8715));
		ENTITIES.put("prod", new Integer(8719));
		ENTITIES.put("sum", new Integer(8721));
		ENTITIES.put("minus", new Integer(8722));
		ENTITIES.put("lowast", new Integer(8727));
		ENTITIES.put("radic", new Integer(8730));
		ENTITIES.put("prop", new Integer(8733));
		ENTITIES.put("infin", new Integer(8734));
		ENTITIES.put("ang", new Integer(8736));
		ENTITIES.put("and", new Integer(8743));
		ENTITIES.put("or", new Integer(8744));
		ENTITIES.put("cap", new Integer(8745));
		ENTITIES.put("cup", new Integer(8746));
		ENTITIES.put("int", new Integer(8747));
		ENTITIES.put("there4", new Integer(8756));
		ENTITIES.put("sim", new Integer(8764));
		ENTITIES.put("cong", new Integer(8773));
		ENTITIES.put("asymp", new Integer(8776));
		ENTITIES.put("ne", new Integer(8800));
		ENTITIES.put("equiv", new Integer(8801));
		ENTITIES.put("le", new Integer(8804));
		ENTITIES.put("ge", new Integer(8805));
		ENTITIES.put("sub", new Integer(8834));
		ENTITIES.put("sup", new Integer(8835));
		ENTITIES.put("nsub", new Integer(8836));
		ENTITIES.put("sube", new Integer(8838));
		ENTITIES.put("supe", new Integer(8839));
		ENTITIES.put("oplus", new Integer(8853));
		ENTITIES.put("otimes", new Integer(8855));
		ENTITIES.put("perp", new Integer(8869));
		ENTITIES.put("sdot", new Integer(8901));
		ENTITIES.put("lceil", new Integer(8968));
		ENTITIES.put("rceil", new Integer(8969));
		ENTITIES.put("lfloor", new Integer(8970));
		ENTITIES.put("rfloor", new Integer(8971));
		ENTITIES.put("lang", new Integer(9001));
		ENTITIES.put("rang", new Integer(9002));
		ENTITIES.put("loz", new Integer(9674));
		ENTITIES.put("spades", new Integer(9824));
		ENTITIES.put("clubs", new Integer(9827));
		ENTITIES.put("hearts", new Integer(9829));
		ENTITIES.put("diams", new Integer(9830));
		ENTITIES.put("quot", new Integer(34));
		ENTITIES.put("amp", new Integer(38));
		ENTITIES.put("lt", new Integer(60));
		ENTITIES.put("gt", new Integer(62));
		ENTITIES.put("OElig", new Integer(338));
		ENTITIES.put("oelig", new Integer(339));
		ENTITIES.put("Scaron", new Integer(352));
		ENTITIES.put("scaron", new Integer(353));
		ENTITIES.put("Yuml", new Integer(376));
		ENTITIES.put("circ", new Integer(710));
		ENTITIES.put("tilde", new Integer(732));
		ENTITIES.put("ensp", new Integer(8194));
		ENTITIES.put("emsp", new Integer(8195));
		ENTITIES.put("thinsp", new Integer(8201));
		ENTITIES.put("zwnj", new Integer(8204));
		ENTITIES.put("zwj", new Integer(8205));
		ENTITIES.put("lrm", new Integer(8206));
		ENTITIES.put("rlm", new Integer(8207));
		ENTITIES.put("ndash", new Integer(8211));
		ENTITIES.put("mdash", new Integer(8212));
		ENTITIES.put("lsquo", new Integer(8216));
		ENTITIES.put("rsquo", new Integer(8217));
		ENTITIES.put("sbquo", new Integer(8218));
		ENTITIES.put("ldquo", new Integer(8220));
		ENTITIES.put("rdquo", new Integer(8221));
		ENTITIES.put("bdquo", new Integer(8222));
		ENTITIES.put("dagger", new Integer(8224));
		ENTITIES.put("Dagger", new Integer(8225));
		ENTITIES.put("permil", new Integer(8240));
		ENTITIES.put("lsaquo", new Integer(8249));
		ENTITIES.put("rsaquo", new Integer(8250));
		ENTITIES.put("euro", new Integer(8364));
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