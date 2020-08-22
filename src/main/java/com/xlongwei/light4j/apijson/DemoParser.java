/*Copyright ©2016 TommyLemon(https://github.com/TommyLemon/APIJSON)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/

package com.xlongwei.light4j.apijson;

import apijson.RequestMethod;
import apijson.framework.APIJSONParser;
import apijson.orm.SQLExecutor;


/**请求解析器
 * @author Lemon
 */
public class DemoParser extends APIJSONParser {

	public DemoParser() {
		super();
	}
	public DemoParser(RequestMethod method) {
		super(method);
	}
	public DemoParser(RequestMethod method, boolean needVerify) {
		super(method, needVerify);
	}
	
	/**
	 * bug修复：父类parseResponse总是调createSQLExecutor，而getStructure是正确的，问题为修改时借了两个数据库连接，最后只还了一个，原因为sqlExecutor被覆盖，没有调用到close方法
	 * @return 每个parser只有一个sqlExecutor，并且每次都有close
	 */
	@Override
	public SQLExecutor createSQLExecutor() {
		if(sqlExecutor != null) {
			return sqlExecutor;
		}else {
			return super.createSQLExecutor();
		}
	}

	//	//可重写来设置最大查询数量
	//	@Override
	//	public int getMaxQueryCount() {
	//		return 50;
	//	}

}
