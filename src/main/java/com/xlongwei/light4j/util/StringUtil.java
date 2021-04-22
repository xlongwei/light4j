package com.xlongwei.light4j.util;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.lang3.ArrayUtils;

import com.networknt.utility.CharUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * string util
 * @author xlongwei
 *
 */
@Slf4j
public class StringUtil {
	private static final String LINE_SPLIT = "[\r\n]+";

	/** 将多行路径转换为列表，用于上传多张图片 */
	@SuppressWarnings("unchecked")
	public static List<String> linesToList(String lines) {
		if(StringUtil.isBlank(lines)) {
			return Collections.EMPTY_LIST;
		}
		List<String> list = new ArrayList<String>();
		for(String line:lines.split(LINE_SPLIT)) {
			if(StringUtil.isBlank(line)==false) {
				list.add(line.trim());
			}
		}
		return list;
	}
	
	/** 将多张图片路径列表转换为多行字符串 */
	public static String listToLines(List<String> list) {
		if(list == null || list.size() == 0) {
			return "";
		}
		StringBuilder lines = new StringBuilder();
		for(String line:list) {
			lines.append(line);
			lines.append('\n');
		}
		lines.deleteCharAt(lines.length()-1);
		return lines.toString();
	}
	
	/** 以逗号分隔的字符串是否包含某个值 */
	public static boolean splitContains(String content, String contain) {
		if(isBlank(content)) {
			return false;
		}
		String[] splits = content.split("[,;]");
		for(String s : splits) {
			if(s.equals(contain)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @param algorithm one of [SHA, MD5, MD2, SHA-256, SHA-384, SHA-512]
	 */
	public static String digest(String content, String algorithm) {
		byte[] digest = null;
		MessageDigest md;
		try {
			md = MessageDigest.getInstance(algorithm);
			digest = md.digest(content.getBytes(StandardCharsets.UTF_8));
		} catch (Exception e) {
			log.warn("fail to digest "+algorithm+" of content: "+content, e);
		}
		return toHexString(digest);
	}
	
	/**
	 * @return kmp fail array of pattern
	 */
	public static int[] getKmpFail(String pattern) {
		if (!hasLength(pattern)) {
			throw new IllegalArgumentException("null or empty pattern is not allowed to get kmp fail array.");
		}
		int i, j, len = pattern.length();
		int[] fail = new int[len];
		fail[0] = -1;
		for (j = 1; j < len; j++) {
			i = fail[j - 1];
			while ((pattern.charAt(j) != pattern.charAt(i + 1)) && (i >= 0)) {
				i = fail[i];
			}
			if (pattern.charAt(j) == pattern.charAt(i + 1)) {
				fail[j] = i + 1;
			} else {
				fail[j] = -1;
			}
		}
		return fail;
	}
	
	/**
	 * get kmp fail array for pattern array
	 */
	public static int[][] getKmpFails(String[] patterns) {
		if (patterns == null || patterns.length == 0) {
			throw new IllegalArgumentException("null or empty patterns is not allowed to get kmp fail array.");
		}
		int[][] fails = new int[patterns.length][];
		for (int i = 0; i < patterns.length; i++) {
			fails[i] = getKmpFail(patterns[i]);
		}
		return fails;
	}
	
	/**
	 * @return array of char[] for pattern array
	 */
	public static char[][] getStepChars(String[] patterns) {
		if (patterns == null || patterns.length == 0) {
			throw new IllegalArgumentException("null or empty patterns is not allowed to get kmp fail array.");
		}
		char[][] chars = new char[patterns.length][];
		for (int i = 0; i < patterns.length; i++) {
			if (!StringUtil.hasLength(patterns[i])) {
				throw new IllegalArgumentException("null or empty patterns is not allowed to get kmp fail array.");
			}
			chars[i] = patterns[i].toCharArray();
		}
		return chars;
	}
	
	/**
	 * @return true if string is not null or empty
	 */
	public static boolean hasLength(String string) {
		return string != null && string.length() > 0;
	}
	
	/** 存在空白串 */
	public static boolean hasAnyBlank(String ... strs) {
		if(strs==null || strs.length==0) {
			return false;
		}
		for(String str : strs) {
			if(isBlank(str)) {
				return true;
			}
		}
		return false;
	}
	
	/** 不存在空白串 */
	public static boolean hasNoneBlank(String ... strs) {
		if(strs==null || strs.length==0) {
			return true;
		}
		for(String str : strs) {
			if(isBlank(str)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * 判断空串
	 */
	public static boolean isBlank(String str) {
        if(!hasLength(str)) {
			return true;
		}
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
				return false;
			}
        }
        return true;
	}
	
	public static String trim(String str) {
		if(!hasLength(str)) {
			return str;
		}
		int from = 0, to = str.length();
		while(from<to && Character.isWhitespace(str.charAt(from))) {
			from++;
		}
		while(to>from && Character.isWhitespace(str.charAt(to-1))) {
			to--;
		}
		return str.substring(from, to);
	}
	
	public static String firstNotBlank(String ... strings) {
		if(strings==null || strings.length==0) {
			return null;
		}
		for(String string : strings) {
			if(!StringUtil.isBlank(string)) {
				return string;
			}
		}
		return null;
	}
	
	public static String rootDomain(String domain) {
		if(!hasLength(domain)) {
			return domain;
		}
		int dot = domain.lastIndexOf('.');
		if(dot <= 0 || isIp(domain)) {
			return domain;
		}
		dot = domain.lastIndexOf('.', dot-1);
		return dot==-1 ? domain : domain.substring(dot+1);
	}
	
	public static String rootUrl(String url) {
		int dot = url.indexOf("://"), slash = url.indexOf('/', dot+3);
		return slash < 0 ? url : url.substring(0, slash+1);
	}
	
	/**
	 * 首字母大写
	 */
    public static String capitalize(String str) {
        if (!hasLength(str)) {
			return str;
		}
        return str.substring(0,1).toUpperCase()+str.substring(1);
    }
	
	/**
	 * @return true if left is the same or equals with right.
	 */
	public static boolean equals(Object left, Object right) {
		return (left == right) || (left == null && right == null) || (left != null && right != null && left.equals(right));
	}
	
	/**
	 * @return true for "" == null.
	 */
	public static boolean emptyEquals(Object left, Object right) {
		return ("".equals(left) && right == null) || (left == null && "".equals(right)) || equals(left, right);
	}
	
	/**
	 * @return true for 1 == "1".
	 */
	public static boolean primitiveEquals(Object left, Object right) {
		return emptyEquals(left, right) || String.valueOf(left).equals(String.valueOf(right));
	}
	
	/**
	 * @return null for '' and 'null' or str.
	 */
	public static String nullOrString(String str) {
		return hasLength(str) && !"null".equalsIgnoreCase(str) ? str : null;
	}
	
	public static String toString(Object obj) {
		if(obj != null) {
			if(obj instanceof Date) {
				return DateUtil.format((Date)obj);
			} else if(obj instanceof Object[]) {
				Object[] arr = (Object[])obj;
				int iMax = arr.length-1;
				if(iMax == -1) {
					return "[]";
				}
				StringBuilder sb = new StringBuilder("[");
				for(int i=0; ; i++) {
					sb.append(toString(arr[i]));
					if(i == iMax) {
						return sb.append(']').toString();
					}
					sb.append(',');
				}
			} else {
				return obj.toString();
			}
		}else {
			return "null";
		}
	}
	
	/**
	 * @return pattern's count in source
	 */
	public static int kmpCountMatch(String source, String pattern) {
		int count = 0, from = 0, to = source.length();
		int[] fail = getKmpFail(pattern);
		while ((from = kmpIndexOf(source, pattern, from, fail, to)) != -1) {
			from += pattern.length();
			count++;
		}
		return count;
	}
	
	/**
	 * find first match pattern in source, [0] is index and [1] is length.
	 */
	public static int[] kmpFirstMatch(String source, String[] patterns) {
		return kmpFirstMatch(source, patterns, 0);
	}
	
	/**
	 * find first match pattern in source, [0] is index and [1] is length.
	 */
	public static int[] kmpFirstMatch(String source, String[] patterns, int from) {
		int[][] fails = getKmpFails(patterns);
		return kmpFirstMatch(source, patterns, from, fails);
	}
	
	/**
	 * find first match pattern in source, [0] is index and [1] is length.
	 */
	public static int[] kmpFirstMatch(String source, String[] patterns, int from, int[][] fails) {
		int[] indices = { -1, -1 };
		int to = source.length();
		for (int i = 0; i < patterns.length; i++) {
			if(to - from < patterns[i].length()) {
				continue;
			}
			int index = kmpIndexOf(source, patterns[i], from, fails[i], to);
			boolean b = index != -1 && (index < indices[0] || indices[0] == -1);
			if (b) {
				indices[0] = to = index;
				indices[1] = patterns[i].length();
				to++;
			}
		}
		return indices;
	}
	
	/**
	 * @return index of pattern in source or -1
	 */
	public static int kmpIndexOf(String source, String pattern) {
		return kmpIndexOf(source, pattern, 0);
	}
	
	/**
	 * @return index of pattern in source or -1
	 */
	public static int kmpIndexOf(String source, String pattern, int from) {
		int[] fail = getKmpFail(pattern);
		return kmpIndexOf(source, pattern, from, fail);
	}
	
	/**
	 * @return index of pattern in source or -1
	 */
	public static int kmpIndexOf(String source, String pattern, int from, int[] fail) {
		int to = source.length();
		return kmpIndexOf(source, pattern, from, fail, to);
	}
	
	/**
	 * @return index of pattern in source or -1
	 */
	public static int kmpIndexOf(String source, String pattern, int from, int[] fail, int to) {
		int i = from, j = 0, lenp = pattern.length();
		while ((i < to) && (j < lenp)) {
			if (source.charAt(i) == pattern.charAt(j)) {
				i++;
				j++;
			} else {
				if (j == 0) {
					i++;
				} else {
					j = fail[j - 1] + 1;
				}
			}
		}
		return (j == lenp) ? (i - lenp) : -1;
	}
	
	/**
	 * @return random string of length
	 */
	public static String randomString(int length) {
		if (length < 1) {
            throw new IllegalArgumentException("random string length must be greater than 0.");
        }
		
        char [] randBuffer = new char[length];
        for (int i = 0; i < randBuffer.length; i++) {
            randBuffer[i] = numbersAndLetters[randomGenerator.nextInt(numbersAndLetters.length)];
        }
        return new String(randBuffer);
	}
	
	/**
	 * find first match pattern in source, [0] is index and [1] is length.
	 */
	public static int[] stepFirstMatch(char[] sources, int from, char[][] chars) {
		int[] indices = { -1, -1 };
		for (int i = from; i < sources.length; i++) {
			for (int j = 0; j < chars.length; j++) {
				if (sources[i] == chars[j][0]) {
					int k = 1;
					while (k < chars[j].length && i + k < sources.length && sources[i + k] == chars[j][k]) {
						k++;
					}
					if (k == chars[j].length) {
						indices[0] = i;
						indices[1] = k;
						return indices;
					}
				}
			}
		}
		return indices;
	}
	
	/**
	 * find first match pattern in source, [0] is index and [1] is length.
	 */
	public static int[] stepFirstMatch(String source, String[] patterns) {
		return stepFirstMatch(source, patterns, 0);
	}
	
	/**
	 * find first match pattern in source, [0] is index and [1] is length.
	 */
	public static int[] stepFirstMatch(String source, String[] patterns, int from) {
		char[] sources = source.toCharArray();
		char[][] chars = getStepChars(patterns);
		return stepFirstMatch(sources, from, chars);
	}
	/**
	 * @return DBC(半角) of input
	 */
	public static String toDBC(String input) {
		char c[] = input.toCharArray();
		for (int i = 0; i < c.length; i++) {
			if (c[i] == '\u3000') {
				c[i] = ' ';
			} else if ((c[i] > '\uFF00') && (c[i] < '\uFF5F')) {
				c[i] = (char) (c[i] - 65248);
			}
		}
		return new String(c);
	}
	/**
	 * @return hex string of bytes
	 */
	public static String toHexString(byte[] bytes) {
		StringBuilder hex = new StringBuilder();
		if (bytes != null) {
			for (byte bt : bytes) {
				hex.append(hexChar[(bt & 0xf0) >>> 4]);
				hex.append(hexChar[bt & 0x0f]);
			}
		}
		return hex.toString();
	}
	
	/**
	 * @return byte[] of hexString
	 */
	public static byte[] fromHexString(String hexString) {
		if(hexString == null) {
			return null;
		}
		byte[] bytes = new byte[hexString.length() / 2];
		for(int i = 0; i < hexString.length(); i++) {
			char c = hexString.charAt(i);
			byte b = -1;
			for(int j = 0; j < hexChar.length; j++) {
				if(c == hexChar[j]) {
					b = (byte)j;
					break;
				}
			}
			if(b == -1) {
				return null;
			}
			if(i % 2 == 0) {
				bytes[i / 2] = (byte)(b << 4);
			}else {
				bytes[i / 2] += b;
			}
		}
		return bytes;
	}
	
	/**
	 * @return SBC(全角) of input
	 */
	public static String toSBC(String input) {
		char c[] = input.toCharArray();
		for (int i = 0; i < c.length; i++) {
			if (c[i] == ' ') {
				c[i] = '\u3000';
			} else if ((c[i] < 127) && (c[i] > 32)) {
				c[i] = (char) (c[i] + 65248);
			}
		}
		return new String(c);
	}
	
	/**
	 * 是否包含中文
	 */
	public static boolean isHasChinese(String source){  
	    char[] chars=source.toCharArray();   
	    for(int i=0;i<chars.length;i++){
	    	if(isChinese(chars[i])) {
	    		return true;
	    	}
	    }   
	    return false;   
	}
	
	/** 是否全部是中文 */
	public static boolean isChinese(String source) {
	    char[] chars=source.toCharArray();   
	    for(int i=0;i<chars.length;i++){
	    	if(isChinese(chars[i])==false) {
	    		return false;
	    	}
	    }   
	    return true;		
	}
	
	/** 汉字 */
	public static boolean isChinese(char c) {
		// return String.valueOf(c).matches("[\\u4E00-\\u9FA5]+");
		Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
		return ub.equals(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS)
			|| ub.equals(Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS)
			|| ub.equals(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A)
			|| ub.equals(Character.UnicodeBlock.GENERAL_PUNCTUATION)
			|| ub.equals( Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION)
			|| ub.equals(Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS);
	}
	
	/** 用户名 */
	public static boolean isIdentifier(String string) {
		if(!hasLength(string)) {
			return false;
		}
		char[] chars=string.toCharArray();
		for(int i=0;i<chars.length;i++) {
			if(i==0 && chars[i]!='_' && !CharUtils.isAsciiAlpha(chars[i]))
			 {
				//首字母必须是字母或下划线
				return false;
			}
			if(i>0 && !CharUtils.isAsciiPrintable(chars[i]))
			 {
				//后面的字母可以是字母或数字
				return false;
			}
		}
		return true;
	}
	
	/** 营业执照号码：15位数字或18位字母数字混合 */
	public static boolean isBusinessNo(String bizNo) {
		int businessNoLength = 15;
		if(isNumbers(bizNo) && bizNo.length()==businessNoLength) {
			return true;
		}
		if(isSccNumber(bizNo)) {
			return true;
		}
		return false;
	}
	
	/** 三证合一校验：18位=1位登记部门+1位机构类别+6位行政区划+9位组织机构+1位校验位
	 * <br>营业执照+组织机构代码证+税务登记证：一口受理、并联审批、信息共享、结果互认
	 * @see http://club.excelhome.net/thread-1258807-1-1.html
	 */
	public static boolean isSccNumber(String sccNo) {
		int sccNumberLength = 18;
		if(sccNo==null || sccNo.length()!=sccNumberLength) {
			return false;
		}
		if(!sccPattern.matcher(sccNo).matches()) {
			return false;
		}
		final char[] cs = sccNo.toUpperCase().toCharArray();
		int t = 0, p, c;
		int end = 17;
		for(int i=0;i<end;i++) {
			c = cs[i];
			p = sccwf.indexOf(c);
			if(p<0) {
				return false;
			}
			t += sccwi[i]*p;
		}
		p = 31 - t % 31;
		c = p<10 ? Character.forDigit(p, 10) : sccwf.charAt(p);
		return c == cs[end];
	}
	
	/**
	 * 条形码校验规则：977167121601X
	 * <br>1，从右到左编号：1 2 3 ... 12
	 * <br>2，偶数位相加，乘以3：(1+6+2+7+1+7)*3=24*3=72
	 * <br>3，奇数位相加（不含末位校验位），求和：(0+1+1+6+7+9)+72=24+72=96
	 * <br>4，10减去个位数，X=10-6=4
	 */
	public static boolean isBarcode(String barcode) {
		if(isBlank(barcode) || !isNumbers(barcode)) {
			return false;
		}
		int length = barcode.length();
		if(length <= 1) {
			return false;
		}
		int calc = calcBarcode(barcode.substring(0, length-1));
		return calc>=0 && calc<=9 && calc==(barcode.charAt(length-1)-'0');
	}
	/** 计算条码的校验位 */
	public static int calcBarcode(String barcode) {
		if(isBlank(barcode) || !isNumbers(barcode)) {
			return -1;
		}
		int sums = 0, length = barcode.length();
		for(int i=1;i<=length;i++) {
			char c = barcode.charAt(length-i);
			if(i%2==0) {
				sums += 3*(c-'0');
			}else {
				sums += c-'0';
			}
		}
		sums %= 10;
		return sums==0 ? 0 : 10-sums;
	}
	
	/**
	 * @return true if string is hex string.
	 */
	public static boolean isHexString(String string) {
		char[] chars=string.toCharArray();   
	    for(int i=0;i<chars.length;i++){
	    	if(!isHexChar(chars[i])) {
	    		return false;
	    	}
	    }   
	    return true; 
	}
	
	/**
	 * @return true if c is hex char.
	 */
	public static boolean isHexChar(char c) {
		for(int i = 0; i< hexChar.length; i++) {
			if(c == hexChar[i]) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 编码xml
	 */
	public static String escapeHTMLTags(String in) {
        if (in == null) {
            return null;
        }
        char ch;
        int i = 0;
        int last = 0;
        char[] input = in.toCharArray();
        int len = input.length;
        StringBuffer out = new StringBuffer((int)(len * 1.3));
        for (; i < len; i++) {
            ch = input[i];
            if (ch > '>') {
                // Nothing to do
            }
            else if (ch == '<') {
                if (i > last) {
                    out.append(input, last, i - last);
                }
                last = i + 1;
                out.append(LT_ENCODE);
            }
            else if (ch == '>') {
                if (i > last) {
                    out.append(input, last, i - last);
                }
                last = i + 1;
                out.append(GT_ENCODE);
            }
            else if (ch == '"') {
                if (i > last) {
                    out.append(input, last, i - last);
                }
                last = i + 1;
                out.append(QUOTE_ENCODE);
            }
            else if (ch == '&') {
            	if (i > last) {
            		out.append(input, last, i - last);
            	}
            	last = i + 1;
            	out.append(AMP_ENCODE);
            }
        }
        if (last == 0) {
            return in;
        }
        if (i > last) {
            out.append(input, last, i - last);
        }
        return out.toString();
    }
	
	/**
	 * 解码xml
	 */
    public static String unescapeFromXML(String string) {
        string = string.replaceAll("&lt;", "<");
        string = string.replaceAll("&gt;", ">");
        string = string.replaceAll("&quot;", "\"");
        return string.replaceAll("&amp;", "&");
    }
    public static Pattern getPattern(String pattern) {
    	Pattern p = patterns.get(pattern);
		if(p==null) {
			p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
			patterns.put(pattern, p);
		}
		return p;
    }
	public static String getPatternString(String input, String pattern) {
		Pattern p = getPattern(pattern);
		return getPatternString(input, p);
	}
	public static String getPatternString(String input, Pattern pattern) {
		Matcher matcher = pattern.matcher(input);
		if(matcher.find()) {
			return matcher.group(1);
		}else {
			return null;
		}
	}
	public static boolean matches(String input, String pattern) {
		Pattern p = getPattern(pattern);
		return p.matcher(input).matches();
	}
	public static String getXmlTag(String input, String tag) {
		return getPatternString(input, "<"+tag+">[^<]*</"+tag+">");
	}
	
	/**
	 * @return true if string matches url pattern.
	 */
	public static boolean isUrl(String string) {
		if(!hasLength(string)) {
			return false;
		}
		return urlPattern.matcher(string).matches();
	}
	
	/**
	 * @return true if string matches email pattern.
	 */
	public static boolean isEmail(String string) {
		if(!hasLength(string)) {
			return false;
		}
		return emailPattern.matcher(string).matches();
	}
	
    /**
     * 手机号码: 13[0-9], 14[5,7], 15[0, 1, 2, 3, 5, 6, 7, 8, 9], 17[6, 7, 8], 18[0-9], 170[0-9]
     * <br>移动号段: 134,135,136,137,138,139,150,151,152,157,158,159,182,183,184,187,188,147,178,1705
     * <br>联通号段: 130,131,132,155,156,185,186,145,176,1709
     * <br>电信号段: 133,153,180,181,189,177,1700
     */
	public static boolean isMobile(String string) {
		if(!hasLength(string)) {
			return false;
		}
		return mobilePattern.matcher(string).matches();
	}
	
	/**
	 * 手机号类型：-1-非法 0-未知 1-移动 2-联通 3-电信
	 */
	public static int getMobileType(String string) {
		int mobileLength = 11;
		int mobilePrefixLength = 3;
		if(!hasLength(string) || string.length()<mobilePrefixLength || string.length()>mobileLength) {
			return -1;
		}
		if(string.length() < mobileLength) {
			StringBuilder sb = new StringBuilder(string);
			for(int i=string.length();i<mobileLength;i++) {
				sb.append('1');
			}
			string = sb.toString();
		}
		if(!isMobile(string)) {
			return -1;
		}
		if(mobile1.matcher(string).matches()) {
			return 1;
		}
		if(mobile2.matcher(string).matches()) {
			return 2;
		}
		if(mobile3.matcher(string).matches()) {
			return 3;
		}
		return 0;
	}
	
	/** 是否电话号码 */
	public static boolean isTel(String tel) {
		if(!hasLength(tel)) {
			return false;
		}
		return telPattern.matcher(tel).matches();
	}
	
	/** 是否ip地址 */
	public static boolean isIp(String ip) {
		if(!hasLength(ip)) {
			return false;
		}
		return ipPattern.matcher(ip).matches();
	}
	
	/** 是否车牌号 */
	public static boolean isPlateNumber(String plateNumber) {
		if(!hasLength(plateNumber)) {
			return false;
		}
		return plateNumberPattern.matcher(plateNumber).matches();
	}
	
	/** 校验身份证号码是否合法
	 *  <li>idNumber=6位地址码+8位生日+3位序号+1位校验（15位身份证号年份19无校验位）
	 *  <li>地址码按GB/T2260规定到县
	 *  <li>生日年份在1900至今年之间
	 *  <li>校验位Y=sum(Ai*Wi)，Y=Vi[Y % 11]，Y=idNumber[17]
	 */
	public static boolean isIdNumber(String idNumber) {
		int idLength1 = 15, idLength2 = 18, idLength = idNumber==null?0:idNumber.length();
		boolean idType1 = idLength==idLength1, idType2 = idLength==idLength2;
		if(idNumber==null || (!idType1 && !idType2)) {
			return false;
		}
		
		final char[] cs = idNumber.toUpperCase().toCharArray();
        //校验位数 Y = sum(Ai*Wi) Wi: 7 9 10 5 8 4 2 1 6 3 7 9 10 5 8 4 2
        int power = 0;
        for(int i=0; i<cs.length; i++){
            if(i==cs.length-1 && cs[i] == 'X')
			 {
				break;//最后一位可以 是X或x
			}
            if(cs[i]<'0' || cs[i]>'9') {
				return false;
			}
            if(i < cs.length -1){
                power += (cs[i] - '0') * wi[i];
            }
        }
         
        //校验区位码
        String area = idNumber.substring(0,2);
        if(ArrayUtils.indexOf(areas, area)==-1){
            return false;
        }
        
        //校验年份
        String year = idType1 ? "19" + idNumber.substring(6,8) : idNumber.substring(6, 10);
         
        final int iyear = Integer.parseInt(year);
        int low = 1900;
		if(iyear < low || iyear > Calendar.getInstance().get(Calendar.YEAR))
		 {
        	//1900年的PASS，超过今年的PASS
			return false;
		}
         
        //校验月份
		int monthHigh = 12;
        String month = idType1 ? idNumber.substring(8, 10) : idNumber.substring(10,12);
        final int imonth = Integer.parseInt(month);
        if(imonth <1 || imonth > monthHigh){
            return false;
        }
         
        //校验天数      
        String day = idType1 ? idNumber.substring(10, 12) : idNumber.substring(12, 14);
        final int iday = Integer.parseInt(day);
        int dayHigh = 31;
		if(iday < 1 || iday > dayHigh) {
			return false;
		}       
         
        //校验"校验码"
        if(idType1) {
			return true;
		}
        //Y = mod(S, 11)
        //Y: 0 1 2 3 4 5 6 7 8 9 10 校验码: 1 0 X 9 8 7 6 5 4 3 2
        return cs[cs.length -1] == vi[power % 11];
	}
	
	/** 计算证件号校验位 */
	public static char checkIdNumber(String idNumber) {
		if(idNumber==null || idNumber.length()!=17) {
			return 0;
		}
		
		final char[] cs = idNumber.toUpperCase().toCharArray();
		//校验位数 Y = sum(Ai*Wi) Wi: 7 9 10 5 8 4 2 1 6 3 7 9 10 5 8 4 2
		int power = 0;
		for(int i=0; i<cs.length; i++){
			if(cs[i]<'0' || cs[i]>'9') {
				return 0;
			}
			if(i < cs.length){
				power += (cs[i] - '0') * wi[i];
			}
		}
		
		//校验区位码
		String area = idNumber.substring(0,2);
		if(ArrayUtils.indexOf(areas, area)==-1){
			return 0;
		}
		
		//校验年份
		String year = idNumber.substring(6, 10);
		
		final int iyear = Integer.parseInt(year);
		int low = 1900;
		if(iyear < low || iyear > Calendar.getInstance().get(Calendar.YEAR))
		{
			//1900年的PASS，超过今年的PASS
			return 0;
		}
		
		//校验月份
		int monthHigh = 12;
		String month = idNumber.substring(10,12);
		final int imonth = Integer.parseInt(month);
		if(imonth <1 || imonth > monthHigh){
			return 0;
		}
		
		//校验天数      
		String day = idNumber.substring(12, 14);
		final int iday = Integer.parseInt(day);
		int dayHigh = 31;
		if(iday < 1 || iday > dayHigh) {
			return 0;
		}       
		//Y = mod(S, 11)
		//Y: 0 1 2 3 4 5 6 7 8 9 10 校验码: 1 0 X 9 8 7 6 5 4 3 2
		return (char)vi[power % 11];
	}
	
	/** 组织机构代码证 */
	public static boolean isOrganizationCode(String organizationCode) {
		int organLength1 = 9, organLength2 = 18, organLength = organizationCode==null?0:organizationCode.length();
		boolean organType1 = organLength==organLength1, organType2 = organLength==organLength2;
		if(organType1==false && organType2==false) {
			return false;
		}
		if(organType2) {
			return isSccNumber(organizationCode);
		}
		//最后一个数字是校验码
		int c9 = 0, check=organizationCode.charAt(organLength-1)-'0';
		for(int i=0;i<organLength;i++) {
			char c = organizationCode.charAt(i);
			if(i<8) {
				if(!CharUtils.isAsciiNumeric(c) && !CharUtils.isAsciiAlphaUpper(c))
				 {
					//前8位只能是数字或大写字母
					return false;
				}
				c9 += (c-'0')*orgWi[i];
			}else {
				if(c!='-' && !CharUtils.isAsciiNumeric(c)) {
					return false;
				}
			}
		}
		//1 2 3 4 5 6 7 8 9 X 0
		int eleven = 11, ten = 10;
		c9 = eleven-c9%eleven;
		if(c9==eleven) {
			c9=0;
		} else {
			if(c9==ten) {
				c9='X'-'0';
			}
		}
		return c9==check;
	}
	
	/** 税务登记号码 */
	public static boolean isTaxRegistrationNo(String taxRegistrationNo) {
		int taxLength1 = 15, taxLength2 = 18, taxLength = taxRegistrationNo==null?0:taxRegistrationNo.length();
		boolean taxType1 = taxLength==taxLength1, taxType2 = taxLength==taxLength2;
		if(taxType1==false && taxType2==false) {
			return false;
		}
		if(taxType2) {
			return isSccNumber(taxRegistrationNo);
		}
		int pos = 6;
		if(taxType1==false || !isNumbers(taxRegistrationNo.substring(0, pos))) {
			return false;
		}
		if(!isOrganizationCode(taxRegistrationNo.substring(pos))) {
			//6位行政区划码+组织机构代码
			return false;
		}
		return true;
	}
	
	/** 纯数字 */
	public static boolean isNumbers(String numbers) {
		if(!hasLength(numbers)) {
			return false;
		}
		return getPattern("\\d+").matcher(numbers).matches();
	}
	/** 判断是否数字（包括全角数字） */
	public static boolean isDigit(char c) {
		char left = '\uFF00';
		char right = '\uFF5F';
		if (c > left && c < right) {
			c = (char) (c - 65248);
		}
		return c>='0' && c<='9';
	}
	/** 字母和数字混合 */
	public static boolean isAlphaNumbers(String alphaNumbers) {
		if(!hasLength(alphaNumbers)) {
			return false;
		}
		return getPattern("[0-9a-zA-Z]+").matcher(alphaNumbers).matches();
	}
	/** 小数 */
	public static boolean isDecimal(String decimal) {
		if(!hasLength(decimal)) {
			return false;
		}
		return getPattern("-?\\d+(\\.\\d+)?").matcher(decimal).matches();
	}
	/** 金额 */
	public static boolean isMoney(String numbers) {
		if(!hasLength(numbers)) {
			return false;
		}
		return getPattern("-?\\d+(\\.\\d{1,2})?").matcher(numbers).matches();
	}
	
	/** 银行卡号 */
	public static boolean isBankCardNumber(String bankCard) {
		boolean isBadNumber = !isNumbers(bankCard) || (bankCard.length()!=16 && bankCard.length()!=19);
		if(isBadNumber) {
			//16或19位
			return false;
		}
		char[] chars=bankCard.toCharArray();
		int len = chars.length, checkSum = 0, check = chars[len-1]-'0', noCheck = 2;
		//去掉最后一位校验码
		for(int i=len-noCheck;i>=0;i--) {
			int n = chars[i]-'0';
			int j = len-i-1;
			//从右向左，奇数位*2，个位+十位
			if(j%2==1) {
				n *= 2;
				checkSum += n/10;
				checkSum += n%10;
			//偶数位+
			}else {
				checkSum += n;
			}
		}
		//+校验位，被10整除
		checkSum += check;
		return checkSum % 10 ==0;
	}
	
	/** 遮掉部分字符，reverse=true时显示部分字符，len<0时从右边开始数 */
	public static String mask(String string, char c, int len, boolean reverse) {
		if(!hasLength(string)) {
			return string;
		}
		StringBuilder sb = new StringBuilder();
		int length = string.length();
		for(int i=0;i<length;i++) {
			if(len>0 && i<len) {
				sb.append(reverse ? string.charAt(i) : c);
			} else if(len<0 && i-length>=len) {
				sb.append(reverse ? string.charAt(i) : c);
			} else {
				sb.append(reverse ? c : string.charAt(i));
			}
		}
		return sb.toString();
	}
	
	/** 是否包含数组中的某个 */
	public static boolean containsOneOf(String string, String... ones) {
		if(!hasLength(string)) {
			return false;
		}
		if(ones!=null && ones.length>0) {
			for(String one : ones) {
				if(string.contains(one)) {
					return true;
				}
			}
		}
		return false;
	}
	
	/** 是否包含数组中的某个 */
	public static boolean containsOneOfIgnoreCase(String string, String... ones) {
		if(!hasLength(string)) {
			return false;
		}
		string=string.toLowerCase();
		if(ones!=null && ones.length>0) {
			for(String one : ones) {
				if(string.contains(one.toLowerCase())) {
					return true;
				}
			}
		}
		return false;
	}
	
	/** 转换字符串为参数映射 */
	public static Map<String, String> params(String... param){
		Map<String, String> params = new HashMap<>(2);
		if(param!=null && param.length>1) {
			int step = 2;
			for(int idx = 0, l=param.length-1; idx < l; idx+=step) {
				params.put(param[idx], param[idx+1]);
			}
		}
		return params;
	}
	
	/** 替换{var}参数变量 */
	public static String replace(String value, Map<String, String> params) {
		if(value == null) {
			return null;
		}
		if(params == null || params.size() == 0) {
			return value;
		}
		StringBuilder b = new StringBuilder();
		Matcher matcher = tagPattern.matcher(value);
		int idx = 0;
		while(matcher.find()) {
			int start = matcher.start();
			b.append(value.substring(idx, start));
			idx = matcher.end();
			
			String k = matcher.group(1);
			String v = params.get(k);
			b.append(v!=null ? v : matcher.group(0));
		}
		b.append(value.substring(idx));
		return b.toString();
	}
	
	public static String join(Collection<?> values, String front, String back, String seperator) {
		if(values==null || values.size()==0) {
			return null;
		}
		StringBuilder join = new StringBuilder();
		for(Object obj:values) {
			if(front!=null) {
				join.append(front);
			}
			if(obj!=null) {
				join.append(obj.toString());
			}
			if(back!=null) {
				join.append(back);
			}
			if(seperator!=null) {
				join.append(seperator);
			}
		}
		if(seperator!=null) {
			join.delete(join.length()-seperator.length(), join.length());
		}
		return join.toString();
	}
	
	/** 去掉sql参数里的特殊字符 */
	public static String sqlParam(String sqlParam) {
		return isBlank(sqlParam) ? "" : sqlParam.replaceAll("([;]+|(--)+)", "").replace("'", "\\'");
	}
	
	/** 解析颜色，支持oxffffff和(r,g,b)两种格式 */
	public static Color getColor(String color) {
		if(isBlank(color)) {
			return null;
		}
		color = color.toLowerCase();
		String colorRegex = "(0x)?([0-9a-f]+)";
		if(color.matches(colorRegex)) {
			return new Color(Integer.parseInt(color.startsWith("0x")?color.substring(2):color, 16));
		}
		Matcher matcher = colorPattern.matcher(color);
		if(!matcher.matches()) {
			return null;
		}
		return new Color(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
	}
	
	/** 限制字节数，例如微信公众号恢复时有长度限制，超长时无法回复消息 */
	public static String limited(String text, Charset charset, int bytes) {
		if(text==null || text.getBytes(charset).length < bytes) {
			return text;
		}
		char[] chars = text.toCharArray();
		CharBuffer cb = CharBuffer.allocate(1);
		ByteBuffer buf = ByteBuffer.allocate(bytes);
		int cnt = 0;
		for (int i = 0; i < chars.length; i++) {
			char c = chars[i];
			cb.put(c);
			cb.flip();
			ByteBuffer bb = charset.encode(cb);
			cnt += bb.array().length;
			if(cnt > bytes) {
				break;
			}
			buf.put(bb);
			cb.clear();
	    }
		return new String(buf.array(), charset);
	}
	
	/**
	 * https://blog.csdn.net/xiaoyu411502/article/details/48374745
	 * @return 可以将表情字符转码成mysql的utf8编码支持的字符串
	 */
	public static String toMysqlUtf8(String str) {
		if(!hasLength(str)) {
			return str;
		}
		StringBuilder sb = new StringBuilder();
		byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
		URLCodec urlCodec = new URLCodec();
		for(int i=0; i<bytes.length; i++) {
			byte b = bytes[i];
			if(CharUtils.isAscii((char)b)){
				sb.append(new String(bytes, i, 1, StandardCharsets.UTF_8));
			}else if((b & 0xE0) == 0xC0) {
				sb.append(new String(bytes, i++, 2, StandardCharsets.UTF_8));
			}else if((b & 0xF0) == 0xE0) {
				sb.append(new String(bytes, i, 3, StandardCharsets.UTF_8));
				i += 2;
			}else if((b & 0xF8) == 0xF0) {
				String str1 = new String(bytes, i, 4, StandardCharsets.UTF_8);
				try{
					sb.append(urlCodec.encode(str1, StandardCharsets.UTF_8.name()));
				}catch(Exception e) {
					log.warn("fail to encode str: {}, ex: {}", str1, e.getMessage());
				}
				i += 3;
			}			
		}
		return sb.toString();
	}
	
	/**
	 * 解码toMysqlUtf8编码的字符串
	 */
	public static String fromMysqlUtf8(String str) {
		if(!hasLength(str)) {
			return str;
		}
		try{
			return new URLCodec().decode(str, StandardCharsets.UTF_8.name());
		}catch(Exception ignore) {
			return str;
		}
	}

	private static Random randomGenerator = new Random(System.currentTimeMillis());
	private static char[]	hexChar	= { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
	private static char[] numbersAndLetters = ("0123456789abcdefghijklmnopqrstuvwxyz" + "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ").toCharArray();
	private static char[] QUOTE_ENCODE = "&quot;".toCharArray();
	private static char[] AMP_ENCODE = "&amp;".toCharArray();
	private static char[] LT_ENCODE = "&lt;".toCharArray();
	private static char[] GT_ENCODE = "&gt;".toCharArray();
	private static String urlPatternString = "^([hH][tT][tT][pP]([sS]?)|[fF][tT][pP]|[fF][iI][lL][eE]):\\/\\/(\\S+\\.)+\\S{2,}$";
	private static String emailPatternString = "\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*";
	private static String mobilePatternString = "^1(3[0-9]|4[5-9]|5[0-35-9]|6[2567]|7[0-9]|8[0-9]|9[1389])[0-9]{8}$";
	private static Pattern tagPattern = Pattern.compile("\\{(\\w+)\\}");
	private static Pattern urlPattern = Pattern.compile(urlPatternString, Pattern.CASE_INSENSITIVE);
	private static Pattern emailPattern = Pattern.compile(emailPatternString, Pattern.CASE_INSENSITIVE);
	private static Pattern mobilePattern = Pattern.compile(mobilePatternString);
	private static Pattern mobile1 = Pattern.compile("^1(3[5-9]|4[78]|5[0-27-9]|65|7[28]|8[2-478]|98)\\d{8}$|^(134[0-8]|170[356])\\d{7}$"),
			mobile2 = Pattern.compile("^1(3[012]|4[56]|5[56]|6[67]|7[156]|8[56])\\d{8}$|^170[47-9]\\d{7}$"),
			mobile3 = Pattern.compile("^1(33|49|53|62|7[347]|8[019]|9[139])\\d{8}$|^(1349|170[0-2])\\d{7}$");
	private static Pattern telPattern = Pattern.compile("(\\d{3,4}-?)?\\d{7,8}");
	private static Pattern ipPattern = Pattern.compile("((\\d|[1-9]\\d|[1]\\d{2}|[2][0-4]\\d|[2][5][0-5])\\.){3}(\\d|[1-9]\\d|[1]\\d{2}|[2][0-4]\\d|[2][5][0-5])");
	private static Pattern plateNumberPattern = Pattern.compile("([\\u4E00-\\u9FA5][a-zA-Z]-?[a-zA-Z0-9]{5})|(WJ\\d{2}-?(\\d{5}|[\\u4E00-\\u9FA5]\\d{4}))");
	private static Pattern colorPattern = Pattern.compile("(\\d+),(\\d+),(\\d+)");
	private static Map<String, Pattern> patterns = new HashMap<>();
	private static String[] areas={"11","12","13","14","15","21","22","23","31","32","33","34","35","36","37","41","42","43","44","45","46","50","51","52","53","54","61","62","63","64","65","71","81","82","91"};
	private static int[] vi = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};
	private static int[] wi = { 7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
	/** 组织机构代码证加权因子 */
	private static int[] orgWi = {3, 7, 9, 10, 5, 8, 4, 2};
	private static String sccPatternString = "^([0-9ABCDEFGHJKLMNPQRTUWXY]{2})(\\d{6})([0-9ABCDEFGHJKLMNPQRTUWXY]{9})([0-9ABCDEFGHJKLMNPQRTUWXY])$";
	private static Pattern sccPattern = Pattern.compile(sccPatternString);
	private static String sccwf = "0123456789ABCDEFGHJKLMNPQRTUWXY";
	/** 三证合一加权因子 */
	private static int[] sccwi = {1, 3, 9, 27, 19, 26, 16, 17, 20, 29, 25, 13, 8, 24, 10, 30, 28};
}
