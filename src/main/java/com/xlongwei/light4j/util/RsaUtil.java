package com.xlongwei.light4j.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Enumeration;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * <li>getPrivateKey+getPublicKey 从字节流或字符串获取密钥
 * <li>getKeyPair+getKeyString 生成密钥对并转换成字符串
 * <li>sign+verify 私钥签名公钥校验
 * <li>encrypt+decrypt 公钥加密私钥解密
 * <li>main 打印所有安全服务信息
 * 
 * @author Hongwei
 * @date 2015-05-06
 */
@Slf4j
public class RsaUtil {
	private static String keyStoreType = "PKCS12";
	private static String certificateFactoryAlgorithm = "X.509";
	private static String rsa = "RSA";
	private static String rsaSign = "SHA1withRSA";//MD5WITHRSA SHA512withRSA
	private static String rsaCipher = "RSA/None/PKCS1Padding";//RSA/None/NoPadding重复密文 RSA/None/PKCS1Padding不重复密文（有随机数填充）
	private static int keySize = 1024;//2048 为提高保密强度要求512以上，为支持加密解密要整除8，RSA比较慢建议对称加密数据+RSA加密密码或签名
	
	private static boolean bouncyCastleProviderAvailable = false;
	private static String classBouncyCastleProvider = "org.bouncycastle.jce.provider.BouncyCastleProvider";
	
	static {
		try {
			Class<?> clazz = Class.forName(classBouncyCastleProvider);
			Provider bouncyCastleProvider = (Provider)clazz.newInstance();
			Security.addProvider(bouncyCastleProvider);
			bouncyCastleProviderAvailable = true;
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
			log.warn("fail to Security.addProvider(BC): {}", e.getMessage());
		}
	}
	
	/**
	 * 读取私钥证书
	 */
	public static PrivateKey getPrivateKey(InputStream is, String pwd) {
		try {
			KeyStore ks = KeyStore.getInstance(keyStoreType);
			char[] ps = pwd!=null ? pwd.toCharArray() : null;
			ks.load(is, ps);
			Enumeration<String> as = ks.aliases();
			return (PrivateKey)ks.getKey(as.hasMoreElements() ? as.nextElement() : null, ps);
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException | UnrecoverableKeyException e) {
			log.warn("fail to getPrivateKey from InputStream: {}", e.getMessage());
		}
		return null;
	}
	
	/**
	 * 读取公钥证书
	 */
	public static PublicKey getPublicKey(InputStream is) {
		try {
			CertificateFactory cf = CertificateFactory.getInstance(certificateFactoryAlgorithm);
			Certificate c = cf.generateCertificate(is);
			return c.getPublicKey();
		} catch (CertificateException e) {
			log.warn("fail to getPublicKey from InputStream: {}", e.getMessage());
		}
		return null;
	}
	
	/**
	 * 加载私钥串
	 */
	public static PrivateKey getPrivateKey(String privateKey) {
		try {
			byte[] bs = Base64.decodeBase64(privateKey);
			EncodedKeySpec eks = new PKCS8EncodedKeySpec(bs);
			KeyFactory kf = KeyFactory.getInstance(rsa);
			return kf.generatePrivate(eks);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			log.warn("fail to getPrivateKey from String: {}", e.getMessage());
		}
		return null;
	}
	
	/**
	 * 加载公钥串
	 */
	public static PublicKey getPublicKey(String publicKey) {
		try {
			byte[] bs = Base64.decodeBase64(publicKey);
			KeyFactory kf = KeyFactory.getInstance(rsa);
			EncodedKeySpec eks = new X509EncodedKeySpec(bs);
			return kf.generatePublic(eks);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			log.warn("fail to getPublicKey from String: {}", e.getMessage());
		}
		return null;
	}
	
	/**
	 * 公钥私钥转字符串
	 */
	public static String getKeyString(Key key) {
		byte[] bs = key.getEncoded();
		return Base64.encodeBase64String(bs);
	}
	
