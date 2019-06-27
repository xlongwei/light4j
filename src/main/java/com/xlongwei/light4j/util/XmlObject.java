package com.xlongwei.light4j.util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author hongwei
 * @date 2015-03-31
 */
public class XmlObject implements Cloneable {
	private static final String XML_END = "?>";
	private static final String XML_START = "<?";
	private static final String LT = "<";
	private static final String NAME = "[^</>]+";
	public String name;
	/** 支持链式语法 */
	public XmlObject parent;
	/** 支持属性 */
	public List<FieldValue> attrs = new LinkedList<>();
	/** 支持子节点 */
	public List<Object> nodes = new LinkedList<>();
	/** 自关闭语法 */
	public static boolean shorten = true;
	/** 换行缩进 */
	public static boolean pretty = false;
	/** 缩进4空格 */
	public static String tabs = "    ";
	/** 标签风格，支持：驼峰、大小写、下划线、减号、点号 */
	public static Style style = Style.NOCHANGE;
	
	/**
	 * @param name 不能包含特殊字符</>
	 */
	public XmlObject(String name) {
		if(name!=null && name.matches(NAME)) {
			this.name = name;
		}else {
			throw new IllegalArgumentException("bad name: "+name);
		}
	}
	public XmlObject add(String field, String value) {
		nodes.add(new FieldValue(field, value));
		return this;
	}
	public XmlObject add(XmlObject node) {
		node.parent = this;
		nodes.add(node);
		return this;
	}
	public XmlObject addAttr(String attr, String value) {
		attrs.add(new FieldValue(attr, value));
		return this;
	}
	public XmlObject addNode(String name) {
		XmlObject node = new XmlObject(name);
		node.parent = this;
		nodes.add(node);
		return node;
	}
	public XmlObject parent() {
		return parent;
	}
	public String get(String field) {
		field = Style.off(field);
		for(Object node : nodes) {
			if(node instanceof FieldValue) {
				FieldValue fv = (FieldValue)node;
				if(field.equals(Style.off(fv.getField()))) {
					return fv.getValue();
				}
			}
		}
		return null;
	}
	public List<String> getAll(String field) {
		field = Style.off(field);
		List<String> list = new ArrayList<>();
		for(Object node : nodes) {
			if(node instanceof FieldValue) {
				FieldValue fv = (FieldValue)node;
				if(field.equals(Style.off(fv.getField()))) {
					list.add(fv.getValue());
				}
			}
		}
		return list;
	}
	public String getAttr(String attr) {
		attr = Style.off(attr);
		for(FieldValue fv : attrs) {
			if(attr.equals(Style.off(fv.getField()))) {
				return fv.getValue();
			}
		}
		return null;
	}
	public XmlObject getNode(String field) {
		field = Style.off(field);
		for(Object node : nodes) {
			if(node instanceof XmlObject) {
				XmlObject obj = (XmlObject)node;
				if(field.equals(Style.off(obj.name))) {
					return obj;
				}
			}
		}
		return null;
	}
	public List<XmlObject> getNodes(String field) {
		field = Style.off(field);
		List<XmlObject> objs = new LinkedList<>();
		for(Object node : nodes) {
			if(node instanceof XmlObject) {
				XmlObject obj = (XmlObject)node;
				if(field.equals(Style.off(obj.name))) {
					objs.add(obj);
				}
			}
		}
		return objs;
	}
	
	@Override
	public String toString() {
		StringBuilder xml = new StringBuilder();
		toString(xml, "");
		return xml.toString();
	}
	
	private void toString(StringBuilder xml, String tab) {
		xml.append(tab+LT+style.toString(name));
		for(FieldValue attr : attrs) {
			xml.append(" "+style.toString(attr.field)+"=\""+attr.value+"\"");
		}
		if(shorten && nodes.isEmpty()) {
			xml.append("/>");
		}else {
			xml.append('>');
			for(Object node : nodes) {
				if(pretty) {
					xml.append("\n");
				}
				if(node instanceof FieldValue) {
					FieldValue fv = (FieldValue)node;
					xml.append(tab+(pretty?tabs:"")+LT+style.toString(fv.field)+">");
					xml.append(fv.value);
					xml.append("</"+style.toString(fv.field)+">");
				}else if(node instanceof XmlObject) {
					XmlObject obj = (XmlObject)node;
					obj.toString(xml, tab+(pretty?tabs:""));
				}
			}
			if(pretty) {
				xml.append("\n");
			}
			xml.append(tab+"</"+style.toString(name)+">");
		}
	}
	
