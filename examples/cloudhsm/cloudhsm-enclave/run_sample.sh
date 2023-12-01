#! /bin/bash

# preparation
echo -e "\n* Preparing enclaves..."
ifconfig lo 127.0.0.1
ifconfig lo:0 $HSM_IP

# run
echo -e "\n* Running samples..."

# Add a hosts record, pointing API endpoint to local loopback
echo "127.0.0.1    secretsmanager.ap-southeast-1.amazonaws.com" >> /etc/hosts

# socat tcp vsock mapping
socat TCP4-LISTEN:443,bind=127.0.0.1,fork VSOCK-CONNECT:3:443 &
socat TCP4-LISTEN:2223,bind=$HSM_IP,fork VSOCK-CONNECT:3:2223 &

# run
cd /app
java -ea -jar app.jar