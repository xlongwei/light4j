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
	private static final String MISS_FUNCTION_PARAMS = "Miss function params";
	private static final String NO_EXPRESSION = "No Expression";
	private static final String MISS_OPERANDS = "Miss operands";
	private static final String NEGATIVE = "~";

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
	private static void addFunction(String function, Integer params, FunctionTypes functionType) {
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
	private static void addOperator(String operator, Integer priority, OperatorTypes operatorType) {
		priorities.put(operator, priority);
		operatorTypes.put(operator, operatorType);
	}
	
	/**
	 * 执行初始化
	 */
	private static void init() {
		priorities = new HashMap<String, Integer>(8);
		operatorTypes = new HashMap<String, OperatorTypes>(8);
		functionTypes = new HashMap<String, FunctionTypes>(16);
		functionParams = new HashMap<String, Integer>(16);
		constants = new HashMap<String, Double>(4);
		radix = new HashMap<Carriage, Integer>(4);
		
		// 添加运算符，(符号，优先级，类型)
		addOperator("+", 1, OperatorTypes.ADD);
		addOperator("-", 1, OperatorTypes.SUBTRACT);
		addOperator("*", 2, OperatorTypes.MULTIPLY);
		addOperator("/", 2, OperatorTypes.DIVIDE);
		addOperator("%", 2, OperatorTypes.MOD);
		addOperator("^", 3, OperatorTypes.POWER);
		addOperator(NEGATIVE, 4, OperatorTypes.NEGATIVE);
		
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
	
	private static Map<Carriage, Integer>		radix;
																
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
		if (exp == null) { throw new ExpException(NO_EXPRESSION); }
		operators.clear(); operands.clear();
		TokenTypes lastTokenType = null;
		for (int i = 0; i < exp.length(); i++) {
			char c = exp.charAt(i);
			if (c == '(') {
				lastTokenType = leftP(lastTokenType);
			} else if (c == ')') {
				lastTokenType = rightP(lastTokenType);
			} else if (Character.isDigit(c) || (c == '.')) {
				StringBuilder sb = new StringBuilder();
				boolean isDouble = false;
				do {
					if (c == '.') {
						if (isDouble == false) {
							isDouble = true;
						} else {
							throw new ExpException("Invalid Dot");
						}
					}
					sb.append(c);
				} while ((++i < exp.length()) && (((c=exp.charAt(i)) == '.') || Character.isDigit(c) || ("abcdef".indexOf(c) > -1)));
				i--; lastTokenType = operand(sb, isDouble);
			} else if (operatorTypes.keySet().contains(String.valueOf(c))) {
				lastTokenType = operator(lastTokenType, c);
			} else if (Character.isLetter(c)) {
				StringBuilder operator = new StringBuilder();
				do {
					operator.append(c);
				}
				while ((++i < exp.length()) && ((Character.isLetter(c=exp.charAt(i))) || (Character.isDigit(c))));
				String function = operator.toString();
				if (functionParams.containsKey(operator.toString().toLowerCase())) {
					function = function.toLowerCase();
					lastTokenType = checkFunction(lastTokenType, c);
					i++; operators.push(function);
					int params = functionParams.get(function);
					// 用表达式递归提取params-1个参数
					for (int j = 1; j < params; j++) {
						int paramIdx = findParam(exp, ',', i);
						if (paramIdx != -1) {
							// 每个参数都是表达式，其中仍然可以嵌套函数
							operands.push(ExpUtil.exp(exp.substring(i, paramIdx)).context(context).parse().getResult());
							i = paramIdx + 1;
						//支持log(10)等价于log(e,10)
						} else if("log".equals(function) && j==1){ 
							operands.push(Math.E);
						}else {
							throw new ExpException(MISS_FUNCTION_PARAMS);
						}
					}
					// 以右括号结束函数式
					int rpIdx = findParam(exp, ')', i);
					// 最后一个参数
					if (rpIdx != -1) {
						operands.push(ExpUtil.exp(exp.substring(i, rpIdx)).context(context).parse().getResult());
						i = rpIdx;
					} else {
						throw new ExpException(MISS_FUNCTION_PARAMS);
					}
					// 计算函数
					computeFunction();
					lastTokenType = TokenTypes.RIGHTP;
				} else if ((context!=null && context.containsKey(function)) || (constants.containsKey(function.toLowerCase()) && (!"e".equals(function.toLowerCase()) ? true : carriage != Carriage.HEX))) {
					operands.push(context!=null && context.containsKey(function) ? context.get(function) : constants.get(function.toLowerCase()));
					i--; lastTokenType = TokenTypes.NUMBER;
				} else if ((carriage == Carriage.HEX) && isHexNumber(function)) {
					operands.push(Integer.parseInt(function, radix.get(carriage)));
					i--; lastTokenType = TokenTypes.NUMBER;
				} else {
					throw new ExpException("Unsupported function:" + function);
				}
			} else {
				throw new ExpException("Unknown Character:" + c);
			}
		}
		return lastCompute();
	}

	private TokenTypes checkFunction(TokenTypes lastTokenType, char c) throws ExpException {
		// sin(1)
		if ((lastTokenType != null)
				// 1+sin(1)
				&& (lastTokenType != TokenTypes.OPERATOR)
				// 1+(sin(1))
				&& (lastTokenType != TokenTypes.LEFTP)) {
			throw new ExpException(
					"Function should follow an operator");
		}
		// sin(1)
		char leftP = '(';
		if (c != leftP) {
			throw new ExpException(
					"function should followed by a (");
		} 
		return TokenTypes.FUNCTION;
	}

	private ExpUtil lastCompute() throws ExpException {
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

	private TokenTypes operator(TokenTypes lastTokenType, char c) throws ExpException {
		String operator = String.valueOf(c);
		boolean isMinus = "-".equals(operator) && ((lastTokenType == null) || (lastTokenType == TokenTypes.LEFTP));
		boolean isPlus = "+".equals(operator) && ((lastTokenType == null) || (lastTokenType == TokenTypes.LEFTP));
		if (isMinus) {
			// 处理负号
			operators.push(NEGATIVE);
		} else if(isPlus == false) {
			// 2+1
			if ((lastTokenType != TokenTypes.NUMBER)
					// (2)+1
					&& (lastTokenType != TokenTypes.RIGHTP)) {
				throw new ExpException(MISS_OPERANDS);
			} else {
				if ((operators.size() > 0)
						&& !"(".equals(operators.peek())
						// [*] <=> +
						&& (priority(operator, operators.peek()) <= 0)) {
					// compute '*'
					computeOperator();
				}
				// push '+'
				operators.push(operator);
				lastTokenType = TokenTypes.OPERATOR;
			}
		}
		return lastTokenType;
	}

	private TokenTypes operand(StringBuilder sb, boolean isDouble) throws ExpException {
		// 小数
		if (isDouble == true) {
			try {
				operands.push(Double.parseDouble(sb.toString()));
			} catch (NumberFormatException e) {
				throw new ExpException("a dot is not allowed in radix: " + radix.get(carriage));
			}
		// 整数
		} else {
			try {
				operands.push(Integer.parseInt(sb.toString(), radix.get(carriage)));
			} catch (NumberFormatException e) {
				throw new ExpException("illegal character for radix: "+ radix.get(carriage));
			}
		}
		return TokenTypes.NUMBER;
	}

	private TokenTypes rightP(TokenTypes lastTokenType) throws ExpException {
		// 没有左括号则异常
		String leftP = "(";
		if (!operators.contains(leftP)) {
			throw new ExpException("Miss left parenthese");
		} else {
			// ()
			if (lastTokenType == TokenTypes.LEFTP) {
				throw new ExpException("Empty parentheses");
			} else {
				// [(,+] <=> (1+1)
				while ((operators.size() > 0)
						&& (operators.peek().charAt(0) != '(')) {
					// compute '+'
					computeOperator();
				}
				// pop '('
				operators.pop();
				lastTokenType = TokenTypes.RIGHTP;
			}
		}
		return lastTokenType;
	}

	private TokenTypes leftP(TokenTypes lastTokenType) throws ExpException {
		// (1+1)
		if ((lastTokenType != null)
				// 3-(1+1)
				&& (lastTokenType != TokenTypes.OPERATOR)
				// ((1+1)*2)
				&& (lastTokenType != TokenTypes.LEFTP)
				// sin(1)
				&& (lastTokenType != TokenTypes.FUNCTION)) {
			throw new ExpException(
					"Left parenthese should follow an operator");
		} else {
			operators.push(String.valueOf('('));
			lastTokenType = TokenTypes.LEFTP;
		}
		return lastTokenType;
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
	
	public ExpUtil context(String name, Number value) {
		if(context == null) {
			context = new HashMap<>(4);
		}
		context.put(name, value);
		return this;
	}
	
	/** 批量设置上下文 */
	public ExpUtil context(Map<String, Number> ctx) {
		if(ctx!=null && ctx.size()>0) {
			ctx.forEach(this::context);
		}
		return this;
	}
	
	public static ExpUtil exp(String exp) {
		return new ExpUtil(exp);
	}
	
	/**
	 * 计算函数，函数运算前已检查参数个数
	 * 
	 * @throws ExpException
	 */
	private void computeFunction() throws ExpException {
		String function = operators.pop();
		FunctionTypes functionType = functionTypes.get(function);
		switch (functionType) {
			case SIN: operands.push(Math.sin(operands.pop().doubleValue())); break;
			case COS: operands.push(Math.cos(operands.pop().doubleValue())); break;
			case TAN: operands.push(Math.tan(operands.pop().doubleValue())); break;
			case SINH: operands.push(Math.sinh(operands.pop().doubleValue())); break;
			case COSH: operands.push(Math.cosh(operands.pop().doubleValue())); break;
			case TANH: operands.push(Math.tanh(operands.pop().doubleValue())); break;
			case ASIN: operands.push(Math.asin(operands.pop().doubleValue())); break;
			case ACOS: operands.push(Math.acos(operands.pop().doubleValue())); break;
			case ATAN: operands.push(Math.atan(operands.pop().doubleValue())); break;
			case SQRT: operands.push(Math.sqrt(operands.pop().doubleValue())); break;
			case CBRT: operands.push(Math.cbrt(operands.pop().doubleValue())); break;
			case CEIL: operands.push(Math.ceil(operands.pop().doubleValue())); break;
			case FLOOR: operands.push(Math.floor(operands.pop().doubleValue())); break;
			case ABS: operands.push(Math.abs(operands.pop().doubleValue())); break;
			case ROUND: operands.push(Math.round(operands.pop().doubleValue())); break;
			case LN: operands.push(Math.log(operands.pop().doubleValue())); break;
			case LOG2: operands.push(Math.log(operands.pop().doubleValue())/Math.log(2.0)); break;
			case LOG10: operands.push(Math.log10(operands.pop().doubleValue())); break;
			case EXP: operands.push(Math.exp(operands.pop().doubleValue())); break;
			case LOG: case POW: case ATAN2: case FLZZ: case FLXZ: case NJZZ: case NJXZ: case JFZZ: case JFXZ: case DYZZ: case FLLX:
				binaryOp(functionType); break;
			case DYXZ: dyxz(); break;
			case NJZZLX: case NJXZLX: case DEBXLX: lxCalc(functionType); break;
			default:
				throw new ExpException("Unsupport function:" + function);
		}
	}

	private void dyxz() {
		//方法一：p2+p3期的年金现值-p3期的年金现值
		//方法二：先计算p2期的年金现值，再计算对应p3期的复利现值
		//方法三：先计算p2期的年金终值，再计算对应p2+p3期的复利现值
		double p3 = operands.pop().doubleValue();
		double p2 = operands.pop().doubleValue();
		double p1 = operands.pop().doubleValue();
		operands.push(((1-Math.pow(1+p1, -p2))/p1)*(1.0/Math.pow(1+p1, p3)));
	}

	private void binaryOp(FunctionTypes functionType) {
		double p2 = operands.pop().doubleValue();
		double p1 = operands.pop().doubleValue();
		switch(functionType) {
			case LOG: operands.push(Math.log(p2)/Math.log(p1)); break;
			case POW: operands.push(Math.pow(p1, p2)); break;
			case ATAN2: operands.push(Math.atan2(p1, p2)); break;
			case FLZZ: operands.push(Math.pow(1+p1, p2)); break;
			case FLXZ: operands.push(1.0/Math.pow(1+p1, p2)); break;
			case NJZZ: operands.push((Math.pow(1+p1, p2)-1)/p1); break;
			case NJXZ: operands.push((1-Math.pow(1+p1, -p2))/p1); break;
			case JFZZ: operands.push((Math.pow(1+p1, 1+p2)-1)/p1-1); break;
			case JFXZ: operands.push((1-Math.pow(1+p1, -p2+1))/p1+1); break;
			case DYZZ: operands.push((Math.pow(1+p1, 1+p2)-1)/p1-1); break;
			case FLLX: operands.push(Math.pow(p1, 1.0/p2)-1); break;
			default: break;
		}
	}

	private void lxCalc(FunctionTypes functionType) {
		//等额本息利息（本金，月供，期数）=年金现值利息（现值/月供，期数）
		double p2 = operands.pop().doubleValue();
		double p1 = operands.pop().doubleValue();
		if(FunctionTypes.DEBXLX==functionType) {
			p1 = operands.pop().doubleValue()/p1;
			functionType = FunctionTypes.NJXZLX;
		}
		double bLow = 0.000001, bHigh = 0.999999, bTemp = 0.0, xValue = 0.0, xTimes = 36.0, delta = 0.0001;
		do {
			bTemp = (bLow+bHigh)/2.0;
			xValue = FunctionTypes.NJZZLX==functionType ? (Math.pow(1+bTemp, p2)-1)/bTemp : (1-Math.pow(1+bTemp, -p2))/bTemp;
			if(Math.abs(xValue-p1)<delta) {
				break;
			}
			boolean isHigh = (xValue>p1&&FunctionTypes.NJZZLX==functionType) || (xValue<p1&&FunctionTypes.NJXZLX==functionType);
			if(isHigh) {
				bHigh = bTemp;
			} else {
				bLow = bTemp;
			}
			//限制循环次数，避免死循环
			xTimes -= 1.0; 
		}while(xTimes > 0.0);
		operands.push(bTemp);
	}
	
	/**
	 * 计算运算符
	 * 
	 * @throws ExpException
	 */
	private void computeOperator() throws ExpException {
		if (operands.size() == 0) { throw new ExpException(MISS_OPERANDS); }
		String operator = operators.pop();
		Number operand2 = operands.pop();
		if (NEGATIVE.equals(operator)) {
			if (operand2 instanceof Integer) {
				operands.push(0 - (Integer) operand2);
			} else {
				operands.push(0.0 - (Double) operand2);
			}
			return;
		} else if (operands.size() == 0) {
			throw new ExpException(MISS_OPERANDS);
		} else {
			Number operand1 = operands.pop();
			OperatorTypes operatorType = operatorTypes.get(operator);
			boolean isBothInteger = false;
			if ((operand1 instanceof Integer) && (operand2 instanceof Integer)) {
				isBothInteger = true;
			} else {
				isBothInteger = false;
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
						if (isBothInteger) {
							operands.push((Integer) operand1
									/ (Integer) operand2);
						} else {
							operands.push(operand1.doubleValue()
									/ operand2.doubleValue());
						}
					break;
				case MOD:
						if (isBothInteger) {
							operands.push((Integer) operand1
									% (Integer) operand2);
						} else {
							operands.push(operand1.doubleValue()
									% operand2.doubleValue());
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
		exp = StringUtil.toDBC(exp);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < exp.length(); i++) {
			if (!Character.isWhitespace(exp.charAt(i))) {
				sb.append(exp.charAt(i));
			}
		}
		return sb.toString();
	}
	
	private Carriage		carriage	= Carriage.DECIMAL;
	private String			exp			= null;
	private Stack<Number>	operands	= new Stack<Number>();
	private Stack<String>	operators	= new Stack<String>();
	private Number			result		= null;
	private Map<String, Number> context = null;
																
	public enum Carriage {
		/**
		 * 进制
		 */
		BINARY, DECIMAL, HEX, OCTAL
	}
	
	/**
	 * 表达式错误异常
	 * 
	 * @author hongwei
	 * @date 2009-10-14
	 */
	public class ExpException extends Exception {
		private static final long serialVersionUID = 3492746779913024756L;
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
		/**
		 * 函数
		 */
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
		/**
		 * 运算符
		 */
		ADD, DIVIDE, MOD, MULTIPLY, NEGATIVE, POWER, SUBTRACT
	}
	
	/**
	 * 标识符枚举
	 * 
	 * @author hongwei
	 * @date 2009-10-14
	 */
	private enum TokenTypes {
		/**
		 * 标识符类型
		 */
		FUNCTION, LEFTP, NUMBER, OPERATOR, RIGHTP
	}
}
