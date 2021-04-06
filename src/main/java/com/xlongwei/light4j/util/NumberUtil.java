package com.xlongwei.light4j.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.regex.Pattern;

import com.networknt.utility.StringUtils;

import lombok.extern.slf4j.Slf4j;



/**
 * 解析字符串值为基本类型：整数，长整数，浮点数，布尔值
 * @author xlongwei
 */
@Slf4j
public class NumberUtil {
	
	public static Integer parseInt(String number, Integer defValue) {
		try {
			if(StringUtils.isNotBlank(number)) {
				//[+-]?\\d+$
				return Integer.parseInt(number);
			}
		} catch (Exception e) {
			log.warn("fail to parse int: {}, ex: {}", number, e.getMessage());
		}
		return defValue;
	}

	public static Long parseLong(String number, Long defValue) {
		try {
			if(StringUtils.isNotBlank(number)) {
				//[+-]?\\d+$
				return Long.parseLong(number);
			}
		} catch (Exception e) {
			log.warn("fail to parse long: {}, ex: {}", number, e.getMessage());
		}
		return defValue;
	}

	public static Double parseDouble(String number, Double defValue) {
		try {
			if(StringUtils.isNotBlank(number)) {
				//[+-]?\\d+(\\.\\d*)?
				return Double.parseDouble(number);
			}
		} catch (Exception e) {
			log.warn("fail to parse double: {}, ex: {}", number, e.getMessage());
		}
		return defValue;
	}
	
