package com.xlongwei.light4j.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * 农历日期转换
 * @author Wang, Yi
 * @see https://github.com/CutePandaSh/zhdate.git
 */
public class ZhDate {
	
	/**
	 * 从公历日期生成农历日期
	 * @param dt 公历的日期
	 * @return 生成的农历日期对象
	 */
	public static ZhDate fromDate(Date dt) {
		Calendar c = Calendar.getInstance();
		c.setTime(dt);
		int lunarYear = c.get(Calendar.YEAR);
		Date newyear = parse(CHINESENEWYEAR[lunarYear - 1900]);
		if(Duration.ofMillis(newyear.getTime()-dt.getTime()).toDays()>0) {
			lunarYear -= 1;
			newyear = parse(CHINESENEWYEAR[lunarYear - 1900]);
		}
		int daysPassed = (int)Duration.ofMillis(dt.getTime() - newyear.getTime()).toDays();
		int yearCode = CHINESEYEARCODE[lunarYear - 1900];
		List<Integer> monthDays = ZhDate.decode(yearCode);
		
		int lunarMonth = 0, lunarDay = 0, month = 0;
		List<Integer> accumulateDays = accumulate(monthDays);
		for(int pos = 0; pos < accumulateDays.size(); pos++) {
			int days = accumulateDays.get(pos);
			if(daysPassed + 1 <= days) {
				month = pos+1;
				lunarDay = monthDays.get(pos) - (days-daysPassed) + 1;
				break;
			}
		}
		boolean leapMonth = false;
		if((yearCode & 0xf) == 0 || month <= (yearCode & 0xf)) {
			lunarMonth = month;
		}else {
			lunarMonth = month - 1;
		}
		if((yearCode & 0xf) != 0 && month == (yearCode & 0xf) + 1) {
			leapMonth = true;
		}
		return new ZhDate(lunarYear, lunarMonth, lunarDay, leapMonth);
	}
	
	/**
	 * 返回当天的农历日期
	 */
	public static ZhDate today() {
		return fromDate(new Date());
	}
	
	/**
	 * 农历日期校验
	 * @param lunarYear 农历年份
	 * @param lunarMonth 农历月份
	 * @param lunarDay 农历日期
	 * @param leapMonth 农历是否为闰月日期
	 * @return 校验是否通过
	 */
	public static boolean validate(int lunarYear, int lunarMonth, int lunarDay, boolean leapMonth) {
		if(!(lunarYear>=1900 && lunarYear<=2100 && lunarMonth>=1 && lunarMonth<=12 && lunarDay>=1 && lunarDay<=30)) {
			return false;
		}
		int yearCode = CHINESEYEARCODE[lunarYear - 1900];
		if(leapMonth) {
			if((yearCode & 0xf) != lunarMonth) {
				return false;
			}else if(lunarDay==30) {
				return (yearCode >> 16) == 1;
			}else {
				return true;
			}
		}else if(lunarDay<=29) {
			return true;
		}else {
			return ((yearCode >> ((12 - lunarMonth) + 4)) & 1) == 1;
		}
	}
	
	/**
	 * 初始化农历日期
	 */
	public ZhDate(int lunarYear, int lunarMonth, int lunarDay) {
		this(lunarYear, lunarMonth, lunarDay, false);
	}
	
	/**
	 * 初始化农历日期
	 * @param leapMonth 是否闰月
	 */
	public ZhDate(int lunarYear, int lunarMonth, int lunarDay, boolean leapMonth) {
		if(!validate(lunarYear, lunarMonth, lunarDay, leapMonth)) {
			throw new RuntimeException("农历日期不支持，超出农历1900年1月1日至2100年12月29日，或日期不存在");
		}
		this.lunarYear = lunarYear;
		this.lunarMonth = lunarMonth;
		this.lunarDay = lunarDay;
		this.leapMonth = leapMonth;
		yearCode = CHINESEYEARCODE[this.lunarYear - 1900];
		newyear = parse(CHINESENEWYEAR[lunarYear - 1900]);
	}
	
