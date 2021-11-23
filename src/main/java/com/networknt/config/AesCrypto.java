package com.networknt.config;

import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import com.networknt.decrypt.Decryptor;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;

import cn.hutool.core.util.StrUtil;

public class AesCrypto implements Decryptor {
    private static final String RANDOM_IV_CONFIG = "config.random_iv";
    
    /** 加密时是否随机填充iv值，否时与config.yml里的示例保持一致 */
    private static final boolean RANDOM_IV = "true"
            .equalsIgnoreCase(System.getProperty(RANDOM_IV_CONFIG, System.getenv(RANDOM_IV_CONFIG)));

    private static final int ITERATIONS = 65536;

    private static final String STRING_ENCODING = "UTF-8";

    /**
     * If we user Key size of 256 we will get java.security.InvalidKeyException:
     * Illegal key size or default parameters , Unless we configure Java
     * Cryptography Extension 128
     */
    private static final int KEY_SIZE = 128;

    private static final byte[] SALT = { (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0,
            (byte) 0x0, (byte) 0x0 };

    private SecretKeySpec secret;

    private Cipher cipher;

    private Base64.Decoder base64Decoder = Base64.getDecoder();

    private Base64.Encoder base64Encoder = Base64.getEncoder();

    private final static String LIGHT_4J_CONFIG_PASSWORD = "light_4j_config_password";

    private final static char[] PASSWORD = StringUtils.defaultIfBlank(System.getenv(LIGHT_4J_CONFIG_PASSWORD), "light")
            .toCharArray();

    private final static byte[] IV = new byte[] { -45, -57, -105, -125, -44, -26, 43, -83, 56, -5, 13, 58, 69, -96, 101,
            -80 };

    public AesCrypto() {
        try {
            /* Derive the key, given password and salt. */
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            KeySpec spec;

            spec = new PBEKeySpec(PASSWORD, SALT, ITERATIONS, KEY_SIZE);
            SecretKey tmp = factory.generateSecret(spec);
            secret = new SecretKeySpec(tmp.getEncoded(), "AES");

            // CBC = Cipher Block chaining
            // PKCS5Padding Indicates that the keys are padded
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        } catch (Exception e) {
            throw new RuntimeException("Unable to initialize " + this.getClass().getName(), e);
        }
    }

    @Override
    public String decrypt(String input) {
        if (!input.startsWith(CRYPT_PREFIX)) {
            throw new RuntimeException("Unable to decrypt, input string does not start with 'CRYPT'.");
        }

        try {
            String encodedValue = input.substring(6, input.length());
            byte[] data = base64Decoder.decode(encodedValue);
            int keylen = KEY_SIZE / 8;
            byte[] iv = new byte[keylen];
            System.arraycopy(data, 0, iv, 0, keylen);// IV=Arrays.toString(iv)
            cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));
            return new String(cipher.doFinal(data, keylen, data.length - keylen), STRING_ENCODING);
        } catch (Exception e) {
            throw new RuntimeException("Unable to decrypt because the decrypted password is incorrect.", e);
        }
    }

    public String encrypt(String input) {
        try {
            byte[] iv = RANDOM_IV ? RandomUtils.nextBytes(KEY_SIZE / 8) : IV;
            cipher.init(Cipher.ENCRYPT_MODE, secret, new IvParameterSpec(iv));
            byte[] bs = cipher.doFinal(input.getBytes(STRING_ENCODING));
            bs = ArrayUtils.addAll(iv, bs);
            return CRYPT_PREFIX + StrUtil.COLON + base64Encoder.encodeToString(bs);
        } catch (Exception e) {
            throw new RuntimeException("Unable to encrypt because " + e.getMessage(), e);
        }
    }

    public static void main(String[] args) {
        if (args != null && args.length == 1) {
            String input = args[0];
            AesCrypto aes = new AesCrypto();
            if (input.startsWith(CRYPT_PREFIX)) {
                System.out.println(aes.decrypt(input));
            } else {
                System.out.println(aes.encrypt(input));
            }
        }
    }
}
