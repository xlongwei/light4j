package com.xlongwei.light4j;

import java.io.File;
import java.io.PrintWriter;
import java.lang.Character.UnicodeBlock;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import com.github.houbb.heaven.util.lang.StringUtil;
import com.github.houbb.nlp.common.format.impl.ChineseTsCharFormat;
import com.github.houbb.pinyin.spi.IPinyinTone;
import com.github.houbb.pinyin.support.style.PinyinToneStyles;
import com.github.houbb.pinyin.support.tone.PinyinTones;
import com.github.houbb.pinyin.util.PinyinHelper;
import com.networknt.utility.Tuple;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ReflectUtil;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.pinyin4j.PinyinHelper2;

@Slf4j
@SuppressWarnings({ "unchecked" })
public class PinyinTest {
    Map<String, List<String>> charMap;
    int[] blockStarts;
    UnicodeBlock[] blocks;
    Map<Character, Character> TS_CHAR_MAP;

    @Test
    public void test() throws Exception {
        py();
        block();
        int miss = 0, diff = 0, fan = 0;// 缺少，差异
        PrintWriter writer = FileUtil.getPrintWriter(new File("target/diff.txt"), StandardCharsets.UTF_8, false);
        PrintWriter charTxt = FileUtil.getPrintWriter(new File("target/char.txt"),
                StandardCharsets.UTF_8, false);
        Map<UnicodeBlock, Tuple<AtomicInteger, AtomicInteger>> missBlocks = newHashMap(16,
                (block) -> new Tuple<>(new AtomicInteger(0), new AtomicInteger(0)));
        Map<String, AtomicInteger> fans = newHashMap(4189, (ch) -> new AtomicInteger(0));
        for (Entry<String, List<String>> entry : charMap.entrySet()) {
            String str = entry.getKey();
            int codePoint = str.codePointAt(0);
            String hex = Integer.toHexString(codePoint).toUpperCase();

            UnicodeBlock block = Character.UnicodeBlock.of(codePoint);
            String blockStr = blockStr(block);

            List<String> list = entry.getValue();
            String pinyinStr = String.join(StringUtil.BLANK, list) + "(" + PinyinToneStyles.numLast().style(list.get(0))
                    + ")";
            String[] array = PinyinHelper2.toHanYuPinyinString(str, null);
            if (PinyinHelper2.tsMap.containsKey(str)) {
                fan++;
                fans.get(str).getAndDecrement();
            }
            if (array == null || (array.length == 1 && str.equals(array[0]))) {
                miss++;
                writer.println(String.format("miss %s: %s=%s %s <> null %s", miss, str, hex,
                        pinyinStr, blockStr));
                missBlocks.get(block).first.incrementAndGet();
                hex = StringUtils.defaultIfBlank(PinyinHelper2.tsMap.get(str), hex);
                charTxt.println(hex + " (" + PinyinToneStyles.numLast().style(list.get(0)) + ")");
            } else {
                if (array.length != list.size()) {
                    diff++;
                    writer.println(String.format("diff %s: %s=%s %s <> %s %s", diff, str, hex,
                            pinyinStr, String.join(StringUtil.BLANK, array), blockStr));
                    missBlocks.get(block).second.incrementAndGet();
                } else {
                    for (String py : array) {
                        if (!list.contains(py)) {
                            diff++;
                            writer.println(
                                    String.format("diff %s: %s=%s %s <> %s %s", diff, str, hex,
                                            pinyinStr, String.join(StringUtil.BLANK, array),
                                            blockStr));
                            missBlocks.get(block).second.incrementAndGet();
                            break;
                        }
                    }
                }
            }
        }
        writer.println("missBlocks=" + missBlocks.size());
        // CJK_UNIFIED_IDEOGRAPHS=0x4E00..9fff miss=9 diff=9759
        // CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A=0x3400..4dbf miss=5764 diff=0
        // HIGH_SURROGATES=0xD800..db7f miss=14715 diff=0
        // PRIVATE_USE_AREA=0xE000..f8ff miss=70 diff=0
        // CJK_SYMBOLS_AND_PUNCTUATION=0x3000..303f miss=0 diff=1
        missBlocks.forEach((block, tuple) -> {
            String blockStr = blockStr(block);
            writer.println(String.format("%s miss=%s diff=%s", blockStr, tuple.first.get(), tuple.second.get()));
        });
        writer.close();
        charTxt.close();
        fans.entrySet().stream().filter(entry -> entry.getValue().get() > 1).forEach(entry -> {
            String ch = entry.getKey();
            String hex = StringUtils.defaultIfBlank(PinyinHelper2.tsMap.get(ch),
                    Integer.toHexString(ch.codePointAt(0)).toUpperCase());
            log.info("fan={} count={}", hex, entry.getValue());
        });
        // miss=21163 diff=9426 fan=4057 => miss=0 diff=15130 fan=4057
        log.info("miss={} diff={} fan={}", miss, diff, fan);
    }

