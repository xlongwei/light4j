package com.xlongwei.light4j;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

// import org.apache.shiro.util.StringUtils;
import org.junit.Test;

import com.xlongwei.light4j.util.EcdictUtil;
import com.xlongwei.light4j.util.FileUtil;
import com.xlongwei.light4j.util.FileUtil.TextReader;
import com.xlongwei.light4j.util.FileUtil.TextWriter;
import com.xlongwei.light4j.util.MySqlUtil;
import com.xlongwei.light4j.util.SqlInsert;
import com.xlongwei.light4j.util.StringUtil;

import cn.hutool.core.text.TextSimilarity;

/**
 * ecdict数据处理
 * @author xlongwei
 */
public class EcdictTest {
	String dir = "D:/OpenSource/wuyaSama/ECDICT.git";
	String name = "stardict.csv";//ecdict stardict
	
	/**解析stardict.csv文件，将合适的单词和音标写入sql文件
	 * <pre>
            CREATE TABLE IF NOT EXISTS `%s`.`stardict` (
            `id` INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
            `word` VARCHAR(64) NOT NULL UNIQUE KEY,
            `sw` VARCHAR(64) NOT NULL,
            `phonetic` VARCHAR(64),
            `definition` TEXT,
            `translation` TEXT,
            `pos` VARCHAR(16),
            `collins` SMALLINT DEFAULT 0,
            `oxford` SMALLINT DEFAULT 0,
            `tag` VARCHAR(64),
            `bnc` INT DEFAULT NULL,
            `frq` INT DEFAULT NULL,
            `exchange` TEXT,
            `detail` TEXT,
            `audio` TEXT,
            KEY(`sw`, `word`),
            KEY(`collins`),
            KEY(`oxford`),
            KEY(`tag`)
            )
            '''%(database)
        sql = '\n'.join([ n.strip('\t') for n in sql.split('\n') ])
        sql = sql.strip('\n')
        sql += ' ENGINE=MyISAM DEFAULT CHARSET=utf8;'
	 * </pre>
	 */
	@Test
	public void count() {
		TextReader reader = new FileUtil.TextReader(new File(dir, name));
		String line = reader.read();//忽略首行
		int fields = 13;//word,phonetic,definition,translation,pos,collins,oxford,tag,bnc,frq,exchange,detail,audio
		long lines = 0, words = 0, start = System.currentTimeMillis();
		int emptyPhonetic = 0, hasSpace = 0, badStart = 0, hasNumbers = 0, others = 0;
		int wordMaxLength = 0, phoneticMaxLength = 0;
		TextWriter writer = new FileUtil.TextWriter(new File("target", "ecdict.sql"));
		SqlInsert sqlInsert = new SqlInsert("ecdict");
		sqlInsert.addColumns("word","phonetic");
		Set<String> added = new HashSet<>();
		while((line=reader.read())!=null) {
			lines++;
			String[] split = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");//这个正则表达式可以分割csv格式，但是结果长度不准确
			// String[] split = StringUtils.split(line, ',');//shiro的工具类可以准确分割csv格式，默认quote字符是双引号，默认不保留双引号，并处理空白
			if(split.length != fields) {
				System.out.println("bad length="+split.length+"："+line);
				break;
			}
			int idx = 0;
			String word = split[idx++].toLowerCase();//varchar(64)，单词
			String phonetic = split[idx++];//varchar(64)，音标
			if(StringUtil.isBlank(phonetic)) {
				emptyPhonetic++;
			}else if(word.indexOf(' ')>0) {
				hasSpace++;
			}else if("-'.?".contains(word.substring(0, 1))) {
				badStart++;
			}else if(EcdictUtil.isWord(word)) {
//				words++;
//				String definition = split[idx++];//text，英文释义
//				String translation = split[idx++];//text，中文释义
//				String pos = split[idx++];//varchar(16)，位置
//				String collins = split[idx++];//smallint，柯林斯星级
//				String oxford = split[idx++];//smallint，是否牛津三千核心词汇
//				String tag = split[idx++];//varchar(64)，标签zk中考gk高考cet4四级
//				String bnc = split[idx++];//int，英语国家语料库词频
//				String frq = split[idx++];//int，当代语料库词频
//				String exchange = split[idx++];//text，时态复数变换
//				String detail = split[idx++];//text，json扩展信息
//				String audio = split[idx++];//text，音频地址
				int pos = phonetic.indexOf(';');
				if(pos>0) {
//					System.out.println(word+"="+phonetic);
					phonetic = phonetic.substring(0, pos);
				}
				if(phonetic.indexOf(' ')==-1) {
					words++;
					if(phonetic.startsWith("/")) {
						phonetic = phonetic.replace("/", "");//abackward=/əˈbæk.wəd/
					}
					if(word.length()>wordMaxLength) {
						wordMaxLength=word.length();
					}
					if(phonetic.length()>phoneticMaxLength) {
						phoneticMaxLength=phonetic.length();
					}
					if(!added.contains(word)) {
						added.add(word);
						sqlInsert.addValues(word, phonetic);
						if(sqlInsert.size()>=100) {
							writer.writeln(sqlInsert.toString());
							sqlInsert.clear();
						}
					}
//					System.out.println(word+"="+phonetic);
				}else {
					emptyPhonetic++;
				}
			}else if(line.matches(".*[0-9].*")){
				hasNumbers++;
			}else {
//				System.out.println(line);
				others++;
			}
//			if(words>1000) break;
		}
		if(sqlInsert.size()>0) {
			writer.writeln(sqlInsert.toString());
			sqlInsert.clear();
		}
		reader.close();
		writer.close();
		System.out.println("lines="+lines+",words="+words+",millis="+(System.currentTimeMillis()-start)+",wordMax="+wordMaxLength+",phoneticMax="+phoneticMaxLength);
		System.out.println("emptyPhonetic="+emptyPhonetic+",hasSpace="+hasSpace+",badStart="+badStart+",hasNumbers="+hasNumbers+",others="+others+",added="+added.size());
	}
	
