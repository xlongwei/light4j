package net.sourceforge.pinyin4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.networknt.utility.Tuple;
import com.xlongwei.light4j.util.FileUtil;
import com.xlongwei.light4j.util.FileUtil.CharsetNames;

import org.apache.commons.lang3.StringUtils;

import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import net.sourceforge.pinyin4j.multipinyin.MultiPinyinConfig;
import net.sourceforge.pinyin4j.multipinyin.Trie;

public class PinyinHelper2 {
  static public HanyuPinyinOutputFormat defaultFormat = new HanyuPinyinOutputFormat();
  static public Map<String,String> tsMap = new HashMap<>(4189);//繁体=>简体编码
  static {
    defaultFormat.setToneType(HanyuPinyinToneType.WITH_TONE_MARK);
    defaultFormat.setVCharType(HanyuPinyinVCharType.WITH_U_UNICODE);
    MultiPinyinConfig.multiPinyinPath="/houbb.pinyin";
    try{
      String str="𠀛",py1=toHanYuPinyinString(str, null)[0];//char.txt内容为2001B (yu4)，默认编码即可
      ChineseToPinyinResource.getInstance().getUnicodeToHanyuPinyinTable().load(PinyinHelper2.class.getResourceAsStream("/houbb/char.txt"));
      String py2=toHanYuPinyinString(str, null)[0];
      System.out.println(py1+"="+py2);
      FileUtil.handleLines(PinyinHelper2.class.getResourceAsStream("/houbb/ts.txt"), CharsetNames.UTF_8, line->{
        List<Tuple<String, String>> list = PinyinHelper2.list(line);
        String j = list.get(2).first;//繁体=>简体编码，tx.txt需要使用UFT_8加载
        tsMap.put(list.get(0).first, Integer.toHexString(j.codePointAt(0)).toUpperCase());
      });
      System.out.println("慶="+tsMap.get("慶"));
    }catch(Exception e){
      e.printStackTrace();
    }
  }

    static public String[] toHanYuPinyinString(String str, HanyuPinyinOutputFormat outputFormat) throws BadHanyuPinyinOutputFormatCombination {
        ChineseToPinyinResource resource = ChineseToPinyinResource.getInstance();
        List<Tuple<String, String>> list = list(str);
        if (outputFormat == null) outputFormat = defaultFormat;
        if(list.size()==1){
          String codepointHexStr = StringUtils.defaultIfBlank(tsMap.get(str), list.get(0).second);
          Trie trie = ChineseToPinyinResource.getInstance().getUnicodeToHanyuPinyinTable().get(codepointHexStr);
          String[] pinyinStrArray = trie == null ? null : trie.getPinyinArray();
          if (null != pinyinStrArray) {
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
          Tuple<String,String>tuple=list.get(i);
          Trie currentTrie = resource.getUnicodeToHanyuPinyinTable();
          int success = i;
          int current = i;
          do {
            String hexStr = StringUtils.defaultIfBlank(tsMap.get(tuple.first), tuple.second);
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
          String codepointHexStr = StringUtils.defaultIfBlank(PinyinHelper2.tsMap.get(str), Integer.toHexString(codePoint).toUpperCase());
          Trie trie = ChineseToPinyinResource.getInstance().getUnicodeToHanyuPinyinTable().get(codepointHexStr);
          return trie != null;
        }
        return false;
      }
}
