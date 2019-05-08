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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;

/**
 * DES对称加密工具
 */
public final class DesUtil {
	private static Logger 				logger			= LoggerFactory.getLogger(DesUtil.class);
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
			logger.warn("fail to get desUtil instance: "+password, e);
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
			logger.warn("fail to encrypt bytes: "+bytes.length, e);
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
			logger.warn("fail to decrypt bytes: "+bytes.length, e);
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
			logger.warn("fail to encryptHex str: "+str, e);
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
			logger.warn("fail to decryptHex str: "+str, e);
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
			logger.warn("fail to encrypt InputStream", e);
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
			int bufRead = 1032;//1024字节加密后得1032字节
			byte[] buf = new byte[bufRead];
			while ((bufRead = cin.read(buf)) >= 0) {
				out.write(buf, 0, bufRead);
			}
			return true;
		}catch (IOException e) {
			logger.warn("fail to decrypt InputStream", e);
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
			logger.info("can't encrypt bad file or directory");
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
			logger.warn("fail to encrypt file", e);
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
			logger.info("can't decrypt bad file or directory");
			return false;
		}
		
		FileInputStream fis = null;
		FileOutputStream fos = null;
		try{
			fis = new FileInputStream(src);
			fos = new FileOutputStream(dst);
			int bufRead = 1032;//1024字节加密后得1032字节
			byte[] buf = new byte[bufRead];
			while ((bufRead = fis.read(buf)) >= 0) {
				byte[] doFinal = desUtil.dcipher.doFinal(buf, 0, bufRead);
				fos.write(doFinal);
			}
			return true;
		}catch(Exception e) {
			logger.warn("fail to decrypt file");
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
			logger.warn("fail to encrypt serializable", e);
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
			logger.warn("fail to decrypt serializable", e);
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
			return Base64.encodeBase64String(enc);
		}catch (Exception e) {
			logger.warn("fail to encrypt str: "+str, e);
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
			logger.warn("fail to decrypt str: "+str, e);
			return null;
		}
	}
}