package com.xlongwei.light4j.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.xlongwei.light4j.util.QnObject.Condition.Cond;

/**
 * 用法：
 * <pre>
 * try{
 *     String qn = "您好：{姓名}(({性别}=男 and {年龄}>60) or {机构}=北京、上海)[老({机构}=北京)[{机构}]先生]<>[<列表>[{姓名}、-]]";
 *     String js = QnObject.fromQn(qn).toJs();
 *     String qc = "({性别}=男 and {年龄}>60) or {机构}=北京、上海";
 *     String qcJs = QnObject.fromQc(qc).toJs();
 * }catch(QnException ex){
 *     int pos = ex.getPos();
 *     String error = ex.getError();
 * }
 * String data = "{姓名:'老张',性别:'男',年龄: "60", 机构: "北京"}";
 * String result = "您好：老张老北京先生"; //data + js = QnObject.toJs(qnObj)
 * </pre>
 * 语法规则：
 * <li>{变量}，值为空时原样输出
 * <li>(条件)[内容]，条件支持and、or、分组括号
 * <li><>[内容]，顶层循环，支持内嵌列表循环
 * <li><{列表}>[内容]，列表循环，支持-]删除最后一个字符
 * <li>条件和循环里的[内容]可以嵌套语法规则，并且只能顶层循环嵌套列表循环
 * <li>({列表}!=EMPTY)，判断{列表}是否非空
 * @author xlongwei
 * @date 2020-03-24
 */
public class QnObject {
	private static final char VAR_START = '{';
	private static final char VAR_END = '}';
	private static final String DATA_DOT = "json.";
	private static final char CONDITION_START = '(';
	private static final String CONDITION_INNER = ")[";
	private static final char CONDITION_END = ']';
	private static final char COND_START = '(';
	private static final char COND_END = ')';
	private static final char BACKSLASH = '\\';
	private static final char LOOP_START = '<';
	private static final String LOOP_INNER = ">[";
	private static final String AND = " and ";
	private static final String OR = " or ";
	private static final String COND = String.valueOf(COND_START);
	private static final String EQUAL = "=";
	private static final String EQUAL_2 = "==";
	private static final String NOT_EQUAL = "!=";
	private static final String NOT_EQUAL2 = "<>";
	private static final String NOT = "!";
	private static final String NOT_2 = "!!";
	private static final String EMPTY = "EMPTY";
	private static final String[] OPS = new String[] {EQUAL, "<", ">"};
	private static final String[] OPS2 = new String[] {"<=", ">=", EQUAL_2, NOT_EQUAL, NOT_EQUAL2};
	private static final char BLANK = ' ';
	private static final char PAUSE = '、';
	private List<Object> nodes = new LinkedList<>();

	/**
	 * 解析问题模板
	 * <br>String js = QnObject.fromQn(qn).toJs();
	 * @param qn 问题定义
	 * @return QnObject，可调用方法toJs()
	 */
	public static QnObject fromQn(String qn) {
		QnObject obj = new QnObject();
		int from = 0, len = qn.length();
		int p = indexOf(qn, from, len, VAR_START, CONDITION_START, LOOP_START);
		while(p != -1) {
			if(p > from) {
				if(BACKSLASH == qn.charAt(p-1)) {
					p = indexOf(qn, p+1, len, VAR_START, CONDITION_START);
					continue;
				}
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
			}else if(LOOP_START == c) {
				p = fromLoop(obj, qn, from, len);
				from = p+1;
			}
			p = indexOf(qn, from, len, VAR_START, CONDITION_START, LOOP_START);
		}
		if(len > from) {
			obj.nodes.add(qn.substring(from));
		}
		return obj;
	}
	
	/**
	 * 解析问题条件
	 * <br>String js = QnObject.fromQc(qc).toJs();
	 * @param qc 问题条件
	 * @return Cond，可调用方法toJs()
	 */
	public static Cond fromQc(String qc) {
		return Cond.from(qc, 0, qc.length());
	}

