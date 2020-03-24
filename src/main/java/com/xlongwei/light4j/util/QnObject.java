package com.xlongwei.light4j.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.xlongwei.light4j.util.QnObject.Condition.Cond;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用法：
 * <pre>
 * try{
 *     String qn = "您好：{姓名}(({性别}=男 and {年龄}>60) or {机构}=北京、上海)[老({机构}=北京)[{机构}]先生]";
 *     QnObject qnObj = QnObject.fromString(qn);
 *     String js = QnObject.toJs(qnObj);
 * }catch(QnException ex){
 *     int pos = ex.getPos();
 *     String error = ex.getError();
 * }
 * String data = "{姓名:'老张',性别:'男',年龄: "60", 机构: "北京"}";
 * String result = "您好：老张老北京先生"; //data + js = QnObject.toJs(qnObj)
 * </pre>
 * @author xlongwei
 * @date 2020-03-24
 */
public class QnObject {
	private static final char VAR_START = '{';
	private static final char VAR_END = '}';
	private static final String DATA_DOT = "data.";
	private static final char CONDITION_START = '(';
	private static final String CONDITION_INNER = ")[";
	private static final char CONDITION_END = ']';
	private static final char COND_START = '(';
	private static final char COND_END = ')';
	private static final String AND = " and ";
	private static final String OR = " or ";
	private static final String COND = String.valueOf(COND_START);
	private static final String EQUAL = "=";
	private static final String[] OPS = new String[] {EQUAL, "<", ">"};
	private static final String[] OPS2 = new String[] {"<=", ">=", "!="};
	private static final char BLANK = ' ';
	private static final char PAUSE = '、';
	private List<Object> nodes = new LinkedList<>();

	public static QnObject fromString(String qn) {
		QnObject obj = new QnObject();
		int from = 0, len = qn.length();
		int p = indexOf(qn, from, len, VAR_START, CONDITION_START);
		while(p != -1) {
			if(p > from) {
				obj.nodes.add(qn.substring(from, p));
			}
			char c = qn.charAt(p);
			from = p;
			if(VAR_START == c) {
				p = fromVar(obj, qn, from);
				from = p+1;
			}else if(CONDITION_START == c) {
				p = fromCondition(obj, qn, from, len);
				from = p+1;
			}
			p = indexOf(qn, from, len, VAR_START, CONDITION_START);
		}
		if(len > from) {
			obj.nodes.add(qn.substring(from));
		}
		return obj;
	}

	private static int fromCondition(QnObject obj, String qn, int from, int len) {
		int p = qn.indexOf(CONDITION_INNER, from+1);
		if(p==-1) {
			throw new QnException(from, QnException.MISS_CONDITION_INNER);
		}else {
			int e = conditionEnd(qn, p+2, len, 0);
			if(e==-1) {
				throw new QnException(p+1, QnException.MISS_CONDITION_END);
			}else {
				Cond cond = Cond.from(qn, from+1, p);
				String content = qn.substring(p+2, e);
				QnObject contentObj = QnObject.fromString(content);
				obj.nodes.add(new Condition(cond, contentObj));
			}
			return e;
		}
	}

	private static int fromVar(QnObject obj, String qn, int from) {
		int p = qn.indexOf(VAR_END);
		if(p==-1) {
			throw new QnException(from+1, QnException.MISS_VAR_END);
		}else if(p==from+1) {
			throw new QnException(from+1, QnException.EMPTY_VAR);
		}else {
			obj.nodes.add(new Var(qn.substring(from+1, p)));
		}
		return p;
	}
	
	public static String toJs(QnObject qnObj) {
		StringBuilder sb = new StringBuilder("var result = '';\n");
		qnObj.toJs(sb);
		sb.append("result");
		return sb.toString();
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(Object obj : nodes) {
			sb.append(obj.toString());
		}
		return sb.toString();
	}