	/**
	 * 生成非对称密钥对
	 */
	public static KeyPair getKeyPair() {
		try {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance(rsa);
			kpg.initialize(keySize);
			return kpg.generateKeyPair();
		} catch (NoSuchAlgorithmException e) {
			log.warn("fail to getKeyPair: {}", e.getMessage());
		}
		return null;
	}
	
	/** 校验公钥与私钥是否匹配 */
	public static boolean verify(String publicKey, String privateKey) {
		return verify(getPublicKey(publicKey), getPrivateKey(privateKey));
	}
	
	/** 校验公钥与私钥是否匹配 */
	public static boolean verify(PublicKey publicKey, PrivateKey privateKey) {
		String data = publicKey.toString()+privateKey.toString();
		String sign = RsaUtil.sign(privateKey, data);
		return RsaUtil.verify(publicKey, data, sign);
	}
	
	/**
	 * 私钥签名
	 */
	public static String sign(PrivateKey privateKey, String data) {
		try {
			Signature s = Signature.getInstance(rsaSign);
			s.initSign(privateKey);
			s.update(data.getBytes(Charsets.UTF_8));
			return Base64.encodeBase64String(s.sign());
		} catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
			log.warn("fail to sign: {}", e.getMessage());
		}
		return null;
	}
	
	/**
	 * 公钥校验签名
	 */
	public static boolean verify(PublicKey publicKey, String data, String signature) {
		try {
			Signature s = Signature.getInstance(rsaSign);
			s.initVerify(publicKey);
			s.update(data.getBytes(Charsets.UTF_8));
			return s.verify(Base64.decodeBase64(signature));
		} catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
			log.warn("fail to verify: {}", e.getMessage());
		}
		return false;
	}
	
	/**
	 * 公钥加密数据
	 */
	public static String encrypt(PublicKey publicKey, String data) {
		try {
			Cipher c = bouncyCastleProviderAvailable ? Cipher.getInstance(rsaCipher, "BC") : Cipher.getInstance(rsaCipher);
			c.init(Cipher.ENCRYPT_MODE, publicKey);
			byte[] bs = data.getBytes(Charsets.UTF_8);
			int bufferSize = keySize/8 - 11;//1024限制117字节 2048限制245字节
			byte[] buffer = new byte[bufferSize];
			int position = 0;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			do {
				int read = Math.min(bufferSize, bs.length-position);
				System.arraycopy(bs, position, buffer, 0, read);
				c.update(buffer, 0, read);
				byte[] cs = c.doFinal();//加密结果全是256字节
				baos.write(cs);
				position+=read;
			}while(position<bs.length);
			byte[] bss = baos.toByteArray();
			return Base64.encodeBase64String(bss);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchProviderException | IOException e) {
			log.warn("fail to encrypt: {}", e.getMessage());
		}
		return null;
	}
	
	/**
	 * 私钥解密数据
	 */
	public static String decrypt(PrivateKey privateKey, String data) {
		try {
			Cipher c = bouncyCastleProviderAvailable ? Cipher.getInstance(rsaCipher, "BC") : Cipher.getInstance(rsaCipher);
			c.init(Cipher.DECRYPT_MODE, privateKey);
			byte[] bs = Base64.decodeBase64(data);
			int bufferSize = keySize/8;//密文长度等于密钥长度
			byte[] buffer = new byte[bufferSize];
			int position = 0;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			do {
				int read = Math.min(bufferSize, bs.length-position);
				System.arraycopy(bs, position, buffer, 0, read);
				c.update(buffer, 0, read);
				byte[] cs = c.doFinal();
				baos.write(cs);
				position+=read;
			}while(position<bs.length);
			byte[] bss = baos.toByteArray();
			return StringUtils.newStringUtf8(bss);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchProviderException | IOException e) {
			log.warn("fail to decrypt: {}", e.getMessage());
		}
		return null;
	}
}
