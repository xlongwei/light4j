package com.xlongwei.light4j.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * 表达式计算类
 * 
 * @author hongwei
 * @date 2009-10-14
 */
public class ExpUtil {
	/**
	 * 静态初始化，仅执行一次
	 */
	static {
		init();
	}
	
	/**
	 * 添加常量
	 * 
	 * @param name
	 *            常量名
	 * @param value
	 *            常量值
	 */
	private static void addConstants(String name, Double value) {
		constants.put(name, value);
	}
	
	/**
	 * 添加函数
	 * 
	 * @param function
	 *            函数名
	 * @param params
	 *            函数参数个数
	 * @param functionType
	 *            函数类型
	 */
	private static void addFunction(String function, Integer params,
			FunctionTypes functionType) {
		functionParams.put(function, params);
		functionTypes.put(function, functionType);
	}
	
	/**
	 * 添加运算符
	 * 
	 * @param operator
	 *            运算符
	 * @param priority
	 *            优先级
	 * @param operatorType
	 *            运算符类型
	 */
	private static void addOperator(String operator, Integer priority,
			OperatorTypes operatorType) {
		priorities.put(operator, priority);
		operatorTypes.put(operator, operatorType);
	}
	
	/**
	 * 执行初始化
	 */
	private static void init() {
		priorities = new HashMap<String, Integer>();
		operatorTypes = new HashMap<String, OperatorTypes>();
		functionTypes = new HashMap<String, FunctionTypes>();
		functionParams = new HashMap<String, Integer>();
		constants = new HashMap<String, Double>();
		radix = new HashMap<Carriage, Integer>();
		
		// 添加运算符，(符号，优先级，类型)
		addOperator("+", 1, OperatorTypes.ADD);
		addOperator("-", 1, OperatorTypes.SUBTRACT);
		addOperator("*", 2, OperatorTypes.MULTIPLY);
		addOperator("/", 2, OperatorTypes.DIVIDE);
		addOperator("%", 2, OperatorTypes.MOD);
		addOperator("^", 3, OperatorTypes.POWER);
		addOperator("N", 4, OperatorTypes.NEGATIVE);// 负号
		
		// 添加函数，(函数名，参数个数，类型)
		addFunction("sin", 1, FunctionTypes.SIN);
		addFunction("cos", 1, FunctionTypes.COS);
		addFunction("tan", 1, FunctionTypes.TAN);
		addFunction("asin", 1, FunctionTypes.ASIN);
		addFunction("acos", 1, FunctionTypes.ACOS);
		addFunction("atan", 1, FunctionTypes.ATAN);
		addFunction("sinh", 1, FunctionTypes.SINH);
		addFunction("cosh", 1, FunctionTypes.COSH);
		addFunction("tanh", 1, FunctionTypes.TANH);
		addFunction("sqrt", 1, FunctionTypes.SQRT);
		addFunction("cbrt", 1, FunctionTypes.CBRT);
		addFunction("ln", 1, FunctionTypes.LN);
		addFunction("log", 2, FunctionTypes.LOG);
		addFunction("log2", 1, FunctionTypes.LOG2);
		addFunction("log10", 1, FunctionTypes.LOG10);
		addFunction("exp", 1, FunctionTypes.EXP);
		addFunction("ceil", 1, FunctionTypes.CEIL);
		addFunction("floor", 1, FunctionTypes.FLOOR);
		addFunction("abs", 1, FunctionTypes.ABS);
		addFunction("round", 1, FunctionTypes.ROUND);
		addFunction("pow", 2, FunctionTypes.POW);
		addFunction("atan2", 2, FunctionTypes.ATAN2);
		
		//财务函数
		addFunction("flzz", 2, FunctionTypes.FLZZ);
		addFunction("flxz", 2, FunctionTypes.FLXZ);
		addFunction("njzz", 2, FunctionTypes.NJZZ);
		addFunction("njxz", 2, FunctionTypes.NJXZ);
		addFunction("jfzz", 2, FunctionTypes.JFZZ);
		addFunction("jfxz", 2, FunctionTypes.JFXZ);
		addFunction("dyzz", 2, FunctionTypes.DYZZ);
		addFunction("dyxz", 3, FunctionTypes.DYXZ);
		addFunction("fllx", 2, FunctionTypes.FLLX);
		addFunction("njzzlx", 2, FunctionTypes.NJZZLX);
		addFunction("njxzlx", 2, FunctionTypes.NJXZLX);
		addFunction("debxlx", 3, FunctionTypes.DEBXLX);
		
		// 添加常量，(常量名，数值)
		addConstants("pi", Math.PI);
		addConstants("e", Math.E);
		
		// 设置进制radix
		radix.put(Carriage.HEX, 16);
		radix.put(Carriage.DECIMAL, 10);
		radix.put(Carriage.OCTAL, 8);
		radix.put(Carriage.BINARY, 2);
	}
	