	/**
	 * 从1900年到2100年的农历月份数据代码 20位二进制代码表示一个年份的数据， 
	 * <br>前四位0:表示闰月为29天，1:表示闰月为30天
	 * <br>中间12位：从左起表示1-12月每月的大小，1为30天，0为29天
	 * <br>最后四位：表示闰月的月份，0表示当年无闰月
	 * <br>前四位和最后四位应该结合使用，如果最后四位为0，则不考虑前四位
	 * <br>例： 
	 * <br>1901年代码为 19168，转成二进制为 0b100101011100000, 最后四位为0，当年无闰月，月份数据为 010010101110 分别代表12月的大小情况
	 * <br>1903年代码为 21717，转成二进制为 0b101010011010101，最后四位为5，当年为闰五月，首四位为0，闰月为29天， 月份数据为 010101001101，分别代表12月的大小情况
	 */
	public static int CHINESEYEARCODE[] = {
	       19416,
	       19168,  42352,  21717,  53856,  55632,  91476,  22176,  39632,
	       21970,  19168,  42422,  42192,  53840, 119381,  46400,  54944,
	       44450,  38320,  84343,  18800,  42160,  46261,  27216,  27968,
	       109396,  11104,  38256,  21234,  18800,  25958,  54432,  59984,
	       92821,  23248,  11104, 100067,  37600, 116951,  51536,  54432,
	       120998,  46416,  22176, 107956,   9680,  37584,  53938,  43344,
	       46423,  27808,  46416,  86869,  19872,  42416,  83315,  21168,
	       43432,  59728,  27296,  44710,  43856,  19296,  43748,  42352,
	       21088,  62051,  55632,  23383,  22176,  38608,  19925,  19152,
	       42192,  54484,  53840,  54616,  46400,  46752, 103846,  38320,
	       18864,  43380,  42160,  45690,  27216,  27968,  44870,  43872,
	       38256,  19189,  18800,  25776,  29859,  59984,  27480,  23232,
	       43872,  38613,  37600,  51552,  55636,  54432,  55888,  30034,
	       22176,  43959,   9680,  37584,  51893,  43344,  46240,  47780,
	       44368,  21977,  19360,  42416,  86390,  21168,  43312,  31060,
	       27296,  44368,  23378,  19296,  42726,  42208,  53856,  60005,
	       54576,  23200,  30371,  38608,  19195,  19152,  42192, 118966,
	       53840,  54560,  56645,  46496,  22224,  21938,  18864,  42359,
	       42160,  43600, 111189,  27936,  44448,  84835,  37744,  18936,
	       18800,  25776,  92326,  59984,  27296, 108228,  43744,  37600,
	       53987,  51552,  54615,  54432,  55888,  23893,  22176,  42704,
	       21972,  21200,  43448,  43344,  46240,  46758,  44368,  21920,
	       43940,  42416,  21168,  45683,  26928,  29495,  27296,  44368,
	       84821,  19296,  42352,  21732,  53600,  59752,  54560,  55968,
	       92838,  22224,  19168,  43476,  41680,  53584,  62034,  54560
	};
	