	/**
	 * 字符串转换成BigDecimal，最好的方式就是new BigDecimal(string)
	 */
	public static BigDecimal parseBigDecimal(String number, BigDecimal defValue) {
		try {
			if(StringUtils.isNotBlank(number)) {
				//[+-]?\\d+(\\.\\d*)?
				return new BigDecimal(number);
			}
		} catch (Exception e) {
			log.warn("fail to parse big decimal: {}, ex: {}", number, e.getMessage());
		}
		return defValue;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T parse(String number, Class<T> type, T defValue) {
		if(type==Integer.class) {
			return (T) parseInt(number, (Integer)defValue);
		}else if(type==Long.class) {
			return (T) parseLong(number, (Long)defValue);
		}else if(type==Double.class) {
			return (T) parseDouble(number, (Double)defValue);
		}else if(type==Boolean.class) {
			return (T) parseBoolean(number, (Boolean)defValue);
		}else if(type==BigDecimal.class) {
			return (T) parseBigDecimal(number, (BigDecimal)defValue);
		}
		return null;
	}
	
    private static String[] trueStrings = {"true", "yes", "y", "on", "1"};
    private static String[] falseStrings = {"false", "no", "n", "off", "0"};
    public static Boolean parseBoolean(String value, Boolean defValue) {
		if(StringUtils.isNotBlank(value)) {
	        String stringValue = value.toString().toLowerCase();
	        for(int i=0; i<trueStrings.length; ++i) {
				if (trueStrings[i].equals(stringValue)) {
					return Boolean.TRUE;
				}
			}
	        for(int i=0; i<falseStrings.length; ++i) {
				if (falseStrings[i].equals(stringValue)) {
					return Boolean.FALSE;
				}
			}
		}
		return defValue;
    }
    
    /**@param defValue 不能为空 */
    @SuppressWarnings({ "unchecked", "rawtypes" })
	public static <E extends Enum> E parseEnum(String value, E defValue) {
    	try{
    		if(StringUtils.isBlank(value)) {
				return defValue;
			}
    		return (E)Enum.valueOf(defValue.getDeclaringClass(), value);
    	}catch(Exception e) {
    		log.warn("fail to parse enum {} : {}", defValue, value);
    		return defValue;
    	}
    }
    
    /**
     * [0#.,%\u2030-'] 0显示0，#不显示0，.小数点，,分隔符，%百分比，\u2030千分比‰，-负号，'字符'<br>
     * #### 4位整数<br>
     * ,####.00 4位分隔，两位小数，支持格式化金额为：亿,万,元.分<br>
     * ,###.00 3位分隔，两位小数，支持西式风格显示<br>
     * #.##% 百分比，支持1%和1.01%自动忽略0.00%<br>
     * #% 百分比，取整数部分<br>
     * #.###‰ 千分比<br>
     */
    public static String format(Number number, String format) {
    	return new DecimalFormat(format).format(number);
    }
    
    /** 是否有效的主键值 */
    public static boolean validId(Long id) {
    	return id!=null && id>0;
    }
    
    /** 去掉空格和逗号等分隔符 */
    public static String correctMoney(String money) {
    	if(StringUtil.isBlank(money)) {
    		return null;
    	}
		return money.chars().filter(c -> c == '.' || Character.isDigit(c))
				.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
    }
    
    /** 返回正确格式的数字或空串 */
    public static String correctNumber(String numbers) {
    	return StringUtil.isNumbers(numbers) ? numbers : "";
    }
    
    /** 判断是否有效数字金额 */
    public static boolean isMoney(String money) {
    	return money!=null && MONEY_PATTERN.matcher(money).matches();
    }
    
    /** 转换数字金额为大写汉字 */
	public static String daxie(String string) {
		if(!isMoney(string=correctMoney(string))) {
			return null;
		}
		BigDecimal money = new BigDecimal(string);
	    StringBuffer sb = new StringBuffer();
	    // -1 0 1
	    int signum = money.signum();
	    if (signum == 0) {
			return CN_ZEOR_FULL;
		}
	    //这里会进行金额的四舍五入
	    long number = money.movePointRight(MONEY_PRECISION).setScale(0, RoundingMode.HALF_UP).abs().longValue();
	    // 得到小数点后两位值
	    long scale = number % 100;
	    int numUnit = 0;
	    int numIndex = 0;
	    boolean getZero = false;
	    // 判断最后两位数，一共有四中情况：00 = 0, 01 = 1, 10, 11
	    if (scale <= 0) {
	        numIndex = 2;
	        number = number / 100;
	        getZero = true;
	    }
	    int ten = 10;
		if (scale > 0 && (scale % ten) <= 0) {
	        numIndex = 1;
	        number = number / ten;
	        getZero = true;
	    }
	    int zeroSize = 0;
	    while (true) {
	        if (number <= 0) {
	            break;
	        }
	        // 每次获取到最后一个数
	        numUnit = (int) (number % ten);
	        if (numUnit > 0) {
	            if ((numIndex == 9) && (zeroSize >= 3)) {
	                sb.insert(0, CN_UPPER_MONETRAY_UNIT[6]);
	            }
	            if ((numIndex == 13) && (zeroSize >= 3)) {
	                sb.insert(0, CN_UPPER_MONETRAY_UNIT[ten]);
	            }
	            sb.insert(0, CN_UPPER_MONETRAY_UNIT[numIndex]);
	            sb.insert(0, CN_UPPER_NUMBER[numUnit]);
	            getZero = false;
	            zeroSize = 0;
	        } else {
	            ++zeroSize;
	            if (!(getZero)) {
	                sb.insert(0, CN_UPPER_NUMBER[numUnit]);
	            }
	            if (numIndex == 2) {
	                if (number > 0) {
	                    sb.insert(0, CN_UPPER_MONETRAY_UNIT[numIndex]);
	                }
	            } else if (((numIndex - 2) % 4 == 0) && (number % 1000 > 0)) {
	                sb.insert(0, CN_UPPER_MONETRAY_UNIT[numIndex]);
	            }
	            getZero = true;
	        }
	        // 让number每次都去掉最后一个数
	        number = number / ten;
	        ++numIndex;
	    }
	    // 如果signum == -1，则说明输入的数字为负数，就在最前面追加特殊字符：负
	    if (signum == -1) {
	        sb.insert(0, CN_NEGATIVE);
	    }
	    // 输入的数字小数点后两位为"00"的情况，则要在最后追加特殊字符：整
	    if (scale <= 0) {
	        sb.append(CN_FULL);
	    }
	    return sb.toString();
	}
	
	/** 计算表达式的值 */
	public static String parseExp(String exp) {
		try {
			return new ExpUtil().parse(exp).getResult().toString();
		} catch (Exception e) {
			log.warn("fail to parse exp: {}, ex: {}", exp, e.getMessage());
			return null;
		}
	}
	
	/** 计算表达式的值 */
	public static String parseExp(String exp, Map<String, Number> context) {
		try {
			ExpUtil expUtil = ExpUtil.exp(exp);
			if(context!=null && context.size()>0) {
				context.forEach(expUtil::context);
			}
			return expUtil.parse().getResult().toString();
		} catch (Exception e) {
			log.warn("fail to parse exp: {}, ex: {}", exp, e.getMessage());
			return null;
		}
	}
	
    private static final String[] CN_UPPER_NUMBER = { "零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖" };
	private static final String[] CN_UPPER_MONETRAY_UNIT = { "分", "角", "元", "拾", "佰", "仟", "万", "拾", "佰", "仟", "亿", "拾", "佰", "仟", "兆", "拾", "佰", "仟" };
	private static final String CN_FULL = "整", CN_NEGATIVE = "负", CN_ZEOR_FULL = "零元整";
	private static final int MONEY_PRECISION = 2;
	private static final Pattern MONEY_PATTERN = Pattern.compile("[+-]?\\d{1,16}(\\.\\d{1,2})?");
}
