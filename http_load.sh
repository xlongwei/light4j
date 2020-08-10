hostPort=localhost:8080
clients=3
seconds=5

echo "http://$hostPort/ws/ok" > /tmp/urls.txt
http_load -parallel $clients -seconds $seconds /tmp/urls.txt