	/**
	 * 处理lemma.en.txt，将合适的单词，取词根的音标，写入sql文件
	 */
	@Test
	public void lemma() {
		TextReader reader = new FileUtil.TextReader(new File(dir, "lemma.en.txt"));
		String line = null;
		int lines = 0, goods = 0, bads = 0;
		TextWriter writer = new FileUtil.TextWriter(new File("target", "lemma.sql"));
		SqlInsert sqlInsert = new SqlInsert("ecdict");
		sqlInsert.addColumns("word","phonetic");
		Set<String> added = new HashSet<>();
		while((line=reader.read())!=null) {
			if(line.startsWith(";")) {
				continue;
			}else {
				lines++;
				int pos = line.indexOf("->");
				if(pos>0) {
					String lemma = line.substring(0, pos).trim().toLowerCase();
					String words = line.substring(pos+2).trim().toLowerCase();
					pos = lemma.indexOf('/');
					if(pos>0) {
						lemma = lemma.substring(0, pos);
					}
					String[] row = resultSet(lemma);
					if(row==null) {
						continue;//没有lemma信息，直接跳过
					}
					String[] split = words.split("[,]");
					for(String str:split) {
						if(EcdictUtil.isWord(str)) {
							goods++;
//							if("programmed".equals(str)) {
//								System.out.println(line);
//							}
							String[] info = resultSet(str);
							//计算相似度，排除am,bi之类
							if(info==null && TextSimilarity.similar(lemma, str)>0.66) {
								if(!added.contains(str)) {
									added.add(str);
									sqlInsert.addValues(str, row[1]);
									if(sqlInsert.size()>=100) {
										writer.writeln(sqlInsert.toString());
										sqlInsert.clear();
									}
								}
							}
						}else {
							bads++;
						}
					}
				}
			}
		}
		if(sqlInsert.size()>0) {
			writer.writeln(sqlInsert.toString());
			sqlInsert.clear();
		}
		reader.close();
		writer.close();
		System.out.println("lines="+lines+",goods="+goods+",bads="+bads+",adds="+added.size());
	}
	
	@Test
	public void sentences() {
		System.out.println(EcdictUtil.sentences("中en文abc def"));
		System.out.println(EcdictUtil.pinyin(EcdictUtil.words("Christmas won't be Christmas without any presents")));
	}
	
	private String[] resultSet(String word) {
		try(Connection conn=MySqlUtil.DATASOURCE.getConnection()){
			try(PreparedStatement ps = conn.prepareStatement("select word,phonetic from ecdict where word=?")){
				ps.setString(1, word);
				ResultSet rs = ps.executeQuery();
				if(rs.next()) {
					return new String[] {rs.getString(1), rs.getString(2)};
				}
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
