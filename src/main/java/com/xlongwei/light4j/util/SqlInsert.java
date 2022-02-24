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
 * <li>multi batch insert<br>
 * new SqlInsert("table").addColumns("name,mobile".split(","))<br>
 * .batch(100) //默认100<br>
 * .addValues("xlongwei","18810761776")<br>
 * .sqls()
 */
public class SqlInsert {
	public static enum Type { TrimToNull, TrimToEmpty, LongType, BigDecimalType; };
	private List<String> columns = new LinkedList<>();
	private List<String> values = new LinkedList<>();
	private List<List<String>> values2 = new ArrayList<>();
	private List<Type> types = new LinkedList<>();
	private String table;
	private int batchSize = 100;//批量insert，数据太多时生成多条sql
	private Boolean ignoreReplace = null;//true=ignore false=replace
	private List<String> sqls = new LinkedList<>();
	public static SqlInsert of(String table, String ... columns) { return new SqlInsert(table).addColumns(columns); }
	public SqlInsert(String table) { this.table = table; }
	public SqlInsert addColumn(String column, String value, Type type) { columns.add(column); values.add(StringUtil.sqlParam(value)); types.add(type); return this; }
	public SqlInsert addColumns(String ... columns) {
		if(columns!=null && columns.length>0) {
			for(String column:columns) {
				this.columns.add(column);
			}
		}
		return this;
	}
	public SqlInsert addValues(String ... values) { 
		if(values!=null && values.length>0) {
			List<String> list = new ArrayList<>();
			for(String value:values) {
				list.add(StringUtil.sqlParam(value));
			}
			values2.add(list);
			if(values2.size() >= batchSize) {
				sqls.add(toString());
				values2.clear();
			}
		}
		return this;
	}
	@Override
	public String toString() {
		if(columns.size()==0 || (values.size()==0 && values2.size()==0)) {
			return null;
		}
		StringBuilder sqlInsert = new StringBuilder();
		if(ignoreReplace==null) sqlInsert.append("insert into ");
		else if(ignoreReplace) sqlInsert.append("insert ignore ");
		else sqlInsert.append("replace into ");
		sqlInsert.append(table);
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
	public SqlInsert clear() {
		values.clear();
		values2.clear();
		return this;
	}
	public SqlInsert batch(int batchSize) {
		this.batchSize = batchSize<1 ? 1 : batchSize;
		return this;
	}
	public SqlInsert ignore(boolean ignore) {
		ignoreReplace = ignore ? Boolean.TRUE : null;
		return this;
	}
	public SqlInsert replace(boolean replace) {
		ignoreReplace = replace ? Boolean.FALSE : null;
		return this;
	}
	public List<String> sqls(){
		if(!values2.isEmpty()) {
			sqls.add(toString());
			values2.clear();
		}
		return sqls;
	}
}
