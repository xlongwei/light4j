package com.xlongwei.light4j.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.binary.StringUtils;
import org.xnio.IoUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * DES对称加密工具
 * @author xlongwei
 */
@Slf4j
public final class DesUtil {
	private Cipher				ecipher;
	private Cipher				dcipher;
	private static Map<String, Cipher>	eciphers		= new HashMap<String, Cipher>();
	private static Map<String, Cipher>	dciphers		= new HashMap<String, Cipher>();
	private static byte[]				salt			= { (byte) 0xF1, (byte) 0x9B, (byte) 0xC8, (byte) 0x50, (byte) 0xD2, (byte) 0x64, (byte) 0xE3, (byte) 0xA7 };
	private static int					iterationCount	= 19;
	private static DesUtil				desUtil			= getInstance(null);
	
	public static DesUtil getInstance(String password) {
		try {
			DesUtil desUtil = new DesUtil();
			String pwdkey = password==null ? "" : password;
			desUtil.ecipher = eciphers.get(pwdkey);
			desUtil.dcipher = dciphers.get(pwdkey);
			if(desUtil.ecipher==null || desUtil.dcipher==null) {
				KeySpec keySpec = new PBEKeySpec(pwdkey.toCharArray(), salt, iterationCount);
				SecretKey key = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(keySpec);
				AlgorithmParameterSpec paramSpec = new PBEParameterSpec(salt, iterationCount);
				desUtil.ecipher = Cipher.getInstance("PBEWithMD5AndDES/CBC/PKCS5Padding");
				desUtil.ecipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);
				eciphers.put(pwdkey, desUtil.ecipher);
				
				desUtil.dcipher = Cipher.getInstance("PBEWithMD5AndDES/CBC/PKCS5Padding");
				desUtil.dcipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
				dciphers.put(pwdkey, desUtil.dcipher);
			}
			return desUtil;
		}catch (Exception e) {
			log.warn("fail to get desUtil instance: "+password, e);
		}
		return desUtil;
	}
	
	/**
	 * 加密字节数组
	 */
	public static byte[] encrypt(byte[] bytes) {
		try{
			return desUtil.ecipher.doFinal(bytes);
		}catch(Exception e) {
			log.warn("fail to encrypt bytes: "+bytes.length, e);
			return null;
		}
	}
	
	/**
	 * 解密字节数组
	 */
	public static byte[] decrypt(byte[] bytes) {
		try{
			return desUtil.dcipher.doFinal(bytes);
		}catch(Exception e) {
			log.warn("fail to decrypt bytes: "+bytes.length, e);
			return null;
		}
	}
	
	/**
	 * 加密为utf-8字符串
	 */
	public static String encrypt(String str) {
		return desUtil.doEncrypt(str);
	}
	
	/**
	 * 解密utf-8字符串
	 */
	public static String decrypt(String str) {
		return desUtil.doDecrypt(str);
	}
	
	/**
	 * 加密为hex字符串
	 */
	public static String encryptHex(String str) {
		try {
			byte[] utf8 = StringUtils.getBytesUtf8(str);
			byte[] enc = desUtil.ecipher.doFinal(utf8);
			return Hex.encodeHexString(enc);
		}catch (Exception e) {
			log.warn("fail to encryptHex str: "+str, e);
			return null;
		}
	}
	
	/**
	 * 解密hex字符串
	 */
	public static String decryptHex(String str) {
		try {
			byte[] dec = Hex.decodeHex(str.toCharArray());
			byte[] utf8 = desUtil.dcipher.doFinal(dec);
			return StringUtils.newStringUtf8(utf8);
		}catch (Exception e) {
			log.warn("fail to decryptHex str: "+str, e);
			return null;
		}
	}
	
	/**
	 * 加密输入流至输出流
	 */
	public static boolean encrypt(InputStream in, OutputStream out) {
		try(CipherOutputStream cout = new CipherOutputStream(out, desUtil.ecipher)) {
			int bufRead = 1024;
			byte[] buf = new byte[bufRead];
			while ((bufRead = in.read(buf)) >= 0) {
				cout.write(buf, 0, bufRead);
			}
			return true;
		}catch (IOException e) {
			log.warn("fail to encrypt InputStream", e);
			return false;
		}finally {
			IoUtils.safeClose(in);
			IoUtils.safeClose(out);
		}
	}
	
	/**
	 * 解密输入流至输出流
	 */
	public static boolean decrypt(InputStream in, OutputStream out) {
		try(CipherInputStream cin = new CipherInputStream(in, desUtil.dcipher)) {
			//1024字节加密后得1032字节
			int bufRead = 1032;
			byte[] buf = new byte[bufRead];
			while ((bufRead = cin.read(buf)) >= 0) {
				out.write(buf, 0, bufRead);
			}
			return true;
		}catch (IOException e) {
			log.warn("fail to decrypt InputStream", e);
			return false;
		}finally {
			IoUtils.safeClose(in);
			IoUtils.safeClose(out);
		}
	}
	
	/**
	 * 加密文件
	 */
	public static boolean encrypt(File src, File dst) {
		if(!src.exists() || src.isDirectory() || dst.exists() || dst.isDirectory()) {
			log.info("can't encrypt bad file or directory");
			return false;
		}
		
		FileInputStream fis = null;
		FileOutputStream fos = null;
		try{
			fis = new FileInputStream(src);
			fos = new FileOutputStream(dst);
			int bufRead = 1024;
			byte[] buf = new byte[bufRead];
			while ((bufRead = fis.read(buf)) >= 0) {
				byte[] doFinal = desUtil.ecipher.doFinal(buf, 0, bufRead);
				fos.write(doFinal);
			}
			return true;
		}catch(Exception e) {
			log.warn("fail to encrypt file", e);
			return false;
		}finally {
			IoUtils.safeClose(fis);
			IoUtils.safeClose(fos);
		}
	}
	
	/**
	 * decrypt file.
	 */
	public static boolean decrypt(File src, File dst) {
		if(!src.exists() || src.isDirectory() || dst.exists() || dst.isDirectory()) {
			log.info("can't decrypt bad file or directory");
			return false;
		}
		
		FileInputStream fis = null;
		FileOutputStream fos = null;
		try{
			fis = new FileInputStream(src);
			fos = new FileOutputStream(dst);
			//1024字节加密后得1032字节
			int bufRead = 1032;
			byte[] buf = new byte[bufRead];
			while ((bufRead = fis.read(buf)) >= 0) {
				byte[] doFinal = desUtil.dcipher.doFinal(buf, 0, bufRead);
				fos.write(doFinal);
			}
			return true;
		}catch(Exception e) {
			log.warn("fail to decrypt file");
			return false;
		}finally {
			IoUtils.safeClose(fis);
			IoUtils.safeClose(fos);
		}
	}
	
	/**
	 * 设置密码
	 */
	public static void setPassword(String password) {
		desUtil = getInstance(password);
	}
	
	/**
	 * 移除密码
	 */
	public static void removePassword(String password) {
		if (!"".equals(password)) {
			eciphers.remove(password);
			dciphers.remove(password);
		}
		
		desUtil.ecipher = eciphers.get("");
		desUtil.dcipher = dciphers.get("");
	}
	
	/**
	 * 加密对象
	 */
	public static SealedObject encrypt(Serializable serializable) {
		try {
			return new SealedObject(serializable, desUtil.ecipher);
		}catch (Exception e) {
			log.warn("fail to encrypt serializable", e);
			return null;
		}
	}
	
	/**
	 * 解密对象
	 */
	public static Serializable decrypt(SealedObject sealedObject) {
		try {
			return (Serializable) sealedObject.getObject(desUtil.dcipher);
		}catch (Exception e) {
			log.warn("fail to decrypt serializable", e);
			return null;
		}
	}
	
	
	/**
	 * 加密为utf-8字符串
	 */
	public String doEncrypt(String str) {
		try {
			byte[] utf8 = StringUtils.getBytesUtf8(str);
			byte[] enc = ecipher.doFinal(utf8);
			return Base64.encodeBase64URLSafeString(enc);
		}catch (Exception e) {
			log.warn("fail to encrypt str: "+str, e);
			return null;
		}
	}
	
	/**
	 * 解密utf-8字符串
	 */
	public String doDecrypt(String str) {
		try {
			byte[] dec = Base64.decodeBase64(str);
			byte[] utf8 = dcipher.doFinal(dec);
			return StringUtils.newStringUtf8(utf8);
		}catch (Exception e) {
			log.warn("fail to decrypt str: "+str, e);
			return null;
		}
	}

	/**
	 * 密码强度检测
	 * @param pwd
	 * @return {score:0-100}
     * @see https://blog.csdn.net/u010156024/article/details/45673581
     * @see http://www.passwordmeter.com/
	 */
	public static Map<String, Object> pwcheck(String pwd) {
		int nScore=0, nLength=0, nAlphaUC=0, nAlphaLC=0, nNumber=0, nSymbol=0, nMidChar=0, nRequirements=0, nAlphasOnly=0, nNumbersOnly=0, nUnqChar=0, nRepChar=0, nConsecAlphaUC=0, nConsecAlphaLC=0, nConsecNumber=0, nSeqAlpha=0, nSeqNumber=0, nSeqSymbol=0, nReqChar=0;
		double nRepInc=0;
		int nMultMidChar=2, nMultConsecAlphaUC=2, nMultConsecAlphaLC=2, nMultConsecNumber=2;
		int nMultSeqAlpha=3, nMultSeqNumber=3, nMultSeqSymbol=3;
		int nMultLength=4, nMultNumber=4;
		int nMultSymbol=6;
		int nTmpAlphaUC=-1, nTmpAlphaLC=-1, nTmpNumber=-1;
		int sAlphaUC=0, sAlphaLC=0, sNumber=0, sSymbol=0, sMidChar=0, sRequirements=0, sAlphasOnly=0, sNumbersOnly=0, sRepChar=0, sConsecAlphaUC=0, sConsecAlphaLC=0, sConsecNumber=0, sSeqAlpha=0, sSeqNumber=0, sSeqSymbol=0;
		String sAlphas = "abcdefghijklmnopqrstuvwxyz";
		String sNumerics = "01234567890";
		String sSymbols = ")!@#$%^&*()";
		int nMinPwdLen = 8;
		if (pwd!=null&&pwd.length()>0) {
			nScore = pwd.length() * nMultLength;
			nLength = pwd.length();
			String[] arrPwd = pwd.replaceAll("\\s", "").split("");
			int arrPwdLen = arrPwd.length;
			
			/* Loop through password to check for Symbol, Numeric, Lowercase and Uppercase pattern matches */
			for (int a=0; a < arrPwdLen; a++) {
				char charAt = arrPwd[a].charAt(0);
				if (Character.isUpperCase(charAt)) {
					if (nTmpAlphaUC>=0) { if ((nTmpAlphaUC + 1) == a) { nConsecAlphaUC++; } }
					nTmpAlphaUC = a;
					nAlphaUC++;
				}
				else if (Character.isLowerCase(charAt)) { 
					if (nTmpAlphaLC>=0) { if ((nTmpAlphaLC + 1) == a) { nConsecAlphaLC++; } }
					nTmpAlphaLC = a;
					nAlphaLC++;
				}
				else if (Character.isDigit(charAt)) { 
					if (a > 0 && a < (arrPwdLen - 1)) { nMidChar++; }
					if (nTmpNumber>=0) { if ((nTmpNumber + 1) == a) { nConsecNumber++; } }
					nTmpNumber = a;
					nNumber++;
				}
				else if (charAt!='_') { 
					if (a > 0 && a < (arrPwdLen - 1)) { nMidChar++; }
					nSymbol++;
				}
				/* Internal loop through password to check for repeat characters */
				boolean bCharExists = false;
				for (int b=0; b < arrPwdLen; b++) {
					if (charAt == arrPwd[b].charAt(0) && a != b) { /* repeat character exists */
						bCharExists = true;
						/* 
						Calculate icrement deduction based on proximity to identical characters
						Deduction is incremented each time a new match is discovered
						Deduction amount is based on total password length divided by the
						difference of distance between currently selected match
						*/
						nRepInc += Math.abs(((double)arrPwdLen)/(b-a));
					}
				}
				if (bCharExists) { 
					nRepChar++; 
					nUnqChar = arrPwdLen-nRepChar;
					nRepInc = (nUnqChar>0) ? (int)Math.ceil(nRepInc/nUnqChar) : (int)Math.ceil(nRepInc); 
				}
			}
			
			/* Check for sequential alpha string patterns (forward and reverse) */
			for (int s=0; s < 23; s++) {
				String sFwd = sAlphas.substring(s,(s+3));
				String sRev = new StringBuilder(sFwd).reverse().toString();
				if (pwd.toLowerCase().indexOf(sFwd) != -1 || pwd.toLowerCase().indexOf(sRev) != -1) { nSeqAlpha++; }
			}
			
			/* Check for sequential numeric string patterns (forward and reverse) */
			for (int s=0; s < 8; s++) {
				String sFwd = sNumerics.substring(s,(s+3));
				String sRev = new StringBuilder(sFwd).reverse().toString();
				if (pwd.toLowerCase().indexOf(sFwd) != -1 || pwd.toLowerCase().indexOf(sRev) != -1) { nSeqNumber++; }
			}
			
			/* Check for sequential symbol string patterns (forward and reverse) */
			for (int s=0; s < 8; s++) {
				String sFwd = sSymbols.substring(s,(s+3));
				String sRev = new StringBuilder(sFwd).reverse().toString();
				if (pwd.toLowerCase().indexOf(sFwd) != -1 || pwd.toLowerCase().indexOf(sRev) != -1) { nSeqSymbol++; }
			}
			
			if (nAlphaUC > 0 && nAlphaUC < nLength) {	
				nScore = (nScore + ((nLength - nAlphaUC) * 2));
				sAlphaUC = ((nLength - nAlphaUC) * 2); 
			}
			if (nAlphaLC > 0 && nAlphaLC < nLength) {	
				nScore = (nScore + ((nLength - nAlphaLC) * 2)); 
				sAlphaLC = ((nLength - nAlphaLC) * 2);
			}
			if (nNumber > 0 && nNumber < nLength) {	
				nScore = (nScore + (nNumber * nMultNumber));
				sNumber = (nNumber * nMultNumber);
			}
			if (nSymbol > 0) {	
				nScore = (nScore + (nSymbol * nMultSymbol));
				sSymbol = (nSymbol * nMultSymbol);
			}
			if (nMidChar > 0) {	
				nScore = (nScore + (nMidChar * nMultMidChar));
				sMidChar = (nMidChar * nMultMidChar);
			}
			
			/* Point deductions for poor practices */
			if ((nAlphaLC > 0 || nAlphaUC > 0) && nSymbol == 0 && nNumber == 0) {  // Only Letters
				nScore = (nScore - nLength);
				nAlphasOnly = nLength;
				sAlphasOnly = -1* nAlphasOnly;
			}
			if (nAlphaLC == 0 && nAlphaUC == 0 && nSymbol == 0 && nNumber > 0) {  // Only Numbers
				nScore = (nScore - nLength); 
				nNumbersOnly = nLength;
				sNumbersOnly = -1* nNumbersOnly;
			}
			if (nRepChar > 0) {  // Same character exists more than once
				nScore = (nScore - (int)nRepInc);
				sRepChar = -1* (int)nRepInc;
			}
			if (nConsecAlphaUC > 0) {  // Consecutive Uppercase Letters exist
				nScore = (nScore - (nConsecAlphaUC * nMultConsecAlphaUC)); 
				sConsecAlphaUC = -1*(nConsecAlphaUC * nMultConsecAlphaUC);
			}
			if (nConsecAlphaLC > 0) {  // Consecutive Lowercase Letters exist
				nScore = (nScore - (nConsecAlphaLC * nMultConsecAlphaLC)); 
				sConsecAlphaLC = -1*(nConsecAlphaLC * nMultConsecAlphaLC);
			}
			if (nConsecNumber > 0) {  // Consecutive Numbers exist
				nScore = (nScore - (nConsecNumber * nMultConsecNumber));  
				sConsecNumber = -1*(nConsecNumber * nMultConsecNumber);
			}
			if (nSeqAlpha > 0) {  // Sequential alpha strings exist (3 characters or more)
				nScore = (nScore - (nSeqAlpha * nMultSeqAlpha)); 
				sSeqAlpha = -1* (nSeqAlpha * nMultSeqAlpha);
			}
			if (nSeqNumber > 0) {  // Sequential numeric strings exist (3 characters or more)
				nScore = (nScore - (nSeqNumber * nMultSeqNumber)); 
				sSeqNumber = -1*(nSeqNumber * nMultSeqNumber);
			}
			if (nSeqSymbol > 0) {  // Sequential symbol strings exist (3 characters or more)
				nScore = nScore - (nSeqSymbol * nMultSeqSymbol); 
				sSeqSymbol = -1* nSeqSymbol * nMultSeqSymbol;
			}
			/* Determine if mandatory requirements have been met and set image indicators accordingly */
			int[] arrChars = new int[]{nLength,nAlphaUC,nAlphaLC,nNumber,nSymbol};
			String[] arrCharsIds = new String[]{"nLength","nAlphaUC","nAlphaLC","nNumber","nSymbol"};
			int arrCharsLen = arrChars.length;
			for (int c=0; c < arrCharsLen; c++) {
				int minVal = arrCharsIds[c].endsWith("nLength") ? nMinPwdLen-1 : 0;
				if (arrChars[c] == (minVal + 1)) { nReqChar++; }
				else if (arrChars[c] > (minVal + 1)) { nReqChar++; }
			}
			nRequirements = nReqChar;
			int nMinReqChars = nLength >= nMinPwdLen ? 3 : 4;
			if (nRequirements > nMinReqChars) {  // One or more required characters exist
				nScore = (nScore + (nRequirements * 2)); 
				sRequirements = (nRequirements * 2);
			}
		}
		nScore = Math.min(100, Math.max(0, nScore));
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("score", nScore);

		map.put("sLength", nLength*nMultLength);
		map.put("sAlphaUC", sAlphaUC);
		map.put("sAlphaLC", sAlphaLC);
		map.put("sNumber", sNumber);
		map.put("sSymbol", sSymbol);
		map.put("sMidChar", sMidChar);
		map.put("sRequirements", sRequirements);

		map.put("sAlphasOnly", sAlphasOnly);
		map.put("sNumbersOnly", sNumbersOnly);
		map.put("sRepChar", sRepChar);
		map.put("sConsecAlphaUC", sConsecAlphaUC);
		map.put("sConsecAlphaLC", sConsecAlphaLC);
		map.put("sConsecNumber", sConsecNumber);
		map.put("sSeqAlpha", sSeqAlpha);
		map.put("sSeqNumber", sSeqNumber);
		map.put("sSeqSymbol", sSeqSymbol);
		return map;
	}
}