	public boolean update(String field, String value) {
		field = Style.off(field);
		for(Object node : nodes) {
			if(node instanceof FieldValue) {
				FieldValue fv = (FieldValue)node;
				if(field.equals(Style.off(fv.getField()))) {
					fv.value = value;
					return true;
				}
			}
		}
		return false;
	}
	public boolean updateAttr(String attr, String value) {
		attr = Style.off(attr);
		for(FieldValue fv : attrs) {
			if(attr.equals(Style.off(fv.getField()))) {
				fv.value = value;
				return true;
			}
		}
		return false;
	}
	public boolean removeNode(String name) {
		name = Style.off(name);
		List<Object> objs = new ArrayList<>();
		for(Object obj : nodes) {
			if(obj instanceof FieldValue && Style.off(((FieldValue)obj).getField()).equals(name)) {
				objs.add(obj);
			}
			if(obj instanceof XmlObject && Style.off(((XmlObject)obj).name).equals(name)) {
				objs.add(obj);
			}
		}
		for(Object obj : objs) {
			nodes.remove(obj);
		}
		return !objs.isEmpty();
	}
	public boolean removeAttr(String name) {
		name = Style.off(name);
		List<FieldValue> objs = new ArrayList<>();
		for(FieldValue attr : attrs) {
			if(Style.off(attr.getField()).equals(name)) {
				objs.add(attr);
				break;
			}
		}
		for(FieldValue attr : objs) {
			attrs.remove(attr);
		}
		return !objs.isEmpty();
	}
	
	@Override
	protected XmlObject clone() {
		XmlObject xml = new XmlObject(name);
		xml.nodes = new LinkedList<>();
		for(Object node : this.nodes) {
			if(node instanceof FieldValue) {
				FieldValue fv = (FieldValue)node;
				xml.add(fv.field, fv.value);
			}else  if(node instanceof XmlObject) {
				XmlObject obj = (XmlObject)node;
				obj = obj.clone();
				xml.add(obj);
			}
		}
		xml.attrs = new LinkedList<>();
		for(FieldValue attr : this.attrs) {
			xml.addAttr(attr.getField(), attr.getValue());
		}
		return xml;
	}
	
