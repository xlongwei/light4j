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

1. Fork 本仓库
2. 新建 Feat_xxx 分支
3. 提交代码
4. 新建 Pull Request


#### 码云特技

1. 使用 Readme\_XXX.md 来支持不同的语言，例如 Readme\_en.md, Readme\_zh.md
2. 码云官方博客 [blog.gitee.com](https://blog.gitee.com)
3. 你可以 [https://gitee.com/explore](https://gitee.com/explore) 这个地址来了解码云上的优秀开源项目
4. [GVP](https://gitee.com/gvp) 全称是码云最有价值开源项目，是码云综合评定出的优秀开源项目
5. 码云官方提供的使用手册 [https://gitee.com/help](https://gitee.com/help)
6. 码云封面人物是一档用来展示码云会员风采的栏目 [https://gitee.com/gitee-stars/](https://gitee.com/gitee-stars/)