package com.xlongwei.light4j.util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author hongwei
 * @date 2015-03-31
 */
public class XMLObject implements Cloneable {
	public String name;
	public XMLObject parent;//支持链式语法
	public List<FieldValue> attrs = new LinkedList<>();//支持属性
	public List<Object> nodes = new LinkedList<>();//支持子节点
	public static boolean shorten = true;//自关闭语法
	public static boolean pretty = false;//换行缩进
	public static String tabs = "    ";//缩进4空格
	public static Style style = Style.NOCHANGE;//标签风格，支持：驼峰、大小写、下划线、减号、点号
	
	/**
	 * @param name 不能包含特殊字符</>
	 */
	public XMLObject(String name) {
		if(name!=null && name.matches("[^</>]+")) {
			this.name = name;
		}else {
			throw new IllegalArgumentException("bad name: "+name);
		}
	}
	public XMLObject add(String field, String value) {
		nodes.add(new FieldValue(field, value));
		return this;
	}
	public XMLObject add(XMLObject node) {
		node.parent = this;
		nodes.add(node);
		return this;
	}
	public XMLObject addAttr(String attr, String value) {
		attrs.add(new FieldValue(attr, value));
		return this;
	}
	public XMLObject addNode(String name) {
		XMLObject node = new XMLObject(name);
		node.parent = this;
		nodes.add(node);
		return node;
	}
	public XMLObject parent() {
		return parent;
	}
	public String get(String field) {
		field = Style.off(field);
		for(Object node : nodes) {
			if(node instanceof FieldValue) {
				FieldValue fv = (FieldValue)node;
				if(field.equals(Style.off(fv.getField())))
					return fv.getValue();
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
				if(field.equals(Style.off(fv.getField())))
					list.add(fv.getValue());
			}
		}
		return list;
	}
	public String getAttr(String attr) {
		attr = Style.off(attr);
		for(FieldValue fv : attrs) {
			if(attr.equals(Style.off(fv.getField())))
				return fv.getValue();
		}
		return null;
	}
	public XMLObject getNode(String field) {
		field = Style.off(field);
		for(Object node : nodes) {
			if(node instanceof XMLObject) {
				XMLObject obj = (XMLObject)node;
				if(field.equals(Style.off(obj.name)))
					return obj;
			}
		}
		return null;
	}
	public List<XMLObject> getNodes(String field) {
		field = Style.off(field);
		List<XMLObject> objs = new LinkedList<>();
		for(Object node : nodes) {
			if(node instanceof XMLObject) {
				XMLObject obj = (XMLObject)node;
				if(field.equals(Style.off(obj.name)))
					objs.add(obj);
			}
		}
		return objs;
	}
	
	public String toString() {
		StringBuilder xml = new StringBuilder();
		toString(xml, "");
		return xml.toString();
	}
	