    private String blockStr(UnicodeBlock block) {
        int index = ArrayUtil.indexOf(blocks, block);
        int blockStart = blockStarts[index], blockEnd = blockStarts[index + 1] - 1;
        String start = "0x" + Integer.toHexString(blockStart).toUpperCase(),
                end = Integer.toHexString(blockEnd);
        String blockStr = String.format("%s=%s..%s", block, start, end);
        return blockStr;
    }

    public static <K, V> HashMap<K, V> newHashMap(int capacity, Function<K, V> function) {
        return new HashMap<K, V>(capacity) {
            @Override
            public V get(Object key) {
                V v = super.get(key);
                if (v == null) {
                    K k = (K) key;
                    v = function.apply(k);
                    put(k, v);
                }
                return v;
            }
        };
    }

    @Test
    public void py() throws Exception {
        String str = "重庆女", p1, p2;
        p1 = PinyinHelper.toPinyin("重庆女");
        p2 = String.join(StringUtil.BLANK, PinyinHelper2.toHanYuPinyinString(str, null));
        log.info("p1={} p2={} equals={}", p1, p2, p1.equals(p2));
        IPinyinTone tone = PinyinTones.defaults();
        charMap = (Map<String, List<String>>) ReflectUtil.getFieldValue(tone, "charMap");
    }

    @Test
    public void block() throws Exception {
        blockStarts = (int[]) ReflectUtil.getFieldValue(UnicodeBlock.class, "blockStarts");
        blocks = (UnicodeBlock[]) ReflectUtil.getFieldValue(UnicodeBlock.class, "blocks");
        char ch = '\u39FA';
        UnicodeBlock block = UnicodeBlock.of(ch);
        int index = ArrayUtil.indexOf(blocks, block);
        int blockStart = blockStarts[index], blockEnd = blockStarts[index + 1] - 1;
        String start = Integer.toHexString(blockStart), end = Integer.toHexString(blockEnd);
        log.info("ch={} {}={}..{}", String.valueOf(ch), block, start, end);
    }

    @Test
    public void jf() throws Exception {
        TS_CHAR_MAP = (Map<Character, Character>) ReflectUtil
                .getFieldValue(ChineseTsCharFormat.class, "TS_CHAR_MAP");
        char f = '慶', j = TS_CHAR_MAP.get(f);
        log.info("jf f={} j={}", f, j);
    }

    @Test
    public void codepoint() throws Exception {
        String str = "𠀛𠀝重慶𠐊〇";//𠐊=2040A=2B74B
        List<Tuple<String, String>> list = PinyinHelper2.list(str);
        for (Tuple<String, String> tuple : list) {
            System.out.println(tuple.first + "=" + tuple.second+" hasPinyin="+PinyinHelper2.hasPinyin(tuple.first));
        }
        System.out.println(String.join(StringUtil.BLANK, PinyinHelper2.toHanYuPinyinString(str, null)));
        System.out.println(PinyinHelper2.tsMap.get("𠐊"));
    }
}