	private static int fromLoop(QnObject obj, String qn, int from, int len) {
		int p = qn.indexOf(LOOP_INNER, from+1);
		if(p==-1) {
			throw new QnException(from, QnException.MISS_LOOP_INNER);
		}else {
			int e = conditionEnd(qn, p+2, len, 0);
			if(e==-1) {
				throw new QnException(p+1, QnException.MISS_LOOP_END);
			}else {
				String name = qn.substring(from+1, p);
				if(StringUtil.isBlank(name)==false) {
					if(name.charAt(0)==VAR_START && name.charAt(name.length()-1)==VAR_END) {
						name = name.substring(1, name.length()-1);
					}
				}
				String content = qn.substring(p+2, e);
				QnObject contentObj = QnObject.fromQn(content);
				obj.nodes.add(new Loop(name, contentObj));
			}
			return e;
		}
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
				QnObject contentObj = QnObject.fromQn(content);
				obj.nodes.add(new Condition(cond, contentObj));
			}
			return e;
		}
	}

	private static int fromVar(QnObject obj, String qn, int from) {
		int p = qn.indexOf(VAR_END, from);
		if(p==-1) {
			throw new QnException(from+1, QnException.MISS_VAR_END);
		}else if(p==from+1) {
			throw new QnException(from+1, QnException.EMPTY_VAR);
		}else {
			obj.nodes.add(new Var(qn.substring(from+1, p)));
		}
		return p;
	}
	
	public String toJs() {
		StringBuilder sb = new StringBuilder();
		sb.append("var array=(data.toString()[0]=='[') ? data : [data];\n");
		sb.append("var json = array[0];\n");
		sb.append("var result = '';\n");
		this.toJs(sb);
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
			}else if(obj instanceof Loop) {
				((Loop)obj).toJs(sb);
			}
		}
	}

	static class Var {
		Var(String name) {
			this.name = name;
		}
		String name;
		@Override
		public String toString() {
			return new StringBuilder().append(VAR_START).append(name).append(VAR_END).toString();
		}
		void toJs(StringBuilder sb) {
			//result += !data.name ? {name} : data.name
			sb.append("result += !").append(DATA_DOT).append(name).append(" ? '{").append(name).append("}' : ").append(DATA_DOT).append(name).append(";\n");
		}
	}
	
	public static class Condition {
		Condition(Cond cond, QnObject obj) {
			this.cond = cond;
			this.obj = obj;
		}
		Cond cond;
		QnObject obj;
		@Override
		public String toString() {
			return new StringBuilder().append(CONDITION_START).append(cond.toString()).append(CONDITION_INNER).append(obj.toString()).append(CONDITION_END).toString();
		}
		void toJs(StringBuilder sb) {
			sb.append("if(");
			cond.toJs(sb);
			sb.append("){\n");
			obj.toJs(sb);
			sb.append("}\n");
		}
		public static class Cond {
			/** 0 a=b, 1 and, 2 or */
			private int type = 0;
			private Comp comp;
			private List<Cond> conds = new ArrayList<>();
			public static Cond from(String qn, int from, int to) {
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
			public String toJs() {
				StringBuilder sb = new StringBuilder();
				sb.append("var array=(data.toString()[0]=='[') ? data : [data];\n");
				sb.append("var json = array[0];\n");
				toJs(sb);
				return sb.toString();
			}
			void toJs(StringBuilder sb) {
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
			private static List<String> tokens(String qn, int from, int to){
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

			private static int condEnd(String qn, int from, int to, int level) {
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
			void toJs(StringBuilder sb) {
				if(right.indexOf(PAUSE)>0 && (EQUAL.equals(op) || EQUAL_2.equals(op) || NOT_EQUAL.equals(op) || NOT_EQUAL2.equals(op))) {
					//{机构}=北京、上海
					sb.append("[");
					sb.append(StringUtil.join(Arrays.asList(right.split("[、]")), "'", "'", ","));
					sb.append("].indexOf(");
					toJs(sb, left);
					if(EQUAL.equals(op) || EQUAL_2.equals(op)) {
						sb.append(")>-1");
					}else {
						sb.append(")==-1");
					}
				}else if(EMPTY.equals(right) && (EQUAL.equals(op) || EQUAL_2.equals(op) || NOT_EQUAL.equals(op) || NOT_EQUAL2.equals(op))){
					boolean isEqual = EQUAL.equals(op) || EQUAL_2.equals(op);
					sb.append(isEqual ? NOT : NOT_2);
					toJs(sb, left);
					sb.append(isEqual ? " || (" : " && (");
					toJs(sb, left);
					sb.append(".toString()[0]=='[' ? ");
					toJs(sb, left);
					sb.append(isEqual ? ".length==0" : ".length>0");
					sb.append(isEqual ? " : false)" : " : true)");
				}else {
					toJs(sb, left);
					if(EQUAL.equals(op)) {
						sb.append("==");
					}else if(NOT_EQUAL2.equals(op)){
						sb.append("!=");
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
	
	static class Loop {
		Loop(String name, QnObject obj) {
			this.name = name;
			this.obj = obj;
		}
		private String name;
		QnObject obj;
		@Override
		public String toString() {
			if(StringUtil.isBlank(name)) {
				return new StringBuilder().append(LOOP_START).append(LOOP_INNER).append(obj.toString()).append(CONDITION_END).toString();
			}else {
				return new StringBuilder().append(LOOP_START).append(name).append(LOOP_INNER).append(obj.toString()).append(CONDITION_END).toString();
			}
		}
		void toJs(StringBuilder sb) {
			StringBuilder temp = new StringBuilder();
			obj.toJs(temp);
			String tempJs = temp.toString().replace("json.", "item.");
			boolean hasSubLoop = tempJs.contains(".forEach(");
			String array = StringUtil.isBlank(name) ? "array" : (hasSubLoop ? "item." : "json.")+name;
			sb.append(NOT_2).append(array).append(" && ").append(array).append(".toString()[0]=='['").append(" && ").append(array).append(".forEach(function(item){\n");
			if(hasSubLoop) {
				tempJs = tempJs.replace("data.", "item.");
			}
			boolean hasMinus = tempJs.contains("-'");
			if(hasMinus) {
				tempJs = tempJs.replace("-'", "'");
			}
			sb.append(tempJs);
			sb.append("});\n");
			if(hasMinus) {
				sb.append("if(").append(NOT_2).append(array).append(" && ").append(array).append(".length>0){\n");
				sb.append("result = result.substring(0,result.length-1);\n");
				sb.append("}\n");
			}
		}
	}

	public static class QnException extends RuntimeException {
		private static final long serialVersionUID = 6020260112358514571L;
		public static final String MISS_VAR_END = String.format("变量缺少结束符：%s", VAR_END);
		public static final String MISS_CONDITION_INNER = String.format("条件缺少分隔符：%s", CONDITION_INNER);
		public static final String MISS_LOOP_INNER = String.format("循环缺少分隔符：%s", LOOP_INNER);
		public static final String EMPTY_VAR = "变量不能为空";
		public static final String BAD_VAR = "变量不能有空格";
		public static final String MISS_CONDITION_END = "条件括号不匹配";
		public static final String MISS_LOOP_END = "循环括号不匹配";
		public static final String MISS_AND_OR = "多个条件之间需要有and或or";
		public static final String BAD_AND_OR = "同时使用and和or时需要使用括号";
		public static final String BAD_AND_OR2 = "and或or之后需要有条件";
		public static final String BAD_AND = "and之前需要有条件";
		public static final String BAD_OR = "or之前需要有条件";
		public static final String MISS_COND_OP = "条件缺少比较符";
		public static final String MISS_COND_VAR = "条件缺少比较参数";
		private int pos;
		public int getPos() {
			return pos;
		}
		public QnException(int pos, String error) {
			super(error);
			this.pos = pos;
		}
	}
	
	private static int indexOf(String str, int from, int to, char ... cs) {
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
	
	private static int indexOf(String qn, int from, int to, String ... strs) {
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
	
	private static int minIndexOf(int from, int to, int ... ps) {
		if(from >= to-1) {
			return ps[from];
		}else if(from >= to-2){
			return minIndexOf(ps[from], ps[from+1]);
		}else {
			int p1 = ps[from], p2 = minIndexOf(from+1, to, ps);
			return minIndexOf(p1, p2);
		}
	}
	
	private static int minIndexOf(int p1, int p2) {
		return p1==-1 ? p2 : (p2==-1 ? p1 : (p1<p2 ? p1 : p2));
	}
	
	private static int conditionEnd(String qn, int from, int to, int level) {
		int p = indexOf(qn, from, to, CONDITION_START, LOOP_START, CONDITION_END);
		if(p==-1) {
			return p;
		}else {
			char c = qn.charAt(p);
			if(CONDITION_END == c) {
				return level<=0 ? p : conditionEnd(qn, p+1, to, level-1);
			}else {
				p = qn.indexOf(LOOP_START==c ? LOOP_INNER : CONDITION_INNER, p+1);
				if(p==-1) {
					throw new QnException(from, LOOP_START==c ? QnException.MISS_LOOP_INNER : QnException.MISS_CONDITION_INNER);
				}else {
					return conditionEnd(qn, p+2, to, level+1);
				}
			}
		}
	}
}