	/**
	 * 从1900年，至2100年每年的农历春节的公历日期
	 */
	public static String[] CHINESENEWYEAR = {
          "19000131",
          "19010219", "19020208", "19030129", "19040216", "19050204",
          "19060125", "19070213", "19080202", "19090122", "19100210",
          "19110130", "19120218", "19130206", "19140126", "19150214",
          "19160203", "19170123", "19180211", "19190201", "19200220",
          "19210208", "19220128", "19230216", "19240205", "19250124",
          "19260213", "19270202", "19280123", "19290210", "19300130",
          "19310217", "19320206", "19330126", "19340214", "19350204",
          "19360124", "19370211", "19380131", "19390219", "19400208",
          "19410127", "19420215", "19430205", "19440125", "19450213",
          "19460202", "19470122", "19480210", "19490129", "19500217",
          "19510206", "19520127", "19530214", "19540203", "19550124",
          "19560212", "19570131", "19580218", "19590208", "19600128",
          "19610215", "19620205", "19630125", "19640213", "19650202",
          "19660121", "19670209", "19680130", "19690217", "19700206",
          "19710127", "19720215", "19730203", "19740123", "19750211",
          "19760131", "19770218", "19780207", "19790128", "19800216",
          "19810205", "19820125", "19830213", "19840202", "19850220",
          "19860209", "19870129", "19880217", "19890206", "19900127",
          "19910215", "19920204", "19930123", "19940210", "19950131",
          "19960219", "19970207", "19980128", "19990216", "20000205",
          "20010124", "20020212", "20030201", "20040122", "20050209",
          "20060129", "20070218", "20080207", "20090126", "20100214",
          "20110203", "20120123", "20130210", "20140131", "20150219",
          "20160208", "20170128", "20180216", "20190205", "20200125",
          "20210212", "20220201", "20230122", "20240210", "20250129",
          "20260217", "20270206", "20280126", "20290213", "20300203",
          "20310123", "20320211", "20330131", "20340219", "20350208",
          "20360128", "20370215", "20380204", "20390124", "20400212",
          "20410201", "20420122", "20430210", "20440130", "20450217",
          "20460206", "20470126", "20480214", "20490202", "20500123",
          "20510211", "20520201", "20530219", "20540208", "20550128",
          "20560215", "20570204", "20580124", "20590212", "20600202",
          "20610121", "20620209", "20630129", "20640217", "20650205",
          "20660126", "20670214", "20680203", "20690123", "20700211",
          "20710131", "20720219", "20730207", "20740127", "20750215",
          "20760205", "20770124", "20780212", "20790202", "20800122",
          "20810209", "20820129", "20830217", "20840206", "20850126",
          "20860214", "20870203", "20880124", "20890210", "20900130",
          "20910218", "20920207", "20930127", "20940215", "20950205",
          "20960125", "20970212", "20980201", "20990121", "21000209"
	};
	
	/** 天干 */
	public static String TIANGAN = "甲乙丙丁戊己庚辛壬癸";
	/** 地址 */
	public static String DIZHI = "子丑寅卯辰巳午未申酉戌亥";
	/** 生肖 */
	public static String SHENGXIAO = "鼠牛虎兔龙蛇马羊猴鸡狗猪";
	/** 中文数字 */
	public static String ZHNUMS = "零一二三四五六七八九十";
	
	public static String ganzhi(int year) {
		int anum = year - 1900 + 36;
		return new StringBuilder().append(TIANGAN.charAt(anum(anum,10))).append(DIZHI.charAt(anum(anum, 12))).toString();
	}
	
	public static String shengxiao(int year) {
		int anum = year - 1900;
		return String.valueOf(SHENGXIAO.charAt(anum(anum,12)));
	}

	/**
	 * 解析年度农历代码函数
	 * @param yearCode 从年度代码数组中获取的代码整数
	 * @return 当前年度代码解析以后形成的每月天数数组，已将闰月嵌入对应位置，即有闰月的年份返回长度为13，否则为12
	 */
	public static List<Integer> decode(int yearCode) {
		List<Integer> monthDays = new ArrayList<>();
		for(int i=5;i<17;i++) {
			if(((yearCode >> (i - 1)) & 1) > 0) {
				monthDays.add(0, 30);
			}else {
				monthDays.add(0, 29);
			}
		}
		if((yearCode & 0xf) > 0) {
			if((yearCode >> 16) > 0) {
				monthDays.add((yearCode & 0xf), 30);
			}else {
				monthDays.add((yearCode & 0xf), 29);
			}
		}
		return monthDays;
	}
	
	/**
	 * 根据年份返回当前农历月份天数list
	 * @param year 1900到2100的之间的整数
	 * @return 农历年份所对应的农历月份天数列表
	 */
	public static List<Integer> monthDays(int year){
		return decode(CHINESEYEARCODE[year - 1900]);
	}
	
	private int lunarYear;
	private int lunarMonth;
	private int lunarDay;
	private boolean leapMonth;
	private int yearCode;
	private Date newyear;
	
	/**
	 * 农历日期转换称公历日期
	 * @param year
	 * @param month
	 * @param day
	 * @param leap
	 * @return
	 */
	public Date toDate() {
		Calendar c = Calendar.getInstance();
		c.setTime(newyear);
		int daysPassed = daysPassed();
		c.add(Calendar.DAY_OF_MONTH, daysPassed);
		Date date = c.getTime();
		return date;
	}
	
