PIDS=`ps -ef | grep java | grep "light4j-3.0.1.jar" |awk '{print $2}'`

if [ -z "$PIDS" ]; then
    echo "light4j is not running!"
else
	echo -e "Stopping light4j ..."
	for PID in $PIDS ; do
	    kill $PID > /dev/null 2>&1
	done
fi

echo "compile light4j ..."
mvn compile jar:jar

echo "copy dependencies ..."
mvn dependency:copy-dependencies -DoutputDirectory=target

echo "starting light4j ..."
setsid java -Dlight4j.directory=/soft/softwares/library/ -Dlogserver -jar target/light4j-3.0.1.jar >> /dev/null 2>&1 &
