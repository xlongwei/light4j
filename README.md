# light4j

#### 介绍
基于light-4j微服务框架的个人应用

#### 软件架构
软件架构说明


#### 安装教程

1. 调试运行主类：运行redis-server，调试com.networknt.server.Server，自定义：-Dupload.save=H:/works/itecheast/Servers/uploads/
2. 部署打包命令：mvn package -P release -Dmaven.javadoc.skip=true
3. 服务运行命令：mvn exec:java -P release，jar -jar target/light4j-3.0.1.jar

#### 使用说明

1. 依赖jar列表：mvn dependency:build-classpath -Dmdep.outputFile=cp.txt -Dmdep.includeScope=runtime
2. 依赖jar列表：mvn -q exec:exec -Dexec.executable=echo -Dexec.args="%classpath"
3. 服务运行命令：mvn -q exec:java -Dexec.mainClass="com.networknt.server.Server"
4. 使用postman：使用Import Collection功能导入postman.json，测试已有接口

#### 参与贡献

1. 搜索jar包：https://mvnrepository.com/
