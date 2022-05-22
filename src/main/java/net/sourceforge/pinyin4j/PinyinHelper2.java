package net.sourceforge.pinyin4j;

import java.util.ArrayList;
import java.util.List;

import com.networknt.utility.Tuple;

import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import net.sourceforge.pinyin4j.multipinyin.Trie;

public class PinyinHelper2 {
  static public HanyuPinyinOutputFormat defaultFormat = new HanyuPinyinOutputFormat();
  static {
    defaultFormat.setToneType(HanyuPinyinToneType.WITH_TONE_MARK);
    defaultFormat.setVCharType(HanyuPinyinVCharType.WITH_U_UNICODE);
  }

    static public String[] toHanYuPinyinString(String str, HanyuPinyinOutputFormat outputFormat) throws BadHanyuPinyinOutputFormatCombination {
        ChineseToPinyinResource resource = ChineseToPinyinResource.getInstance();
        List<Tuple<String, String>> list = list(str);
        if (outputFormat == null) outputFormat = defaultFormat;
        if(list.size()==1){
          String codepointHexStr = list.get(0).second;
          Trie trie = ChineseToPinyinResource.getInstance().getUnicodeToHanyuPinyinTable().get(codepointHexStr);
          String[] pinyinStrArray = trie == null ? null : trie.getPinyinArray();
          if (null != pinyinStrArray) {
            for (int i = 0; i < pinyinStrArray.length; i++) {
              pinyinStrArray[i] = PinyinFormatter.formatHanyuPinyin(pinyinStrArray[i], outputFormat);
            }
          }else{
            pinyinStrArray = new String[]{list.get(0).first};
          }
          return pinyinStrArray;
        }
        int length=list.size();
        String[] array = new String[length];
        for (int i = 0; i < length; i++) {
          String[] pinyinStrArray=null;
          Tuple<String,String>tuple=list.get(i);
          Trie currentTrie = resource.getUnicodeToHanyuPinyinTable();
          int success = i;
          int current = i;
          do {
            String hexStr = tuple.second;
            currentTrie = currentTrie.get(hexStr);
            if (currentTrie != null) {
              if (currentTrie.getPinyinArray() != null) {
                pinyinStrArray = currentTrie.getPinyinArray();
                success = current;
              }
              currentTrie = currentTrie.getNextTire();
            }
            current++;
            if (current < length)
              tuple = list.get(current);
            else
              break;
          } while (currentTrie != null);
    
          if (pinyinStrArray == null) {//如果在前缀树中没有匹配到，那么它就不能转换为拼音，直接输出或者去掉
            array[i]=list.get(i).first;
          } else {
            if (pinyinStrArray != null) {
              for (int j = 0; j < pinyinStrArray.length; j++) {
                array[i+j]=(PinyinFormatter.formatHanyuPinyin(pinyinStrArray[j], outputFormat));
                if (i == success) break;
              }
            }
          }
          i = success;
        }
    
        return array;
      }

      static public List<Tuple<String,String>> list(String str) {
        List<Tuple<String, String>> list = new ArrayList<>(str.codePointCount(0, str.length()));
        for (int i = 0; i < str.length();) {
            char c = str.charAt(i);
            if (Character.isHighSurrogate(c)) {
                int codePoint = Character.toCodePoint(c, str.charAt(i + 1));
                list.add(new Tuple<>(str.substring(i, i + 2), Integer.toHexString(codePoint).toUpperCase()));
                i += 2;
            } else {
                int codePoint = (int) c;
                list.add(new Tuple<>(str.substring(i, i + 1), Integer.toHexString(codePoint).toUpperCase()));
                i += 1;
            }
        }
        return list;
      }

      static public boolean isWord(String str) {
        return str.codePointCount(0, str.length())==1;
      }

      static public boolean hasPinyin(String str) {
        if (isWord(str)) {
          int codePoint = str.codePointAt(0);
          String codepointHexStr = Integer.toHexString(codePoint).toUpperCase();
          Trie trie = ChineseToPinyinResource.getInstance().getUnicodeToHanyuPinyinTable().get(codepointHexStr);
          return trie != null;
        }
        return false;
      }
}
