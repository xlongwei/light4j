### apijson配置选项

  * -Dapijson.enabled=true，配置false停用apijson
  * -Dapijson.debug=false，配置true开启调试
  * -Dapijson.test=false，配置true开启框架测试
  * -Dapijson.verify=60，配置验证码有效期（秒），非调试时有效

### apijson框架引用1

  * 创建数据库apijson，创建用户apijson/apijson，导入脚本apijson.sql
  * 下载[apijson-framework-4.1.0.jar](http://t.xlongwei.com/windows/apijson-framework-4.1.0.jar)、[apijson-orm-4.1.0.jar](http://t.xlongwei.com/windows/apijson-orm-4.1.0.jar)
  * mvn install:install-file -DgroupId=apijson.orm -DartifactId=apijson-orm -Dversion=4.1.0 -Dpackaging=jar -Dfile=apijson-orm-4.1.0.jar
  * mvn install:install-file -DgroupId=apijson.framework -DartifactId=apijson-framework -Dversion=4.1.0 -Dpackaging=jar -Dfile=apijson-framework-4.1.0.jar

### apijson框架引用2

  * git clone https://gitee.com/APIJSON/APIJSON.git
  * 导入脚本MySQL/sys.sql
  * vi APIJSON-Java-Server/APIJSONORM/pom.xml，groupId=apijson.orm，artifactId=apijson-orm
  * mvn install -f APIJSON-Java-Server/APIJSONORM/pom.xml
  * vi APIJSON-Java-Server/APIJSONFramework/pom.xml，groupId=apijson.framework，artifactId=apijson-framework
  * mvn install -f APIJSON-Java-Server/APIJSONFramework/pom.xml

### apijson.sql与sys.sql的差异

  * APIJSONSQLConfig默认MySQL为5.7.22，light4j的MySQL版本为5.1.63
  * json类型替换为varchar(65535)，apijson.JSON.getCorrectJson正确处理String="[1,2,3]"为JSONArray=[1,2,3]
  * sys.sql表名首字母为大写，apijson.sql表名统一为小写字母，/etc/my.cnf需配置`lower_case_table_names=1`
  * apijson_privacy字段pasword、payPassword添加注解@JSONField，避免fastjson解析报错，密码启用加密，修改字段长度
  * apijson.sql增加了beetlsql示例里的user表
  * apijson权限管理：优先开放指定ip权限：grant all on apijson.* to apijson@'127.0.0.1' identified by 'apijson';

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

### apijson账户管理

  * 生成验证码：http://localhost:8080/service/apijson/postVerify，{"type":1,"phone":"13000038710"}，type=0登录 1注册 2密码 3支付 4重载
  * 校验验证码：http://localhost:8080/service/apijson/headVerify，{"type":1,"phone":"13000038710","verify":"1979"}，非调试时60秒过期
  * 验证码注册：http://localhost:8080/service/apijson/register，{"Privacy":{"phone":"18810761776","_password":"666666"},"User":{"name":"xlongwei"},"verify":"1979"}
  * 原密码改密：http://localhost:8080/service/apijson/putPassword，{"oldPassword":666666,"Privacy":{"id":38710,"_password":"123456"}}
  * 获取验证码：http://localhost:8080/service/apijson/getVerify，{"type":2,"phone":"13000038710"}，响应报文不出现验证码，适合短信发送等场景
  * 验证码改密：http://localhost:8080/service/apijson/putPassword，{"verify":"2798","Privacy":{"phone":"13000038710","_password":"666666"}}
  * 改支付密码：http://localhost:8080/service/apijson/putPassword，{"verify":"2798","Privacy":{"phone":"13000038710","_payPassword":"666666"}}
  * 验证码重载：http://localhost:8080/service/apijson/reload，{"phone":"13000038710","verify":"1950"}，type=ALL, FUNCTION, REQUEST, ACCESS
  * 密码登录：http://localhost:8080/service/apijson/login，{"phone":"13000038710","password":"666666"}
  * 验证码登录：http://localhost:8080/service/apijson/login，{"phone":"13000038710","password":"1979",type:"1"}，或使用"verify":"1979"更合适{"phone":"13000038710","verify":"1979"}
  * 新增：http://localhost:8080/service/apijson/post，{"Moment":{"content":"今天天气不错，到处都是提拉米苏雪","userId":38710},"tag":"Moment"}，tag为Request表中配置，一般是Table表名
  * 修改：http://localhost:8080/service/apijson/put，{"Moment":{"id":12,"content":"海洋动物数量减少，如果非吃不可，不点杀也是在保护它们"},"tag":"Moment"}
  * 查询余额：http://localhost:8080/service/apijson/gets，{"Privacy":{"id":38710},"tag":"Privacy"}，根据Access表配置不运行get方法，而gets方法不会返回隐藏字段
  * 充值提现：http://localhost:8080/service/apijson/putBalance，{"Privacy":{"balance+":-1000,"_payPassword":666666},"tag":"Privacy"}，充值为正数，提现为负数，Privacy.id可以自动获取
  * 登出：http://localhost:8080/service/apijson/logout

### Request请求校验

  * tag表名+method方法+version版本：唯一定位请求配置，版本默认为1
  * structure：请求参数校验，NECESSARY必填字段，DISALLOW禁止字段，
