package com.xlongwei.light4j.handler.service;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.ConfigUtil;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.NumberUtil;
import com.xlongwei.light4j.util.RedisConfig;
import com.xlongwei.light4j.util.StringUtil;
import com.xlongwei.light4j.util.TaskUtil;

import org.apache.commons.io.IOUtils;

import cn.hutool.core.map.MapUtil;
import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;

/**
 * mobile handler
 * 
 * @author xlongwei
 * @date 2019-10-30
 */
@Slf4j
public class MobileHandler extends AbstractHandler {

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        String mobile = HandlerUtil.getParam(exchange, "mobile");
        if (StringUtil.isNumbers(mobile)) {
            int type = StringUtil.getMobileType(mobile);
            Map<String, String> map = new HashMap<>(16);
            map.put("type", String.valueOf(type));
            map.put("valid", String.valueOf(type >= 0 && mobile.length() == 11));
            Map<String, String> searchToMap = searchToMap(mobile);
            if (MapUtil.isNotEmpty(searchToMap)) {
                map.putAll(searchToMap);
            }
            HandlerUtil.setResp(exchange, map);
        } else if ("reload".equals(mobile)) {
            String memory = HandlerUtil.getParam(exchange, "memory");
            Boolean search = "true".equals(memory) ? Boolean.TRUE : ("false".equals(memory) ? Boolean.FALSE : null);
            if (search != null && search.booleanValue() != memorySearch) {
                memorySearch = search.booleanValue();
                reload();
            }
            HandlerUtil.setResp(exchange, Collections.singletonMap("memory", memorySearch));
        }
    }

    public static final String[] NUMBER_TYPE = { null, "移动", "联通", "电信", "电信虚拟运营商", "联通虚拟运营商", "移动虚拟运营商" };
    private static final int INDEX_SEGMENT_LENGTH = 9;

    private static byte[] DATA_BYTE_ARRAY;
    private static ByteBuffer BYTE_BUFFER;
    private static int INDEX_AREA_OFFSET;
    private static int INDEX_AREA_END;
    private static int PHONE_RECORD_COUNT;
    private static final int PHONE_MAX_LENGTH = 11, PHONE_REGION_LENGTH = 7;
    static RandomAccessFile raf = null;
    static boolean memorySearch = NumberUtil.parseBoolean(RedisConfig.get("mobile.memorySearch"),
            "true".equalsIgnoreCase(System.getenv("mobile.memorySearch")));

    static {
        reload();
    }

    private static void reload() {
        IOUtils.closeQuietly(raf);// reload manually
        DATA_BYTE_ARRAY = null;
        BYTE_BUFFER = null;
        // memorySearch = true;
        try (InputStream inputStream = memorySearch ? new BufferedInputStream(ConfigUtil.stream("phone.dat")) : null) {
            ByteArrayOutputStream byteData = new ByteArrayOutputStream();
            if (memorySearch) {
                IOUtils.copy(inputStream, byteData);
                DATA_BYTE_ARRAY = byteData.toByteArray();
                BYTE_BUFFER = ByteBuffer.wrap(DATA_BYTE_ARRAY);
                BYTE_BUFFER.order(ByteOrder.LITTLE_ENDIAN);
            } else {
                raf = new RandomAccessFile(new File(ConfigUtil.DIRECTORY, "phone.dat"), "r");
                TaskUtil.addShutdownHook((Runnable) () -> {
                    IOUtils.closeQuietly(raf);
                });
            }
            // memorySearch = false;

            int dataVersion = memorySearch ? BYTE_BUFFER.getInt() : readInt(raf);// 942682418
            INDEX_AREA_OFFSET = memorySearch ? BYTE_BUFFER.getInt() : readInt(raf);// 9889
            INDEX_AREA_END = memorySearch ? DATA_BYTE_ARRAY.length : (int) raf.length();// 4098913

            PHONE_RECORD_COUNT = (INDEX_AREA_END - INDEX_AREA_OFFSET) / INDEX_SEGMENT_LENGTH;// 454336
            log.info("phone.dat loaded, dataVersion={}, recordCount={}", dataVersion, PHONE_RECORD_COUNT);
        } catch (Exception e) {
            log.warn("fail to init phone.dat, ex: {}", e.getMessage());
        }
    }

    // ByteOrder.LITTLE_ENDIAN
    private static int readInt(RandomAccessFile raf) throws Exception {
        int ch4 = raf.read();
        int ch3 = raf.read();
        int ch2 = raf.read();
        int ch1 = raf.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new EOFException();
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }

    /**
     * 查找手机号归属信息
     * 
     * @param phoneNumber
     * @return null 或 province|city|zipCode|areaCode|type
     */
    public static synchronized String search(String phoneNumber) throws Exception {
        if (phoneNumber == null || phoneNumber.length() > PHONE_MAX_LENGTH || phoneNumber.length() < PHONE_REGION_LENGTH
                || PHONE_RECORD_COUNT < 1) {
            return null;
        }
        int phoneNumberPrefix;
        try {
            phoneNumberPrefix = Integer.parseInt(phoneNumber.substring(0, 7));
        } catch (Exception e) {
            return null;
        }
        int left = 0;
        int right = PHONE_RECORD_COUNT;
        while (left <= right) {
            int middle = (left + right) >> 1;// 227168
            int currentOffset = INDEX_AREA_OFFSET + middle * INDEX_SEGMENT_LENGTH;// 2054401
            if (currentOffset >= INDEX_AREA_END) {
                return null;
            }
            if (memorySearch) {
                BYTE_BUFFER.position(currentOffset);
            } else {
                raf.seek(currentOffset);
            }
            int currentPrefix = memorySearch ? BYTE_BUFFER.getInt() : readInt(raf);// 1655906
            if (currentPrefix > phoneNumberPrefix) {
                right = middle - 1;
            } else if (currentPrefix < phoneNumberPrefix) {
                left = middle + 1;
            } else {
                int infoBeginOffset = memorySearch ? BYTE_BUFFER.getInt() : readInt(raf);// 1991
                int phoneType = memorySearch ? BYTE_BUFFER.get() : raf.read();// 1

                int infoLength = -1;// 25
                byte[] bs = new byte[64];
                if (memorySearch == false) {
                    raf.seek(infoBeginOffset);
                }
                for (int i = infoBeginOffset; i < INDEX_AREA_OFFSET; ++i) {
                    byte b = memorySearch ? DATA_BYTE_ARRAY[i] : raf.readByte();
                    if (b == 0) {// 2016
                        infoLength = i - infoBeginOffset;
                        break;
                    } else {
                        bs[i - infoBeginOffset] = b;
                    }
                }

                String infoString = new String(bs, 0, infoLength, StandardCharsets.UTF_8);
                return infoString + "|" + phoneType;
            }
        }
        return null;
    }

    public static synchronized Map<String, String> searchToMap(String mobile) throws Exception {
        String infoString = search(mobile);
        if (infoString != null) {
            String[] infoSegments = infoString.split("\\|");
            Map<String, String> map = MapUtil.newHashMap();
            map.put("province", infoSegments[0]);
            map.put("city", infoSegments[1]);
            map.put("zipCode", infoSegments[2]);
            map.put("areaCode", infoSegments[3]);
            map.put("isp", NUMBER_TYPE[Integer.parseInt(infoSegments[4])]);
            map.put("region",
                    (infoSegments[0].equals(infoSegments[1]) ? infoSegments[0] : infoSegments[0] + infoSegments[1])
                            + map.get("isp"));
            return map;
        }
        return Collections.emptyMap();
    }
}
