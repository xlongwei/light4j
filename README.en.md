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

### Test

By default, all endpoints are protected by OAuth jwt token verifier. It can be turned off with config change through for development.


In order to access the server, there is a long lived token below issued by my
oauth2 server [light-oauth2](https://github.com/networknt/light-oauth2)

```
Bearer eyJraWQiOiIxMDAiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ1cm46Y29tOm5ldHdvcmtudDpvYXV0aDI6djEiLCJhdWQiOiJ1cm46Y29tLm5ldHdvcmtudCIsImV4cCI6MTg4MjUwNjM1NiwianRpIjoiQTlaUHVjM3RsS1BoWmM0RnpzTlJjQSIsImlhdCI6MTU2NzE0NjM1NiwibmJmIjoxNTY3MTQ2MjM2LCJ2ZXJzaW9uIjoiMS4wIiwidXNlcl9pZCI6ImFkbWluIn0.Jb89PTAOY7zDQBUpLS-5L9iDz28__fBUhXgmXqjXByiu6HG1sSemHHs-C0n-ZFUH4Tn3yfVbcHndSjNtVQ__gZMmpKi4PCg7NTiSo7TZZmVYI9uinQEdnDlFT2YA97AL6jBCGJW2Ol6q-odSajpCdoMfOh9KM2yXKQPqr95P5v4Du7L-MNL8dW7evfa0gBpGA2FF4Sr4txerS_SXJg3ED4_px_WbbkqZYpzo6_MupNK9nfJVG7ycP50r21-HMrSnBR7pUN1JvF8mxpfmcQi8j0W4TiYFZV2PKV2AGqsJ9d4IuPu--3YHNpevG3Pv78982o6qK22o_4h4Z8VFzr_NUQ
```

Postman is the best tool to test REST APIs

Add "Authorization" header with value as above token and a dummy message will return from the generated stub.


