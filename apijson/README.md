### apijson.sql与sys.sql的差异

  * APIJSONSQLConfig默认MySQL为5.7.22，light4j的MySQL版本为5.1.63
  * json类型替换为varchar(65535)，apijson.JSON.getCorrectJson正确处理String="[1,2,3]"为JSONArray=[1,2,3]
  * sys.sql表名首字母为大写，apijson.sql表名统一为小写字母，/etc/my.cnf需配置`lower_case_table_names=1`
  * apijson_privacy字段pasword、payPassword去掉下划线，相应修改Privacy类
  * apijson.sql增加了beetlsql示例里的user表
  * apijson权限管理：优先开放指定ip权限：grant all on apijson.* to apijson@'localhost' identified by 'apijson';
  * apijson有很多测试数据，暂不清理

### apijson请求示例

  * http://localhost:8080/service/apijson/get
  * 单条数据：{"Moment":{"id":12}}
  * 部分字段：{"Moment":{"id":12,"@column":"content"}}
  * 字段别名：{"Moment":{"id":12,"@column":"id,date:time,content:text"}}
  * 多条数据：{"[]":{"Moment":{"id{}":[12,15,32],"@column":"id,date,content"}}}
  * 逻辑非：{"[]":{"Moment":{"id!{}":[12,15,32],"@column":"id"}}}
  * 逻辑与：{"[]":{"Moment":{"id&{}":">=300,<=400","@column":"id"}}}
  * 模糊查询：{"[]":{"Moment":{"content$":"%APIJSON%","@column":"id,date,content:text"}}}
  * 正则匹配：{"[]":{"Moment":{"content?":"^[0-9]+$","@column":"id,date,content:text"}}}
  * json数组（老版本MySQL不支持）：{"[]":{"Moment":{"praiseUserIdList<>":82001,"@column":"id,date,content,praiseUserIdList"}}}
  * 分页：{"[]":{"Moment":{"@column":"id,date,content,praiseUserIdList"},"page":0,"count":5}}
  * 查询总数：{"[]":{"Moment":{},"query":1},"total@":"/[]/total"}
  * 查询某页：{"[]":{"Moment":{"@column":"id"},"query":2,page:1},"total@":"/[]/total"}
  * 排序：{"[]":{"Moment":{"@column":"id,date,content","@order":"date-,id,content+"}}}
  * 关联查询（Access表控制权限和实际表名）：{"[]":{"Moment":{"@column":"id,date,userId","id":12},"User":{"id@":"/Moment/userId","@column":"id,name"}}}
  * 最大值：{"[]":{"Moment":{"@column":"max(id):maxid"}}}
  * http://localhost:8080/service/apijson/login
  * 登录：{"phone":"13000038710","password":"666666"}
  * http://localhost:8080/service/apijson/post
  * 新增：{"Moment":{"content":"今天天气不错，到处都是提拉米苏雪","userId":38710},"tag":"Moment"}
  * http://localhost:8080/service/apijson/put
  * 修改：{"Moment":{"id":1508072491570,"content":"海洋动物数量减少，如果非吃不可，不点杀也是在保护它们"},"tag":"Moment"}