	/**
	 * 映射(常量,值):PI,E,
	 */
	private static Map<String, Double>			constants;
	/**
	 * 映射(函数名,参数个数)
	 */
	private static Map<String, Integer>			functionParams;
	/**
	 * 映射(函数,函数类型)
	 */
	private static Map<String, FunctionTypes>	functionTypes;
	/**
	 * 映射(运算符,运算符类型)
	 */
	private static Map<String, OperatorTypes>	operatorTypes;
	/**
	 * 映射(运算符,优先级)
	 */
	private static Map<String, Integer>			priorities;
	
	private static Map<Carriage, Integer>		radix;			// 各进制基数
																
	public ExpUtil() {
	}
	
	/**
	 * 用表达式构造
	 * 
	 * @param exp
	 *            要计算的表达式
	 */
	public ExpUtil(String exp) {
		this.exp = modify(exp);
	}
	
	public Carriage getCarriage() {
		return carriage;
	}
	
	public String getExp() {
		return exp;
	}
	
	public int getRadix(Carriage carriage) {
		return radix.get(carriage);
	}
	
	/**
	 * 返回结果
	 * 
	 * @return Integer or Double
	 */
	public Number getResult() {
		return result;
	}
	
	/**
	 * 解析表达式
	 * 
	 * @throws ExpException
	 *             表达式错误异常
	 */
	public ExpUtil parse() throws ExpException {
		if (exp == null) { throw new ExpException("No Expression"); }
		operators.clear();
		operands.clear();
		TokenTypes lastTokenType = null;
		for (int i = 0; i < exp.length(); i++) {
			if (exp.charAt(i) == '(') {// 左括号
				if ((lastTokenType != null)// (1+1)
						&& (lastTokenType != TokenTypes.OPERATOR)// 3-(1+1)
						&& (lastTokenType != TokenTypes.LEFTP)// ((1+1)*2)
						&& (lastTokenType != TokenTypes.FUNCTION)) {// sin(1)
					throw new ExpException(
							"Left parenthese should follow an operator");
				} else {
					operators.push(String.valueOf('('));
					lastTokenType = TokenTypes.LEFTP;
				}
			} else if (exp.charAt(i) == ')') {// 右括号
				if (!operators.contains("(")) {// 没有左括号则异常
					throw new ExpException("Miss left parenthese");
				} else {
					if (lastTokenType == TokenTypes.LEFTP) {// ()
						throw new ExpException("Empty parentheses");
					} else {
						while ((operators.size() > 0)// [(,+] <=> (1+1)
								&& (operators.peek().charAt(0) != '(')) {
							computeOperator();// compute '+'
						}
						operators.pop();// pop '('
						lastTokenType = TokenTypes.RIGHTP;
					}
				}
			} else if (Character.isDigit(exp.charAt(i))// 数字
					|| (exp.charAt(i) == '.')) {
				StringBuilder sb = new StringBuilder();
				boolean isDouble = false;
				do {
					if (exp.charAt(i) == '.') {
						if (isDouble == false) {
							isDouble = true;
						} else {
							throw new ExpException("Invalid Dot");// 小数点太多
						}
					}
					sb.append(exp.charAt(i));
					i++;
				} while ((i < exp.length())
						&& ((exp.charAt(i) == '.')
								|| Character.isDigit(exp.charAt(i)) || ("abcdef"
								.indexOf(exp.charAt(i)) > -1)));
				if (isDouble == true) {// 小数
					try {
						operands.push(Double.parseDouble(sb.toString()));
					} catch (NumberFormatException e) {
						throw new ExpException(
								"a dot is not allowed in radix: "
										+ radix.get(carriage));
					}
				} else {// 整数
					try {
						operands.push(Integer.parseInt(sb.toString(), radix
								.get(carriage)));
					} catch (NumberFormatException e) {
						throw new ExpException("illegal character for radix: "
								+ radix.get(carriage));
					}
				}
				i--;
				lastTokenType = TokenTypes.NUMBER;
			} else if (operatorTypes.keySet().contains(// 运算符
				String.valueOf(exp.charAt(i)))) {
				String operator = String.valueOf(exp.charAt(i));
				if (operator.equals("-")
						&& ((lastTokenType == null) || (lastTokenType == TokenTypes.LEFTP))) {
					operators.push("N");// 处理负号
				} else if (operator.equals("+")
						&& ((lastTokenType == null) || (lastTokenType == TokenTypes.LEFTP))) {
					continue;// 正号跳过
				} else {
					if ((lastTokenType != TokenTypes.NUMBER)// 2+1
							&& (lastTokenType != TokenTypes.RIGHTP)) {// (2)+1
						throw new ExpException("Miss operands");
					} else {
						if ((operators.size() > 0)
								&& !operators.peek().equals("(")
								&& (priority(operator, operators.peek()) <= 0)) {// [*]
							// <=>
							// +
							computeOperator();// compute '*'
						}
						operators.push(operator);// push '+'
						lastTokenType = TokenTypes.OPERATOR;
					}
				}
			} else if (Character.isLetter(exp.charAt(i))) {// 函数或常量
				StringBuilder operator = new StringBuilder();
				while ((i < exp.length())
						&& ((Character.isLetter(exp.charAt(i))) || (Character
								.isDigit(exp.charAt(i))))) {
					operator.append(exp.charAt(i));
					i++;
				}
				String function = operator.toString();
				if (functionParams.containsKey(operator.toString())) {// 函数
					if ((lastTokenType != null)// sin(1)
							&& (lastTokenType != TokenTypes.OPERATOR)// 1+sin(1)
							&& (lastTokenType != TokenTypes.LEFTP)) {// 1+(sin(1))
						throw new ExpException(
								"Function should follow an operator");
					} else {
						operators.push(function);
						lastTokenType = TokenTypes.FUNCTION;
						if (exp.charAt(i) != '(') {// sin(1)
							throw new ExpException(
									"function should followed by a (");
						} else {
							i++;
							int params = functionParams.get(function);// 参数个数
							for (int j = 1; j < params; j++) {// 用表达式递归提取params-1个参数
								int paramIdx = findParam(exp, ',', i);// 处理括号层次查找参数界限
								if (paramIdx != -1) {
									operands
											.push(new ExpUtil().parse(
												exp.substring(i, paramIdx))
													.getResult());// 每个参数都是表达式，其中仍然可以嵌套函数
									i = paramIdx + 1;
								} else if("log".equals(function) && j==1){ //支持log(10)等价于log(e,10)
									operands.push(Math.E);
								}else {// 缺少参数
									throw new ExpException(
											"Miss function params");
								}
							}
							int rpIdx = findParam(exp, ')', i);// 以右括号结束函数式
							if (rpIdx != -1) {// 最后一个参数
								operands.push(new ExpUtil().parse(
									exp.substring(i, rpIdx)).getResult());
								i = rpIdx;
							} else {
								throw new ExpException("Miss function params");
							}
							computeFunction();// 计算函数
							lastTokenType = TokenTypes.RIGHTP;
						}
					}
				} else if (constants.containsKey(function)
						&& (!function.equals("e") ? true
								: carriage != Carriage.HEX)) {// 常量，非十六进制的e
					i--;
					operands.push(constants.get(function));
					lastTokenType = TokenTypes.NUMBER;
				} else if ((carriage == Carriage.HEX) && isHexNumber(function)) {// 十六进制数
					i--;
					operands.push(Integer.parseInt(function, radix
							.get(carriage)));
					lastTokenType = TokenTypes.NUMBER;
				} else {// 不支持函数或常量
					throw new ExpException("Unsupported function:" + function);
				}
			} else {// 非法字符
				throw new ExpException("Unknown Character:" + exp.charAt(i));
			}
		}
		while (operators.size() > 0) {
			if (operators.peek().charAt(0) != '(') {
				computeOperator();
			} else {
				throw new ExpException("Wrong Parentheses");
			}
		}
		if ((operands.size() != 1)) {
			throw new ExpException("Wrong Expression");
		} else {
			result = operands.pop();
		}
		return this;
	}
	
