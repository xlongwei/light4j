package com.xlongwei.light4j;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import com.networknt.config.AesCrypto;
import com.networknt.config.Config;
import com.xlongwei.light4j.util.DesUtil;

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

    @Test
    public void pwcheck() throws Exception {
        assertEquals(0, DesUtil.pwcheck(null).get("score"));
        assertEquals(98, DesUtil.pwcheck("OOTNia!x5#gn").get("score"));
        assertEquals(4, DesUtil.pwcheck("123456").get("score"));
        assertEquals(7, DesUtil.pwcheck("admin").get("score"));
        assertEquals(8, DesUtil.pwcheck("qwerty").get("score"));
        assertEquals(80, DesUtil.pwcheck("Aa123456").get("score"));

        DesUtil.pwcheck("qweasd").entrySet().forEach(System.out::println);
    }
}
