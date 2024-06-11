#!/bin/bash
mvn package > /dev/null

vsock_proxy_pids=$(ps -ef | grep "sudo vsock-proxy" | awk '{print $2}')
if [ -z "$vsock_proxy_pids" ]; then
    echo "No vsock-proxy processes running with sudo found."
else
    # Loop through the PIDs and kill each process
    for pid in $vsock_proxy_pids; do
        echo "Killing vsock-proxy process with PID $pid..."
        sudo kill $pid
    done
fi

sudo vsock-proxy 443 secretsmanager.ap-northeast-1.amazonaws.com 443 &
sudo vsock-proxy 2223 10.0.1.90 2223 &
sudo vsock-proxy 8443 10.0.1.222 443 &

nitro-cli terminate-enclave --enclave-id $(nitro-cli describe-enclaves | jq -r '.[0].EnclaveID') > /dev/null

docker build --build-arg HSM_IP="10.0.1.90" --build-arg S3_IP="10.0.1.222" -t cloudhsm-enclave ./cloudhsm-enclave > /dev/null
echo "docker iamge build"
nitro-cli build-enclave --docker-uri cloudhsm-enclave:latest --output-file cloudhsm-enclave.eif > /dev/null
echo "enclave iamge build"
nitro-cli run-enclave --eif-path cloudhsm-enclave.eif --memory 10240 --cpu-count 2 --enclave-cid 5 --debug-mode > /dev/null
#view the console output of enclave
nitro-cli console --enclave-id $(nitro-cli describe-enclaves | jq -r '.[0].EnclaveID')