	private void toJs(StringBuilder sb) {
		for(Object obj : nodes) {
			if(obj instanceof String) {
				sb.append("result += '").append(obj).append("';\n");
			}else if(obj instanceof Var) {
				((Var)obj).toJs(sb);
			}else if(obj instanceof Condition) {
				((Condition)obj).toJs(sb);
			}
		}
	}

	@AllArgsConstructor
	static class Var {
		String name;
		@Override
		public String toString() {
			return new StringBuilder().append(VAR_START).append(name).append(VAR_END).toString();
		}
		public void toJs(StringBuilder sb) {
			//result += !data.name ? {name} : data.name
			sb.append("result += !").append(DATA_DOT).append(name).append(" ? '{").append(name).append("}' : ").append(DATA_DOT).append(name).append(";\n");
		}
	}
	
	@AllArgsConstructor
	static class Condition {
		Cond cond;
		QnObject obj;
		@Override
		public String toString() {
			return new StringBuilder().append(CONDITION_START).append(cond.toString()).append(CONDITION_INNER).append(obj.toString()).append(CONDITION_END).toString();
		}
		public void toJs(StringBuilder sb) {
			sb.append("if(");
			cond.toJs(sb);
			sb.append("){\n");
			obj.toJs(sb);
			sb.append("}\n");
		}
		static class Cond {
			/** 0 a=b, 1 and, 2 or */
			private int type = 0;
			private Comp comp;
			private List<Cond> conds = new ArrayList<>();
			static Cond from(String qn, int from, int to) {
				List<String> tokens = tokens(qn, from, to);
				if(tokens.isEmpty()) {
					return null;
				}
				Cond cond = new Cond();
				if(tokens.size()==1) {
					cond.comp = Comp.from(qn, from, to);
					return cond;
				}
				cond.type = tokens.contains(AND) ? 1 : 2;
				for(String token : tokens) {
					if(AND.equals(token) || OR.equals(token)) {
						from += token.length();
						continue;
					}else {
						boolean b = COND_START == qn.charAt(from);
						if(b) {
							cond.conds.add(Cond.from(qn, from+1, from+token.length()+1));
							from += token.length()+2;
						}else {
							cond.conds.add(Cond.from(qn, from, from+token.length()));
							from += token.length();
						}
					}
				}
				return cond;
			}
			@Override
			public String toString() {
				if(type == 0) {
					return comp.toString();
				}else {
					StringBuilder sb = new StringBuilder();
					boolean first = true;
					for(Cond cond : conds) {
						if(first==true) {
							first = false;
						}else {
							sb.append(type==1 ? AND : OR);
						}
						if(cond.type==0) {
							sb.append(cond.toString());
						}else {
							sb.append(COND_START).append(cond.toString()).append(COND_END);
						}
					}
					return sb.toString();
				}
			}
			public void toJs(StringBuilder sb) {
				if(type==0) {
					comp.toJs(sb);
				}else {
					boolean first = true;
					for(Cond cond : conds) {
						if(first==true) {
							first=false;
						}else {
							sb.append(type==1 ? " && " : " || ");
						}
						if(cond.type == 0) {
							cond.toJs(sb);
						}else {
							sb.append(COND_START);
							cond.toJs(sb);
							sb.append(COND_END);
						}
					}
				}
			}
			public static List<String> tokens(String qn, int from, int to){
				List<String> tokens = new ArrayList<>();
				int p = indexOf(qn, from, to, COND, AND, OR);
				boolean hasAnd = false, hasOr = false, lastCond = false;
				while(p!=-1) {
					if(p > from) {
						tokens.add(qn.substring(from, p));
						lastCond = true;
					}
					from = p;
					if(COND_START == qn.charAt(p)) {
						p = condEnd(qn, from+1, to, 0);
						if(p == -1) {
							throw new QnException(from, QnException.MISS_CONDITION_END);
						}else {
							tokens.add(qn.substring(from+1, p));
						}
						from = p+1;
						if(lastCond==true) {
							throw new QnException(from, QnException.MISS_AND_OR);
						}else {
							lastCond = true;
						}
					}else if(p == qn.indexOf(AND, p)) {
						if(hasOr==true) {
							throw new QnException(from, QnException.BAD_AND_OR);
						}
						hasAnd = true;
						tokens.add(AND);
						from += AND.length();
						if(lastCond==false) {
							throw new QnException(from, QnException.BAD_AND);
						}else {
							lastCond = false;
						}
					}else if(p == qn.indexOf(OR, p)){
						if(hasAnd==true) {
							throw new QnException(from, QnException.BAD_AND_OR);
						}
						hasOr = true;
						tokens.add(OR);
						from += OR.length();
						if(lastCond==false) {
							throw new QnException(from, QnException.BAD_OR);
						}else {
							lastCond = false;
						}
					}
					p = indexOf(qn, from, to, COND, AND, OR);
				}
				if(lastCond==false) {
					if(from < to) {
						String token = qn.substring(from, to).trim();
						if(token.isEmpty()==false) {
							tokens.add(qn.substring(from, to));
						}else {
							throw new QnException(from, QnException.BAD_AND_OR2);
						}
					}else {
						throw new QnException(from, QnException.BAD_AND_OR2);
					}
				}
				return tokens;
			}

