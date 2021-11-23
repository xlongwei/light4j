package com.xlongwei.light4j;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import com.networknt.config.AesCrypto;
import com.networknt.config.Config;

import org.junit.Test;

public class AesCryptoTest {
    @Test
    public void test() throws Exception {
        AesCrypto aes = new AesCrypto();
        String e = aes.encrypt("password");
        System.out.println(e);
        String d = aes.decrypt(e);
        System.out.println(d);
    }

    @Test
    public void testConfig() throws Exception {
        Map<String, Object> map = Config.getInstance().getJsonMapConfig("secret");
        assertEquals("password", map.get("serverKeystorePass"));
    }
}
