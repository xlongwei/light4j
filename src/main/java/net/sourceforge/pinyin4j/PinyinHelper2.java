package net.sourceforge.pinyin4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.networknt.utility.Tuple;
import com.xlongwei.light4j.util.FileUtil;
import com.xlongwei.light4j.util.FileUtil.CharsetNames;

import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import net.sourceforge.pinyin4j.multipinyin.Trie;

public class PinyinHelper2 {
  static public HanyuPinyinOutputFormat defaultFormat = new HanyuPinyinOutputFormat();
  static public Map<String,Integer> tsMap = new HashMap<>(4189);//繁体=>简体codePoint
  static public Trie root = ChineseToPinyinResource.getInstance().getUnicodeToHanyuPinyinTable();
  static {
    defaultFormat.setToneType(HanyuPinyinToneType.WITH_TONE_MARK);
    defaultFormat.setVCharType(HanyuPinyinVCharType.WITH_U_UNICODE);
    try{
      String str="𠀛",py1=toHanYuPinyinString(str, null)[0];//char.txt内容为2001B (yu4)，默认编码即可
      root.load(PinyinHelper2.class.getResourceAsStream("/houbb/char.txt"));
      String py2=toHanYuPinyinString(str, null)[0];
      System.out.println(py1+"="+py2);
      FileUtil.handleLines(PinyinHelper2.class.getResourceAsStream("/houbb/ts.txt"), CharsetNames.UTF_8, line->{
        String[] split = line.split(" ");
        if (split.length >= 2) {
            tsMap.put(split[0], split[1].codePointAt(0));
        }
      });
      System.out.println("慶="+tsMap.get("慶"));
    }catch(Exception e){
      e.printStackTrace();
    }
  }

    static public String[] toHanYuPinyinString(String str, HanyuPinyinOutputFormat outputFormat) throws BadHanyuPinyinOutputFormatCombination {
        ChineseToPinyinResource resource = ChineseToPinyinResource.getInstance();
        List<Tuple<String, Integer>> list = list(str);
        if(list.size()==1){
          Integer codePoint = tsMap.getOrDefault(str, list.get(0).second);
          Trie trie = root.get(codePoint);
          String[] pinyinStrArray = trie == null ? null : trie.getPinyinArray();
          if (null != pinyinStrArray && null != outputFormat) {
            for (int i = 0; i < pinyinStrArray.length; i++) {
              pinyinStrArray[i] = PinyinFormatter.formatHanyuPinyin(pinyinStrArray[i], outputFormat);
            }
          }else{
            pinyinStrArray = new String[]{str};
          }
          return pinyinStrArray;
        }
        int length=list.size();
        String[] array = new String[length];
        for (int i = 0; i < length; i++) {
          String[] pinyinStrArray=null;
          Tuple<String,Integer>tuple=list.get(i);
          Trie currentTrie = resource.getUnicodeToHanyuPinyinTable();
          int success = i;
          int current = i;
          do {
            Integer codePoint = tsMap.getOrDefault(tuple.first, tuple.second);
            currentTrie = currentTrie.get(codePoint);
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
                array[i+j]=(outputFormat==null ? pinyinStrArray[j] : PinyinFormatter.formatHanyuPinyin(pinyinStrArray[j], outputFormat));
                if (i == success) break;
              }
            }
          }
          i = success;
        }
    
        return array;
      }

      static public List<Tuple<String,Integer>> list(String str) {
        List<Tuple<String, Integer>> list = new ArrayList<>(str.codePointCount(0, str.length()));
        str.codePoints().forEach(codePoint -> {
          list.add(new Tuple<String,Integer>(new StringBuilder().appendCodePoint(codePoint).toString(), codePoint));
        });
        return list;
      }

      static public boolean isWord(String str) {
        return str.codePointCount(0, str.length())==1;
      }

      static public boolean hasPinyin(String str) {
        if (isWord(str)) {
          int codePoint = str.codePointAt(0);
          Trie trie = root.get(codePoint);
          return trie != null;
        }
        return false;
      }
}