	private void toString(StringBuilder xml, String tab) {
		xml.append(tab+"<"+style.toString(name));
		for(FieldValue attr : attrs) {
			xml.append(" "+style.toString(attr.field)+"=\""+attr.value+"\"");
		}
		if(shorten && nodes.isEmpty()) {
			xml.append("/>");
		}else {
			xml.append('>');
			for(Object node : nodes) {
				if(pretty) xml.append("\n");
				if(node instanceof FieldValue) {
					FieldValue fv = (FieldValue)node;
					xml.append(tab+(pretty?tabs:"")+"<"+style.toString(fv.field)+">");
					xml.append(fv.value);
					xml.append("</"+style.toString(fv.field)+">");
				}else if(node instanceof XMLObject) {
					XMLObject obj = (XMLObject)node;
					obj.toString(xml, tab+(pretty?tabs:""));
				}
			}
			if(pretty) xml.append("\n");
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
			if(obj instanceof FieldValue && Style.off(((FieldValue)obj).getField()).equals(name)) objs.add(obj);
			if(obj instanceof XMLObject && Style.off(((XMLObject)obj).name).equals(name)) objs.add(obj);
		}
		for(Object obj : objs) nodes.remove(obj);
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
		for(FieldValue attr : objs) attrs.remove(attr);
		return !objs.isEmpty();
	}
	protected XMLObject clone() {
		XMLObject xml = new XMLObject(name);
		xml.nodes = new LinkedList<>();
		for(Object node : this.nodes) {
			if(node instanceof FieldValue) {
				FieldValue fv = (FieldValue)node;
				xml.add(fv.field, fv.value);
			}else  if(node instanceof XMLObject) {
				XMLObject obj = (XMLObject)node;
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
	public static String CDATA(String value) { return "<![CDATA["+value+"]]>"; }
	public static String toXML(XMLObject xml) { return XMLObject.header()+xml.toString(); }
	public static XMLObject fromXML(String xml) {
		XMLObject obj = null;
		StringBuilder field = new StringBuilder(), value = new StringBuilder();
		if(xml.startsWith("<?")) xml = xml.substring(xml.indexOf("?>")+2);
		boolean tag = false, start = false, hasCDATA = false;
		char[] cs = xml.toCharArray();
		for(int i = 0; i<cs.length; i++) {
			char c = cs[i];
			switch(c) {
			case '<':
				if(xml.substring(i).startsWith("<![CDATA[")) {
					int end = xml.indexOf("]]>", i+9);
					String data = xml.substring(i+9, end);
					value.append(data);
					i = end+2;
					hasCDATA = true;
				}else {
					tag = true;
					start = true;
					if(field.length()>0 && value.length()==0) {
						String fieldWithAttrs = field.toString(), name = null;
						int space = fieldWithAttrs.indexOf(' ');
						if(space>0) {
							name = fieldWithAttrs.substring(0, space);
						}else {
							name = fieldWithAttrs;
						}
						int end = xml.indexOf("</"+name+">", i);
						if(hasCDATA) {
							obj.add(field.toString(), "");
							hasCDATA = false;
						}else {
							String subXml = xml.substring(i-field.length()-2, end+name.length()+3);
							XMLObject sub = fromXML(subXml);
							obj.add(sub);
						}
						i = end+field.length()+3;
						field.delete(0, field.length());
					}
				}
				break;
			case '>':
				tag = false;
				if(start || cs[i-1]=='/') {
					XMLObject bak = obj;
					if(cs[i-1]=='/') obj = null; //self close xml
					
					if(obj==null) {
						String fieldWithAttrs = field.toString();
						int space = fieldWithAttrs.indexOf(' ');
						if(space>0) {
							obj = new XMLObject(fieldWithAttrs.substring(0, space));
							// attr="value"
							int equal = fieldWithAttrs.indexOf('=', space);
							while(equal>0) {
								String attr = fieldWithAttrs.substring(space+1, equal);
								int quot1 = fieldWithAttrs.indexOf('"', equal+1);
								int quot2 = (quot1>0) ? fieldWithAttrs.indexOf('"', quot1+1) : -1;
								String attrValue = (quot1>0 && quot2>0) ? fieldWithAttrs.substring(quot1+1, quot2) : null;
								if(attrValue!=null) obj.addAttr(attr, attrValue);
								
								space = fieldWithAttrs.indexOf(' ', quot2+1);
								equal = space>0 ? fieldWithAttrs.indexOf('=', space+1) : -1;
							}
						}else {
							obj = new XMLObject(field.toString());
						}
						field.delete(0, field.length());
						
						if(cs[i-1]=='/') { //self close xml
							if(bak!=null) {
								bak.add(obj);
								obj = bak;
							}
						}
					}else {
						value.delete(0, value.length());//去掉标签之间的空格<response>    <service>test</service></response>
					}
				}else {
					if(field.length()>0 && value.length()>0) {
						obj.add(field.toString(), value.toString());
						field.delete(0, field.length());
						value.delete(0, value.length());
					}
				}
				break;
			case '/':
				if(i>0 && cs[i-1]=='<') start = false;
				else value.append(c);
				break;
			default:
				if(tag && start) field.append(c) ;
				if(!tag) value.append(c);
				break;
			}
		}
		return obj;
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
				this.field = field; this.value = value==null ? "" : value;
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
		NOCHANGE, //原样
		LOWERCASE, //小写
		UPPERCASE, //大写
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
