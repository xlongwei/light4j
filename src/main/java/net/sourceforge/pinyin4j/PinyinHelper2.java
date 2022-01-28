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
          String result = null;//匹配到的最长的结果
          char ch = chars[i];
          Trie currentTrie = resource.getUnicodeToHanyuPinyinTable();
          int success = i;
          int current = i;
          do {
            String hexStr = Integer.toHexString((int) ch).toUpperCase();
            currentTrie = currentTrie.get(hexStr);
            if (currentTrie != null) {
              if (currentTrie.getPinyin() != null) {
                result = currentTrie.getPinyin();
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
    
          if (result == null) {//如果在前缀树中没有匹配到，那么它就不能转换为拼音，直接输出或者去掉
            array[i]=String.valueOf(chars[i]);
          } else {
            String[] pinyinStrArray = resource.parsePinyinString(result);
            if (pinyinStrArray != null) {
              for (int j = 0; j < pinyinStrArray.length; j++) {
                array[i+j]=(PinyinFormatter.formatHanyuPinyin(pinyinStrArray[j],
                    outputFormat));
                // if (current < chars.length || (j < pinyinStrArray.length - 1 && i != success)) {//不是最后一个,(也不是拼音的最后一个,并且不是最后匹配成功的)
                //   resultPinyinStrBuf.append(separate);
                // }
                if (i == success) break;
              }
            }
          }
          i = success;
        }
    
        return array;
      }
}