	/** 天干地支 */
	public String ganzhi() {
		return ZhDate.ganzhi(this.lunarYear);
	}
	
	/** 生肖 */
	public String shengxiao() {
		return ZhDate.shengxiao(this.lunarYear);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj==null || ZhDate.class!=obj.getClass()) {
			return false;
		}
		ZhDate o = (ZhDate)(obj);
		return this.lunarYear==o.lunarYear
				&& this.lunarMonth==o.lunarMonth
				&& this.lunarDay==o.lunarDay
				&& this.leapMonth==o.leapMonth;
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(lunarYear).append(lunarMonth).append(lunarDay).append(leapMonth).hashCode();
	}

	/**
	 * @return 农历2020年1月1日
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("农历").append(this.lunarYear).append("年");
		if(this.leapMonth) {
			sb.append("闰");
		}
		sb.append(this.lunarMonth).append("月").append(this.lunarDay).append("日");
		return sb.toString();
	}
	
	/**
	 * 农历日期中文表示
	 * @return 二零二零年正月二十八
	 */
	public String chinese() {
		StringBuilder sb = new StringBuilder();
		String.valueOf(this.lunarYear).chars().forEach(c -> sb.append(ZHNUMS.charAt(c-'0')));
		sb.append("年");
		if(this.leapMonth) {
			sb.append("闰");
		}
		if(this.lunarMonth==1) {
			sb.append("正");
		}else if(this.lunarMonth==11) {
			sb.append("冬");
		}else if(this.lunarMonth==12) {
			sb.append("腊");
		}else {
			sb.append(ZHNUMS.charAt(lunarMonth));
		}
		sb.append("月");
		if(this.lunarDay<=10) {
			sb.append("初").append(ZHNUMS.charAt(this.lunarDay));
		}else if(this.lunarDay<20) {
			sb.append("十").append(ZHNUMS.charAt(this.lunarDay-10));
		}else if(this.lunarDay==20) {
			sb.append("二十");
		}else if(this.lunarDay<30) {
			sb.append("二十").append(ZHNUMS.charAt(this.lunarDay-20));
		}else {
			sb.append("三十");
		}
		return sb.toString();
	}

	public int getLunarYear() {
		return lunarYear;
	}

	public int getLunarMonth() {
		return lunarMonth;
	}

	public int getLunarDay() {
		return lunarDay;
	}

	public boolean isLeapMonth() {
		return leapMonth;
	}

	/** 计算当前农历日期和当年农历新年之间的天数差值 */
	private int daysPassed() {
		List<Integer> monthDays = decode(yearCode);
		int monthLeap =  (yearCode & 0xf);
		int daysPassedMonth;
		if((monthLeap == 0) || (lunarMonth < monthLeap)) {
			daysPassedMonth = sum(monthDays, 0, lunarMonth-1);
		}else if(!leapMonth && lunarMonth==monthLeap) {
			daysPassedMonth = sum(monthDays, 0, lunarMonth-1);
		}else {
			daysPassedMonth = sum(monthDays, 0, lunarMonth);
		}
		return daysPassedMonth+lunarDay-1;
	}
	private static int sum(List<Integer> monthDays, int from, int to) {
		int sum = 0;
		for(int i=from;i<to;i++) {
			sum += monthDays.get(i);
		}
		return sum;
	}
	private static Date parse(String day) {
		try {
			return new SimpleDateFormat("yyyyMMdd").parse(day);
		}catch(ParseException e) {
			throw new RuntimeException("日期格式错误："+day);
		}
	}
	private static List<Integer> accumulate(List<Integer> monthDays) {
		List<Integer> accumulateDays = new ArrayList<>();
		int sum = 0;
		for(Integer monthDay : monthDays) {
			sum += monthDay;
			accumulateDays.add(sum);
		}
		return accumulateDays;
	}
	private static int anum(int anum, int mod) {
		anum = anum % mod;
		return anum < 0 ? anum+mod : anum;
	}
}