	public static String header() { return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"; }
	public static String cdata(String value) { return "<![CDATA["+value+"]]>"; }
	public static String toXML(XmlObject xml) { return XmlObject.header()+xml.toString(); }
	public static XmlObject fromXML(String xml) {
		xml = preHandle(xml);
		StringBuilder field = new StringBuilder(), value = new StringBuilder();
		boolean tag = false, start = false, hasCDATA = false;
		XmlObject obj = null; char[] cs = xml.toCharArray();
		for(int i = 0; i<cs.length; i++) {
			char c = cs[i];
			switch(c) {
			case '<':
				if(xml.indexOf("<![CDATA[",i)==i) {
					int end = xml.indexOf("]]>", i+9);
					value.append(xml.substring(i+9, end));
					i = end+2;
					hasCDATA = true;
				}else {
					tag = start = true;
					if(field.length()>0 && value.length()==0) {
						String fieldWithAttrs = field.toString(), name = attrName(fieldWithAttrs);
						int end = xml.indexOf("</"+name+">", i);
						if(hasCDATA) {
							obj.add(field.toString(), "");
							hasCDATA = false;
						}else {
							String subXml = xml.substring(i-field.length()-2, end+name.length()+3);
							obj.add(fromXML(subXml));
						}
						i = end+field.length()+3;
						field.delete(0, field.length());
					}
				}
				break;
			case '>':
				tag = false;
				if(start || cs[i-1]=='/') {
					XmlObject bak = obj;
					if(cs[i-1]=='/') {
						//self close xml
						obj = null; 
					}
					if(obj==null) {
						String fieldWithAttrs = field.toString();
						int space = fieldWithAttrs.indexOf(' ');
						obj = space>0 ? attrHandle(fieldWithAttrs, space) : new XmlObject(field.toString());
						field.delete(0, field.length());
						//self close xml
						if(cs[i-1]=='/' && bak!=null) { 
							bak.add(obj);
							obj = bak;
						}
					}else {
						//去掉标签之间的空格<response>    <service>test</service></response>
						value.delete(0, value.length());
					}
				}else if(field.length()>0 && value.length()>0) {
					obj.add(field.toString(), value.toString());
					field.delete(0, field.length());
					value.delete(0, value.length());
				}
				break;
			case '/':
				if(i>0 && cs[i-1]=='<') {
					start = false;
				} else {
					value.append(c);
				}
				break;
			default:
				if(!tag) {
					value.append(c);
				}else if(start) {
					field.append(c) ;
				}
				break;
			}
		}
		return obj;
	}
	
	private static String attrName(String fieldWithAttrs) {
		String name;
		int space = fieldWithAttrs.indexOf(' ');
		if(space>0) {
			name = fieldWithAttrs.substring(0, space);
		}else {
			name = fieldWithAttrs;
		}
		return name;
	}
	private static XmlObject attrHandle(String fieldWithAttrs, int space) {
		XmlObject obj = new XmlObject(fieldWithAttrs.substring(0, space));
		// attr="value"
		int equal = fieldWithAttrs.indexOf('=', space);
		while(equal>0) {
			String attr = fieldWithAttrs.substring(space+1, equal);
			int quot1 = fieldWithAttrs.indexOf('"', equal+1);
			int quot2 = (quot1>0) ? fieldWithAttrs.indexOf('"', quot1+1) : -1;
			String attrValue = (quot1>0 && quot2>0) ? fieldWithAttrs.substring(quot1+1, quot2) : null;
			if(attrValue!=null) {
				obj.addAttr(attr, attrValue);
			}
			space = fieldWithAttrs.indexOf(' ', quot2+1);
			equal = space>0 ? fieldWithAttrs.indexOf('=', space+1) : -1;
		}
		return obj;
	}
	
	private static String preHandle(String xml) {
		xml = xml==null ? "" : xml.trim();
		if(!xml.startsWith(LT)) {
			return "";
		}
		if(xml.startsWith(XML_START)) {
			int p = xml.indexOf(XML_END, 2);
			if(p == -1) {
				return "";
			}
			xml = xml.substring(p + 2);
		}
		if(!xml.startsWith(LT)) {
			return "";
		}
		return xml;
	}
	
	public static class FieldValue {
		private String field;
		private String value;
		/**
		 * @param field 不能包含特殊字符&lt/&gt
		 * @param value 不能包含特殊字符&lt/
		 */
		public FieldValue(String field, String value) {
			if(field!=null && field.length()>0) {
				this.field = field; this.value = value==null ? "" : value.trim();
			}else {
				throw new IllegalArgumentException("bad field: "+field);
			}
		}
		public String getField() {
			return field;
		}
		public String getValue() {
			return value;
		}
	}
	
	public static enum Style {
		/**
		 * 保持原样、小写、大写、下划线、破折号、点号
		 */
		NOCHANGE,
		LOWERCASE,
		UPPERCASE,
		UNDERLINE, UNDERLINE_UPPERCASE, UNDERLINE_LOWERCASE,
		DASH, DASH_UPPERCASE, DASH_LOWERCASE,
		DOT, DOT_UPPERCASE, DOT_LOWERCASE,
		;
		public String toString(String str) {
			switch(this){
			case NOCHANGE : return str;
			case LOWERCASE: return str.toLowerCase();
			case UPPERCASE: return str.toUpperCase();
			case UNDERLINE: return str.replaceAll("([a-z])([A-Z])", "$1_$2");
			case UNDERLINE_UPPERCASE: return str.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
			case UNDERLINE_LOWERCASE: return str.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
			case DASH: return str.replaceAll("([a-z])([A-Z])", "$1-$2");
			case DASH_UPPERCASE: return str.replaceAll("([a-z])([A-Z])", "$1-$2").toUpperCase();
			case DASH_LOWERCASE: return str.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
			case DOT: return str.replaceAll("([a-z])([A-Z])", "$1.$2");
			case DOT_UPPERCASE: return str.replaceAll("([a-z])([A-Z])", "$1.$2").toUpperCase();
			case DOT_LOWERCASE: return str.replaceAll("([a-z])([A-Z])", "$1.$2").toLowerCase();
			default: return str;
			}
		}
		public static String off(String str) {
			return str.replaceAll("[\\.\\-_]", "").toLowerCase();
		}
	}
}