			public static int condEnd(String qn, int from, int to, int level) {
				int p = indexOf(qn, from, to, COND_START, COND_END);
				char c = qn.charAt(p);
				if(COND_END == c) {
					return level<=0 ? p : condEnd(qn, p+1, to, level-1);
				}else {
					p = qn.indexOf(CONDITION_INNER, p+1);
					if(p==-1) {
						throw new QnException(from, QnException.MISS_CONDITION_INNER);
					}else {
						return conditionEnd(qn, p+2, to, level+1);
					}
				}
			}
		}
		static class Comp {
			private String left, op, right;
			public static Comp from(String qn, int from, int to) {
				int p1 = indexOf(qn, from, to, OPS2), p2 = p1==-1 ? indexOf(qn, from, to, OPS) : -1;
				p1 = minIndexOf(p1, p2);
				if(p1 == -1) {
					throw new QnException(from, QnException.MISS_COND_OP);
				}else if(p1 <= from+1 || p1 >= to-1) {
					throw new QnException(from, QnException.MISS_COND_VAR);
				}
				Comp comp = new Comp();
				comp.left = qn.substring(from, p1).trim();
				p2 = p2==-1 ? p1+2 : p1+1;
				comp.op = qn.substring(p1, p2);
				comp.right = qn.substring(p2, to).trim();
				//检查left和right是否还有比较符
				if(indexOf(qn, from, p1, OPS) != -1) {
					throw new QnException(p1, QnException.MISS_AND_OR);
				}
				if(indexOf(qn, p2, to, OPS) != -1) {
					throw new QnException(p2, QnException.MISS_AND_OR);
				}
				//检查left和right变量语法
				p1 = comp.left.indexOf(VAR_START);
				p2 = comp.left.indexOf(VAR_END);
				boolean missVarEnd = (p1==-1 && p2!=-1) || (p1!=-1 && p2==-1);
				if(missVarEnd) {
					throw new QnException(from, QnException.MISS_VAR_END);
				}else if(p1!=-1 && comp.left.length()<=2) {
					throw new QnException(from, QnException.EMPTY_VAR);
				}else if(comp.left.indexOf(BLANK)!=-1) {
					throw new QnException(from, QnException.BAD_VAR);
				}
				p1 = comp.right.indexOf(VAR_START);
				p2 = comp.right.indexOf(VAR_END);
				missVarEnd = (p1==-1 && p2!=-1) || (p1!=-1 && p2==-1);
				if(missVarEnd) {
					throw new QnException(from, QnException.MISS_VAR_END);
				}else if(p1!=-1 && comp.right.length()==2) {
					throw new QnException(from, QnException.EMPTY_VAR);
				}else if(comp.right.indexOf(BLANK)!=-1) {
					throw new QnException(from, QnException.BAD_VAR);
				}
				return comp;
			}
			@Override
			public String toString() {
				return new StringBuilder(left).append(op).append(right).toString();
			}
			public void toJs(StringBuilder sb) {
				if(EQUAL.equals(op) && right.indexOf(PAUSE)>0) {
					//{机构}=北京、上海
					sb.append("[");
					sb.append(StringUtil.join(Arrays.asList(right.split("[、]")), "'", "'", ","));
					sb.append("].indexOf(");
					toJs(sb, left);
					sb.append(")>-1");
				}else {
					toJs(sb, left);
					if(EQUAL.equals(op)) {
						sb.append("==");
					}else {
						sb.append(op);
					}
					toJs(sb, right);
				}
			}
			private void toJs(StringBuilder sb, String name) {
				int p1 = name.indexOf(VAR_START), p2 = name.indexOf(VAR_END);
				if(p1==-1 && p2==-1) {
					sb.append("'").append(name).append("'");
				}else {
					name = name.substring(1, name.length()-1);
					sb.append(DATA_DOT).append(name);
				}
			}
		}
	}
	
