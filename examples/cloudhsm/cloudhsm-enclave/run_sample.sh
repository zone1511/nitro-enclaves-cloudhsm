#! /bin/bash

# preparation
echo -e "\n* Preparing enclaves..."
ifconfig lo 127.0.0.1
ifconfig lo:0 $HSM_IP
ifconfig lo:1 $S3_IP

# run
echo -e "\n* Running samples..."

# Add a hosts record, pointing API endpoint to local loopback
echo "127.0.0.1    secretsmanager.ap-northeast-1.amazonaws.com" >> /etc/hosts
#echo "127.0.0.1    kl-enclave-upload.s3.ap-northeast-1.amazonaws.com" >> /etc/hosts
#echo "127.0.0.1    s3.ap-northeast-1.amazonaws.com" >> /etc/hosts
#echo "$S3_IP       vpce-003d1123c1095a6a5-z7qkv9kq-ap-northeast-1a.s3.ap-northeast-1.vpce.amazonaws.com" >> /etc/hosts
echo "$S3_IP       kl-enclave-upload.s3.ap-northeast-1.amazonaws.com" >> /etc/hosts

# socat tcp vsock mapping
socat TCP4-LISTEN:443,bind=127.0.0.1,fork VSOCK-CONNECT:3:443 &
socat TCP4-LISTEN:2223,bind=$HSM_IP,fork VSOCK-CONNECT:3:2223 &
socat TCP4-LISTEN:443,bind=$S3_IP,fork VSOCK-CONNECT:3:8443 &

# run
cd /app
java -ea -jar app.jar