	/**
	 * 解析表达式
	 * 
	 * @param exp
	 *            表达式
	 * @throws ExpException
	 *             表达式错误异常
	 */
	public ExpUtil parse(String exp) throws ExpException {
		this.exp = modify(exp);
		return this.parse();
	}
	
	/**
	 * 比较运算符优先级高低
	 */
	public int priority(char op1, char op2) {
		return priority(String.valueOf(op1), String.valueOf(op2));
	}
	
	/**
	 * 比较运算符优先级高低
	 */
	public int priority(String op1, String op2) {
		return priorities.get(op1).compareTo(priorities.get(op2));
	}
	
	/**
	 * set new carriage for exp
	 * 
	 * @param carriage
	 */
	public void setCarriage(Carriage carriage) {
		this.carriage = carriage;
	}
	
	public void setExp(String exp) {
		this.exp = exp;
	}
	
	/**
	 * 计算函数
	 * 
	 * @throws ExpException
	 */
	private void computeFunction() throws ExpException {
		// 函数运算前已检查参数个数
		String function = operators.pop();
		FunctionTypes functionType = functionTypes.get(function);
		double p1, p2, p3;
		switch (functionType) {
			case SIN:
				operands.push(Math.sin(operands.pop().doubleValue()));
				break;
			case COS:
				operands.push(Math.cos(operands.pop().doubleValue()));
				break;
			case TAN:
				operands.push(Math.tan(operands.pop().doubleValue()));
				break;
			case SINH:
				operands.push(Math.sinh(operands.pop().doubleValue()));
				break;
			case COSH:
				operands.push(Math.cosh(operands.pop().doubleValue()));
				break;
			case TANH:
				operands.push(Math.tanh(operands.pop().doubleValue()));
				break;
			case ASIN:
				operands.push(Math.asin(operands.pop().doubleValue()));
				break;
			case ACOS:
				operands.push(Math.acos(operands.pop().doubleValue()));
				break;
			case ATAN:
				operands.push(Math.atan(operands.pop().doubleValue()));
				break;
			case SQRT:
				operands.push(Math.sqrt(operands.pop().doubleValue()));
				break;
			case CBRT:
				operands.push(Math.cbrt(operands.pop().doubleValue()));
				break;
			case CEIL:
				operands.push(Math.ceil(operands.pop().doubleValue()));
				break;
			case FLOOR:
				operands.push(Math.floor(operands.pop().doubleValue()));
				break;
			case ABS:
				operands.push(Math.abs(operands.pop().doubleValue()));
				break;
			case ROUND:
				operands.push(Math.round(operands.pop().doubleValue()));
				break;
			case LN:
				operands.push(Math.log(operands.pop().doubleValue()));
				break;
			case LOG:
				operands.push(Math.log(operands.pop().doubleValue())/Math.log(operands.pop().doubleValue()));
				break;
			case LOG2:
				operands.push(Math.log(operands.pop().doubleValue())/Math.log(2.0));
				break;
			case LOG10:
				operands.push(Math.log10(operands.pop().doubleValue()));
				break;
			case EXP:
				operands.push(Math.exp(operands.pop().doubleValue()));
				break;
			case POW:
				p2 = operands.pop().doubleValue();
				p1 = operands.pop().doubleValue();
				operands.push(Math.pow(p1, p2));
				break;
			case ATAN2:
				p2 = operands.pop().doubleValue();
				p1 = operands.pop().doubleValue();
				operands.push(Math.atan2(p1, p2));
				break;
			case FLZZ:
				p2 = operands.pop().doubleValue();
				p1 = operands.pop().doubleValue();
				operands.push(Math.pow(1+p1, p2));
				break;
			case FLXZ:
				p2 = operands.pop().doubleValue();
				p1 = operands.pop().doubleValue();
				operands.push(1.0/Math.pow(1+p1, p2));
				break;
			case NJZZ:
				p2 = operands.pop().doubleValue();
				p1 = operands.pop().doubleValue();
				operands.push((Math.pow(1+p1, p2)-1)/p1);
				break;
			case NJXZ:
				p2 = operands.pop().doubleValue();
				p1 = operands.pop().doubleValue();
				operands.push((1-Math.pow(1+p1, -p2))/p1);
				break;
			case JFZZ:
				p2 = operands.pop().doubleValue();
				p1 = operands.pop().doubleValue();//对应年金终值，期数+1，系数-1
				operands.push((Math.pow(1+p1, 1+p2)-1)/p1-1);
				break;
			case JFXZ:
				p2 = operands.pop().doubleValue();
				p1 = operands.pop().doubleValue();//对应年金现值，期数-1，系数+1
				operands.push((1-Math.pow(1+p1, -p2+1))/p1+1);
				break;
			case DYZZ:
				p2 = operands.pop().doubleValue();
				p1 = operands.pop().doubleValue();
				operands.push((Math.pow(1+p1, 1+p2)-1)/p1-1);
				break;
			case DYXZ:
				p3 = operands.pop().doubleValue();//方法一：p2+p3期的年金现值-p3期的年金现值
				p2 = operands.pop().doubleValue();//方法二：先计算p2期的年金现值，再计算对应p3期的复利现值
				p1 = operands.pop().doubleValue();//方法三：先计算p2期的年金终值，再计算对应p2+p3期的复利现值
				operands.push(((1-Math.pow(1+p1, -p2))/p1)*(1.0/Math.pow(1+p1, p3)));
				break;
			case FLLX:
				p2 = operands.pop().doubleValue();
				p1 = operands.pop().doubleValue();
				operands.push(Math.pow(p1, 1.0/p2)-1);
				break;
			case NJZZLX:
			case NJXZLX:
			case DEBXLX: //等额本息利息（本金，月供，期数）=年金现值利息（现值/月供，期数）
				p2 = operands.pop().doubleValue();
				p1 = operands.pop().doubleValue();
				if(FunctionTypes.DEBXLX==functionType) {
					p1 = operands.pop().doubleValue()/p1;
					functionType = FunctionTypes.NJXZLX;
				}
				double BL = 0.000001, BH = 0.999999, BT = 0.0, XI = 0.0, XT = 36.0;
				do {
					BT = (BL+BH)/2.0;
					XI = FunctionTypes.NJZZLX==functionType ? (Math.pow(1+BT, p2)-1)/BT : (1-Math.pow(1+BT, -p2))/BT;
					if(Math.abs(XI-p1)<0.0001) break;
					if((XI>p1&&FunctionTypes.NJZZLX==functionType) || (XI<p1&&FunctionTypes.NJXZLX==functionType)) BH = BT;
					else BL = BT;
					XT -= 1.0; //限制循环次数，避免死循环
				}while(XT > 0.0);
				operands.push(BT);
				break;
			default:
				throw new ExpException("Unsupport function:" + function);
		}
	}
	
