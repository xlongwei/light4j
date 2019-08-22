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
Bearer eyJraWQiOiIxMDAiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ1cm46Y29tOm5ldHdvcmtudDpvYXV0aDI6djEiLCJhdWQiOiJ1cm46Y29tLm5ldHdvcmtudCIsImV4cCI6MTc5MDAzNTcwOSwianRpIjoiSTJnSmdBSHN6NzJEV2JWdUFMdUU2QSIsImlhdCI6MTQ3NDY3NTcwOSwibmJmIjoxNDc0Njc1NTg5LCJ2ZXJzaW9uIjoiMS4wIiwidXNlcl9pZCI6InN0ZXZlIiwidXNlcl90eXBlIjoiRU1QTE9ZRUUiLCJjbGllbnRfaWQiOiJmN2Q0MjM0OC1jNjQ3LTRlZmItYTUyZC00YzU3ODc0MjFlNzIiLCJzY29wZSI6WyJ3cml0ZTpwZXRzIiwicmVhZDpwZXRzIl19.mue6eh70kGS3Nt2BCYz7ViqwO7lh_4JSFwcHYdJMY6VfgKTHhsIGKq2uEDt3zwT56JFAePwAxENMGUTGvgceVneQzyfQsJeVGbqw55E9IfM_uSM-YcHwTfR7eSLExN4pbqzVDI353sSOvXxA98ZtJlUZKgXNE1Ngun3XFORCRIB_eH8B0FY_nT_D1Dq2WJrR-re-fbR6_va95vwoUdCofLRa4IpDfXXx19ZlAtfiVO44nw6CS8O87eGfAm7rCMZIzkWlCOFWjNHnCeRsh7CVdEH34LF-B48beiG5lM7h4N12-EME8_VDefgMjZ8eqs1ICvJMxdIut58oYbdnkwTjkA
```

Postman is the best tool to test REST APIs

Add "Authorization" header with value as above token and a dummy message will return from the generated stub.


