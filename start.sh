#!/bin/sh

daemon=false
appname=light4j
#profile="-P mysql5"
jarfile=target/light4j.jar
[ ! -e "$jarfile" ] && jarfile=light4j.jar
Survivor=2 Old=64 NewSize=$[Survivor*10] Xmx=$[NewSize+Old] #NewSize=Survivor*(1+1+8) Xmx=NewSize+Old
JVM_OPS="-Xmx${Xmx}m -Xms${Xmx}m -XX:NewSize=${NewSize}m -XX:MaxNewSize=${NewSize}m -XX:SurvivorRatio=8 -Xss228k"
#JVM_OPS="$JVM_OPS -Dredis -Dredis.host=localhost -Dredis.port=6379 -Dredis.pubsub=false -Dredis.pushpop=true -Dredis.queueSize=10240"
JVM_OPS="$JVM_OPS -Djava.compiler=none -Dlogserver -DcontextName=light4j"
#JVM_OPS="$JVM_OPS -Dapijson.enabled=true -Dapijson.debug=false -Dapijson.test=false"
#JVM_OPS="$JVM_OPS -Dweixin.appid=wx78b808148023e9fa -Dweixin.appidTest=wx5bb3e90365f54b7a -Dweixin.touserTest=gh_f6216a9ae70b"
#JVM_OPS="$JVM_OPS -Dservice.controller.ips.config=service.controller.ips.config"
#ENV_OPS="PATH=/usr/java/jdk1.8.0_161/bin:$PATH"
#ENV_OPS="$ENV_OPS db.hostPort=localhost:3306 db.username=apijson db.password=apijson"
JVM_OPS="$JVM_OPS -Dlight4j.directory=/soft/softwares/library/"
#JVM_OPS="$JVM_OPS -Dredis.configDb=xlongwei:6379:1"
#JVM_OPS="$JVM_OPS -Dredis.cacheDbs=xlongwei:6379:3-7"
#JVM_OPS="$JVM_OPS -Dsoffice.hosts=xlongwei:8100-8102:true"
#JVM_OPS="$JVM_OPS -Dupload.url=http://ip/uploads/"
JVM_OPS="$JVM_OPS -Duser.timezone=GMT+8 -DclientThreads=1"
#JVM_OPS="$JVM_OPS -Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n"
#ENV_OPS="$ENV_OPS enableHttp=false httpPort=8080"
ENV_OPS="$ENV_OPS enableHttps=true httpsPort=8443"
ENV_OPS="$ENV_OPS ioThreads=2 workerThreads=3"
JVM_OPS="$JVM_OPS -Dlight-config-server-uri=https://git.xlongwei.com"
ENV_OPS="$ENV_OPS config_server_authorization=Z3Vlc3Q6MTIzNDU2"
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
    echo "  redeploy    package fat-jar $jarfile and restart"
    echo "  keystore    prepare keystore、crt、trustore"
    echo "  install     download some jars and install to local repository"
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
		COUNT=0
		while [ $COUNT -lt 1 ]; do
		    echo -e ".\c"
		    sleep 1
		    COUNT=1
		    for PID in $PIDS ; do
		        PID_EXIST=`ps -f -p $PID | grep "$jarfile"`
		        if [ -n "$PID_EXIST" ]; then
		            COUNT=0
		            break
		        fi
		    done
		done
	fi
}

clean(){
	mvn clean
}

jar(){
	mvn $profile compile jar:jar
}

jars(){
	mvn $profile dependency:copy-dependencies -DoutputDirectory=target
}

deploy(){
	mvn package -Prelease $profile -Dmaven.test.skip=true -Dmaven.javadoc.skip=true
}

start(){
	echo "starting $appname ..."
	JVM_OPS="-server -Djava.awt.headless=true $JVM_OPS"
	#env $ENV_OPS java $JVM_OPS -jar target/light4j-3.0.1.jar 2>&1
	if [ "$daemon" = "true" ]; then
		env $ENV_OPS setsid java $JVM_OPS -jar $jarfile >> /dev/null 2>&1 &
	else
		env $ENV_OPS java $JVM_OPS -jar $jarfile 2>&1
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

install(){
	[ ! -e target ] && mkdir target
	repos=http://nexus.xlongwei.com/repos/
	install_file "$repos" "de.rrze" "jpwgen" "1.2.0"
	install_file "$repos" "com.qq.weixin.mp" "aes" "1.6"
	install_file "$repos" "com.lowagie" "itext" "2.0.8.1"
	install_file "$repos" "com.lowagie" "itext-asian" "2.0.8.1"
	repos=https://jitpack.io/
	install_file "$repos" "com.github.APIJSON" "apijson-framework" "4.6.7"
	install_file "$repos" "com.github.tencent" "APIJSON" "4.6.7"
}
install_file(){
    groupId="$2" && artifactId="$3" && version="$4" && url="$1${groupId//.//}/${artifactId}/${version}/${artifactId}-${version}"
    echo "install $url.jar to $groupId:$artifactId:jar:$version"
    out="target/${artifactId}-${version}"
    if [ ! -e "$out.jar" ]; then
        echo "download jar ."
        curl -s "$url.jar" -o "$out.jar"
        echo "download pom .."
        curl -s "$url.pom" -o "$out.pom"
        echo "install-file ..."
        mvn install:install-file -DgroupId=$groupId -DartifactId=$artifactId -Dversion=$version -Dpackaging=jar -Dfile="$out.jar" -DpomFile="$out.pom"
    fi
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
	redeploy) stop && deploy && start ;;
	keystore) keystore $@;;
	install) install ;;
	*) usage ;;
	esac
fi