	/**
	 * 计算运算符
	 * 
	 * @throws ExpException
	 */
	private void computeOperator() throws ExpException {
		if (operands.size() == 0) { throw new ExpException("Miss operands"); }
		String operator = operators.pop();
		Number operand2 = operands.pop();
		if (operator.equals("N")) {// 一元负号
			if (operand2 instanceof Integer) {
				operands.push(0 - (Integer) operand2);
			} else {
				operands.push(0.0 - (Double) operand2);
			}
			return;
		} else if (operands.size() == 0) {// 二元算符少操作数
			throw new ExpException("Miss operands");
		} else {
			Number operand1 = operands.pop();
			OperatorTypes operatorType = operatorTypes.get(operator);
			boolean isBothInteger = false;
			if ((operand1 instanceof Integer) && (operand2 instanceof Integer)) {
				isBothInteger = true;// 整数运算
			} else {
				isBothInteger = false;// 小数运算
			}
			switch (operatorType) {
				case ADD:
					if (isBothInteger) {
						operands.push((Integer) operand1 + (Integer) operand2);
					} else {
						operands.push(operand1.doubleValue()
								+ operand2.doubleValue());
					}
					break;
				case SUBTRACT:
					if (isBothInteger) {
						operands.push((Integer) operand1 - (Integer) operand2);
					} else {
						operands.push(operand1.doubleValue()
								- operand2.doubleValue());
					}
					break;
				case MULTIPLY:
					if (isBothInteger) {
						operands.push((Integer) operand1 * (Integer) operand2);
					} else {
						operands.push(operand1.doubleValue()
								* operand2.doubleValue());
					}
					break;
				case DIVIDE:
					if (((operand2 instanceof Integer) && operand2
							.equals(Integer.valueOf(0)))
							|| ((operand2 instanceof Double) && (operand2
									.equals(Double.valueOf(0.0))))) {
						throw new ExpException("Divide By Zero");
					} else {
						if (isBothInteger) {
							operands.push((Integer) operand1
									/ (Integer) operand2);
						} else {
							operands.push(operand1.doubleValue()
									/ operand2.doubleValue());
						}
					}
					break;
				case MOD:
					if (((operand2 instanceof Integer) && operand2
							.equals(Integer.valueOf(0)))
							|| ((operand2 instanceof Double) && (operand2
									.equals(Double.valueOf(0.0))))) {
						throw new ExpException("Mod By Zero");
					} else {
						if (isBothInteger) {
							operands.push((Integer) operand1
									% (Integer) operand2);
						} else {
							operands.push(operand1.doubleValue()
									% operand2.doubleValue());
						}
					}
					break;
				case POWER:
					if (isBothInteger) {
						operands.push((int) Math.pow(operand1.doubleValue(),
							operand2.doubleValue()));
					} else {
						operands.push(Math.pow(operand1.doubleValue(), operand2
								.doubleValue()));
					}
					break;
				default:
					throw new ExpException("Unsupport operator:" + operator);
			}
		}
	}
	
