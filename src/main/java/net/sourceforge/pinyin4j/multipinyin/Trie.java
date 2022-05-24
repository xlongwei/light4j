package net.sourceforge.pinyin4j.multipinyin;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Created by 刘一波 on 16/3/4.
 * E-Mail:yibo.liu@tqmall.com
 */
public class Trie {

  private Map<Integer, Trie> values = null;//本节点包含的值

  private String[] array;

  private Trie nextTire;//下一个节点,也就是匹配下一个字符

  static int news,puts,maps=1;//new和put计数

  public Trie(){
      if(news++==0){
          values=new HashMap<>(20904);//根节点减少扩容次数
      }
  }

  public String getPinyin() {
    StringJoiner joiner = new StringJoiner(",", "(", ")");
    for(int i=0;i<array.length;i++) joiner.add(array[i]);
    return joiner.toString();//这里可选抛异常，从而禁用PinyinHelper，只使用PinyinHelper2就够了
  }

  public String[] getPinyinArray() {
    return array;//省掉joiner和parsePinyinString
  }

  public void setPinyin(String pinyin) {
    String[] split=pinyin.substring(1, pinyin.length()-1).split(",");
    array=new String[split.length];
    for(int i=0;i<split.length;i++){
      array[i]=split[i].intern();//拼音总数只有几百个，再组合5种声调约两千个
    }
  }

  public Trie getNextTire() {
    return nextTire;
  }

  public void setNextTire(Trie nextTire) {
    this.nextTire = nextTire;
  }

  /**
   * 加载拼音
   *
   * @param inStream 拼音文件输入流
   * @throws IOException
   */
  public synchronized void load(InputStream inStream) throws IOException {
    BufferedReader bufferedReader = null;
    InputStreamReader inputStreamReader = null;
    try {
      inputStreamReader = new InputStreamReader(inStream, "UTF-8");
      bufferedReader = new BufferedReader(inputStreamReader);
      String s;
      while ((s = bufferedReader.readLine()) != null) {
        String[] keyAndValue = s.split(" ");
        if (keyAndValue.length != 2) continue;
        Trie trie = new Trie();
        trie.setPinyin(keyAndValue[1]);
        put(Integer.parseInt(keyAndValue[0],16), trie);
      }
      //load pinyin news=20904,maps=1,puts=20903
      System.out.println("load pinyin news="+news+",maps="+maps+",puts="+puts);
    } finally {
      if (inputStreamReader != null) inputStreamReader.close();
      if (bufferedReader != null) bufferedReader.close();
    }
  }

  /**
   * 加载多音字拼音词典
   *
   * @param inStream 拼音文件输入流
   */
  public synchronized void loadMultiPinyin(InputStream inStream) throws IOException {
    BufferedReader bufferedReader = null;
    InputStreamReader inputStreamReader = null;
    try {
      inputStreamReader = new InputStreamReader(inStream, "UTF-8");
      bufferedReader = new BufferedReader(inputStreamReader);
      String s;
      while ((s = bufferedReader.readLine()) != null) {
        String[] keyAndValue = s.split(" ");
        if (keyAndValue.length != 2) continue;

        String key = keyAndValue[0];//多于一个字的字符串
        String value = keyAndValue[1];//字符串的拼音
        char[] keys = key.toCharArray();

        Trie currentTrie = this;
        for (int i = 0; i < keys.length; i++) {
          int codePoint = keys[i];

          Trie trieParent = currentTrie.get(codePoint);
          if (trieParent == null) {//如果没有此值,直接put进去一个空对象
            currentTrie.put(codePoint, new Trie());
            trieParent = currentTrie.get(codePoint);
          }
          Trie trie = trieParent.getNextTire();//获取此对象的下一个

          if (keys.length - 1 == i) {//最后一个字了,需要把拼音写进去
            trieParent.setPinyin(value);
            break;//此行其实并没有意义
          }

          if (trie == null) {
            if (keys.length - 1 != i) {
              //不是最后一个字,写入这个字的nextTrie,并匹配下一个
              Trie subTrie = new Trie();
              trieParent.setNextTire(subTrie);
              codePoint = keys[i+1];
              subTrie.put(codePoint, new Trie());
              currentTrie = subTrie;
            }
          } else {
            currentTrie = trie;
          }

        }
      }
      //load multi pinyin news=71081,maps=20543,puts=50537
      System.out.println("load multi pinyin news="+news+",maps="+maps+",puts="+puts);
    } finally {
      if (inputStreamReader != null) inputStreamReader.close();
      if (bufferedReader != null) bufferedReader.close();
    }
  }

  /**
   * 加载用户自定义的扩展词库
   */
  public void loadMultiPinyinExtend() throws IOException {
    String path = MultiPinyinConfig.multiPinyinPath;
    if (path != null) {
      File userMultiPinyinFile = new File(path);
      if (userMultiPinyinFile.exists()) {
        loadMultiPinyin(new FileInputStream(userMultiPinyinFile));
      }
    }
  }

  public Trie get(Integer codePoint) {
      if(values==null) return null;
    return values.get(codePoint);
  }

  public Trie get(String hexString) {//兼容PinyinHelper
    return get(Integer.parseInt(hexString, 16));
  }

  public void put(Integer codePoint, Trie trie) {
      if(values==null) { values=new HashMap<>(4); maps++; }
    values.put(codePoint, trie);
    puts++;
  }
}
