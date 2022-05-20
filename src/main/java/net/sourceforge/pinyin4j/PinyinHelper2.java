package net.sourceforge.pinyin4j;

import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import net.sourceforge.pinyin4j.multipinyin.Trie;

public class PinyinHelper2 {
    static public String[] toHanYuPinyinString(String str, HanyuPinyinOutputFormat outputFormat) throws BadHanyuPinyinOutputFormatCombination {
        ChineseToPinyinResource resource = ChineseToPinyinResource.getInstance();
        String[] array = new String[str.length()];
    
        char[] chars = str.toCharArray();
    
        for (int i = 0; i < chars.length; i++) {
          String[] pinyinStrArray=null;
          char ch = chars[i];
          Trie currentTrie = resource.getUnicodeToHanyuPinyinTable();
          int success = i;
          int current = i;
          do {
            String hexStr = Integer.toHexString((int) ch).toUpperCase();
            currentTrie = currentTrie.get(hexStr);
            if (currentTrie != null) {
              if (currentTrie.getPinyinArray() != null) {
                pinyinStrArray = currentTrie.getPinyinArray();
                success = current;
              }
              currentTrie = currentTrie.getNextTire();
            }
            current++;
            if (current < chars.length)
              ch = chars[current];
            else
              break;
          } while (currentTrie != null);
    
          if (pinyinStrArray == null) {//如果在前缀树中没有匹配到，那么它就不能转换为拼音，直接输出或者去掉
            array[i]=String.valueOf(chars[i]);
          } else {
            if (pinyinStrArray != null) {
              for (int j = 0; j < pinyinStrArray.length; j++) {
                array[i+j]=(PinyinFormatter.formatHanyuPinyin(pinyinStrArray[j],
                    outputFormat));
                if (i == success) break;
              }
            }
          }
          i = success;
        }
    
        return array;
      }

      static public String[] toHanyuPinyinStringArray(char ch, HanyuPinyinOutputFormat outputFormat) throws BadHanyuPinyinOutputFormatCombination {
        String codepointHexStr = Integer.toHexString((int) ch).toUpperCase();
        Trie trie = ChineseToPinyinResource.getInstance().getUnicodeToHanyuPinyinTable().get(codepointHexStr);
        String[] pinyinStrArray = trie == null ? null : trie.getPinyinArray();
        if (null != pinyinStrArray) {
          for (int i = 0; i < pinyinStrArray.length; i++) {
            pinyinStrArray[i] = PinyinFormatter.formatHanyuPinyin(pinyinStrArray[i], outputFormat);
          }
        }
        return pinyinStrArray;
      }
}