	/**
	 * 处理括号层次查找参数界限
	 * 
	 * @param exp
	 *            表达式
	 * @param split
	 *            参数分隔符
	 * @param from
	 *            参数的起始索引
	 * @return 参数的结束索引
	 */
	private int findParam(String exp, char split, int from) {
		int layer = 0, len = exp.length();
		for (int i = from; i < len; i++) {
			if (exp.charAt(i) == '(') {
				layer++;
			} else if (exp.charAt(i) == ')') {
				if ((exp.charAt(i) == split) && (layer == 0)) {
					return i;
				} else {
					layer--;
				}
			} else if (exp.charAt(i) == split) {
				if (layer == 0) {
					return i;
				} else {
					continue;
				}
			} else {
				continue;
			}
		}
		return -1;
	}
	
	private boolean isHexNumber(String function) {
		String hexNumbers = "0123456789abcdef";
		for (int i = 0; i < function.length(); i++) {
			if (hexNumbers.indexOf(function.charAt(i)) == -1) { return false; }
		}
		return true;
	}
	
	/**
	 * 全角转半角，去掉空白
	 * 
	 * @param exp
	 *            源表达式
	 */
	private String modify(String exp) {
		String BJexp = StringUtil.toDBC(exp).toLowerCase();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < BJexp.length(); i++) {
			if (!Character.isWhitespace(BJexp.charAt(i))) {
				sb.append(BJexp.charAt(i));
			}
		}
		return sb.toString();
	}
	
	private Carriage		carriage	= Carriage.DECIMAL;	// 十进制
	private String			exp			= null;				// 表达式
	private Stack<Number>	operands	= new Stack<Number>();	// 操作数
	private Stack<String>	operators	= new Stack<String>();	// 运算符
	private Number			result		= null;				// 结果
																
	public enum Carriage {
		BINARY, DECIMAL, HEX, OCTAL
	}
	
	/**
	 * 表达式错误异常
	 * 
	 * @author hongwei
	 * @date 2009-10-14
	 */
	@SuppressWarnings("serial")
	public class ExpException extends Exception {
		public ExpException(String message) {
			super(message);
		}
	}
	
	/**
	 * 函数类型枚举
	 * 
	 * @author hongwei
	 * @date 2009-10-14
	 */
	public enum FunctionTypes {
		ABS, ACOS, ASIN, ATAN, ATAN2, CBRT, CEIL, COS, COSH, EXP, FLOOR, LN, LOG, LOG2, LOG10, POW, ROUND, SIN, SINH, SQRT, TAN, TANH, FLZZ, FLXZ, NJZZ, NJXZ, JFZZ, JFXZ, DYZZ, DYXZ
		, FLLX, NJZZLX, NJXZLX, DEBXLX
	}
	
	/**
	 * 运算符类型枚举
	 * 
	 * @author hongwei
	 * @date 2009-10-14
	 */
	private enum OperatorTypes {
		ADD, DIVIDE, MOD, MULTIPLY, NEGATIVE, POWER, SUBTRACT
	}
	
	/**
	 * 标识符枚举
	 * 
	 * @author hongwei
	 * @date 2009-10-14
	 */
	private enum TokenTypes {
		FUNCTION, LEFTP, NUMBER, OPERATOR, RIGHTP
	}
}
