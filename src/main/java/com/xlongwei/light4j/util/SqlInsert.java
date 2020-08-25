package com.xlongwei.light4j.util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;


/**
 * 生成sql insert语句
 * <li>single value insert<br>
 * new SqlInsert("table")<br>
 * .addColumn("name","xlongwei",Type.TrimToNull)<br>
 * .addColumn("mobile","18810761776",Type.TrimToEmpty)<br>
 * .toString()
 * <li>multi values insert<br>
 * new SqlInsert("table").addColumns("name","mobile")<br>
 * .addValues("xlongwei","18810761776")<br>
 * .addValues("caifuxiangxia","15123011395)<br>
 * .toString()
 */
public class SqlInsert {
	public static enum Type { TrimToNull, TrimToEmpty, LongType, BigDecimalType; };
	private List<String> columns = new LinkedList<>();
	private List<String> values = new LinkedList<>();
	private List<List<String>> values2 = new ArrayList<>();
	private List<Type> types = new LinkedList<>();
	private String table;
	public SqlInsert(String table) { this.table = table; }
	public void addColumn(String column, String value, Type type) { columns.add(column); values.add(StringUtil.sqlParam(value)); types.add(type); }
	public void addColumns(String ... columns) { if(columns!=null && columns.length>0) for(String column:columns) this.columns.add(column); }
	public void addValues(String ... values) { 
		if(values!=null && values.length>0) {
			List<String> list = new ArrayList<>();
			for(String value:values) list.add(StringUtil.sqlParam(value));
			values2.add(list);
		}
	}
	@Override
	public String toString() {
		if(columns.size()==0 || (values.size()==0 && values2.size()==0)) return null;
		StringBuilder sqlInsert = new StringBuilder("insert into "+table);
		sqlInsert.append("(");
		sqlInsert.append(StringUtil.join(columns, null, null, ","));
		sqlInsert.append(") values ");
		if(values.size()>0) {
			sqlInsert.append("(");
			for(int i=0;i<values.size();i++) {
				String value = values.get(i);
				Type type = types.get(i);
				switch(type) {
					case TrimToNull: sqlInsert.append((value=StringUtils.trimToNull(value))==null?"null":"'"+value+"'"); break;
					case TrimToEmpty: sqlInsert.append("'"+StringUtils.trimToEmpty(value)+"'"); break;
					case LongType: sqlInsert.append(StringUtil.hasLength(value)?value:"null"); break;
					case BigDecimalType: sqlInsert.append(StringUtil.hasLength(value)?"'"+value+"'":"'0.00'"); break;
					default: sqlInsert.append(value); break;
				}
				sqlInsert.append(",");
			}
			sqlInsert.replace(sqlInsert.length()-1, sqlInsert.length(), "),");
		}
		if(values2.size()>0) {
			for(List<String> list:values2) {
				sqlInsert.append("(");
				sqlInsert.append(StringUtil.join(list, "'", "'", ","));
				sqlInsert.append("),");
			}
		}
		sqlInsert.replace(sqlInsert.length()-1, sqlInsert.length(), ";");
		return sqlInsert.toString();
	}
	public int size() {
		return values.isEmpty() ? values2.size() : 1;
	}
	public void clear() {
		values.clear();
		values2.clear();
	}
}