	@Getter
	@SuppressWarnings("serial")
	public static class QnException extends RuntimeException {
		public static final String MISS_VAR_END = String.format("变量缺少结束符：%s", VAR_END);
		public static final String MISS_CONDITION_INNER = String.format("条件缺少分隔符：%s", CONDITION_INNER);
		public static final String EMPTY_VAR = "变量不能为空";
		public static final String BAD_VAR = "变量不能有空格";
		public static final String MISS_CONDITION_END = "条件括号不匹配";
		public static final String MISS_AND_OR = "多个条件之间需要有and或or";
		public static final String BAD_AND_OR = "同时使用and和or时需要使用括号";
		public static final String BAD_AND_OR2 = "and或or之后需要有条件";
		public static final String BAD_AND = "and之前需要有条件";
		public static final String BAD_OR = "or之前需要有条件";
		public static final String MISS_COND_OP = "条件缺少比较符";
		public static final String MISS_COND_VAR = "条件缺少比较参数";
		private int pos;
		public QnException(int pos, String error) {
			super(error);
			this.pos = pos;
		}
	}
	
	public static int indexOf(String str, int from, int to, char ... cs) {
		for(int i=from;i<to;i++) {
			char c = str.charAt(i);
			for(char c1 : cs) {
				if(c==c1) {
					return i;
				}
			}
		}
		return -1;
	}
	
	public static int indexOf(String qn, int from, int to, String ... strs) {
		int length = strs.length;
		int[] ps = new int[length];
		for(int i=0; i<length; i++) {
			ps[i] = qn.indexOf(strs[i], from);
			if(ps[i] >= to) {
				ps[i] = -1;
			}
		}
		return minIndexOf(0, ps.length, ps);
	}
	
	public static int minIndexOf(int from, int to, int ... ps) {
		if(from >= to-1) {
			return ps[from];
		}else if(from >= to-2){
			return minIndexOf(ps[from], ps[from+1]);
		}else {
			int p1 = ps[from], p2 = minIndexOf(from+1, to, ps);
			return minIndexOf(p1, p2);
		}
	}
	
	public static int minIndexOf(int p1, int p2) {
		return p1==-1 ? p2 : (p2==-1 ? p1 : (p1<p2 ? p1 : p2));
	}
	
	public static int minIndexOf2(int ... ps) {
		return minIndexOf(0, ps.length, ps);
	}
	
	private static int conditionEnd(String qn, int from, int to, int level) {
		int p = indexOf(qn, from, to, CONDITION_START, CONDITION_END);
		if(p==-1) {
			return p;
		}else {
			char c = qn.charAt(p);
			if(CONDITION_END == c) {
				return level<=0 ? p : conditionEnd(qn, p+1, to, level-1);
			}else {
				p = qn.indexOf(CONDITION_INNER, p+1);
				if(p==-1) {
					throw new QnException(from, QnException.MISS_CONDITION_INNER);
				}else {
					return conditionEnd(qn, p+2, to, level+1);
				}
			}
		}
	}
}
