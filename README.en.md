# light4j

#### Description
a microservice project using light-4j

#### Software Architecture
Software architecture description

#### Installation

1. redis-server
2. mvn compile
3. mvn exec:exec

profile "debug" is activated by default.

#### Instructions

1. rewrite configs in config/light4j.yml

-Dupload.save=/soft/uploads -Dlight4j.directory=/soft/softwares/library/

2. append logs to logserver or logfile

-Dlogserver -Dlogfile=path/log

### Debug

1. com.networknt.server.Server

you can config vm arguments -Dupload.save=/soft/uploads -Dlight4j.directory=/soft/softwares/library/

2. using postman(chrome plugin) to import postman.json

#### Deploy

1. mvn compile jar:jar

2. mvn dependency:copy-dependencies -DoutputDirectory=target/deploy

3. copy target/light4j-3.0.1.jar target/deploy

you need only upload "deploy" directory once, after that you just need to update light4j-3.0.1.jar.

4. java -Dlight4j.directory=H:/works/itecheast/Servers/library/ -Dupload.save=H:/works/itecheast/Servers/uploads/ -jar target/deploy/light4j-3.0.1.jar

redis-server is required. "mvn package -P release -Dmaven.javadoc.skip=true" will package one-jar "target/light4j-3.0.1.jar" that can be deployed also. 

#### Features

1. lombok

org.projectlombok:lombok:1.16.18+

2. logback if/else

org.codehaus.janino:janino:2.6.1




