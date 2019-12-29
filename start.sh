#!/bin/sh

daemon=true
appname=light4j
jarfile=target/light4j.jar
[ ! -e "$jarfile" ] && jarfile=light4j.jar
JVM_OPS="-Xmx196m -Xms196m -XX:NewSize=100m -XX:MaxNewSize=100m -Xss228k"
JVM_OPS="$JVM_OPS -Dlogserver -DcontextName=light4j"
#ENV_OPS="PATH=/usr/java/jdk1.8.0_161/bin:$PATH"
JVM_OPS="$JVM_OPS -Dlight4j.directory=/soft/softwares/library/"
#JVM_OPS="$JVM_OPS -Dredis.configDb=xlongwei:6379:1"
#JVM_OPS="$JVM_OPS -Dredis.cacheDbs=xlongwei:6379:3-7"
#JVM_OPS="$JVM_OPS -Dsoffice.hosts=xlongwei:8100-8102:true"
#JVM_OPS="$JVM_OPS -Dupload.url=http://ip/uploads/"
#ENV_OPS="$ENV_OPS enableHttp=false httpPort=8080"
ENV_OPS="$ENV_OPS enableHttps=true httpsPort=8443"
#ENV_OPS="$ENV_OPS workerThreads=18"
#ENV_OPS="$ENV_OPS enableRegistry=true STATUS_HOST_IP=api.xlongwei.com"

usage(){
    echo "Usage: start.sh ( commands ... )"
    echo "commands: "
    echo "  status      check the running status"
    echo "  start       start $appname"
    echo "  stop        stop $appname"
    echo "  restart     stop && start"
    echo "  clean       clean target"
    echo "  jar         build $jarfile"
    echo "  jars        copy dependencies to target"
    echo "  package     jar && jars"
    echo "  rebuild     stop && jar && start"
    echo "  refresh     stop && clean && jar && jars && start"
    echo "  deploy      package fat-jar $jarfile"
    echo "  keystore    prepare keystore、crt、trustore"
}

status(){
    PIDS=`ps -ef | grep java | grep "$jarfile" |awk '{print $2}'`

	if [ -z "$PIDS" ]; then
	    echo "$appname is not running!"
	else
		for PID in $PIDS ; do
		    echo "$appname has pid: $PID!"
		done
	fi
}

stop(){
    PIDS=`ps -ef | grep java | grep "$jarfile" |awk '{print $2}'`

	if [ -z "$PIDS" ]; then
	    echo "$appname is not running!"
	else
		echo -e "Stopping $appname ..."
		for PID in $PIDS ; do
			echo -e "kill $PID"
		    kill $PID > /dev/null 2>&1
		done
	fi
}

clean(){
	mvn clean
}

jar(){
	mvn compile jar:jar
}

jars(){
	mvn dependency:copy-dependencies -DoutputDirectory=target
}

deploy(){
	mvn package -Prelease -Dmaven.test.skip=true -Dmaven.javadoc.skip=true
}

start(){
	echo "starting $appname ..."
	JVM_OPS="-server -Djava.awt.headless=true $JVM_OPS"
	#env $ENV_OPS java $JVM_OPS -jar target/light4j-3.0.1.jar 2>&1
	if [ "$daemon" = "true" ]; then
		env $ENV_OPS setsid java $JVM_OPS -cp $jarfile com.networknt.server.Servers >> /dev/null 2>&1 &
	else
		env $ENV_OPS java $JVM_OPS -cp $jarfile com.networknt.server.Servers 2>&1
	fi
}

keystore(){
	dir=src/main/resources/config
	if [ $# -gt 1 ]; then 
	    cert=$dir
	else
	    cert=/soft/cert
	fi
	openssl pkcs12 -export -in $cert/xlongwei.pem -inkey $cert/xlongwei.key -name server -out $cert/xlongwei.p12
	keytool -delete -alias server -keystore $dir/server.keystore -storepass password
	keytool -importkeystore -deststorepass password -destkeystore $dir/server.keystore -srckeystore $cert/xlongwei.p12 -srcstoretype PKCS12
	keytool -export -alias server -keystore $dir/server.keystore -storepass password -rfc -file $dir/server.crt
	keytool -delete -alias server -keystore $dir/client.truststore -storepass password
	keytool -import -file $cert/xlongwei.pem -alias server -keystore $dir/client.truststore -storepass password
}

if [ $# -eq 0 ]; then 
    usage
else
	case $1 in
	status) status ;;
	start) start ;;
	stop) stop ;;
	clean) clean ;;
	jar) jar ;;
	jars) jars ;;
	package) jar && jars ;;
	restart) stop && start ;;
	rebuild) stop && jar && start ;;
	refresh) stop && clean && jar && jars && start ;;
	deploy) deploy ;;
	keystore) keystore $@;;
	*) usage ;;
	esac
fi
