hostPort=localhost:8080
clients=3
seconds=5
serviceUrl=http://$hostPort/service

curl $serviceUrl/weixin/chat.json?text=serviceCountOff

echo "$serviceUrl/datetime.json" > /tmp/urls.txt
http_load -parallel $clients -seconds $seconds /tmp/urls.txt

curl $serviceUrl/weixin/chat.json?text=serviceCountOn
